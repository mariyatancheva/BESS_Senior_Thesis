package bess;
import java.util.Objects;
public class SpeculationCalculator {
    public double calculateProfit(SpeculationRecord record, double positionMW){
        Objects.requireNonNull(record, "Speculation record cannot be null");
        if(!Double.isFinite(positionMW)){
            throw new IllegalArgumentException("Position must be a finite number.");
        }
        if(positionMW==0.0){
            return 0.0;
        }
        double energyMwh=positionMW*0.25;
        double originalImbalance=record.getSystemImbalanceMWh();
        double adjustedImbalance=originalImbalance+(positionMW>0.0 ? (positionMW-1.0)/4.0 : (positionMW+1.0)/4.0);
        if(!Double.isFinite(adjustedImbalance)){
            throw new IllegalStateException("Adjusted system imbalance is not finite.");
        }
        boolean sameSign=originalImbalance==0.0 || adjustedImbalance==0.0 || (originalImbalance>0.0 && adjustedImbalance>0.0)|| (originalImbalance<0.0 && adjustedImbalance<0.0);
        double settlementPrice;
        if(sameSign){
            settlementPrice=record.getImbalancePriceEURPerMWh();
        } else {
            settlementPrice=adjustedImbalance>=0.0 ? 0.0 :299.0;
        }
        double dayAheadCF=-energyMwh*record.getDAMPrice();
        double imbalanceCF=energyMwh*settlementPrice;
        double profit=dayAheadCF+imbalanceCF;

        if(!Double.isFinite(profit)){
            throw new IllegalStateException("Calculated profit is not finite.");
        }
        return profit;
    }
}
