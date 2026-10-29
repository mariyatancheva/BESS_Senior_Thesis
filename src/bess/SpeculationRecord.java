package bess;
import java.time.Instant;
import java.util.Objects;

public class SpeculationRecord {
    private final Instant startTime;
    private final double imbalancePriceEURPerMWh;
    private final double systemImbalanceMWh;
    private final double DAMPrice;

    public SpeculationRecord(Instant startTime, double imbalancePriceEURPerMWh, double systemImbalanceMWh, double DAMPrice){
        this.startTime=Objects.requireNonNull(startTime,"Start time cannot be null.");
        if(startTime.getNano() !=0 || Math.floorMod(startTime.getEpochSecond(),900)!=0){
            throw new IllegalArgumentException("The interval must start on a 15-minute boundary");
        }
        if(!Double.isFinite(imbalancePriceEURPerMWh)|| !Double.isFinite(systemImbalanceMWh)|| !Double.isFinite(DAMPrice)){
            throw new IllegalArgumentException("Prices and system imbalance must be finite numbers.");
        }
        this.imbalancePriceEURPerMWh=imbalancePriceEURPerMWh;
        this.systemImbalanceMWh=systemImbalanceMWh;
        this.DAMPrice=DAMPrice;
    }
    public Instant getStartTime(){
        return startTime;
    }
    public Instant getEndTime(){
        return startTime.plusSeconds(900);
    }
    public double getImbalancePriceEURPerMWh(){
        return imbalancePriceEURPerMWh;
    }
    public double getSystemImbalanceMWh(){
        return systemImbalanceMWh;
    }
    public double getDAMPrice(){
        return DAMPrice;
    }
}
