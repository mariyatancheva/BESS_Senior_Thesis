package bess;
import java.time.Instant;
import java.util.Objects;

public class GenerationRecord {
    private final Instant startTime;
    private final double energyKWh;

    public GenerationRecord(Instant startTime, double energyKWh){
        this.startTime=Objects.requireNonNull(startTime,"Start time cannot be null.");
        if(!Double.isFinite(energyKWh)||energyKWh<0){
            throw new IllegalArgumentException("Generated energy must be a finite, positive number.");
        }
        this.energyKWh=energyKWh;
    }
    public Instant getStartTime(){
        return startTime;
    }
    public Instant getEndTime(){
        return startTime.plusSeconds(15*60);
    }
    public double getEnergyKWh(){
        return energyKWh;
    }

}
