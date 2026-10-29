package bess;
import java.util.Objects;
public class SpeculationCalculator {
    private static final double ESOpenalty=299.0;
    public double calculateProfit(SpeculationRecord record, double positionMW){
        Objects.requireNonNull(record, "Speculation record cannot be null");
        if(!Double.isFinite(positionMW)){
            throw new IllegalArgumentException("Position must be a finite number.");
        }
        if(positionMW==0.0){
            return 0.0;
        }
        double energyMwh=positionMW*0.25;
        double DAMCashFlow= -energyMwh*record.getDAMPrice();
        double imbalanceCF=energyMwh* record.getImbalancePriceEURPerMWh();
        double originalSystemImbalance=record.getSystemImbalanceMWh();
        double adjustedSystemImbalance=originalSystemImbalance-energyMwh;
        boolean systemReversed=(originalSystemImbalance>0.0 && adjustedSystemImbalance<0.0)|| (originalSystemImbalance<0.0 && adjustedSystemImbalance>0.0);
        double penalty=systemReversed ? ESOpenalty:0.0;
        double profit=DAMCashFlow+imbalanceCF-penalty;
        if(!Double.isFinite(adjustedSystemImbalance)|| !Double.isFinite(profit)){
            throw new IllegalStateException("Calculated financial result or imabalnce is not finite");
        }
        return profit;
    }
}
