package bess;
public class BatterySimulator {
    private final BatterySpecification specification;
    private static final double Energy_tolerance= 1e-9;
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
        double newEnergyMWh= currentEnergyMWh+addedEnergyMWh;
        if(newEnergyMWh> specification.getMaxEnergyMWh()+Energy_tolerance){
            throw new IllegalArgumentException("Charging will exceed maximum allowed stored energy.");
        }
        currentEnergyMWh=Math.min(newEnergyMWh, specification.getMaxEnergyMWh());
    }

    public void discharge(double powerMW){
        if (!Double.isFinite(powerMW) || powerMW<0 || powerMW> specification.getMaxDischargeMW()){
            throw new IllegalArgumentException("Discharging power must be a valid number.");
        }
        double intervalDurationHours=0.25;
        double removedEnergyMWh= powerMW*intervalDurationHours/ specification.getDischargeEfficiency();
        double newEnergyMWh= currentEnergyMWh-removedEnergyMWh;
        if(newEnergyMWh< specification.getMinEnergyMWh()-Energy_tolerance)
        {
            throw new IllegalArgumentException("Discharging will leave less than the minimum allowed stored energy. ");
        }
        currentEnergyMWh=Math.max(newEnergyMWh, specification.getMinEnergyMWh());
    }
    public void idle(){

    }
    public double getAvailableChargePowerMW(){
        double intervalDurationHours=0.25;
        double remainingCapacity=specification.getMaxEnergyMWh()- currentEnergyMWh;
        double powerToReachMaximumMW= remainingCapacity/(intervalDurationHours* specification.getChargeEfficiency());
        return Math.min(specification.getMaxChargeMW(), powerToReachMaximumMW);
    }
    public double getAvailableDischargePowerMW(){
        double intervalDurationHours=0.25;
        double availableCapacity=currentEnergyMWh-specification.getMinEnergyMWh();
        double powerToReachMinimumMW= availableCapacity* specification.getDischargeEfficiency()/intervalDurationHours;
        return Math.min(specification.getMaxDischargeMW(), powerToReachMinimumMW);
    }
    public void executeSchedule(Schedule step){
        switch (step.getAction()){
            case CHARGE -> charge(step.getPowerMW());
            case DISCHARGE -> discharge(step.getPowerMW());
            case IDLE -> idle();
        }
    }
}
