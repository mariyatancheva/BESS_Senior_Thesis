package bess;
import java.util.Objects;

public class LinearRegression {
    private static final double tolerance=1e-10;
    public LinearModel fit(double[][] features, double [] targets){
        Objects.requireNonNull(features,"Features cannot be null");
        Objects.requireNonNull(targets,"Targets cannot be null");
        int n= features.length;
        if(n==0 || targets.length !=n){
            throw new IllegalArgumentException("Matching and non-empty data must be provided");
        }
        Objects.requireNonNull(features[0],"Feature row cannot be null.");
        int p=features[0].length;
        for(int i=0;i<n;i++){
            Objects.requireNonNull(features[i],"Feature row cannot be null.");
            if(features[i].length !=p || !Double.isFinite(targets[i])){
                throw new IllegalArgumentException("Invalid row length or target");
            }
            for(double value :features[i]){
                if(!Double.isFinite(value)){
                    throw new IllegalArgumentException("Features must be finite");
                }
            }
        }
        double meanY=0;
        double[] means=new double[p];
        for(int i=0;i<n;i++) {
            meanY += targets[i] / n;
            for (int j = 0; j < p; j++) {
                means[j] += features[i][j] / n;
            }
        }
            double [] y=new double[n];
            for(int i=0; i<n;i++){
                y[i]=targets[i]-meanY;
            }
                double [] scales=new double[p];
                double [][] q=new double[p][n];
                double [][] r=new double[p][p];
                int[] selected=new int[p];
                int rank=0;
                for(int j=0; j<p;j++){
                double[] column= new double[n];
                for(int i=0;i<n;i++){
                    column[i]=features[i][j]-means[j];
                    scales[j]=Math.hypot(scales[j],column[i]);
                }
                if(scales[j]==0){
                    continue;
                }
                for(int i=0;i<n;i++){
                    column[i]/=scales[j];
                }
                    for(int pass=0;pass<2;pass++) {
                        for (int k = 0; k < rank; k++) {
                            double projection = dot(q[k], column);
                            r[k][rank] += projection;
                            for (int i = 0; i < n; i++) {
                                column[i] -= projection * q[k][i];
                            }
                        }
                    }
                        double norm =0;
                        for(double value:column){
                            norm=Math.hypot(norm,value);
                        }
                            if(norm<=tolerance){
                                for(int k=0;k<rank;k++) {
                                    r[k][rank] = 0;
                                }
                                    continue;
                                }
                                r[rank][rank]=norm;
                                for(int i=0;i<n;i++) {
                                    q[rank][i] = column[i] / norm;
                                }
                                    selected [rank]=j;
                                rank++;
                }

                                double[] weights= new double[rank];
                                for(int k=rank-1;k>=0;k--){
                                    double value=dot(q[k],y);
                                    for(int j=k+1;j<rank;j++) {
                                        value -= r[k][j] * weights[j];
                                    }
                                        weights[k] = value / r[k][k];
                                    }
                                    double[] coefficients=new double[p];
                                    double intercept=meanY;
                                    for(int k=0;k<rank;k++){
                                        int j=selected[k];
                                        coefficients[j]=weights[k]/scales[j];
                                        intercept-=coefficients[j]*means[j];
                                    }
                                    return new LinearModel(intercept,coefficients);
                                }
                                private static double dot(double[] a, double [] b){
                                    double result=0;
                                    for(int i=0; i<a.length;i++) {
                                        result += a[i] * b[i];
                                    }
                                        return result;

                                }
                            }

