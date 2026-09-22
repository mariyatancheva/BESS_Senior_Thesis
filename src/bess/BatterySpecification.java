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

        if(!Double.isFinite(batteryCapacityMWh) || batteryCapacityMWh<=0) {
            throw new IllegalArgumentException(
                    "Battery capacity must be a posititve, finite number"
            );
        }
        if(!Double.isFinite(maxChargeMW)|| maxChargeMW<=0){
            throw new IllegalArgumentException(
                    "The charging power must be a positive, finite number"
            );
        }
        if(!Double.isFinite(maxDischargeMW)|| maxDischargeMW <=0){
            throw new IllegalArgumentException(
                    "The dicharging power must be a positive, finite number"
            );
        }
        if(!Double.isFinite(minSoC)|| minSoC<0 || minSoC>=100) {
            throw new IllegalArgumentException(
                    "Minimum state of charge must be between 0 and 100"
            );
        }
        if(!Double.isFinite(maxSoC)|| maxSoC>100 || maxSoC<0 || maxSoC<=minSoC) {
            throw new IllegalArgumentException(
                    "Maximum state of charge must be between 0 and 100 and greater than the Minimum state of charge"
            );
        }
        if(!Double.isFinite(chargeEfficiency)|| chargeEfficiency<=0 || chargeEfficiency>1 ) {
            throw new IllegalArgumentException(
                    "Charging efficency must be between 0 and 1"
            );
        }
        if(!Double.isFinite(dischargeEfficiency)|| dischargeEfficiency<=0 || dischargeEfficiency>1 ) {
            throw new IllegalArgumentException(
                    "Discharging efficency must be between 0 and 1"
            );
        }

        this.batteryCapacityMWh=batteryCapacityMWh;
        this.maxChargeMW=maxChargeMW;
        this.maxDischargeMW=maxDischargeMW;
        this.minSoC=minSoC;
        this.maxSoC=maxSoC;
        this.chargeEfficiency=chargeEfficiency;
        this.dischargeEfficiency=dischargeEfficiency;
    }
    public double getBatteryCapacityMWh() {
        return batteryCapacityMWh;
    }
    public double getMaxChargeMW(){
        return maxChargeMW;
    }
    public double getMaxDischargeMW(){
        return maxDischargeMW;
    }
    public double getMinSoC(){
        return minSoC;
    }
    public double getMaxSoC(){
        return maxSoC;
    }
    public double getChargeEfficiency(){
        return chargeEfficiency;
    }
    public double getDischargeEfficiency(){
        return dischargeEfficiency;
    }
    public double getMinEnergyMWh(){
        return batteryCapacityMWh*minSoC/100.0;
    }
    public double getMaxEnergyMWh(){
        return batteryCapacityMWh*maxSoC/100.0;
    }
    public double getUsableEnergyMWh(){
        return getMaxEnergyMWh()- getMinEnergyMWh();
    }
}

