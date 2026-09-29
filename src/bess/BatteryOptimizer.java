package bess;

import java.util.Objects;
import java.util.Arrays;
import java.util.List;

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
            else if (differenceEnergyMWh < 0) {
                return ((-differenceEnergyMWh)* specification.getDischargeEfficiency())/durationInterval;

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

    } //optimization
        public double maximumTradingprofit(List<PriceInterval> prices){
            Objects.requireNonNull(prices,"Price list must not be null.");
            if(prices.isEmpty()){
                throw new IllegalArgumentException("The price list must not be empty.");
            }
            double[] currentProfit= new double[numOfStates];
            Arrays.fill(currentProfit,Double.NEGATIVE_INFINITY);
            currentProfit[0]=0;
            for(PriceInterval interval: prices){
                double [] nextProfit= new double[numOfStates];
                Arrays.fill(nextProfit,Double.NEGATIVE_INFINITY);

                for(int startState=0; startState< numOfStates;startState++){
                    if (currentProfit[startState]==Double.NEGATIVE_INFINITY) {
                        continue;
                    }
                for(int endState=0;endState<numOfStates;endState++) {
                    if (!isTransitionToNewStateIsAllowed(startState, endState)) {
                        continue;
                    }
                        double transitionProfit = getTransitionProfitEUR(startState, endState, interval.getPricePerMWh());
                        double nonFinalProfit = currentProfit[startState] + transitionProfit;
                        if(nonFinalProfit>nextProfit[endState]){
                            nextProfit[endState]=nonFinalProfit;
                        }
                }


                }
                currentProfit=nextProfit;
            }
            return currentProfit[0];
        }
}
