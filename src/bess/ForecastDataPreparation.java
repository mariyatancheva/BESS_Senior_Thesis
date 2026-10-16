package bess;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ForecastDataPreparation {
    public List<HourlyGenerationData> aggregateHourly(List<GenerationRecord> records){
        Objects.requireNonNull(records,"Generation recorods cannot be null");
        if(records.isEmpty()){
            throw new IllegalArgumentException("Generatio records cannot be empty. ");
        }
        if (records.size()%4!=0){
            throw new IllegalArgumentException("There is a missing interval.");
        }
        List<HourlyGenerationData> hourlyRecords= new ArrayList<>();
        for(int i=0; i<records.size(); i+=4){
            Instant hourStart= records.get(i).getStartTime();
            if(!hourStart.equals(hourStart.truncatedTo(ChronoUnit.HOURS))){
                throw new IllegalArgumentException("Each group must start on a whole hour. "+ hourStart);
            }
            if(!hourlyRecords.isEmpty()){
                HourlyGenerationData previous=hourlyRecords.get(hourlyRecords.size()-1);
                if(!hourStart.equals(previous.getEndTime())){
                    throw new IllegalArgumentException("Missing or repeated solar generation hour. ");
                }
            }
            double hourlyEnergyKWh=0;
            for(int j=0; j<4; j++){
                GenerationRecord record=records.get(i+j);
                Instant expectedStart=hourStart.plusSeconds(j*900L);
                if(!record.getStartTime().equals(expectedStart)|| !record.getStartTime().equals(expectedStart.plusSeconds(900))){
                    throw new IllegalArgumentException("Invalid 15-minute interval in hour."+hourStart);
                }
                hourlyEnergyKWh+= record.getEnergyKWh();
            }
            hourlyRecords.add(new HourlyGenerationData(hourStart,hourlyEnergyKWh));
        }
        return hourlyRecords;
    }
}
