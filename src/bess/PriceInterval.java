package bess;
import java.time.ZonedDateTime;
import java.util.Objects;

public class PriceInterval {
    private final ZonedDateTime startTime;
    private final double pricePerMWh;

    public PriceInterval(ZonedDateTime startTime, double pricePerMWh){
        this.startTime=Objects.requireNonNull(startTime, "Start time must not be null");
        if (!Double.isFinite(pricePerMWh)){
            throw new IllegalArgumentException("Electricity price must be a valid number.");
        }
        this.pricePerMWh=pricePerMWh;

    }
    public ZonedDateTime getStartTime(){
        return startTime;
    }
    public double getPricePerMWh(){
        return pricePerMWh;
    }
    public ZonedDateTime getEndTime(){
        return startTime.plusMinutes(15);
    }

}
