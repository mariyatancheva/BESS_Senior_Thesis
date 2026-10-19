package bess;

public class LinearRegressionCheck {
    public static void main(String[] args) {
        LinearRegression regrression=new LinearRegression();
        double[][] features={
                {0,0,0},
                {1,0,0},
                {0,1,0},
                {0,0,1},
                {2,3,4},
                {3,1,2},
        };
        double[] targets=new double[features.length];
        for(int i=0;i< features.length;i++){
            targets[i]=5+2*features[i][0]+3*features[i][1]+4*features[i][2];
        }
        LinearModel model=regrression.fit(features,targets);
        double[] coefficients=model.getCoefficients();
        checkClose(model.getIntercept(),5,"Intercept");
        checkClose(coefficients[0],2,"First coefficient");
        checkClose(coefficients[1],3,"Second coefficient");
        checkClose(coefficients[2],4,"Third coefficient");
        System.out.println("Known coefficients check passed");
        double prediction = model.predict(new double[]{4,2,3});
        checkClose(prediction,31,"Prediction");
        System.out.println("Prediction check passed");
        LinearModel noisyModel=regrression.fit(new double[][]{{0},{1},{2}}, new double[]{1,2,2});
        checkClose(noisyModel.getIntercept(),7.0/6.0, "Noisy data intercept");
        checkClose(noisyModel.getCoefficients()[0],0.5, "Noisy data coefficient");
        System.out.println("Least-squares check passed.");
        LinearModel constantModel= regrression.fit(new double[][]{
                {20,50,0},
                {20,50,0},
                {20,50,0}
        }, new double[]{100,200,300} );
        checkClose(constantModel.predict(new double[]{20,50,0}),200,"Constant features prediction");
        System.out.println("Constant features check passed");
        System.out.println("All linear regression checks passed");
    }
    private static void checkClose(double actual, double expected, String description){
        if(!Double.isFinite(actual)|| Math.abs(actual-expected)>1e-8){
            throw new AssertionError(description+ ": expected"+ expected+", received" + actual);
        }
    }
}
