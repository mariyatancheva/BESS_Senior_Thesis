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
        double addedEnergyMWh= powerMW*intervalDurationHours* specification.getChargeEfficiency();
        if(currentEnergyMWh+addedEnergyMWh > specification.getMaxEnergyMWh()){
            throw new IllegalArgumentException("Charging will exceed the max allowed stored energy.");
        }
        currentEnergyMWh=currentEnergyMWh+addedEnergyMWh;
    }
    public void discharge(double powerMW){
        if (!Double.isFinite(powerMW) || powerMW<0 || powerMW> specification.getMaxDischargeMW()){
            throw new IllegalArgumentException("Discharging power must be a valid number.");
        }
        double intervalDurationHours=0.25;
        double removedEnergyMWh= powerMW*intervalDurationHours/ specification.getDischargeEfficiency();
        if(currentEnergyMWh-removedEnergyMWh < specification.getMinEnergyMWh()){
            throw new IllegalArgumentException("Discharging will reduce State of Charge below the minimum. ");
        }
        currentEnergyMWh=currentEnergyMWh-removedEnergyMWh;
    }
    public void idle(){

    }
}
