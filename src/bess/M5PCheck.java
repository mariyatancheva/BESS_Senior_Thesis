package bess;

public class M5PCheck {
    public static void main (String[] args){
        double[][] features={
                {1},
                {2},
                {3},
                {4},
                {5},
                {6}
        };
        double[] targets={10,10,10,100,100,100};
        M5P algorithm= new M5P();
        M5P.Split split=algorithm.findBestSplit(features,targets,2);
        if(split==null || Math.abs(split.threshold()-3.5)>1e-8||Math.abs(split.gain()-45)>1e-8){
            throw new AssertionError("Unexpected split"+split);
        }
        System.out.println("Best split check passed.");
        System.out.println("Feature index: "+split.featureIndex());
        System.out.println("Threshold : "+split.threshold());
        System.out.println("Gain : "+split.gain());

        double [][] trainingFeatures={
                {1},
                {2},
                {3},
                {4},
                {5},
                {6},
                {7},
                {8}
        };
        double[] trainingTargets={
                10,10,10,10,
                100,100,100,100
        };
        M5P tree=new M5P();
        tree.fit(trainingFeatures,trainingTargets);
        checkClose(tree.predict(new double[]{2.5}),10,"Left leaf prediction");
        checkClose(tree.predict(new double[]{6.5}),100,"Right leaf prediction");
        System.out.println("Tree prediction check passed.");
        checkClose(tree.predict(new double[]{4.5}),10,"Prediction at the split threshold");
        checkClose(tree.predict(new double[]{4.6}),100,"Prediction above the split threshold");
        System.out.println("Split boundary check passed ");
        M5P constantTree=new M5P();
        constantTree.fit(trainingFeatures, new double[]{50,50,50,50,50,50,50,50});
        checkClose(constantTree.predict(new double[]{3.5}),50,"Constant target prediction ");
        System.out.println("Constant target check passed");
        M5P untrainedTree=new M5P();
        try{
            untrainedTree.predict(new double[]{2.5});
            throw new AssertionError("Prediction before training should be rejected");
        } catch (IllegalStateException expected){
            System.out.println("Prediction before training check passed");
        }
        System.out.println("All basic tree checks passed. ");
        double[][] pruningFeatures={
                {1}, {2}, {3}, {4},
                {5}, {6}, {7}, {8}
        };
        M5P linearTree= new M5P();
        linearTree.fit(pruningFeatures, new double[]{7,9,11,13,15,17,19,21});
        if(linearTree.getLeafCount() !=1){
            throw new AssertionError("A single linear model should be sufficient. ");
        }
        checkClose(linearTree.predict(new double[]{4.5}),14,"Prediction after pruning");
        System.out.println("Unecessary split pruning check passed.");
        M5P stepTree=new M5P();
        stepTree.fit(pruningFeatures, new double[]{10,10,10,10,100,100,100,100});
        if(stepTree.getLeafCount() !=2){
            throw new AssertionError("The useful split should be retained");
        }
        checkClose(stepTree.predict(new double[]{2.5}),10,"Left prediction after pruning");
        checkClose(stepTree.predict(new double[]{6.5}),100,"Right prediction after pruning");
        System.out.println("Useful split retention check passed");

    }
    public static void checkClose(double actual, double expected, String description){
        if(!Double.isFinite(actual)|| Math.abs(actual-expected)>1e-8){
            throw new AssertionError(description+": expected"+expected+", received "+ actual);
        }
    }
}
