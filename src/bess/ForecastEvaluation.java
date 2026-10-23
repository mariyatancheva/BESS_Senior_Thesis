package bess;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.function.ToDoubleFunction;
import java.util.ArrayList;
import java.util.Comparator;
public class ForecastEvaluation {
    private record Metrics(double mae, double rmse, int negativePredictions){
    }
    private record ErrorRow(ForecastDataPreparation.TrainingRow row,double linearPrediction,double treePrediction){
        double linearError(){
            return Math.abs(linearPrediction-row.energyKWh());
        }
        double treeError(){
            return Math.abs(treePrediction-row.energyKWh());
        }
        double extraError(){
            return treeError()-linearError();
        }
    }
    public static void main(String[] args){
        try{
            GenerationCSVReader generationReader=new GenerationCSVReader();
            List<GenerationRecord> generation=generationReader.readGeneration(Path.of("data/generation.csv"));
            WeatherCSVReader weatherReader=new WeatherCSVReader();
            List<WeatherRecord> weather=weatherReader.readWeather(Path.of("data/weather.csv"));
            ForecastDataPreparation preparation=new ForecastDataPreparation();
            List<HourlyGenerationData> hourlyGeneration=preparation.aggregateHourly(generation);
            List<ForecastDataPreparation.TrainingRow> rows=preparation.combineData(hourlyGeneration,weather);
            for(int i=1;i<rows.size();i++){
                if(!rows.get(i).startTime().equals(rows.get(i-1).startTime().plusSeconds(3600))){
                    throw new IllegalArgumentException("Records must contain consecutive UTC hours.");
                }
            }
            int totalCount= rows.size();
            int trainingEnd=(int)(totalCount*0.6);
            int validationEnd=(int)(totalCount*0.8);
            if(trainingEnd<8||validationEnd<=trainingEnd||validationEnd>=totalCount){
                throw new IllegalArgumentException("Not enough records for the three periods.");
            }
            double[][] allFeatures=new double[totalCount][3];
            double[] allTargets=new double[totalCount];
            for(int i=0;i<totalCount;i++){
                ForecastDataPreparation.TrainingRow row= rows.get(i);
                allFeatures[i][0]=row.temperature();
                allFeatures[i][1]=row.cloudCoverPercentage();
                allFeatures[i][2]=row.radiation();
                allTargets[i]=row.energyKWh();
            }
            double[][] trainingFeatures=Arrays.copyOfRange(allFeatures,0,trainingEnd);
            double[] trainingTargets=Arrays.copyOfRange(allTargets,0,trainingEnd);
            double[][] validationFeatures=Arrays.copyOfRange(allFeatures,trainingEnd,validationEnd);
            double[] validationTargets=Arrays.copyOfRange(allTargets,trainingEnd,validationEnd);
            System.out.println("Training records "+trainingTargets.length);
            System.out.println("Validation records"+validationTargets.length);
            System.out.println("Later evaluation records: "+(totalCount-validationEnd));
            printPeriod("Training", rows,0,trainingEnd);
            printPeriod("Validation", rows,trainingEnd,validationEnd);
            printPeriod("Later Evaluation ",rows,validationEnd,totalCount);
            LinearRegression regression = new LinearRegression();
            LinearModel validationLinearModel = regression.fit(
                    trainingFeatures, trainingTargets
            );
            printMetrics("Linear regression — validation", measure(
                            validationFeatures,
                            validationTargets,
                            validationLinearModel::predict
                    ), validationTargets.length
            );
            int[] leafOptions = {4,24,72};
            int[] depthOptions = {6,12,20};
            double[] smoothingOptions = {0.0,15.0,50.0,100.0};
            int bestLeaf = 0;
            int bestDepth = 0;
            double bestSmoothing = 0;
            Metrics bestValidationMetrics = null;
            System.out.println();
            System.out.println("Searching M5P settings...");
            long searchStart = System.nanoTime();
            for (int leafSize :leafOptions) {
                for (int depth : depthOptions) {
                    for (double smoothing : smoothingOptions) {
                        M5P candidate = new M5P(leafSize, depth, smoothing);
                        candidate.fit(trainingFeatures, trainingTargets);
                        Metrics metrics = measure(validationFeatures, validationTargets, candidate::predict);
                        System.out.printf("Leaf=%d, depth=%d, smoothing=%.1f" + " | MAE=%.2f, RMSE=%.2f" + " | leaves=%d%n",
                                leafSize, depth,smoothing,metrics.mae(), metrics.rmse(), candidate.getLeafCount());
                        if (bestValidationMetrics == null || metrics.rmse() < bestValidationMetrics.rmse()) {
                            bestValidationMetrics = metrics;
                            bestLeaf = leafSize;
                            bestDepth = depth;
                            bestSmoothing = smoothing;
                        }
                    }
                }
            }

            double searchSeconds = (System.nanoTime() - searchStart)/1000000000.0;
            System.out.printf("%nSearch time: %.2f seconds%n",searchSeconds);
            System.out.printf("Selected settings: leaf=%d, depth=%d,"+" smoothing=%.1f%n",bestLeaf,bestDepth,bestSmoothing);
            printMetrics("Selected M5P — validation",bestValidationMetrics, validationTargets.length);
            M5P validationTree=new M5P(bestLeaf,bestDepth,bestSmoothing);
            validationTree.fit(trainingFeatures,trainingTargets);
            diagnose (rows.subList(trainingEnd, validationEnd), validationFeatures, validationTargets, validationLinearModel::predict,validationTree::predict);
            double[][] finalTrainingFeatures = Arrays.copyOfRange(allFeatures,0, validationEnd);
            double[] finalTrainingTargets = Arrays.copyOfRange(allTargets,0,validationEnd);
            double[][] testFeatures = Arrays.copyOfRange(allFeatures, validationEnd,totalCount);
            double[] testTargets = Arrays.copyOfRange(allTargets,validationEnd,totalCount);
            LinearModel finalLinearModel = regression.fit(finalTrainingFeatures,finalTrainingTargets);

            M5P defaultTree = new M5P();
            defaultTree.fit(finalTrainingFeatures, finalTrainingTargets);
            M5P selectedTree = new M5P(bestLeaf, bestDepth, bestSmoothing);
            selectedTree.fit(finalTrainingFeatures, finalTrainingTargets);
            System.out.println();
            System.out.println("Results on the lasr 20%");
            printMetrics("Linear regression", measure(testFeatures, testTargets, finalLinearModel::predict), testTargets.length);
            printMetrics("Default M5P", measure(testFeatures,testTargets, defaultTree::predict), testTargets.length);
            printMetrics("Selected M5P",measure(testFeatures,testTargets, selectedTree::predict), testTargets.length);
            System.out.println("Default M5P leaves: " + defaultTree.getLeafCount());
            System.out.println("Selected M5P leaves: " + selectedTree.getLeafCount());

        }catch (IOException| IllegalArgumentException | IllegalStateException exception) {
            System.out.println("Forecast evaluation failed: " + exception.getMessage());
        }
    }
    private static Metrics measure(double[][] features, double[] targets, ToDoubleFunction<double[]> predictor
    ) {
        if (targets.length == 0 || features.length != targets.length) {
            throw new IllegalArgumentException(
                    "Evaluation data must be matching and non-empty.");
        }
        double mae = 0;
        double rmse = 0;
        int negativePredictions = 0;
        double scale = Math.sqrt(targets.length);
        for (int i = 0; i < targets.length; i++) {
            double prediction = predictor.applyAsDouble(features[i]);
            if (!Double.isFinite(prediction)) {
                throw new IllegalStateException(
                        "Prediction is not finite.");
            }

            if(prediction < 0) {
                negativePredictions++;
            }
            double error = prediction - targets[i];
            if(!Double.isFinite(error)) {
                throw new IllegalStateException(
                        "Prediction error is not finite.");
            }
            mae += Math.abs(error)/targets.length;
            rmse = Math.hypot(rmse, error/scale);
        }

        if (!Double.isFinite(mae) || !Double.isFinite(rmse)) {
            throw new IllegalStateException(
                    "Evaluation metrics are not finite.");
        }
        return new Metrics(mae, rmse, negativePredictions);
    }

