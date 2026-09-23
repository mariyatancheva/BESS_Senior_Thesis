package bess;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
public class PriceCSVReader {
    private static final ZoneId hour_zone=ZoneId.of("Europe/Berlin");
    private static final DateTimeFormatter dataFormat= DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm:ss");
    public PriceInterval parseLine(String line){
        String[] columns =line.split(";" ,-1);
        if(columns.length!=2){
            throw new IllegalArgumentException("Each row must have only fate and price");
        }
        String[] dates= columns[0].split(" - ", -1);
        if(dates.length!=2){
            throw new IllegalArgumentException("This interval must have only start and end interval");
        }
        ZonedDateTime start= LocalDateTime.parse(dates[0].trim(),dataFormat).atZone(hour_zone);
        ZonedDateTime end= LocalDateTime.parse(dates[1].trim(),dataFormat).atZone(hour_zone);
        if(!end.equals(start.plusMinutes(15))){
            throw new IllegalArgumentException("The interval must las 15minutes.");
        }
        double price= Double.parseDouble(columns[1].trim().replace(',','.'));
        return new PriceInterval(start,price);
    }
}
