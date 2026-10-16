package bess;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;


public class HourlyGenerationData {
    private final Instant startTime;
    private final double energyKWh;

    public HourlyGenerationData(Instant startTime, double energyKWh){
        this.startTime=Objects.requireNonNull(startTime,"Start time cannot be null");
        if(!startTime.equals(startTime.truncatedTo(ChronoUnit.HOURS))){
            throw new IllegalArgumentException("Hourly generation must start on a whole UTC hour");
        }
        if(!Double.isFinite(energyKWh)|| energyKWh<0){
            throw new IllegalArgumentException("Generated energy must be a finite,positive number. ");
        }
        this.energyKWh=energyKWh;
    }
    public Instant getStartTime(){
        return startTime;
    }
    public Instant getEndTime(){
        return startTime.plusSeconds(3600);
    }
    public double getEnergyKWh(){
        return energyKWh;
    }
}
