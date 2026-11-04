package bess;
import java.util.*;

public class NSGAII {
    private static class Candidate{
        private final TradingStrategy strategy;
        private final StrategyEvaluator.Evaluation evaluation;
        private int rank=-1;
        private double crowdingDistance=0.0;
        private Candidate(TradingStrategy strategy, StrategyEvaluator.Evaluation evaluation){
            this.strategy=Objects.requireNonNull(strategy,"Strategy cannot be null");
            this.evaluation=Objects.requireNonNull(evaluation,"Evaluation cannot be null");
            if(!Double.isFinite(evaluation.totalProfitEUR())){
                throw new IllegalArgumentException("Total profit must be finite.");
            }
            if(!Double.isFinite(evaluation.maxDrawdownEUR()) || evaluation.maxDrawdownEUR()<0){
                throw new IllegalArgumentException("Maximum drawdown must be finite and non-negative.");
            }
        }
    }
    private static boolean dominates(Candidate first, Candidate second){
        double firstProfit=first.evaluation.totalProfitEUR();
        double secondProfit=second.evaluation.totalProfitEUR();
        double firstDrawdown=first.evaluation.maxDrawdownEUR();
        double secondDrawdown=second.evaluation.maxDrawdownEUR();
        boolean noWorse=firstProfit>=secondProfit && firstDrawdown<=secondDrawdown;
        boolean strictlyBetter= firstProfit>secondProfit || firstDrawdown<secondDrawdown;
        return noWorse && strictlyBetter;
    }
    private static List<List<Candidate>> nonDominatedSort(List<Candidate> population){
        Objects.requireNonNull(population,"Population cannot be null.");
        int size=population.size();
        List<List<Integer>> dominatedIndices=new ArrayList<>();
        int[] dominationCount=new int[size];
        for(int i=0;i<size;i++){
            Candidate candidate =Objects.requireNonNull(population.get(i),"Candidate cannot be null.");
            candidate.rank=-1;
            candidate.crowdingDistance=0.0;
            dominatedIndices.add(new ArrayList<>());
        }
        for(int i=0;i<size;i++){
            for(int j=i+1;j<size;j++){
                Candidate first=population.get(i);
                Candidate second=population.get(j);
                if(dominates(first,second)){
                    dominatedIndices.get(i).add(j);
                    dominationCount[j]++;
                } else if (dominates(second,first)){
                    dominatedIndices.get(j).add(i);
                    dominationCount[i]++;
                }
                }
        }
        List<Integer> currentFront=new ArrayList<>();
        for(int i=0;i<size;i++){
            if(dominationCount[i]==0){
                population.get(i).rank=0;
                currentFront.add(i);
            }
        }
        List<List<Candidate>> fronts=new ArrayList<>();
        int currentRank=0;
        while (!currentFront.isEmpty()){
            List<Candidate> front=new ArrayList<>();
            List<Integer> nextFront=new ArrayList<>();
            for(int index:currentFront){
                front.add(population.get(index));
                for(int dominatedIndex:dominatedIndices.get(index)){
                    dominationCount[dominatedIndex]--;
                    if(dominationCount[dominatedIndex]==0){
                        population.get(dominatedIndex).rank=currentRank+1;
                        nextFront.add(dominatedIndex);
                    }
                }
            }
            fronts.add(front);
            currentFront=nextFront;
            currentRank++;
        }
        return fronts;
        }
        private static void calculateCrowdingDistance(List<Candidate> front){
        Objects.requireNonNull(front,"Front cannot be null.");
        if(front.isEmpty()){
            return;
        }
        for(Candidate candidate:front){
            Objects.requireNonNull(candidate,"Candidate cannot be null.");
            candidate.crowdingDistance=0.0;
        }
        addCrowdingForObjective(front,true);
        addCrowdingForObjective(front,false);
        }
        private static void addCrowdingForObjective(List<Candidate> front,boolean useProfit) {
            List<Candidate> sorted = new ArrayList<>(front);
            sorted.sort(Comparator.comparingDouble((Candidate candidate) -> useProfit ? candidate.evaluation.totalProfitEUR() : candidate.evaluation.maxDrawdownEUR()));
            int lastIndex = sorted.size() - 1;
            double minimum = useProfit ? sorted.get(0).evaluation.totalProfitEUR() : sorted.get(0).evaluation.maxDrawdownEUR();
            double maximum = useProfit ? sorted.get(lastIndex).evaluation.totalProfitEUR() : sorted.get(lastIndex).evaluation.maxDrawdownEUR();
            if (minimum == maximum) {
                return;
            }
            double range = maximum - minimum;
            if (!Double.isFinite(range)) {
                throw new IllegalStateException("Objective range is not finite");
            }
            sorted.get(0).crowdingDistance = Double.POSITIVE_INFINITY;
            sorted.get(lastIndex).crowdingDistance = Double.POSITIVE_INFINITY;
            for (int i = 1; i < lastIndex; i++) {
                Candidate previous = sorted.get(i - 1);
                Candidate next = sorted.get(i + 1);
                double previousValue = useProfit ? previous.evaluation.totalProfitEUR() : previous.evaluation.maxDrawdownEUR();
                double nextValue = useProfit ? next.evaluation.totalProfitEUR() : next.evaluation.maxDrawdownEUR();
                double distance = (nextValue - previousValue) / range;
                sorted.get(i).crowdingDistance += distance;
            }
        }
        private static TradingStrategy createRandomStrategy(Random random, int minPeriodQuarters, double minPrice, double maxPrice){
            int[] lenghts={minPeriodQuarters,minPeriodQuarters,minPeriodQuarters,minPeriodQuarters};
            int remaining=96-4*minPeriodQuarters;
            int[] cuts={random.nextInt(remaining+1),random.nextInt(remaining+1),random.nextInt(remaining+1)};
            Arrays.sort(cuts);
            lenghts[0]+=cuts[0];
            lenghts[1]+=cuts[1]-cuts[0];
            lenghts[2]+=cuts[2]-cuts[1];
            lenghts[3]+=remaining-cuts[2];
            int[] boundaries={lenghts[0],lenghts[0]+lenghts[1],lenghts[0]+lenghts[1]+lenghts[2]};
            double[] buyThresholds=new double[4];
            double[] sellThresholds=new double[4];
            for(int i=0;i<4;i++){
                double first=minPrice+random.nextDouble()*(maxPrice-minPrice);
                double second=minPrice+random.nextDouble()*(maxPrice-minPrice);
                buyThresholds[i]=Math.min(first,second);
                sellThresholds[i]=Math.max(first,second);
                if(buyThresholds[i]>=sellThresholds[i]){
                    buyThresholds[i]=minPrice;
                    sellThresholds[i]=maxPrice;
                }
            }
            return new TradingStrategy(boundaries,buyThresholds,sellThresholds);
            }

