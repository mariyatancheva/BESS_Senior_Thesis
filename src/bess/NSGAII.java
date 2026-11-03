package bess;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

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
        boolean noWorse=firstProfit>=secondProfit || firstDrawdown<secondDrawdown;
        boolean strictlyBetter= firstProfit>secondProfit || firstDrawdown<secondDrawdown;
        return noWorse && strictlyBetter;
    }
    private static List<List<Candidate>> nonNominatedSort(List<Candidate> population){
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
        private static void addCrowdingForObjective(List<Candidate> front,boolean useProfit){
        List<Candidate> sorted=new ArrayList<>(front);
        sorted.sort(Comparator.comparingDouble(Candidate candidate) -> useProfit? candidate.evaluation.totalProfitEUR: candidate.evaluation.maxDrawdownEUR());
        int lastIndex=sorted.size()-1;
        double minimum=useProfit ? sorted.get(0).evaluation.totalProfitEUR() : sorted.get(0).evaluation.maxDrawdownEUR();
        double maximum=useProfit? sorted.get(lastIndex).evaluation.totalProfitEUR():sorted.get(lastIndex).evaluation.maxDrawdownEUR();
        if(minimum==maximum){
            return;
        }
        double range=maximum-minimum;
        if(!Double.isFinite(range)){
            throw new IllegalStateException("Objective range is not finite");
        }
        sorted.get(0).crowdingDistance=Double.POSITIVE_INFINITY;
        sorted.get(lastIndex).crowdingDistance=Double.POSITIVE_INFINITY;
        for(int i=1;i<lastIndex;i++){
            Candidate previous=sorted.get(i-1);
            Candidate next=sorted.get(i+1);
            double previousValue=useProfit? previous.evaluation.totalProfitEUR():previous.evaluation.maxDrawdownEUR();
            double nextValue=useProfit? next.evaluation.totalProfitEUR():next.evaluation.maxDrawdownEUR();
             double distance=(nextValue-previousValue)/range;
             sorted.get(i).crowdingDistance+=distance;
        }
        }

    }

