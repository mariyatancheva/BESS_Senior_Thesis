package bess;
import java.util.Objects;

public class Schedule {
    private final PriceInterval priceInterval;
    private final Battery action;
    private final double powerMW;

    public Schedule(PriceInterval priceInterval, Battery action, double powerMW) {
        this.priceInterval = Objects.requireNonNull(priceInterval, "Price interval must bee not null.");
        this.action = Objects.requireNonNull(action, "Battery action should be CHARGE, DISCHAGE or IDLE.");
        this.powerMW = powerMW;
        if (!Double.isFinite(powerMW) || powerMW < 0) {
            throw new IllegalArgumentException("Power must be non-finite and positive number");
        }
        if (action == Battery.IDLE && powerMW != 0) {
            throw new IllegalArgumentException("Power must be zero during idle period.");
        }
        if (action != Battery.IDLE && powerMW == 0) {
            throw new IllegalArgumentException("Power cannot be 0 during charging and discharging.");
        }
    }
    public PriceInterval getPriceInterval(){
            return priceInterval;
        }
        public Battery getAction(){
        return action;
        }
        public double getPowerMW(){
        return powerMW;
        }


}
