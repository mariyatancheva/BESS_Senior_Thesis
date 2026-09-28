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
}
