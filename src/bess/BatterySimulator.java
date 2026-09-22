package bess;
public class BatterySimulator {
    private final BatterySpecification specification;
    private double currentEnergyMWh;
    public BatterySimulator(BatterySpecification specification){
        this.specification=specification;
        this.currentEnergyMWh= specification.getMinEnergyMWh();
    }
    public double getCurrentEnergyMWh(){
        return currentEnergyMWh;
    }
    public double getCurrentSoC(){
        return currentEnergyMWh/ specification.getBatteryCapacityMWh()*100.0;
    }
    public void charge(double powerMW){
        if (!Double.isFinite(powerMW) || powerMW<0 || powerMW> specification.getMaxChargeMW()){
            throw new IllegalArgumentException("Charging power must be a valid number.");
        }
        double intervalDurationHours=0.25;
    }
}
