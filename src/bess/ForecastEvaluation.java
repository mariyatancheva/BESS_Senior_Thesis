package bess;
import javax.sound.sampled.Line;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.ToDoubleFunction;


public class ForecastEvaluation {
    public static void main(String[] args){
        try{
            GenerationCSVReader generationReader=new GenerationCSVReader();
            List<GenerationRecord> generation=generationReader.readGeneration(Path.of("data/generation.csv"));
            WeatherCSVReader weatherReader=new WeatherCSVReader();
            List<WeatherRecord> weather=weatherReader.readWeather(Path.of("data/weather.csv"));
            ForecastDataPreparation preparation= new ForecastDataPreparation();
            List<HourlyGenerationData> hourlyGeneration=preparation.aggregateHourly(generation);
            List<ForecastDataPreparation.TrainingRow> rows=preparation.combineData(hourlyGeneration,weather);
            for(int i =1;i<rows.size();i++){
                if(!rows.get(i).startTime().equals(rows.get(i-1).startTime().plusSeconds(3600))){
                    throw new IllegalArgumentException("Training rows mudt contain consecutive UTC hours.");
                }
            }
            int trainCount=(int)(rows.size()*0.8);
            int testCount=rows.size()-trainCount;
            if(trainCount<8|| testCount==0){
                throw new IllegalArgumentException("Not enough records for training and evaluation ");
            }
            double [][] trainFeatures= new double[trainCount][3];
            double [] trainTargets= new double[trainCount];
            double[][] testFeatures=new double[testCount][3];
            double[] testTargets=new double[testCount];
            for (int i=0;i<rows.size();i++){
                ForecastDataPreparation.TrainingRow row=rows.get(i);
                double[] features={
                        row.temperature(),
                        row.cloudCoverPercentage(),
                        row.radiation()
                };
                if(i<trainCount){
                    trainFeatures[i]=features;
                    trainTargets[i]=row.energyKWh();
                } else{
                    int testIndex=i-trainCount;
                    testFeatures[testIndex]=features;
                    testTargets[testIndex]=row.energyKWh();
                }
            }
            System.out.println("Training records "+ trainCount);
            System.out.println("Test records "+testCount);
            System.out.println("Training period UTC"+rows.get(0).startTime()+" to "+ rows.get(trainCount-1).startTime().plusSeconds(3600)+ "end exclusive");
            System.out.println("Test period UTC: "+ rows.get(trainCount).startTime()+" to "+ rows.get(rows.size()-1).startTime().plusSeconds(3600)+"end exclusive");
            long linearStart=System.nanoTime();
            LinearRegression regression=new LinearRegression();
            LinearModel linearModel=regression.fit(trainFeatures,trainTargets);
            double linearSeconds=(System.nanoTime()-linearStart)/1000000000.0;
            System.out.printf("Linear regression training time: %.2f seconds%n ",linearSeconds);
            System.out.println("Training M5P...");
            long treeStart=System.nanoTime();
            M5P tree=new M5P();
            tree.fit(trainFeatures,trainTargets);
            double treeSeconds=(System.nanoTime()-treeStart)/1000000000.0;
            System.out.printf("M5P training time: %.2f seconds%n",treeSeconds);
            System.out.println("M5P leaves: "+ tree.getLeafCount());
            evaluate("Linear regression",testFeatures,testTargets,linearModel::predict);
            evaluate("M5P without smoothing",testFeatures,testTargets,features->tree.predict(features,false));
            evaluate("M5P with smoothing",testFeatures,testTargets,tree::predict);

        }catch (IOException | IllegalArgumentException | IllegalStateException exception){
            System.out.println("Forecast evaluation failed: "+ exception.getMessage());
        }
    }
    private static void evaluate(String name,double[][] features,double[] targets, ToDoubleFunction<double[]> predictor){
        double mae=0;
        double rmse=0;
        int negativePredictions=0;
        double scale=Math.sqrt(targets.length);
        for(int i=0;i<targets.length;i++){
            double prediction=predictor.applyAsDouble(features[i]);
            if(!Double.isFinite(prediction)){
                throw new IllegalStateException("Non finite prediction from"+name);

            }
            if(prediction<0){
                negativePredictions++;
            }
            double error=prediction-targets[i];
            if(!Double.isFinite(error)){
                throw new IllegalStateException("Non finite prediction error from"+name);
            }
            mae+=Math.abs(error)/targets.length;
            rmse=Math.abs(error)/targets.length;
        }
        if(!Double.isFinite(mae)||!Double.isFinite(rmse)){
            throw new IllegalStateException("Non finite evaluation result from: "+name);
        }
        System.out.println();
        System.out.println(name);
        System.out.printf("MAE: %.2f kWh%n", mae);
        System.out.printf("RMSE: %.2f kWh%n", rmse);
        System.out.println("Negative predictions: "+negativePredictions+"/"+targets.length);
    }
}
