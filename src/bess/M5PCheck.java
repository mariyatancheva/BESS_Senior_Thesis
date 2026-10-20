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
    }
}
