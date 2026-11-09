package bess;
import  java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

public class TradingStaregyTestRun {

    public static void main(String[] args) throws IOException {
        Scanner scanner=new Scanner(System.in);
        run(scanner);
    }
    public static void run(Scanner scanner) throws IOException{
        SpeculationCSVReader reader=new SpeculationCSVReader();
        List<SpeculationRecord> records=reader.readSpeculation(Path.of("data/speculation.csv"));

        double tradePowerMW;
        while(true) {
            System.out.print("Enter trading power in MW: ");
            if(!scanner.hasNextLine()) {
                return;
            }
            try{ tradePowerMW = Double.parseDouble(scanner.nextLine().trim().replace(',', '.'));
                if (!Double.isFinite(tradePowerMW) || tradePowerMW <= 0) {
                    System.out.println(
                            "Trading power must be positive and finite."
                    );
                    continue;
                }
                break;
            } catch (NumberFormatException exception) {
                System.out.println("Enter a valid number.");
            }
        }
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
        System.out.printf("%n%-6s %18s %22s%n","No.", "Profit (EUR)", "Max drawdown (EUR)");
        System.out.println("--------------------------------------------------------------");
        for (int i = 0; i < solutions.size(); i++) {
            StrategyEvaluator.Evaluation evaluation=solutions.get(i).evaluation();
            System.out.printf( "%-6d %18.2f %22.2f%n",i+1, evaluation.totalProfitEUR(),evaluation.maxDrawdownEUR());
        }
        checkSolutions(solutions,minPeriodQuarters,minPrice,maxPrice);
        while(true){
            System.out.print("\nChoose a strategy (1-"+solutions.size()+") or 0 to return:");
            if(!scanner.hasNextLine()){
                break;
            }
            int choice;
            try{
                choice=Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException exception){
                System.out.println("Enter a whole number.");
                continue;
            }
            if(choice==0){
                break;
            }
            if(choice<1 || choice>solutions.size()){
                System.out.println("Invalid strategy number.");
                continue;
            }
            printStrategy(solutions.get(choice-1),choice);
        }

    }
    private static void checkSolutions(List<NSGAII.Solution> solutions,int minPeriodQuarters, double minPrice, double maxPrice){
        if(solutions.isEmpty()){
            throw new AssertionError("No strategies returned.");
        }
        for(NSGAII.Solution solution : solutions){
            TradingStrategy strategy=solution.strategy();
            int[] boundaries=strategy.getBoundaries();
            double[] buy=strategy.getBuyThresholds();
            double[] sell=strategy.getSellThresholds();
            int start=0;
            for(int period=0;period<4;period++){
                int end=period<3? boundaries[period]:96;
                if(end-start<minPeriodQuarters){
                    throw new AssertionError("A trading period is shroter than allowed");
                }
                if(!Double.isFinite(buy[period])|| !Double.isFinite(sell[period])|| buy[period]<minPrice||sell[period]>maxPrice|| buy[period]>=sell[period]){
                    throw new AssertionError("Invalid price thresholds.");
                }
                start=end;
            }
            StrategyEvaluator.Evaluation result=solution.evaluation();
            if(!Double.isFinite(result.totalProfitEUR())|| !Double.isFinite(result.maxDrawdownEUR())||result.maxDrawdownEUR()<0){
                throw new AssertionError("Invalid evaluation results.");
            }

        }
        System.out.println("Strategy constraints check passed.");
        for(int i=0;i<solutions.size();i++){
            for(int j=0;j< solutions.size();j++){
                if(i==j){
                    continue;
                }
                StrategyEvaluator.Evaluation a= solutions.get(i).evaluation();
                StrategyEvaluator.Evaluation b= solutions.get(j).evaluation();
                boolean noWorse=a.totalProfitEUR()>=b.totalProfitEUR() && a.maxDrawdownEUR()<=b.maxDrawdownEUR();
                boolean strictlyBetter=a.totalProfitEUR()>b.totalProfitEUR() || a.maxDrawdownEUR()<b.maxDrawdownEUR();
                if(noWorse && strictlyBetter){
                    throw new AssertionError("Strategy "+(i+1)+" dominates strategy "+(j+1));
                }
            }
        }
        System.out.println("Non-dominated results check passed");
    }
    private static String formatQuarter(int quarter){
        int hour=quarter/4;
        int minute=(quarter%4)*15;
        return String.format("%02d:%02d",hour,minute);
    }
    private static void printStrategy(NSGAII.Solution solution, int num){
        TradingStrategy strategy=solution.strategy();
        StrategyEvaluator.Evaluation evaluation=solution.evaluation();
        System.out.printf("%n---Strategy%d ---%n", num);
        System.out.printf("Historical profit: %.2f EUR%n",evaluation.totalProfitEUR());
        System.out.printf("Maximum drawdown: %.2f EUR%n",evaluation.maxDrawdownEUR());
        System.out.printf("Buy intervals: %d | Sell intervals: %d | Idle intervals: %d%n", evaluation.buyIntervals(), evaluation.sellIntervals(), evaluation.idleIntervals());

        int[] boundaries = strategy.getBoundaries();
        double[] buy = strategy.getBuyThresholds();
        double[] sell = strategy.getSellThresholds();

        System.out.println("\nPeriod(Europe/Sofia) | Buy below(EUR/MWh) | Sell above(EUR/MWh)");
        int periodStart = 0;
        for (int period = 0; period < 4; period++) {
            int periodEnd = period < 3 ? boundaries[period] : 96;
            System.out.printf("%s - %s | %18.2f | %19.2f%n", formatQuarter(periodStart), formatQuarter(periodEnd),buy[period], sell[period]);
            periodStart=periodEnd;
        }
    }
}
