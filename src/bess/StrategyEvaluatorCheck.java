package bess;
import java.time.Instant;
import java.util.List;

public class StrategyEvaluatorCheck {
    public static void main(String[] args){
        TradingStrategy strategy=new TradingStrategy(new int[]{28,44,68}, new double[]{50,50,50,50},new double[]{100,100,100,100});
        Instant start= Instant.parse("2026-08-31T21:00:00Z");
        List<SpeculationRecord> records=List.of(new SpeculationRecord(start,140,10,40),
                new SpeculationRecord(start.plusSeconds(900),170,10,120),
                new SpeculationRecord(start.plusSeconds(1800),100,10,75),
                new SpeculationRecord(start.plusSeconds(2700),10,10,40),
                new SpeculationRecord(start.plusSeconds(3600),200,10,40));
        StrategyEvaluator evaluator=new StrategyEvaluator();
        StrategyEvaluator.Evaluation result=evaluator.evaluate(strategy,records,4);
        checkClose(result.totalProfitEUR(),180,"Total profit");
        checkClose(result.maxDrawdownEUR(),80,"Maximum drawdown");
        if(result.buyIntervals()!=3 || result.sellIntervals() !=1 || result.idleIntervals()!=1){
            throw new AssertionError("Unexpected numbers of buy,sell or idle intervals.");
        }
        System.out.println("Interval counts check passed.");
        StrategyEvaluator.Evaluation initalLoss=evaluator.evaluate(strategy,List.of(new SpeculationRecord(start,10,10,40)),4);
        checkClose(initalLoss.totalProfitEUR(),-30,"Initial loss");
        checkClose(initalLoss.maxDrawdownEUR(),30,"Drawdown from zero");
        System.out.println("All strategy  evaluation checks passed.");

    }
    private static void checkClose(double actual, double expected, String description){
        if(!Double.isFinite(actual)||Math.abs(actual-expected)>1e-9){
            throw new AssertionError(description+": expected"+expected+", received"+actual);
        }
        System.out.println(description+"check passed");
    }
}
