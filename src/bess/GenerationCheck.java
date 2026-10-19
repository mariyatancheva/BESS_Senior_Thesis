package bess;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

public class GenerationCheck {
    public static void main(String[] args){
        GenerationCSVReader reader= new GenerationCSVReader();
        try{
            List<GenerationRecord> records=reader.readGeneration(Path.of("data/generation.csv"));
            System.out.println("Loaded intervals:"+records.size());
            GenerationRecord first=records.get(0);
            GenerationRecord last=records.get(records.size()-1);
            System.out.println("First start UTC: "+ first.getStartTime());
            System.out.println("Last end UTC: "+ last.getEndTime());
            ZoneId zone=ZoneId.of("Europe/Sofia");
            LocalDate autumnDate=LocalDate.of(2025,10,26);
            LocalDate springDate=LocalDate.of(2026,3,29);
            int autumnCount=0;
            int springCount=0;
            for(GenerationRecord record : records){
                LocalDate localDate=record.getStartTime().atZone(zone).toLocalDate();
                if(localDate.equals(autumnDate)){
                    autumnCount++;
                }
                if(localDate.equals(springDate)){
                    springCount++;
                }
            }
            System.out.println("26 October intervals: "+autumnCount);
            System.out.println("29 March intervals: "+springCount);
            if(autumnCount !=100 || springCount !=92){
                throw new IllegalStateException("Unexcpeted interval count n a clock chang day");
            }
            ForecastDataPreparation preparation=new ForecastDataPreparation();
            List<HourlyGenerationData> hourlyRecords= preparation.aggregateHourly(records);
            double originalEnergy=0;
            for(GenerationRecord record:records){
                originalEnergy+= record.getEnergyKWh();
            }
            double aggregatedEnergy=0;
            for(HourlyGenerationData record : hourlyRecords){
                aggregatedEnergy+=record.getEnergyKWh();
            }
            System.out.println("Hourly records: "+hourlyRecords.size());
            System.out.printf("Energy before aggregation: %.2f kWh%n ",originalEnergy);
            System.out.printf("Energy after aggregation: %.2f kWh%n ",aggregatedEnergy);
            if(hourlyRecords.size() !=records.size()/4){
                throw new IllegalArgumentException("Unexpected hourly record count. ");
            }
            double tolerance= Math.max(1e-6,Math.abs(originalEnergy)*1e-12);
            if(Math.abs(originalEnergy-aggregatedEnergy)>tolerance){
                throw new IllegalStateException("Total energy charges during aggregation. ");
            }
            System.out.println("Hourly aggregation check passed.");
            WeatherCSVReader weatherReader = new WeatherCSVReader();
            List<WeatherRecord> weatherRecords=weatherReader.readWeather(Path.of("data/weather.csv"));
            List<ForecastDataPreparation.TrainingRow> trainingRows=preparation.combineData(hourlyRecords,weatherRecords);
            if(trainingRows.size() != hourlyRecords.size()){
                throw new IllegalStateException("Some generation houts were lost during matching process.");
            }
            for(int i=0;i<trainingRows.size();i++){
                ForecastDataPreparation.TrainingRow row=trainingRows.get(i);
                HourlyGenerationData original =hourlyRecords.get(i);
                if(!row.startTime().equals(original.getStartTime())||Double.compare(row.energyKWh(), original.getEnergyKWh()) !=0){
                    throw new IllegalStateException("Generation data changed during matching");
                }
            }
            System.out.println("Combined records: "+ trainingRows.size());
            System.out.println("First combined hour UTC: "+ trainingRows.get(0).startTime());
            System.out.println("Last combined hour UTC: "+ trainingRows.get(trainingRows.size()-1).startTime());
            System.out.println("Data matching check passed.");
            System.out.println("Generation data check passed.");
        } catch (IOException exception){
            System.out.println("Generation data check failed "+ exception.getMessage());
        }
    }
}
