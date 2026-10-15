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
            System.out.println("Generation data check passed.");
        } catch (IOException exception){
            System.out.println("Generation data check failed "+ exception.getMessage());
        }
    }
}
