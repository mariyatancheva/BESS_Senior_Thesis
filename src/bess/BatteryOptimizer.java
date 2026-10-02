package bess;

import java.util.Objects;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public class BatteryOptimizer {
    private final BatterySpecification specification;
    private final double energyStepMWh;
    private final int numOfStates;

    public BatteryOptimizer(BatterySpecification specification) {
        this.specification = Objects.requireNonNull(specification, "Battery specification cannot be null.");
        this.energyStepMWh = specification.getBatteryCapacityMWh() / 100.0;
        this.numOfStates = (int) Math.round(specification.getUsableEnergyMWh() / energyStepMWh) + 1;

    }

    public double getEnergyPerState(int stateIndex) {
        if (stateIndex < 0 || stateIndex >= numOfStates) {
            throw new IllegalArgumentException("State index must be in thee allowed range(between 0 and" + (numOfStates-1)+ ".");
        }
        return specification.getMinEnergyMWh() + stateIndex * energyStepMWh;
    }

    public int getNumOfStates() {
        return numOfStates;
    }

    public double getRequiredPowerMW(int startState, int endState) {
        double durationInterval = 0.25;
        double startEnergyMWh = getEnergyPerState(startState);
        double endEnergyMWh = getEnergyPerState(endState);
        double differenceEnergyMWh = endEnergyMWh - startEnergyMWh;
        if (differenceEnergyMWh > 0) {
            return differenceEnergyMWh / (durationInterval * specification.getChargeEfficiency());
        } else if (differenceEnergyMWh < 0) {
            return ((-differenceEnergyMWh) * specification.getDischargeEfficiency()) / durationInterval;

        }
        return 0;
    }

    public boolean isTransitionToNewStateIsAllowed(int startState, int endState) {
        double requiredPowerMW = getRequiredPowerMW(startState, endState);
        if (endState > startState) {
            return requiredPowerMW <= specification.getMaxChargeMW();
        } else if (endState < startState) {
            return requiredPowerMW <= specification.getMaxDischargeMW();
        } else {
            return true;
        }
    }

    public double getTransitionProfitEUR(int startState, int endState, double pricePerMWh) {
        if (!Double.isFinite(pricePerMWh)) {
            throw new IllegalArgumentException("The price must be a finite number ");
        }
        if (!isTransitionToNewStateIsAllowed(startState, endState)) {
            throw new IllegalArgumentException("Transition exceeds power limits.");
        }
        double requiredPowerMW = getRequiredPowerMW(startState, endState);
        double energyMWh = requiredPowerMW * 0.25;
        if (endState > startState) {
            return -energyMWh * pricePerMWh;
        } else if (endState < startState) {
            return energyMWh * pricePerMWh;
        } else {
            return 0;
        }

    }
    private Cycle getNextPhaseCycle(Cycle currentPhase, int startState, int endState) {
        Objects.requireNonNull(currentPhase, "Cycle Phase must not bee null.");

        if (currentPhase == Cycle.WAITING) {
            if (startState != 0) {
                return null;
            }
            if (endState == 0) {
                return Cycle.WAITING;
            }
            return Cycle.CHARGING;

        }
        if (currentPhase == Cycle.CHARGING) {
            if (endState >= startState) {
                return Cycle.CHARGING;
            }
            if (endState == 0) {
                return Cycle.WAITING;
            }
            return Cycle.DISCHARGING;
        }
        if (endState>startState){
            return null;
        }
        if (endState==0){
            return Cycle.WAITING;
        }
        return Cycle.DISCHARGING;
    }
    //optimization

    public BatteryOptimizationResult maximumTradingprofit(List<PriceInterval> prices) {
        Objects.requireNonNull(prices, "Price list must not be null.");

        if (prices.isEmpty()) {
            throw new IllegalArgumentException("The price list must not be empty.");
        }
        Cycle[] phases = Cycle.values();
        int numberofPhases = phases.length;
        int watingPhaseIndex = Cycle.WAITING.ordinal();

        double[][] profitAtTheBeginningOfTheInterval = new double[numOfStates][numberofPhases];
        for (double[] row : profitAtTheBeginningOfTheInterval) {
            Arrays.fill(row, Double.NEGATIVE_INFINITY);
        }
        profitAtTheBeginningOfTheInterval[0][watingPhaseIndex] = 0;

        int[][][] previousStates = new int[prices.size()][numOfStates][numberofPhases];
        int[][][] previousPhases = new int[prices.size()][numOfStates][numberofPhases];
        for (int intervalIndex = 0; intervalIndex < prices.size(); intervalIndex++) {
                for (int state = 0; state < numOfStates; state++) {
                    Arrays.fill(previousStates[intervalIndex][state], -1);
                    Arrays.fill(previousPhases[intervalIndex][state], -1);
                }
            }

            for (int intervalIndex = 0; intervalIndex < prices.size(); intervalIndex++) {
                PriceInterval interval = prices.get(intervalIndex);
                double[][] profitAtTheEndOfTheInterval = new double[numOfStates][numberofPhases];
                for (double[] row : profitAtTheEndOfTheInterval) {
                    Arrays.fill(row, Double.NEGATIVE_INFINITY);
                }
                for (int startState = 0; startState < numOfStates; startState++) {
                    for (int phaseIndex = 0; phaseIndex < numberofPhases; phaseIndex++) {
                        double currentProfit = profitAtTheBeginningOfTheInterval[startState][phaseIndex];
                        if (currentProfit == Double.NEGATIVE_INFINITY) {
                            continue;
                        }
                        Cycle currentPhase = phases[phaseIndex];

                        for (int endState = 0; endState < numOfStates; endState++) {
                            if (!isTransitionToNewStateIsAllowed(startState, endState)) {
                                continue;
                            }
                            Cycle nextPhase = getNextPhaseCycle(currentPhase, startState, endState);
                            if (nextPhase == null) {
                                continue;
                            }
                            int nextPhaseIndex = nextPhase.ordinal();
                            double transitionProfit = getTransitionProfitEUR(startState, endState, interval.getPricePerMWh());
                            double nonfinalResult = currentProfit + transitionProfit;

                            if (nonfinalResult > profitAtTheEndOfTheInterval[endState][nextPhaseIndex]) {
                                profitAtTheEndOfTheInterval[endState][nextPhaseIndex] = nonfinalResult;
                                previousStates[intervalIndex][endState][nextPhaseIndex] = startState;
                                previousPhases[intervalIndex][endState][nextPhaseIndex] = phaseIndex;
                            }
                        }

                    }
                }
                profitAtTheBeginningOfTheInterval = profitAtTheEndOfTheInterval;
            }
            double bestProfit = profitAtTheBeginningOfTheInterval[0][watingPhaseIndex];
            if (!Double.isFinite(bestProfit)) {
                throw new IllegalArgumentException("A valid schedule was not found,");
            }
            List<Schedule> steps = new ArrayList<>();
            int endState = 0;
            int endPhaseIndex = watingPhaseIndex;
            for (int intervalIndex = prices.size() - 1; intervalIndex >= 0; intervalIndex--) {
                int startState = previousStates[intervalIndex][endState][endPhaseIndex];
                int startPhaseIndex = previousPhases[intervalIndex][endState][endPhaseIndex];
                if (startState == -1 || startPhaseIndex == -1) {
                    throw new IllegalArgumentException("The schedule cannot be reconstructed");
                }
                Battery action;
                if (endState > startState) {
                    action = Battery.CHARGE;
                } else if (startState > endState) {
                    action = Battery.DISCHARGE;
                } else {
                    action = Battery.IDLE;
                }
                double powerMW = getRequiredPowerMW(startState, endState);
                steps.add(new Schedule(prices.get(intervalIndex), action, powerMW));
                endState = startState;
                endPhaseIndex = startPhaseIndex;


            }
            Collections.reverse(steps);
            return new BatteryOptimizationResult(bestProfit, steps);


        }
    public BatteryOptimizationResult oneFullCycle(List<PriceInterval> prices){
        Objects.requireNonNull(prices,"Price list must not be null.");
        if (prices.isEmpty()){
            throw new IllegalArgumentException("Price list must not be empty.");
        }
        Cycle[] phases=Cycle.values();
        int numberOfPhases=phases.length;
        int waitingIndex=Cycle.WAITING.ordinal();
        int maxState= numOfStates-1;

        double [][][] currentProfit= new double[numOfStates][numberOfPhases][2];
        for (int state=0;state<numOfStates;state++){
            for (int phase=0;phase<numberOfPhases;phase++){
                Arrays.fill(currentProfit[state][phase],Double.NEGATIVE_INFINITY);

            }
        }
        currentProfit[0][waitingIndex][0]=0;
        int [][][][] previousState= new int[prices.size()][numOfStates][numberOfPhases][2];
        int [][][][] previousPhase= new int[prices.size()][numOfStates][numberOfPhases][2];
        int [][][][] previousCompleted= new int[prices.size()][numOfStates][numberOfPhases][2];

        for (int time=0; time< prices.size();time++){
            for (int state=0; state<numOfStates; state++){
                for(int phase=0; phase<numberOfPhases;phase++) {
                    Arrays.fill(previousState[time][state][phase],-1);
                    Arrays.fill(previousPhase[time][state][phase],-1);
                    Arrays.fill(previousCompleted[time][state][phase],-1);
                }
            }
        }
        for (int intervaIndex=0;intervaIndex<prices.size();intervaIndex++){
            PriceInterval interval=prices.get(intervaIndex);
            double [][][] nextProfit= new double [numOfStates][numberOfPhases][2];
            for (int state=0; state< numOfStates;state++){
                for(int phase=0;phase<numberOfPhases;phase++){
                    Arrays.fill(nextProfit[state][phase],Double.NEGATIVE_INFINITY);
                }
            }

        for(int startState=0;startState<numOfStates;startState++){
            for(int phaseIndex=0;phaseIndex<numberOfPhases;phaseIndex++){
                for(int completed=0; completed<2; completed++){
                    double currentResult= currentProfit[startState][phaseIndex][completed];
                    if(currentResult==Double.NEGATIVE_INFINITY){
                        continue;
                    }
                    Cycle currentPhase=phases[phaseIndex];
                    for(int endState=0;endState<numOfStates;endState++){
                        if(completed==1&& endState!=startState){
                            continue;
                        }
                        if(!isTransitionToNewStateIsAllowed(startState,endState)){
                            continue;
                        }
                        if(currentPhase==Cycle.CHARGING &&endState<startState &&startState !=maxState){
                            continue;
                        }
                        Cycle nextPhase= getNextPhaseCycle(currentPhase,startState,endState);
                            if(nextPhase==null){
                                continue;
                            }
                            int nextPhaseIndex= nextPhase.ordinal();
                            int nextCompleted= completed;
                            if (endState< startState && endState==0){
                                nextCompleted=1;
                            }
                            double transitionProfit=getTransitionProfitEUR(startState,endState,interval.getPricePerMWh());
                            double candidateProfit=currentResult+transitionProfit;
                            if(candidateProfit>nextProfit[endState][nextPhaseIndex][nextCompleted]){
                                nextProfit[endState][nextPhaseIndex][nextCompleted]=candidateProfit;
                                previousState[intervaIndex][endState][nextPhaseIndex][nextCompleted]=startState;
                                previousPhase[intervaIndex][endState][nextPhaseIndex][nextCompleted]=phaseIndex;
                                previousCompleted[intervaIndex][endState][nextPhaseIndex][nextCompleted]=completed;
                            }
                        }
                    }

                }
            }
        currentProfit=nextProfit;
        }
        double bestProfit=currentProfit[0][waitingIndex][1];
        if(!Double.isFinite(bestProfit)){
            throw new IllegalArgumentException("A full cycle cannot be completed.");
        }
        List<Schedule> steps = new ArrayList<>();
        int endState=0;
        int endPhaseIndex=waitingIndex;
        int endCompleted=1;
        for(int intervalIndex=prices.size()-1;intervalIndex>=0;intervalIndex--){
            int startState=previousState[intervalIndex][endState][endPhaseIndex][endCompleted];
            int startPhaseIndex=previousPhase[intervalIndex][endState][endPhaseIndex][endCompleted];
            int startCompleted=previousCompleted[intervalIndex][endState][endPhaseIndex][endCompleted];
            if(startState==-1|| startPhaseIndex==-1|| startCompleted==-1){
                throw new IllegalArgumentException("The schedule cannot be reconstrcuted");
            }
            Battery action;
            if(endState>startState){ action= Battery.CHARGE;}
            else if(endState<startState){ action= Battery.DISCHARGE;}
            else { action= Battery.IDLE;}
            double powerMW=getRequiredPowerMW(startState,endState);
            steps.add(new Schedule(prices.get(intervalIndex),action,powerMW));
            endState=startState;
            endPhaseIndex=startPhaseIndex;
            endCompleted=startCompleted;
        }
        Collections.reverse(steps);
        return new BatteryOptimizationResult(bestProfit,steps);
    }
    }




