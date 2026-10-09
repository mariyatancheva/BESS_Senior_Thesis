package bess;
import java.util.Objects;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

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
        return optimizeOneCycle(prices,true);
        }
        public BatteryOptimizationResult oneAdditionalCycle(List<PriceInterval> prices){
        return optimizeOneCycle(prices, false);
        }
        public BatteryOptimizationResult optimizeOneCycle(List<PriceInterval>prices, boolean requireFullCycle){
        return optimizeOneCycle(prices,requireFullCycle,null);
        }
        public BatteryOptimizationResult optimizeOneCycle (List<PriceInterval> prices, boolean requireFullCycle, LocalDate cycleStartDate){
        return optimizeOneCycle(prices, requireFullCycle, cycleStartDate, false);
        }
    public BatteryOptimizationResult optimizeOneCycle(List<PriceInterval> prices, boolean requireFullCycle, LocalDate cycleStartDate, boolean exactBounds){
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
                        if(exactBounds &&intervaIndex==0 &&endState<=startState){
                            continue;
                        }
                        if(exactBounds &&intervaIndex<prices.size()-1 && endState<startState && endState==0){
                            continue;
                        }
                        boolean startNewCycle=currentPhase==Cycle.WAITING&& endState>startState;
                        if(startNewCycle && cycleStartDate!=null && !interval.getStartTime().toLocalDate().equals(cycleStartDate)){
                            continue;
                        }
                        if(completed==1&& endState!=startState){
                            continue;
                        }
                        if(!isTransitionToNewStateIsAllowed(startState,endState)){
                            continue;
                        }
                        if(requireFullCycle && currentPhase==Cycle.CHARGING &&endState<startState &&startState !=maxState){
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
    private int getMinFullCycleIntervals(){
        int maxChargeStepsPerInterval=0;
        int maxDischargeStepsPerInterval=0;
        for(int state=1; state< numOfStates; state++){
            if(isTransitionToNewStateIsAllowed(0,state)){
                maxChargeStepsPerInterval=state;
            }
            if(isTransitionToNewStateIsAllowed(state,0)){
                maxDischargeStepsPerInterval=state;
            }
        }
            if(maxChargeStepsPerInterval==0|| maxDischargeStepsPerInterval==0){
                throw new IllegalArgumentException("Power limits are invalid for the capacity of this battery.");
            }
            int levels=numOfStates-1;
            int chargingIntervals=(int)Math.ceil((double)levels/maxChargeStepsPerInterval);
            int dischargingIntervals=(int)Math.ceil((double)levels/maxDischargeStepsPerInterval);
            return chargingIntervals+dischargingIntervals;
    }
    public BatteryOptimizationResult optimizedSchedule(List<PriceInterval> prices, LocalDate selectedDate){
        Objects.requireNonNull(selectedDate,"Selected date must not be null");
        BatteryOptimizationResult bestResult=optimizeOneCycle(prices,true,selectedDate);
        double minMargin=2* specification.getCostPerCycle();
        int minFullCycleIntervals=getMinFullCycleIntervals();
        if(prices.size()<minFullCycleIntervals+2){
            return bestResult;
        }
        for (int split=2;split<=prices.size()-2;split++){
            List<PriceInterval> firstPart=prices.subList(0,split);
            List<PriceInterval> secondPart=prices.subList(split,prices.size());
            if(!secondPart.get(0).getStartTime().toLocalDate().equals(selectedDate)){
                continue;
            }
            for (int order=0;order<2;order++){
                boolean fullCycleFirst=order==0;
                List<PriceInterval> fullCyclePrices;
                List<PriceInterval> additionalCyclePrices;
                if(fullCycleFirst){
                    fullCyclePrices=firstPart;
                    additionalCyclePrices=secondPart;
                } else{
                    fullCyclePrices=secondPart;
                    additionalCyclePrices=firstPart;
                }
                if(fullCyclePrices.size()<minFullCycleIntervals){
                    continue;
                }
                BatteryOptimizationResult optionalCycleResult= optimizeOneCycle(additionalCyclePrices,false,selectedDate);
                if(optionalCycleResult.getProfitEUR()<minMargin){
                    continue;
                }
                BatteryOptimizationResult fullCycleResult=optimizeOneCycle(fullCyclePrices,true,selectedDate);
                double profitFromBothCycles=fullCycleResult.getProfitEUR()+optionalCycleResult.getProfitEUR();
                if(profitFromBothCycles>bestResult.getProfitEUR()){
                    List<Schedule> combinedSteps=new ArrayList<>();
                    if(fullCycleFirst){
                        combinedSteps.addAll(fullCycleResult.getSteps());
                        combinedSteps.addAll(optionalCycleResult.getSteps());
                    }else {
                        combinedSteps.addAll(optionalCycleResult.getSteps());
                        combinedSteps.addAll(fullCycleResult.getSteps());
                    }
                    bestResult=new BatteryOptimizationResult(profitFromBothCycles, combinedSteps);
                }
            }
        }
        return bestResult;


    }
    public double [][] createCycleProfitTable(int intervalCount){
        double[][] cycleProfits= new double[intervalCount][intervalCount+1];
        for(double[] row : cycleProfits){
            Arrays.fill(row, Double.NEGATIVE_INFINITY);
        }
        return cycleProfits;
    }
    private double[][] calcCycleProfits(List<PriceInterval> prices, boolean requireFullCycle){
        int intervalCount=prices.size();
        double [][] cycleProfit=createCycleProfitTable(intervalCount);
        Cycle[] phases=Cycle.values();
        int waitingIndex=Cycle.WAITING.ordinal();
        int maximumState=numOfStates-1;
        for(int startIndex=0;startIndex<intervalCount;startIndex++){
            double [][] currentProfit=new double[numOfStates][phases.length];
            for(double [] row: currentProfit){
                Arrays.fill(row,Double.NEGATIVE_INFINITY);
            }
            currentProfit[0][waitingIndex]=0;
            for(int intervalIndex=startIndex;intervalIndex<intervalCount;intervalIndex++){
                double price=prices.get(intervalIndex).getPricePerMWh();
                double[][] nextProfit=new double[numOfStates][phases.length];
                for(double [] row:nextProfit){
                    Arrays.fill(row,Double.NEGATIVE_INFINITY);
                }
                for(int startState=0;startState<numOfStates;startState++){
                    for(int phaseIndex=0; phaseIndex<phases.length;phaseIndex++){
                        double currentResult=currentProfit[startState][phaseIndex];
                        if(currentResult==Double.NEGATIVE_INFINITY){
                            continue;
                        }
                        Cycle currentPhase=phases[phaseIndex];
                        for(int endState=0;endState<numOfStates;endState++){
                            if(intervalIndex==startIndex &&endState<=startState){
                                continue;
                            }
                            if(!isTransitionToNewStateIsAllowed(startState,endState)){
                                continue;
                            }
                            if(requireFullCycle && currentPhase==Cycle.CHARGING && endState<startState && startState!=maximumState){
                                continue;
                            }
                            Cycle nextPhase= getNextPhaseCycle(currentPhase,startState,endState);
                            if(nextPhase==null){
                                continue;
                            }
                            double transitionProfit=getTransitionProfitEUR(startState,endState,price);
                                double candidateProfit=currentResult+transitionProfit;
                                if(endState==0 && endState<startState){
                                    int endIndex=intervalIndex+1;
                                    if (candidateProfit>cycleProfit[startIndex][endIndex]){
                                        cycleProfit[startIndex][endIndex]=candidateProfit;
                                    }
                                    continue;
                                }
                                int nextPhaseIndex=nextPhase.ordinal();
                                if(candidateProfit>nextProfit[endState][nextPhaseIndex]){
                                    nextProfit[endState][nextPhaseIndex]=candidateProfit;
                                }

                            }
                        }
                    }
                currentProfit=nextProfit;
                }
            }
        return cycleProfit;
        }
        private int getDailyCycleStatusAfterTransition(int startDay, int endDay, int lastDay, int cycleStatus) {
            if (startDay == endDay) {
                return cycleStatus;
            }
            if((cycleStatus&1)==0){
                return -1;
            }
            if(endDay>startDay+1&& startDay+1<lastDay) {
                return -1;
            }

            return 0;
        }
        private List<int[]> selectMainCycle(List<PriceInterval> prices, double [][] fullCycleProfit){
        int intervalCount=prices.size();
        LocalDate firstDate=prices.get(0).getStartTime().toLocalDate();
        int[] BoundaryDay=new int[intervalCount+1];
        for(int i=0;i<intervalCount;i++){
            LocalDate date=prices.get(i).getStartTime().toLocalDate();
            BoundaryDay[i]=(int) ChronoUnit.DAYS.between(firstDate,date);
        }
        LocalDate finalBoundaryDate=prices.get(intervalCount-1).getEndTime().toLocalDate();
        BoundaryDay[intervalCount]=(int) ChronoUnit.DAYS.between(firstDate,finalBoundaryDate);
        int lastDay=BoundaryDay[intervalCount-1];
        double [][] bestProfit=new double[intervalCount+1][4];
        int [][] previousTime=new int[intervalCount+1][4];
        int [][] previousCycleState=new int[intervalCount+1][4];
        int [][] previousAction=new int[intervalCount+1][4];
        for (int i=0;i<=intervalCount;i++){
            Arrays.fill(bestProfit[i], Double.NEGATIVE_INFINITY);
            Arrays.fill(previousTime[i],-1);
            Arrays.fill(previousCycleState[i],-1);
        }
        bestProfit[0][0]=0;
        for(int startTime=0;startTime<intervalCount;startTime++){
            int startDay=BoundaryDay[startTime];
            for(int cycleState=0;cycleState<4;cycleState++){
                double currentProfit=bestProfit[startTime][cycleState];
                if(currentProfit== Double.NEGATIVE_INFINITY){
                    continue;
                }
                int nextCycleState= getDailyCycleStatusAfterTransition(startDay,BoundaryDay[startTime+1], lastDay,cycleState);
                if (nextCycleState != -1 && currentProfit>bestProfit[startTime+1][nextCycleState]){
                    bestProfit[startTime+1][nextCycleState]=currentProfit;
                    previousTime[startTime+1][nextCycleState]=startTime;
                    previousCycleState[startTime+1][nextCycleState]=cycleState;
                    previousAction[startTime+1][nextCycleState]=0;
                }

                for(int cycleType=1;cycleType<=1;cycleType++){
                    if((cycleState & cycleType)!=0){continue;}
                    int cycleWithState= cycleState | cycleType;
                    for(int end=startTime+1;end<=intervalCount;end++){
                        int lastUsedDay=BoundaryDay[end-1];
                        if (lastUsedDay>startDay){
                            break;
                        }
                        double cycleProfit=fullCycleProfit[startTime][end];
                        if(cycleProfit==Double.NEGATIVE_INFINITY){continue;}
                        int endCycleState=getDailyCycleStatusAfterTransition(startDay,BoundaryDay[end],lastDay,cycleWithState);
                        if (endCycleState==-1){
                            continue;
                        }
                        double candidateProfit=currentProfit+cycleProfit;
                        if(candidateProfit>bestProfit[end][endCycleState]){
                            bestProfit[end][endCycleState]=candidateProfit;
                            previousTime[end][endCycleState]=startTime;
                            previousCycleState[end][endCycleState]=cycleState;
                            previousAction[end][endCycleState]=cycleType;
                        }
                    }
                }
            }
        }
        int finalCycleState=-1;
        double finalProfit=Double.NEGATIVE_INFINITY;
        for(int state=0;state<4;state++){
            if(bestProfit[intervalCount][state]>finalProfit){
                finalProfit=bestProfit[intervalCount][state];
                finalCycleState=state;
            }
        }
        if(finalCycleState==-1){
            throw new IllegalStateException("Cycle requirements are not met.");
        }
        List<int[]> selectedCycles= new ArrayList<>();
        int time= intervalCount;
        int state=finalCycleState;
        while(time>0){
            int start=previousTime[time][state];
            int oldState=previousCycleState[time][state];
            int action=previousAction[time][state];
            if(start==-1 || oldState==-1){
                throw new IllegalStateException("The cycle cannot be reconstructed");
            }
            if(action !=0){
                selectedCycles.add(new int[]{start,time,action});
            }
            time=start;
            state=oldState;
        }
        Collections.reverse(selectedCycles);
        return selectedCycles;

        }
        private List<int[]> addAditionalCycle(List<PriceInterval> prices, List<int[]>mainCycle, double[][] addtionalCycleProfit,LocalDate selectedDate){
        int intervalCount=prices.size();
        boolean[] occupiedIntervals= new boolean[intervalCount];
        for(int[]cycle:mainCycle) {
            for(int i=cycle[0];i <cycle[1];i++){
                occupiedIntervals[i]=true;
            }
        }
        LocalDate lastDate=prices.get(intervalCount-1).getStartTime().toLocalDate();
        double minProfit=2* specification.getCostPerCycle();
        double [][] bestProfit=new double[intervalCount+1][2];
        int [][] previousTime= new int[intervalCount+1][2];
        int [][] previousStatus= new int[intervalCount+1][2];
        boolean [][] selectedCycle=new boolean[intervalCount+1][2];
            for (int i=0;i<=intervalCount;i++){
                Arrays.fill(bestProfit[i], Double.NEGATIVE_INFINITY);
                Arrays.fill(previousTime[i],-1);
                Arrays.fill(previousStatus[i],-1);
            }
            bestProfit[0][0]=0;
            for(int time=0;time<intervalCount;time++){
                LocalDate startDate=prices.get(time).getStartTime().toLocalDate();
                for(int status=0;status<2;status++){
                    double currentProfit=bestProfit[time][status];
                    if(currentProfit== Double.NEGATIVE_INFINITY){
                        continue;
                    }
                    LocalDate nextDate=prices.get(time).getEndTime().toLocalDate();
                    int nextStatus= nextDate.equals(startDate)?status:0;
                    if (currentProfit >bestProfit[time+1][nextStatus]){
                        bestProfit[time+1][nextStatus]=currentProfit;
                        previousTime[time+1][nextStatus]=time;
                        previousStatus[time+1][nextStatus]=status;
                        selectedCycle[time+1][nextStatus]=false;
                    }
                    if (status==1 || occupiedIntervals[time] || !startDate.equals(selectedDate)){
                        continue;
                    }
                    for (int end=time+1;end<=intervalCount;end++){
                        if (occupiedIntervals[end-1]){
                            break;
                        }
                        LocalDate lastUsedDate=prices.get(end-1).getStartTime().toLocalDate();
                        if(lastUsedDate.isAfter(startDate.plusDays(1))){
                            break;
                        }
                        double cycleProfit=addtionalCycleProfit[time][end];
                        if(cycleProfit==Double.NEGATIVE_INFINITY||cycleProfit<minProfit){
                            continue;
                        }
                        LocalDate endDate=prices.get(end-1).getEndTime().toLocalDate();
                        int endStatus=endDate.equals(startDate) ?1:0;
                        double candidateProfit=currentProfit+cycleProfit;
                        if(candidateProfit>bestProfit[end][endStatus]){
                            bestProfit[end][endStatus]=candidateProfit;
                            previousTime[end][endStatus]=time;
                            previousStatus[end][endStatus]=status;
                            selectedCycle[end][endStatus]=true;
                    }
                    }
                }
        }
            int status=bestProfit[intervalCount][1]>bestProfit[intervalCount][0]?1:0;
            List<int[]> allCycles= new ArrayList<>(mainCycle);
            int time= intervalCount;
            while(time>0){
                int start= previousTime[time][status];
                int oldStatus= previousStatus[time][status];
                if (start==-1 || oldStatus==-1){
                    throw new IllegalStateException("Additional cycle cannot be reconstrcuted.");
                }
                if(selectedCycle[time][status]){
                    allCycles.add(new int[]{start, time,2});
                }
                time=start;
                status=oldStatus;
            }
            allCycles.sort((first,second)->Integer.compare(first[0],second[0]));
            return allCycles;
        }
        private BatteryOptimizationResult buildingSchedule(List<PriceInterval> prices, List<int[]> selectedCycles) {
            List<Schedule> steps = new ArrayList<>();
            double totalProfit = 0;
            int nextInterval = 0;
            for (int[] cycle : selectedCycles) {
                int start = cycle[0];
                int end = cycle[1];
                int cycleType = cycle[2];
                while (nextInterval < start) {
                    steps.add(new Schedule(prices.get(nextInterval), Battery.IDLE, 0));

                    nextInterval++;
                }
                BatteryOptimizationResult cycleResult = optimizeOneCycle(prices.subList(start, end), cycleType == 1, null, true);

                steps.addAll(cycleResult.getSteps());
                totalProfit+=cycleResult.getProfitEUR();
                nextInterval=end;
            }
            while(nextInterval<prices.size()){
                steps.add(new Schedule(prices.get(nextInterval),Battery.IDLE,0));
                nextInterval++;
            }
            return new BatteryOptimizationResult(totalProfit, steps);
        }
        public BatteryOptimizationResult optimizePeriod(List <PriceInterval> prices,LocalDate selectedDate){
        Objects.requireNonNull(selectedDate,"Selected date must not be null");
        Objects.requireNonNull(prices,"Prices list must not be null");
        if (prices.isEmpty()){
            throw new IllegalStateException("Price list must not be empty.");
        }
        PriceInterval firstInterval=prices.get(0);
        PriceInterval lastInterval=prices.get(prices.size()-1);
        LocalDate firstDate=firstInterval.getStartTime().toLocalDate();
        LocalDate lastDate=lastInterval.getStartTime().toLocalDate();
        if (!firstInterval.getStartTime().equals(firstDate.atStartOfDay(firstInterval.getStartTime().getZone()))){
            throw new IllegalArgumentException("Price data must start from the beginnig of the the first day");
            }
            if (!lastDate.isAfter(firstDate)){
                throw new IllegalArgumentException("At least two days with prices are needed");
            }
            if (!lastInterval.getEndTime().equals(lastDate.plusDays(1).atStartOfDay(firstInterval.getStartTime().getZone()))){
                throw new IllegalArgumentException("Price data must end with the last date interval");
            }
            for (int i =1; i< prices.size();i++){
                if(!prices.get(i-1).getEndTime().toInstant().equals(prices.get(i).getStartTime().toInstant())){
                    throw new IllegalArgumentException("Price interval must be consecutive.");
                }
            }
            double [][] fullCycleProfit=calcCycleProfits(prices,true);
            List<int[]> mainCycle= selectMainCycle(prices, fullCycleProfit);
            double [][] additionalCycleProfit=calcCycleProfits(prices,false);
            List<int[]> allCycles=addAditionalCycle(prices,mainCycle,additionalCycleProfit,selectedDate);
            List<int[]> selectedDayCycle= new ArrayList<>();
            for(int[] cycle: allCycles){
                LocalDate startDate=prices.get(cycle[0]).getStartTime().toLocalDate();
                if(startDate.equals(selectedDate)){
                    selectedDayCycle.add(cycle);
                }
            }
            return buildingSchedule(prices, selectedDayCycle);

        }

    }





