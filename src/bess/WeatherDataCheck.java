package bess;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class WeatherDataCheck {
    public static void main(String[] args){
        WeatherCSVReader reader= new WeatherCSVReader();
        try {
            List<WeatherRecord> records=reader.readWeather(Path.of("data/weather.csv"));
            if(records.isEmpty()){
                throw new IOException("No weather records are loaded. ");
            }
            WeatherRecord first=records.get(0);
            WeatherRecord last=records.get(records.size()-1);
            System.out.println("Loaded weather records are: "+ records.size());
            System.out.println("First start time is : "+ first.getTime());
            System.out.println("Last time is : "+ last.getTime());
            System.out.println("First tempereature is : "+ first.getTemperature());
            System.out.println("First radiation is : "+ first.getRadiationWm2());


        } catch (IOException exception){
            System.out.println("Weather data check failed."+ exception.getMessage());
        }
    }
}