    private static void printMetrics(String name, Metrics metrics, int recordCount
    ) {
        System.out.println();
        System.out.println(name);
        System.out.printf("MAE: %.2f kWh%n", metrics.mae());
        System.out.printf("RMSE: %.2f kWh%n", metrics.rmse());
        System.out.println("Negative predictions: " + metrics.negativePredictions() + "/" + recordCount);
    }
    private static void printPeriod(String name, List<ForecastDataPreparation.TrainingRow> rows, int start, int end
    ) {System.out.println(name + " period UTC: " + rows.get(start).startTime() + " to " + rows.get(end - 1).startTime().plusSeconds(3600) + " (end exclusive)");
    }
    private static void diagnose(List<ForecastDataPreparation.TrainingRow> rows,double[][] features, double[]targets, ToDoubleFunction<double[]> linearPredictor,ToDoubleFunction<double[]> treePredictor){
        List<ErrorRow> errors=new ArrayList<>();
        for(int i=0;i< rows.size();i++){
            double linearPrediction=linearPredictor.applyAsDouble(features[i]);
            double treePrediction=treePredictor.applyAsDouble(features[i]);
            if(!Double.isFinite(linearPrediction)|| !Double.isFinite(treePrediction)){
                throw new IllegalStateException("Non-finite prediction during diagnisis");
            }
            errors.add(new ErrorRow(rows.get(i),linearPrediction,treePrediction));
        }
        System.out.println();
        System.out.println("Validation diagnostics");
        diagnoseRadiationGroup(rows, features, targets,linearPredictor, treePredictor, true);
        diagnoseRadiationGroup(rows, features, targets,linearPredictor, treePredictor, false);
        errors.sort(Comparator.comparingDouble(ErrorRow::treeError).reversed());
        printErrorRows("10 largest M5P absolute errors",errors);
        List<ErrorRow> worseCases = new ArrayList<>();
        for (ErrorRow error : errors) {
            if (error.extraError() > 0) {
                worseCases.add(error);
            }
        }
        worseCases.sort(Comparator.comparingDouble(ErrorRow::extraError).reversed());
        printErrorRows("10 largest M5P disadvantages against linear regression",worseCases);
    }
    private static void diagnoseRadiationGroup(List<ForecastDataPreparation.TrainingRow> rows, double[][] features, double[] targets, ToDoubleFunction<double[]> linearPredictor, ToDoubleFunction<double[]> treePredictor, boolean zeroRadiation)
    {int count = 0;
        for (ForecastDataPreparation.TrainingRow row : rows) {
            boolean belongs = zeroRadiation ? row.radiation() == 0.0 : row.radiation() > 0.0;
            if (belongs) {
                count++;
            }
        }
        String groupName = zeroRadiation ? "Zero radiation" : "Positive radiation";
        if (count == 0) {
            System.out.println(groupName + ": no records.");
            return;
        }
        double[][] groupFeatures = new double[count][];
        double[] groupTargets = new double[count];
        int position = 0;
        for (int i = 0; i < rows.size(); i++) {
            double radiation = rows.get(i).radiation();
            boolean belongs = zeroRadiation ? radiation == 0.0 : radiation > 0.0;
            if (belongs) {
                groupFeatures[position] = features[i];
                groupTargets[position] = targets[i];
                position++;
            }
        }
        System.out.println();
        System.out.println(groupName + " — records: " + count);
        printMetrics("Linear regression", measure(groupFeatures, groupTargets, linearPredictor), count);
        printMetrics("Selected M5P", measure(groupFeatures, groupTargets, treePredictor), count);
    }


    private static void printErrorRows(String title, List<ErrorRow> errors)
    {
        System.out.println();
        System.out.println(title);
        if (errors.isEmpty()) {
            System.out.println("No matching records.");
            return;
        }
        System.out.println("Hour start UTC | Radiation W/m2 | Actual kWh" + " | Linear kWh | M5P kWh" + " | Linear abs. error | M5P abs. error" + " | Extra M5P error");
        int limit = Math.min(10, errors.size());
        for (int i = 0; i < limit; i++) {
            ErrorRow error = errors.get(i);
            System.out.printf(
                    "%s | %.2f | %.2f | %.2f | %.2f"
                            + " | %.2f | %.2f | %.2f%n", error.row().startTime(),error.row().radiation(),error.row().energyKWh(),error.linearPrediction(),error.treePrediction(),error.linearError(),error.treeError(),error.extraError());
        }
    }
        }

