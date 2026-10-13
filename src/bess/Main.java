package bess;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;
import java.time.LocalDate;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        BatterySpecification battery;

        while(true) {
            try {
                System.out.print("Enter battery capacity in MWh: ");
                double capacity = scanner.nextDouble();
                System.out.print("Enter maximum charging power in MW: ");
                double maxCharge = scanner.nextDouble();
                System.out.print("Enter maximum discharging power in MW: ");
                double maxDischarge = scanner.nextDouble();

                battery = new BatterySpecification(
                            capacity,
                            maxCharge,
                            maxDischarge,
                            1,
                            99,
                            Math.sqrt(0.9),
                            Math.sqrt(0.9)

                    );
                    break;
                } catch (java.util.InputMismatchException exception) {
                    System.out.println("Invalid input. Enter numerical values for capacity and power. Try again. ");
                    scanner.nextLine();


                } catch (IllegalArgumentException exception) {
                System.out.println("Invalid battery parameters." + exception.getMessage() + "Try again");
                    }
                }


                System.out.println("Battery capacity:" + battery.getBatteryCapacityMWh() + "MWh");
                System.out.println("Max charging power:" + battery.getMaxChargeMW() + "MW");
                System.out.println("Max discharging power:" + battery.getMaxDischargeMW() + "MW");
                System.out.println("SoC limits:" + battery.getMinSoC() + "% and " + battery.getMaxSoC() + "%");
                System.out.println("Usable energy within SoC boundaries:" + battery.getUsableEnergyMWh() + "MWh");
                PriceCSVReader reader = new PriceCSVReader();
                try {
                    List<PriceInterval> datesAndPrices = reader.readPrices(Path.of("data/prices.csv"));
                    PriceInterval first = datesAndPrices.get(0);
                    PriceInterval last = datesAndPrices.get(datesAndPrices.size() - 1);
                    DateTimeFormatter formatData = DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm XXX '['VV']'");
                    System.out.println("First interval starts at: " + first.getStartTime().format(formatData));
                    System.out.println("First price is: " + first.getPricePerMWh() + "Eur/MWh");
                    System.out.println("Last interval ends at: " + last.getEndTime().format(formatData));
                    System.out.println("Last price is: " + last.getPricePerMWh() + "Eur/MWh");

                    BatteryOptimizer optimizer = new BatteryOptimizer(battery);

                    long optimizationStart = System.nanoTime();
                    BatteryOptimizationResult periodResult = optimizer.optimizePeriod(datesAndPrices);
                    double optimizationSeconds = (System.nanoTime() - optimizationStart) / 1000000000.0;
                    System.out.printf("Period optimization time: %.2f seconds%n", optimizationSeconds);
                    LocalDate selectedDate;
                    while (true) {
                        try {
                            System.out.print("Enter a year: ");
                            int year = Integer.parseInt(scanner.next());
                            System.out.print("Enter a month (1-12): ");
                            int month = Integer.parseInt(scanner.next());
                            System.out.print("Enter a day (1-31): ");
                            int day = Integer.parseInt(scanner.next());
                            selectedDate = LocalDate.of(year, month, day);
                            break;
                        } catch (NumberFormatException | java.time.DateTimeException exception) {
                            System.out.println("Enter valid date using whole numbers");
                        }

                    }

                    LocalDate nextDate = selectedDate.plusDays(1);
                    List<PriceInterval> schedulingPrices = new ArrayList<>();
                    for (PriceInterval interval : datesAndPrices) {
                        LocalDate intervalDate = interval.getStartTime().toLocalDate();
                        if (intervalDate.equals(selectedDate) || intervalDate.equals(nextDate)) {
                            schedulingPrices.add(interval);
                        }
                    }
                    if (schedulingPrices.isEmpty()) {
                        System.out.println("No prices available for this period.");
                        return;


                    }
                    ZoneId schedulingZone = schedulingPrices.get(0).getStartTime().getZone();
                    ZonedDateTime expectedStart = selectedDate.atStartOfDay(schedulingZone);
                    ZonedDateTime expectedEnd = selectedDate.plusDays(2).atStartOfDay(schedulingZone);
                    PriceInterval firstInterval = schedulingPrices.get(0);
                    PriceInterval lastInterval = schedulingPrices.get(schedulingPrices.size() - 1);
                    if (!firstInterval.getStartTime().toInstant().equals(expectedStart.toInstant()) || !lastInterval.getEndTime().toInstant().equals(expectedEnd.toInstant())) {
                        System.out.println("Complete price data is required for the selected and following day");
                        return;
                    }
                    BatteryOptimizationResult dailyFinancialResult = optimizer.getDailyResult(periodResult, selectedDate);
                    System.out.printf("Daily trading profit is: %.2f EUR%n", dailyFinancialResult.getProfitEUR());
                    BatterySimulator dailyCheck = new BatterySimulator(battery);
                    double checkedDailyProfit = 0;
                    int cycleCount = 0;
                    boolean cycleInprogress = false;
                    ZonedDateTime cycleStart = null;
                    double maxSoC = battery.getMinSoC();
                    double cycleCostEUR = 0;
                    ;
                    double cycleRevenueEUR = 0;
                    ;

                    for (Schedule step : dailyFinancialResult.getSteps()) {
                        if (!cycleInprogress && step.getAction() == Battery.CHARGE) {
                            cycleInprogress = true;
                            cycleStart = step.getPriceInterval().getStartTime();
                            maxSoC = dailyCheck.getCurrentSoC();
                            cycleCostEUR = 0;
                            cycleRevenueEUR = 0;
                        }
                        dailyCheck.executeSchedule(step);
                        double amount = step.getPowerMW() * 0.25 * step.getPriceInterval().getPricePerMWh();
                        if (step.getAction() == Battery.CHARGE) {
                            cycleCostEUR += amount;
                            checkedDailyProfit -= amount;
                        } else if (step.getAction() == Battery.DISCHARGE) {
                            cycleRevenueEUR += amount;
                            checkedDailyProfit += amount;
                        }
                        if (cycleInprogress) {
                            maxSoC = Math.max(maxSoC, dailyCheck.getCurrentSoC());
                            if (step.getAction() == Battery.DISCHARGE && Math.abs(dailyCheck.getCurrentEnergyMWh() - battery.getMinEnergyMWh()) < 1e-6) {
                                cycleCount++;
                                System.out.println("Cycle " + cycleCount);
                                System.out.println("Start " + cycleStart.format(formatData));
                                System.out.println("End " + step.getPriceInterval().getEndTime().format(formatData));
                                System.out.printf("Maximum SoC: %.2f%% %n  ", maxSoC);
                                double cycleProfitEUR = cycleRevenueEUR - cycleCostEUR;
                                System.out.printf("Charging cost: %.2f EUR%n", cycleCostEUR);
                                System.out.printf("Revenue: %.2f EUR%n", cycleRevenueEUR);
                                System.out.println("============================");
                                System.out.printf("Trading profit: %.2f EUR%n%n", cycleProfitEUR);
                                System.out.println("============================");
                                cycleInprogress = false;
                            }
                        }
                    }
                    System.out.println("Completed cycles: " + cycleCount);
                    System.out.println("===============================");
                    System.out.printf("Total trading profit: %.2f EUR%n ", checkedDailyProfit);
                    System.out.println("===============================");
                    System.out.println("Daily profit matches:" + (Math.abs(checkedDailyProfit - dailyFinancialResult.getProfitEUR())));
                    System.out.println("Daily schedule returned to minimum:" + (Math.abs(dailyCheck.getCurrentEnergyMWh() - battery.getMinEnergyMWh()) < 1e-6));


                } catch (IOException exception) {
                    System.out.println("The file could not be read." + exception.getMessage());
                }catch (IllegalArgumentException exception){
                    System.out.println("Invalid data or parameters."+ exception.getMessage());


                } catch (IllegalStateException exception) {
                    System.out.println("A valid schedule cannot be created. " + exception.getMessage());
                }

            }
        }







