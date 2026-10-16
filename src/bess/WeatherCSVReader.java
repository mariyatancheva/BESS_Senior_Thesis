package bess;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

public class WeatherCSVReader {
    public WeatherRecord parseLine(String line){
        String[] columns=line.split(",",-1);
        if(columns.length !=4){
            throw new IllegalArgumentException("Each weather row must have four columns. ");
        }
        LocalDateTime dateTime=LocalDateTime.parse(columns[0].trim());
        Instant time= dateTime.toInstant(ZoneOffset.UTC);
        double temperature=Double.parseDouble(columns[1].trim());
        double cloudCoverPercentage=Double.parseDouble(columns[2].trim());
        double radiation=Double.parseDouble(columns[3].trim());
        return new WeatherRecord(time,temperature,cloudCoverPercentage,radiation);
    }
    public List<WeatherRecord> readWeather(Path path)
        throws IOException{
            List<String> lines= Files.readAllLines(path, StandardCharsets.UTF_8);
            if(lines.size()<5){
                throw new IOException("The weather file is empty. ");
            }
            String[] headers=lines.get(3).split(",",-1);
            if(headers.length !=4|| !headers[0].trim().equals("time")|| !headers[1].trim().equals("temperature_2m (°C)")|| !headers[2].trim().equals("cloud_cover (%)") || !headers[3].trim().equals("shortwave_radiation (W/m²)")){
                throw new IOException("Unexpected columns");
            }
            List<WeatherRecord> records=new ArrayList<>();
            for(int i=4; i<lines.size();i++){
                String line=lines.get(i);
                try{
                    if(line.isBlank()){
                        throw new IllegalArgumentException("Empty row.");
                    }
                    WeatherRecord record=parseLine(line);
                    if(!records.isEmpty()){
                        WeatherRecord previous= records.get(records.size()-1);
                        Instant expectedTime =previous.getTime().plusSeconds(3600);
                        if(!record.getTime().equals(expectedTime)){
                            throw new IllegalArgumentException("Missing or repeated historical weather hourly data. ");
                        }
                    }
                    records.add(record);
                } catch (IllegalArgumentException | DateTimeException exception){
                    throw new IOException("There is invalid data at line "+ (i+1) + ": "+ exception.getMessage(), exception);
                }
            }
            return records;
        }
    }

