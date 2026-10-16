package bess;
import java.time.Instant;
import java.util.Objects;

public class WeatherRecord {
    private final Instant time;
    private final double temperature;
    private final double cloudCoverPercentage;
    private final double radiation;
    public WeatherRecord(Instant time, double temperature, double cloudCoverPercentage, double radiation){
        this.time=Objects.requireNonNull(time,"Weather time cannot be null.");
        if(!Double.isFinite(temperature)){
            throw new IllegalArgumentException("Temperature must be a finite number.");
        }
        if(!Double.isFinite(cloudCoverPercentage) || cloudCoverPercentage<0 || cloudCoverPercentage>100){
            throw new IllegalArgumentException("Cloud cover must be a finite, positive number.");
        }
        this.temperature =temperature;
        this.cloudCoverPercentage=cloudCoverPercentage;
        this.radiation=radiation;
    }
    public Instant getTime(){
        return time;
    }
    public double getTemperature(){
        return temperature;
    }
    public double getCloudCoverPercentage(){
        return cloudCoverPercentage;
    }

    public double getRadiationWm2() {
        return radiation;
    }
}
