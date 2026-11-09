package bess;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

public class StrategyEvaluator {
    private static final ZoneId timeZone=ZoneId.of("Europe/Sofia");
    private final SpeculationCalculator calculator = new SpeculationCalculator();
    public record Evaluation(double totalProfitEUR, double maxDrawdownEUR, int buyIntervals, int sellIntervals, int idleIntervals){}
        public Evaluation evaluate(TradingStrategy strategy, List<SpeculationRecord> records, double tradePowerMW){
            Objects.requireNonNull(strategy,"Strategy cannot be null.");
            Objects.requireNonNull(records,"Records cannot be null.");
            if(records.isEmpty()){
                throw new IllegalArgumentException("Evaluation records cannot be empty");
            }
            if(!Double.isFinite(tradePowerMW)||tradePowerMW<=0){
                throw new IllegalArgumentException("Trading power must be positive and finite.");
            }
            double totalProfit=0;
            double highestProfit=0;
            double maxDrawdown=0;
            int buyIntervals=0;
            int sellIntervals=0;
            int idleIntervals=0;
            SpeculationRecord previous=null;
            for(SpeculationRecord record:records){
                Objects.requireNonNull(record,"A speculation record cannot be null.");
                if(previous !=null && !record.getStartTime().equals(previous.getEndTime())){
                    throw new IllegalArgumentException("Evaluation intervals must be consecutive.");
                }
                double positionMW=strategy.ChoosePosition(record.getStartTime().atZone(timeZone).toLocalTime(),record.getDAMPrice(),tradePowerMW);
                double intervalProfit=calculator.calculateProfit(record,positionMW);
                totalProfit+=intervalProfit;
                if(!Double.isFinite(totalProfit)){
                    throw new IllegalStateException("Accumulated profit is not finite.");
                }
                highestProfit=Math.max(highestProfit, totalProfit);
                double currentDrawdown=highestProfit-totalProfit;
                if(!Double.isFinite(currentDrawdown)){
                    throw new IllegalStateException("Drawdown is not finite.");
                }
                maxDrawdown=Math.max(maxDrawdown,currentDrawdown);
                if(positionMW>0){
                    buyIntervals++;
                } else if (positionMW<0){
                    sellIntervals++;
                } else {
                    idleIntervals++;
                }
                previous=record;
            }
            return new Evaluation(totalProfit,maxDrawdown,buyIntervals,sellIntervals,idleIntervals);
        }
    }

