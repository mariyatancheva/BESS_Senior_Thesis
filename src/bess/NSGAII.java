package bess;
import java.util.Objects;
import java.util.zip.DeflaterOutputStream;


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
}
