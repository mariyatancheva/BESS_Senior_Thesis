package bess;
import java.time.LocalTime;
import java.util.Objects;

public class TradingStrategy {
    private final int[] boundaries;
    private final double[] buyThresholds;
    private final double[] sellThresholds;
    public TradingStrategy(int[] boundaries, double[] buyThresholds, double[] sellThresholds){
        Objects.requireNonNull(boundaries,"Period boundaries cannot be null.");
        Objects.requireNonNull(buyThresholds,"Buy thresholds cannot be null.");
        Objects.requireNonNull(sellThresholds,"Sell thresholds cannot be null.");
        if(boundaries.length!=3){
            throw new IllegalArgumentException("Exactly three period boundaries are required ");
        }
        if(buyThresholds.length!=4 || sellThresholds.length!=4){
            throw new IllegalArgumentException("Each of the four periods must have two price thresholds");
        }
        int previousBoundary=0;
        for(int boundary:boundaries){
            if(boundary<=previousBoundary || boundary>=96){
                throw new IllegalArgumentException("Period boundaries must be strictly increasing"+"and between 1 and 95");
            }
            previousBoundary=boundary;
        }
        for(int i=0; i<4;i++){
            if(!Double.isFinite(buyThresholds[i]) || !Double.isFinite(sellThresholds[i])){
                throw new IllegalArgumentException("Price thresholds must be finite");

        }
        if(buyThresholds[i] >=sellThresholds[i]){
            throw new IllegalArgumentException("The buy threshold must be below the sell threshold.");
        }
        }
        this.boundaries=boundaries.clone();
        this.buyThresholds=buyThresholds.clone();
        this.sellThresholds=sellThresholds.clone();
    }
    public int getPeriodIndex(LocalTime time){
        Objects.requireNonNull(time,"Time cannot be null");
        int quarter=time.getHour()*4+time.getMinute()/15;
        for(int i=0;i<boundaries.length;i++){
            if(quarter<boundaries[i]){
                return i;
            }
        }
        return 3;
    }
    public double ChoosePosition(LocalTime time, double dayAheadPrice, double tradePowerMW){
        if(!Double.isFinite(dayAheadPrice)){
            throw new IllegalArgumentException("Day-ahead price must be finite.");
        }
        if(!Double.isFinite(tradePowerMW)||tradePowerMW<=0){
            throw new IllegalArgumentException("Trading power must be positive and finite.");
        }
        int period=getPeriodIndex(time);
        if (dayAheadPrice<buyThresholds[period]){
            return tradePowerMW;
        }
        if(dayAheadPrice>sellThresholds[period]){
            return -tradePowerMW;
        }
        return 0.0;
    }
    public int[] getBoundaries(){
        return boundaries.clone();
    }
    public double[] getBuyThresholds(){
        return buyThresholds.clone();
    }
    public double[] getSellThresholds(){
        return sellThresholds.clone();
    }
}
