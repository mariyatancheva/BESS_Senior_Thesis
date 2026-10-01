package bess;
import java.time.ZoneId;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;
public class Main {
    public static void main(String[] args){
        Scanner scanner= new Scanner(System.in);
        System.out.print("Enter battery capacity in MWh: ");
        double capacity= scanner.nextDouble();
        System.out.print("Enter maximum charging power in MW: ");
        double maxCharge= scanner.nextDouble();
        System.out.print("Enter maximum discharging power in MW: ");
        double maxDischarge= scanner.nextDouble();


         BatterySpecification battery= new BatterySpecification(
                 capacity,
                 maxCharge,
                 maxDischarge,
                 1,
                 99,
                 Math.sqrt(0.9),
                 Math.sqrt(0.9)

         );

         System.out.println( "Battery capacity:"+ battery.getBatteryCapacityMWh()+ "MWh");
        System.out.println( "Max charging power:"+ battery.getMaxChargeMW()+ "MW");
        System.out.println( "Max discharging power:"+ battery.getMaxDischargeMW()+ "MW");
        System.out.println( "SoC limits:"+ battery.getMinSoC()+ "% and "+ battery.getMaxSoC()+ "%");
        System.out.println( "Usable energy within SoC boundaries:"+ battery.getUsableEnergyMWh()+ "MWh");
         BatterySimulator simulator= new BatterySimulator(battery);
         System.out.println("Initial energy: " + simulator.getCurrentEnergyMWh()+"MWh");
         System.out.println("Initial SoC: " + simulator.getCurrentSoC()+"%");
         simulator.charge(150);
         System.out.println("Energy after charging: " + simulator.getCurrentEnergyMWh()+ "Mwh");
         System.out.println("SoC after charging: " + simulator.getCurrentSoC()+ "%");
        simulator.discharge(100);
        System.out.println("Energy after discharging: " + simulator.getCurrentEnergyMWh()+ "Mwh");
        System.out.println("SoC after discharging: " + simulator.getCurrentSoC()+ "%");
        double energyBeforePausing= simulator.getCurrentEnergyMWh();
        simulator.idle();
        System.out.println("The energy before pause is: " + energyBeforePausing+ "MWh");
        System.out.println("The energy after pause is: " + simulator.getCurrentEnergyMWh() + "MWh");
        System.out.println("Available charging power is: " + simulator.getAvailableChargePowerMW()+"MW");
        BatterySimulator cycle = new BatterySimulator(battery);
        for(int interval=0; interval<1000;interval++){
            double powerMW= cycle.getAvailableChargePowerMW();
            if (powerMW<1e-6){break;}
            cycle.charge(powerMW);
        }
        System.out.println("SoC after full charge" + cycle.getCurrentSoC() + "%");
        BatterySimulator limit=new BatterySimulator(battery);
        double energyBeforeRejectedCharge=cycle.getCurrentEnergyMWh();
        try{
            cycle.charge(150);
            System.out.println("The system should have rejected the charge");
        } catch(IllegalArgumentException exception){
            System.out.println("Charge is rejected: "+ exception.getMessage());
        }
        for(int interval=0; interval<1000;interval++){
            double powerMW= cycle.getAvailableDischargePowerMW();
            if (powerMW<1e-6){break;}
            cycle.discharge(powerMW);
        }
        System.out.println("SoC after full discharge is: " + cycle.getCurrentSoC()+ "%");

        double energyBeforeRejectedDischarge=limit.getCurrentEnergyMWh();
        try{
            limit.discharge(150);
            System.out.println("The system should have rejected the discharge");
        } catch(IllegalArgumentException exception){
            System.out.println("Discharge is rejected: "+ exception.getMessage());
        }
        ZonedDateTime start=LocalDateTime.of(2026,1,15,10,0).atZone(ZoneId.of("Europe/Berlin"));
        PriceInterval priceInterval= new PriceInterval(start,80.0);
        System.out.println("Start: "+ priceInterval.getStartTime());
        System.out.println("End: "+ priceInterval.getStartTime());
        System.out.println("Price: "+priceInterval.getPricePerMWh()+ "EUR/MWh");

        PriceCSVReader reader= new PriceCSVReader();
        try{
            List<PriceInterval> datesAndPrices= reader.readPrices(Path.of("data/prices.csv"));
            System.out.println("Loaded intervals: "+ datesAndPrices.size());
            PriceInterval first=datesAndPrices.get(0);
            PriceInterval last= datesAndPrices.get(datesAndPrices.size()-1);
            DateTimeFormatter formatData=DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm XXX '['VV']'");
            System.out.println("First interval starts at: "+first.getStartTime().format(formatData));
            System.out.println("First price is: "+first.getPricePerMWh()+"Eur/MWh");
            System.out.println("Last interval ends at: "+last.getEndTime().format(formatData));
            System.out.println("Last price is: "+last.getPricePerMWh()+"Eur/MWh");

            BatterySimulator financialResult =new BatterySimulator(battery);
            PriceInterval charging= datesAndPrices.get(0);
            double chargingPower= financialResult.getAvailableChargePowerMW();
            double durationInterval=0.25;
            financialResult.charge(chargingPower);
            double purchasedEnergy= chargingPower* durationInterval;
            double costChargingEUR= purchasedEnergy*charging.getPricePerMWh();
            System.out.println("Purchased energy: "+ purchasedEnergy+"MWh");
            System.out.println("The cost of charging is: "+ costChargingEUR+"EUR");
            System.out.println("The stored energy left : "+ financialResult.getCurrentEnergyMWh()+"MWh");
            PriceInterval discharging=datesAndPrices.get(1);
            double dischargingPower=financialResult.getAvailableDischargePowerMW();
            financialResult.discharge((dischargingPower));
            double soldEnergy=dischargingPower*durationInterval;
            double revenueDischargingEUR= soldEnergy*discharging.getPricePerMWh();
            double profit=revenueDischargingEUR-costChargingEUR;
            System.out.println("Sold energy: "+ soldEnergy+"MWh");
            System.out.println("Revenue is: "+ revenueDischargingEUR+"EUR");
            System.out.println("The profit after trading window is : "+ profit+"EUR");
            System.out.println("The final SOC is : "+ financialResult.getCurrentSoC()+"%");

            BatterySimulator scheduleTest= new BatterySimulator(battery);{
                List<Schedule> steps =new ArrayList<>();
                double RTE= battery.getChargeEfficiency()* battery.getDischargeEfficiency();
                double chargingPowerMW= Math.min(scheduleTest.getAvailableChargePowerMW(), battery.getMaxChargeMW()/RTE);
                double dischargingPowerMW= chargingPowerMW* RTE;
                steps.add(new Schedule(datesAndPrices.get(0),Battery.CHARGE,chargingPowerMW));
                steps.add(new Schedule(datesAndPrices.get(1),Battery.IDLE,0));
                steps.add(new Schedule(datesAndPrices.get(2),Battery.DISCHARGE,dischargingPowerMW));
                double totalPurchasedEnergyCost=0;
                double totalRevenue=0;
                double intevalDuration=0.25;

                for(Schedule step: steps){
                    scheduleTest.executeSchedule(step);
                    double energyMWh= step.getPowerMW()*intevalDuration;
                    double pricePerMwh= step.getPriceInterval().getPricePerMWh();
                    if(step.getAction()==Battery.CHARGE) {
                        totalPurchasedEnergyCost+=energyMWh*pricePerMwh;
                    } else if (step.getAction()==Battery.DISCHARGE) {
                        totalRevenue+= energyMWh*pricePerMwh;
                    }
                    System.out.println("The battery:"+step.getAction()+"| with Power:" + step.getPowerMW()+ "| SoC: "+scheduleTest.getCurrentSoC()+ "%");
                }
                double totalProfitEur= totalRevenue-totalPurchasedEnergyCost;
                System.out.println("Total cost: "+ totalPurchasedEnergyCost +" EUR");
                System.out.println("Total revenue: "+totalRevenue+" EUR");
                System.out.println("Total profit: "+ totalProfitEur+" EUR");


            }
            BatteryOptimizer optimizer=new BatteryOptimizer(battery); {
                System.out.println("The number of states is: "+ optimizer.getNumOfStates());
                System.out.println("Energy at state 0 is: "+ optimizer.getEnergyPerState(0));
                System.out.println("The charging power from state 0 to 1 is: "+ optimizer.getRequiredPowerMW(0,1));
                System.out.println("Discharging power from state 1 to 0 is: "+ optimizer.getRequiredPowerMW(1,0));
                System.out.println("Transition from state 0 to state 5 is allowed: "+ optimizer.isTransitionToNewStateIsAllowed(0,5));
                System.out.println("Financial result after charging at 100 EUR/MWh is: "+ optimizer.getTransitionProfitEUR(0,1,100));
                System.out.println("Financial result after didcharging at 100 EUR/MWh is: "+ optimizer.getTransitionProfitEUR(1,0,100));
                System.out.println("Financial result after idle perioid is: "+ optimizer.getTransitionProfitEUR(0,0,100));

                ZonedDateTime startTest=LocalDateTime.of(2026,1,1,0,0).atZone(ZoneId.of("Europe/Berlin"));
                List<PriceInterval> profits=List.of(new PriceInterval(startTest,50), new PriceInterval(startTest.plusMinutes(15),100));
               BatteryOptimizationResult result = optimizer.maximumTradingprofit(profits);
               System.out.println("The financial result is: "+result.getProfitEUR()+"EUR");
               for(Schedule step: result.getSteps())
               {
                   System.out.println("Time:"+ step.getPriceInterval().getStartTime()+"Action:"+step.getAction()+"Power: "+ step.getPowerMW());
               }

            }




        } catch (IOException exception){
            System.out.println("The file could not be read."+ exception.getMessage());
        }




    }
}


