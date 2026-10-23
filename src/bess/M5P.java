package bess;
import javax.sound.sampled.Line;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;

public class M5P {
    private final int minSamplesLeaf;
    private final int maxDepth;
    private final double smoothingConstant;
    private static final double minRelativeGain=1e-6;
    private final LinearRegression regression=new LinearRegression();
    private Node root;
    private int featureCount;
    public M5P(){
        this(4,20,15.0);
    }
    public M5P(int minSamplesLeaf,int maxDepth,double smoothingConstant){
        if(minSamplesLeaf<1){
            throw new IllegalArgumentException("Minimum leaf sizee must bee positive");

        }
        if(maxDepth<0){
            throw new IllegalArgumentException("Maximum depth cannot be negative");
        }
        if(!Double.isFinite(smoothingConstant)|| smoothingConstant<0){
            throw new IllegalArgumentException("Smoothing constant ust be finite and non negative");

        }
        this.minSamplesLeaf=minSamplesLeaf;
        this.maxDepth=maxDepth;
        this.smoothingConstant=smoothingConstant;
    }
    public record Split(int featureIndex, double threshold,double gain ){

    }
    public static class Node{
        private final LinearModel model;
        private final int sampleCount;
        private int featureIndex=-1;
        private double threshold;
        private Node left;
        private Node right;
        private Node(LinearModel model, int sampleCount){
            this.model=Objects.requireNonNull(model,"Node model cannot  be null.");
            if(sampleCount<1){
                throw new IllegalArgumentException("A node must contain at least one training record.");
            }
            this.sampleCount=sampleCount;
        }
        private boolean isLeaf(){
            return left==null &&right==null;
        }
    }
    public void fit(double[][] feautures, double[] targets){
        Objects.requireNonNull(feautures,"Features cannot be null.");
        Objects.requireNonNull(targets,"Targets cannot be null.");
        if(feautures.length==0){
            throw new IllegalArgumentException("Training data cannot be empty");
        }
        Node trainedRoot= buildTree(feautures, targets,0);
        root=trainedRoot;
        featureCount=feautures[0].length;
    }
    private Node buildTree(double[][] features, double[] targets, int depth){
        LinearModel model=regression.fit(features,targets);
        model=simplifyLinearModel(model,features, targets);
        Node node=new Node(model,features.length);
        if(depth>=maxDepth || features.length/2<minSamplesLeaf){
            return node;
        }
        Split split= findBestSplit(features, targets, minSamplesLeaf);
        if(split==null){
            return node;
        }
        double currentSD= standardDev(targets);
        if(split.gain()<=minRelativeGain*currentSD){
            return node;
        }
        int feature =split.featureIndex();
        double threshold=split.threshold();
        int leftCount=0;
        for(int i=0; i<features.length;i++){
            if(features[i][feature]<=threshold){
                leftCount++;
            }
        }
        int rightCount=features.length-leftCount;
        if(leftCount<minSamplesLeaf || rightCount<minSamplesLeaf){
            throw new IllegalArgumentException("The selected split creates an invalid group size");
        }
        double[][] leftFeatures=new double[leftCount][];
        double[][] rightFeatures=new double[rightCount][];
        double[] leftTargets= new double[leftCount];
        double[] rightTargets= new double[rightCount];
        int leftIndex=0;
        int rightIndex=0;
        for(int i=0;i<features.length;i++){
            if(features[i][feature]<=threshold){
                leftFeatures[leftIndex]=features[i];
                leftTargets[leftIndex]=targets[i];
                leftIndex++;
            } else {
                rightFeatures[rightIndex]=features[i];
                rightTargets[rightIndex]=targets[i];
                rightIndex++;
            }
        }
        node.featureIndex=feature;
        node.threshold=threshold;
        node.left=buildTree(leftFeatures,leftTargets,depth+1);
        node.right=buildTree(rightFeatures,rightTargets,depth+1);
        pruneNode(node,features,targets);
        return node;
    }
    private double calculateRMSE(Node node, double[][] features, double[] targets,boolean useSubtree){
        double errorLength=0;
        double scale=Math.sqrt(targets.length);
        for(int i=0;i<targets.length;i++){
            double prediction;
            if(useSubtree){
                prediction=predictFromNode(node,features[i]);
            } else {
                prediction=node.model.predict(features[i]);
            }
            double error= prediction-targets[i];
            if (!Double.isFinite(error)){
                throw new IllegalStateException("Prediction error is not finite");
            }
            errorLength=Math.hypot(errorLength,error/scale);
        }
        return errorLength;
    }
     LinearModel simplifyLinearModel(LinearModel initialModel, double[][] features, double[] targets){
        int featureCount=features[0].length;
        boolean[] active=new boolean[featureCount];
        Arrays.fill(active,true);
        LinearModel currentModel= initialModel;
        double currentScore=adjustedModelError(currentModel,features,targets);
        double tolerance=1e-10*Math.max(1.0,standardDev(targets));
        while(true){
            int  featureToRemove=-1;
            LinearModel bestModel=currentModel;
            double bestScore=currentScore;
            for(int j=0; j<featureCount;j++){
                if(!active[j]){
                    continue;
                }
                boolean[] candidateActive=active.clone();
                candidateActive[j]=false;
                LinearModel candidateModel= fitSelectedModel(features,targets,candidateActive);
                double candidateScore=adjustedModelError(candidateModel, features, targets);
                boolean acceptable=candidateScore<=currentScore+tolerance;
                if(acceptable &&(featureToRemove==-1|| candidateScore<bestScore)){
                    featureToRemove=j;
                    bestModel=candidateModel;
                    bestScore=candidateScore;
                }
            }
            if (featureToRemove == -1){
                return currentModel;
            }
            active[featureToRemove]=false;
            currentModel=bestModel;
            currentScore=bestScore;
        }
    }
    private LinearModel fitSelectedModel(double[][] features, double[] targets, boolean[] active){
        int selectedCount=0;
        for(boolean selected:active){
            if(selected){
                selectedCount++;
            }
        }
        double [][] reducedFeatures=new double[features.length][selectedCount];
        for(int i=0;i<features.length;i++){
            int columnIndex=0;
            for(int j=0;j< active.length;j++){
                if(active[j]){
                    reducedFeatures[i][columnIndex]=features[i][j];
                    columnIndex++;
                }

            }
        }
        LinearModel reducedModel=regression.fit(reducedFeatures,targets);
        double[] reducedCoefficients=reducedModel.getCoefficients();
        double[] fullCoefficients=new double[active.length];
        int coefficientIndex=0;
        for(int j=0; j<active.length;j++){
            if(active[j]){
                fullCoefficients[j]=reducedCoefficients[coefficientIndex];
                coefficientIndex++;
            }
        }
        return new LinearModel(reducedModel.getIntercept(),fullCoefficients);
    }
    private double adjustedModelError(LinearModel model, double[][] features, double[] targets){
        Node temporaryNode=new Node(model, targets.length);
        double error=calculateRMSE(temporaryNode,features,targets,false);
        int parameters=countModelParameters(model);
        double score=error*pruningFactor(targets.length,parameters);
        if(!Double.isFinite(score)){
            throw new IllegalStateException("Adjusted model error is not finite.");
        }
        return score;
    }
    private void pruneNode(Node node, double[][] features, double[] targets){
        if(node.isLeaf()){
            return;
        }
        double modelError= calculateRMSE(node, features, targets, false);
        double subtreeError= calculateRMSE(node, features, targets, true);
        int modelParameters = countModelParameters(node.model);
        int subtreeParameters= countSubtreeParameters(node);
        double adjustedModelError=modelError*pruningFactor(node.sampleCount, modelParameters);
        double adjustedSubtreeError=subtreeError*pruningFactor(node.sampleCount, subtreeParameters);
        if(!Double.isFinite(adjustedModelError)|| !Double.isFinite(adjustedSubtreeError)) {
            throw new IllegalStateException("Pruning error is not finite.");
        }
        double tolerance=1e-10*Math.max(1.0,standardDev(targets));
        if(adjustedModelError<=adjustedSubtreeError+tolerance){
            node.left=null;
            node.right=null;
            node.featureIndex=-1;
            node.threshold=0;

        }
    }
    private double predictFromNode(Node node, double[] features){
        Node current=node;
        while(!current.isLeaf()){
            if(features[current.featureIndex]<= current.threshold){
                current=current.left;
            } else{
                current=current.right;
            }
        }
        return current.model.predict(features);
    }
    private int countModelParameters(LinearModel model){
        int count =1;
        for(double coefficient:model.getCoefficients()){
            if(coefficient !=0.0){
                count++;
            }
        }
        return count;
    }
    private int countSubtreeParameters(Node node){
        if(node.isLeaf()){
            return countModelParameters(node.model);
        }
            return countSubtreeParameters(node.left)+countSubtreeParameters(node.right)+1;
    }
    private double pruningFactor(int sampleCount, int parameterCount){
        if(sampleCount<=parameterCount){
            return 10.0;
        }
        return (sampleCount+ 2.0*parameterCount)/(sampleCount-parameterCount);
    }
    public int getLeafCount(){
        if(root==null){
            throw new IllegalStateException("The model must be trained first. ");
        }
        return countLeaves(root);
    }
    private int countLeaves(Node node){
        if(node.isLeaf()){
            return 1;
        }
        return countLeaves(node.left)+countLeaves(node.right);
    }
    public double predict(double[] features){
        return predict(features,true);
    }
    public double predict(double[] features, boolean useSmoothing){
        if(root==null){
            throw new IllegalStateException("The model must be trained before prediction.");
        }
        Objects.requireNonNull(features,"Features cannot be null");
        if(features.length !=featureCount){
            throw new IllegalArgumentException("Unexpected number of features.");
        }
        for(double value: features){
            if(!Double.isFinite(value)){
                throw new IllegalArgumentException("Features must be finite");
            }
        }
       if(useSmoothing){
           return predictSmoothed(root,features);
       }
       return predictFromNode(root, features);
    }
    public Split findBestSplit(double[][] features, double[] targets, int minSamplesLeaf) {
        Objects.requireNonNull(features, "Features cannot be null");
        Objects.requireNonNull(targets, "Targets cannot be null");
        int n = features.length;
        if (n == 0 || targets.length != n || minSamplesLeaf < 1) {
            throw new IllegalArgumentException("Invalid data size or minimum group size");
        }
        Objects.requireNonNull(features[0], "Feature row cannot be null.");
        int p = features[0].length;
        for (int i = 0; i < n; i++) {
            Objects.requireNonNull(features[i], "Feature row cannot be null.");
            if (features[i].length != p || !Double.isFinite(targets[i])) {
                throw new IllegalArgumentException("Invalid row or target");
            }
            for (double value : features[i]) {
                if (!Double.isFinite(value)) {
                    throw new IllegalArgumentException("Features must be finite");
                }
            }
        }
        if(minSamplesLeaf>n/2){
            return null;
        }
        boolean allEqual=true;
        for(int i=1; i<n; i++){
            if (targets[i] !=targets[0]){
                allEqual=false;
                break;
            }
        }
        if(allEqual){
            return null;
        }
        double parentSD= standardDev(targets);
        if(!Double.isFinite(parentSD)){
            throw new IllegalArgumentException("Target values are too large");
        }
        if(parentSD==0){
            return null;
        }
        Split best=null;
        for(int j=0;j<p;j++){
            final int featureIndex=j;
            Integer[] order=new Integer[n];
            for(int i=0;i<n;i++){
                order[i]=i;
            }
            Arrays.sort(order, Comparator.comparingDouble(index -> features[index][featureIndex]));
            double[] sortedTargets=new double[n];
            for(int i=0; i<n;i++){
                sortedTargets[i]=targets[order[i]];
            }
            double[] leftSD=runningSD(sortedTargets,false);
            double[] rightSD=runningSD(sortedTargets,true);
            for(int cut=minSamplesLeaf; cut<=n-minSamplesLeaf;cut++){
                double lower= features[order[cut-1]][j];
                double upper= features[order[cut]][j];
                if(lower>=upper){
                    continue;
                }
                double gain=parentSD-((double) cut/n)*leftSD[cut-1]- ((double)(n-cut)/n)*rightSD[cut];
                if (gain>0 &&(best==null||gain>best.gain())) {
                    double threshold = lower / 2 + upper / 2;

                    if (threshold < lower || threshold >= upper) {
                        threshold = lower;
                    }
                    best = new Split(j, threshold, gain);
                }
            }
        }
        return best;
    }
    private static double[] runningSD(double[] values, boolean backwards){
        double[] result =new double[values.length];
        double mean=0;
        double squaredDiff=0;
        for(int step=0;step<values.length;step++){
            int i = backwards? values.length -1-step:step;
            int count=step+1;
            double diff=values[i]-mean;
            mean+=diff/count;
            squaredDiff+=diff*(values[i]-mean);
            result[i]=Math.sqrt(Math.max(0,squaredDiff/count));
            if(!Double.isFinite(result[i])){
                throw new IllegalArgumentException("Target values are too high.");
            }
        }
        return result;
    }
    private static double standardDev(double[] values){
        Objects.requireNonNull(values,"Values cannot be null");
        if(values.length==0){
            throw new IllegalArgumentException("Values cannot be empty.");
        }
        double mean=0;
        for(double value: values){
            if(!Double.isFinite(value)){
                throw new IllegalArgumentException("Values must be finite");
            }
            mean+=value/values.length;
        }
        double sumSquaredDiff=0;
        for(double value:values){
            double diff=value-mean;
            sumSquaredDiff+=diff*diff;
        }
        double variance=sumSquaredDiff/values.length;
        return Math.sqrt(variance);
    }
    private double predictSmoothed(Node node, double[] features){
        if(node.isLeaf()){
            return node.model.predict(features);
        }
        Node child;
        if(features[node.featureIndex]<=node.threshold){
            child=node.left;
        }else{
            child=node.right;
        }
        double childPrediction=predictSmoothed(child,features);
        double parentPrediction=node.model.predict(features);
        double childWeight=child.sampleCount/(child.sampleCount+smoothingConstant);
        double parentWeight=smoothingConstant/(child.sampleCount+smoothingConstant);
        double prediction=childWeight*childPrediction+parentWeight*parentPrediction;
        if(!Double.isFinite(prediction)){
            throw new IllegalStateException("Smoothed prediction is not finite");
        }
        return prediction;
    }
}
