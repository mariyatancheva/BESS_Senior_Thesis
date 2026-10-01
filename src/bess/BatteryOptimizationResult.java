package bess;
import java.util.List;
import java.util.Objects;

public class BatteryOptimizationResult {
    private final double profitEUR;
    private List<Schedule> steps;

    public BatteryOptimizationResult(double profitEUR,List<Schedule> steps){
        if(!Double.isFinite(profitEUR)){
            throw new IllegalArgumentException("The profit must be finite number.");
        }
        this.profitEUR=profitEUR;
        this.steps=List.copyOf(Objects.requireNonNull(steps,"SChedule steps must not be null."));
    }
    public double getProfitEUR(){
        return profitEUR;
    }

    public List<Schedule> getSteps() {
        return steps;
    }
}
