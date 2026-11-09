package bess;
import  java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class TradingStaregyTestRun {
    public static void main(String[] args) throws IOException {
        SpeculationCSVReader reader = new SpeculationCSVReader();
        List<SpeculationRecord> records = reader.readSpeculation(Path.of("data/speculation.csv"));
        double tradePowerMW = 5.0;
        int populationSize = 60;
        int generations = 80;
        int minPeriodQuarters = 4;
        double crossoverProbability = 0.9;
        double mutationProbability = 1.0 / 11.0;
        double lowestPrice = Double.POSITIVE_INFINITY;
        double highestPrice = Double.NEGATIVE_INFINITY;
        for (SpeculationRecord record : records) {
            double price = record.getDAMPrice();
            lowestPrice = Math.min(lowestPrice, price);
            highestPrice = Math.max(highestPrice, price);

        }
        double margin = Math.max(1.0, 0.1 * (highestPrice - lowestPrice));
        double minPrice = lowestPrice - margin;
        double maxPrice = highestPrice + margin;
        System.out.printf("Intervals: %d | Power: %.2f MW | Population: %d | Generations: %d%n", records.size(), tradePowerMW, populationSize, generations);
        System.out.printf("Price range: %.2f to %.2f EUR/MWh%n", minPrice, maxPrice);
        NSGAII optimizer = new NSGAII();
        long start = System.nanoTime();
        long seed=42L;
        List<NSGAII.Solution> solutions = optimizer.optimize(records, tradePowerMW, populationSize, generations, minPeriodQuarters, minPrice, maxPrice, crossoverProbability, mutationProbability, seed);
        double elapsedSeconds = (System.nanoTime() - start) / 1000000000.0;
        System.out.printf("%nOptimization time: %.2f seconds%n", elapsedSeconds);
        System.out.println("Non-dominated candidates: " + solutions.size());
        System.out.println("Historical results on the optimization data");
        for (int i = 0; i < solutions.size(); i++) {
            NSGAII.Solution solution = solutions.get(i);
            TradingStrategy strategy=solution.strategy();
            StrategyEvaluator.Evaluation evaluation = solution.evaluation();
            System.out.printf("%n--- Strategy %d ---%n", i + 1);
            System.out.printf("Total profit: %.2f EUR%n", evaluation.totalProfitEUR());
            System.out.printf("Maximum drawdown: %.2f EUR%n", evaluation.maxDrawdownEUR());
            System.out.printf("Buy intervals: %d | Sell Intervals: %d | idle intervals: %d%n", evaluation.buyIntervals(), evaluation.sellIntervals(), evaluation.idleIntervals());
            int[] boundaries = strategy.getBoundaries();
            double[] buy = strategy.getBuyThresholds();
            double[] sell = strategy.getSellThresholds();
            int periodStart = 0;
            System.out.println("Period (Europe/Sofia)|Buye below|Sell above");
            for (int period = 0; period < 4; period++) {
                int periodEnd = period < 3 ? boundaries[period] : 96;
                System.out.printf("%s - %s | %.2f EUR/MWh |%.2f EUR/MWh%n", formatQuarter(periodStart), formatQuarter(periodEnd), buy[period], sell[period]);
                periodStart = periodEnd;
            }
        }


    }
    private static String formatQuarter(int quarter){
        int hour=quarter/4;
        int minute=(quarter%4)*15;
        return String.format("%02d:%02d",hour,minute);
    }
}
