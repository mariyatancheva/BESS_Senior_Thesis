package bess;
import java.util.Objects;

public class M5P {
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
}
