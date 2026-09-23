package bess;
import java.time.ZoneId;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Scanner;
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





    }
}
