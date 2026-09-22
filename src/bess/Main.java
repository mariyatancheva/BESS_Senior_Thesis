package bess;
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
        System.out.println( "Battery capacity:"+ battery.getBatteryCapacityMWh()+ "MWh");
        System.out.println( "Max charging power:"+ battery.getMaxChargeMW()+ "MW");
        System.out.println( "Max discharging power:"+ battery.getMaxDischargeMW()+ "MW");
        System.out.println( "SoC limits:"+ battery.getMinSoC()+ "% and "+ battery.getMaxSoC()+ "%");
        System.out.println( "Usable energy within SoC boundaries:"+ battery.getUsableEnergyMWh()+ "MWh");


    }
}
