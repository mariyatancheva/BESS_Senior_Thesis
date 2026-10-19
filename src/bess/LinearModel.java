package bess;
import java.util.Objects;

public class LinearModel {
    private final double intercept;
    private final double[] coefficients;
    public LinearModel(double intercept, double[] coefficients){
        Objects.requireNonNull(coefficients,"Coefficients cannot be null");
        if(!Double.isFinite(intercept)){
            throw new IllegalArgumentException("Intercept must be a finite number");
        }
        this.coefficients=coefficients.clone();
        for(double coefficient : this.coefficients){
            if(!Double.isFinite(coefficient)){
                throw new IllegalArgumentException("Coefficients must be finite numebrs.");
            }
        }
        this.intercept=intercept;
    }
    public double predict(double[] features){
        Objects.requireNonNull(features,"Features cannot be null.");
        if(features.length != coefficients.length){
            throw new IllegalArgumentException("The number of features must match the coefficients.");
        }
        double prediction= intercept;
        for(int i=0;i<features.length;i++){
            if(!Double.isFinite(features[i])){
                throw new IllegalArgumentException("Feature values must be finite numbers.");
            }
            prediction+=coefficients[i]*features[i];
        }
        if(!Double.isFinite(prediction)){
            throw new IllegalArgumentException("The prediction is not finite.");
        }
        return prediction;

    }
    public double getIntercept(){
        return intercept;
    }
    public double[] getCoefficients(){
        return coefficients.clone();
    }

}
