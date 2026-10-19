package bess;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.HashMap;

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
                if(!record.getStartTime().equals(expectedStart)|| !record.getEndTime().equals(expectedStart.plusSeconds(900))){
                    throw new IllegalArgumentException("Invalid 15-minute interval in hour."+hourStart);
                }
                hourlyEnergyKWh+= record.getEnergyKWh();
            }
            hourlyRecords.add(new HourlyGenerationData(hourStart,hourlyEnergyKWh));
        }
        return hourlyRecords;
    }
    public record TrainingRow(Instant startTime, double energyKWh, double temperature, double cloudCoverPercentage, double radiation){
    }
    public List<TrainingRow> combineData(List<HourlyGenerationData> generation, List<WeatherRecord> weather){
        Objects.requireNonNull(generation,"Generation cannot be null");
        Objects.requireNonNull(generation,"Weather cannot be null");
        if(generation.isEmpty() || weather.isEmpty()){
            throw new IllegalArgumentException("Generation and weather data cannot be empty.");
        }
        Map<Instant, WeatherRecord> weatherByTime = new HashMap<>();
        for(WeatherRecord record:weather){
            if(weatherByTime.containsKey(record.getTime())){
                throw new IllegalArgumentException("Repeated weather time"+record.getTime());
            }
            weatherByTime.put(record.getTime(),record);
        }
        List<TrainingRow> combinedRecords=new ArrayList<>();
        for(HourlyGenerationData hour:generation){
            WeatherRecord matchingWeather= weatherByTime.get(hour.getEndTime());
            if(matchingWeather==null){
                throw new IllegalArgumentException("Missing weather data for hour ending at:" + hour.getEndTime());
            }
            combinedRecords.add(new TrainingRow(hour.getStartTime(),hour.getEnergyKWh(),matchingWeather.getTemperature(),matchingWeather.getCloudCoverPercentage(),matchingWeather.getRadiationWm2()));
        }
        return combinedRecords;
    }
}
