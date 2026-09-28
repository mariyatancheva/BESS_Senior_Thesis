package bess;

import java.util.Objects;

public class BatteryOptimizer {
        private final BatterySpecification specification;
        private final double energyStepMWh;
        private final int numOfStates;

        public BatteryOptimizer(BatterySpecification specification){
            this.specification= Objects.requireNonNull(specification, "Battery specification cannot be null.");
            this.energyStepMWh=specification.getBatteryCapacityMWh()/100.0;
            this.numOfStates=(int) Math.round(specification.getUsableEnergyMWh()/ energyStepMWh)+1;

        }
        public double getEnergyPerState(int stateIndex){
            if(stateIndex<0 || stateIndex>numOfStates){
                System.out.println("State index must be valid number.");
            }
            return specification.getMinEnergyMWh()+stateIndex*energyStepMWh;
        }
        public int getNumOfStates(){
            return numOfStates;
        }
        public double getRequiredPowerMW(int startState, int endState) {
            double durationInterval = 0.25;
            double startEnergyMWh = getEnergyPerState(startState);
            double endEnergyMWh = getEnergyPerState(endState);
            double differenceEnergyMWh = endEnergyMWh - startEnergyMWh;
            if (differenceEnergyMWh > 0) {
                return differenceEnergyMWh / (durationInterval * specification.getChargeEfficiency());
            }
            if (differenceEnergyMWh < 0) {
                return (-differenceEnergyMWh)/(durationInterval * specification.getChargeEfficiency());

            }
            return 0;
        }
        public boolean isTransitionToNewStateIsAllowed(int startState, int endState){
            double requiredPowerMW= getRequiredPowerMW(startState,endState );
            if(endState>startState){
                return requiredPowerMW<= specification.getMaxChargeMW();
            }
            else if(endState<startState){
                return requiredPowerMW<= specification.getMaxDischargeMW();
            }
            else {
                return true;
            }
        }
        public double getTransitionProfitEUR(int startState, int endState, double pricePerMWh){
            if (!Double.isFinite(pricePerMWh)) {
                throw new IllegalArgumentException("The price must be a finite number ");
            }
            if(!isTransitionToNewStateIsAllowed(startState,endState)){
                throw new IllegalArgumentException("Transition exceeds power limits.");
            }
            double requiredPowerMW= getRequiredPowerMW(startState,endState);
            double energyMWh= requiredPowerMW*0.25;
            if(endState>startState){
                return -energyMWh * pricePerMWh;
            }
            else if (endState<startState){
                return energyMWh*pricePerMWh;
            }else{
                return 0;
            }

    }
}
