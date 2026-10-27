package bess;
import javax.management.InstanceNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SolarForecast {
    public static void main (String[] args){
        ZoneId zone= ZoneId.of("Europe/Sofia");
        LocalDate selectedDate=LocalDate.of(2026,9,3);
        Instant dayStart=selectedDate.atStartOfDay(zone).toInstant();
        Instant dayEnd=selectedDate.plusDays(1).atStartOfDay(zone).toInstant();
        try {
            GenerationCSVReader generationReader=new GenerationCSVReader();
            WeatherCSVReader weatherReader=new WeatherCSVReader();
            List<GenerationRecord> generation=generationReader.readGeneration(Path.of("data/generation.csv"));
            List<WeatherRecord> weather=weatherReader.readWeather(Path.of("data/weather.csv"));
            List<GenerationRecord> pastGeneration=new ArrayList<>();
            for(GenerationRecord record:generation){
                if(!record.getEndTime().isAfter(dayStart)){
                    pastGeneration.add(record);
                }
            }
            ForecastDataPreparation preparation=new ForecastDataPreparation();
            List<HourlyGenerationData> hourlyGeneration=preparation.aggregateHourly(pastGeneration);
            List<ForecastDataPreparation.TrainingRow> trainingRows=preparation.combineData(hourlyGeneration,weather);
            double[][] features=new double[trainingRows.size()][3];
            double[] targets=new double[trainingRows.size()];
            for(int i=0;i<trainingRows.size();i++){
                ForecastDataPreparation.TrainingRow row= trainingRows.get(i);
                features[i][0]=row.temperature();
                features[i][1]=row.cloudCoverPercentage();
                features[i][2]=row.radiation();
                targets[i]= row.energyKWh();
            }
            Map<Instant,WeatherRecord> weatherByTime=new HashMap<>();
            for(WeatherRecord record : weather){
                if(weatherByTime.putIfAbsent(record.getTime(),record)!=null){
                    throw new IllegalArgumentException("Repeated weather timestamp: "+record.getTime());
                }
            }
            List<Instant> forecastStarts=new ArrayList<>();
            List<WeatherRecord> forecastWeather=new ArrayList<>();
            for(Instant start=dayStart;start.isBefore(dayEnd);start=start.plusSeconds(3600)){
                Instant end=start.plusSeconds(3600);
                WeatherRecord record=weatherByTime.get(end);
                if (record==null){
                    throw new IllegalArgumentException("Missing weather data for hour ending at "+ end);
                }
                forecastStarts.add(start);
                forecastWeather.add(record);

            }
            M5P model=new M5P(24,6,15.0);
            System.out.println("Training records: "+trainingRows.size());
            System.out.println("Training M5P...");
            model.fit(features,targets);
            double[] predictions=new double[forecastStarts.size()];
            double dailyEnergy=0;
            for(int i=0;i<forecastWeather.size();i++) {
                WeatherRecord record = forecastWeather.get(i);
                double[] inputs = {record.getTemperature(), record.getCloudCoverPercentage(), record.getRadiationWm2()};
                double rawPrediction = model.predict(inputs);
                if (!Double.isFinite(rawPrediction)) {
                    throw new IllegalStateException("Prediction is not finite.");}

                    predictions[i] = Math.max(0.0, rawPrediction);
                    dailyEnergy += predictions[i];
                }

            if(!Double.isFinite(dailyEnergy)){
                throw new IllegalStateException("Daily predicted energy is not finite.");
            }
            DateTimeFormatter format= DateTimeFormatter.ofPattern("dd.MM.uuuu HH:mm XXX");
            System.out.println();
            System.out.println("Solar generation forecast: "+selectedDate);
            System.out.println("Time Zone: Europe/Sofia");
            System.out.println("Start | End | Predicted Energy");
            for(int i=0; i<forecastStarts.size();i++){
                ZonedDateTime start=forecastStarts.get(i).atZone(zone);
                ZonedDateTime end=forecastStarts.get(i).plusSeconds(3600).atZone(zone);
                System.out.printf("%s | %s | %.2f kWh%n",start.format(format),end.format(format),predictions[i]);
            }
            System.out.println();
            System.out.println("Forecast hours: "+predictions.length);
            System.out.printf("Total predicted generation: %.2f kWh%n",dailyEnergy);

        } catch (IOException | IllegalArgumentException | IllegalStateException exception){
            System.out.println("Solar forecast failed: "+exception.getMessage());
        }
    }

}
