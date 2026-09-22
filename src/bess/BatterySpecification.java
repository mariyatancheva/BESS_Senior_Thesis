package bess;

public class BatterySpecification {
    private double batteryCapacityMWh;
    private double maxChargeMW;
    private double maxDischargeMW;
    private double minSoC;
    private double maxSoC;
    private double chargeEfficiency;
    private double dischargeEfficiency;

    public BatterySpecification (
            double batteryCapacityMWh,
            double maxChargeMW,
            double maxDischargeMW,
            double minSoC,
            double maxSoC,
            double chargeEfficiency,
            double dischargeEfficiency){

        if(!Double.isFinite(batteryCapacityMWh) || batteryCapacityMWh<=0)
            throw new IllegalArgumentException(
                    "Battery capacity must be a postitve, finite number"
            );

        this.batteryCapacityMWh=batteryCapacityMWh;
        this.maxChargeMW=maxChargeMW;
        this.maxDischargeMW=maxDischargeMW;
        this.minSoC=minSoC;
        this.maxSoC=maxSoC;
        this.chargeEfficiency=chargeEfficiency;
        this.dischargeEfficiency=dischargeEfficiency;
    }
}