    public static void main(String[] args) {
        TradingStrategy strategy = new TradingStrategy(new int[]{28,44,68}, new double[]{50, 50,50, 50}, new double[]{100, 100, 100, 100});
        Candidate A = new Candidate(strategy,new StrategyEvaluator.Evaluation(1000, 100, 0, 0, 0));
        Candidate B =new Candidate(strategy,new StrategyEvaluator.Evaluation(1200, 200, 0, 0, 0));
        Candidate C = new Candidate(strategy,new StrategyEvaluator.Evaluation(1400, 300, 0, 0, 0));
        Candidate D =new Candidate(strategy, new StrategyEvaluator.Evaluation(900, 200, 0, 0, 0));
        Candidate E= new Candidate(strategy,new StrategyEvaluator.Evaluation(800, 300, 0, 0, 0));

        if (!dominates(A, D) || dominates(D,A)) {
            throw new AssertionError("Incorrect dominance comparison");
        }

        if (dominates(A,B) || dominates(B,A)) {
            throw new AssertionError("Trade-off candidates must not dominate each other.");
        }

        if (!dominates(B,D)) {
            throw new AssertionError("Higher profit with equal drawdown must dominate.");
        }
        if (dominates(A,A)) {
            throw new AssertionError("A candidate must not dominate itself.");
        }
        System.out.println("Dominance checks passed.");

        List<Candidate> population = List.of(E,C,D,A,B);
        List<List<Candidate>> fronts = nonDominatedSort(population);

        if (fronts.size() != 3 ||fronts.get(0).size() != 3 || fronts.get(1).size() != 1 || fronts.get(2).size() != 1) {
            throw new AssertionError("Unexpected front sizes.");
        }
        if (A.rank != 0 || B.rank != 0 || C.rank != 0 || D.rank != 1 || E.rank != 2) {
            throw new AssertionError("Incorrect candidate ranks.");
        }
        System.out.println("Non-dominated sorting check passed");
        calculateCrowdingDistance(fronts.get(0));

        if (A.crowdingDistance != Double.POSITIVE_INFINITY|| C.crowdingDistance != Double.POSITIVE_INFINITY) {
            throw new AssertionError("Extreme candidates must have infinite crowding distance.");
        }

        if (!Double.isFinite(B.crowdingDistance) || Math.abs(B.crowdingDistance - 2.0) > 1e-9) {
            throw new AssertionError("Expected crowding distance 2.0, received " + B.crowdingDistance);
        }

        calculateCrowdingDistance(fronts.get(0));
        if (Math.abs(B.crowdingDistance - 2.0) > 1e-9) {
            throw new AssertionError("Crowding distance was not reset.");
        }

        System.out.println("Crowding distance checks passed.");

        Candidate duplicate = new Candidate(strategy,new StrategyEvaluator.Evaluation(1000, 100, 0, 0, 0));
        if (dominates(A, duplicate) || dominates(duplicate,A)) {throw new AssertionError("Equal results must not dominate each other.");
        }
        List<Candidate> equalFront = List.of(A, duplicate);
        calculateCrowdingDistance(equalFront);

        if (A.crowdingDistance != 0.0 || duplicate.crowdingDistance != 0.0) {
            throw new AssertionError("Identical objectives must produce zero distance.");
        }
        System.out.println("Equal objectives check passed.");
        System.out.println("All NSGA-II ranking checks passed.");
    }

    }

