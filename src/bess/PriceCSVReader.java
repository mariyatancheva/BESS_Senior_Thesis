package bess;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class PriceCSVReader {
    private static final ZoneId timeZone=ZoneId.of("Europe/Berlin");
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
        ZonedDateTime start= LocalDateTime.parse(dates[0].trim(),dataFormat).atZone(timeZone);
        ZonedDateTime end= LocalDateTime.parse(dates[1].trim(),dataFormat).atZone(timeZone);
        if(!end.equals(start.plusMinutes(15))){
            throw new IllegalArgumentException("The interval must las 15minutes.");
        }
        double price= Double.parseDouble(columns[1].trim().replace(',','.'));
        return new PriceInterval(start,price);
    }
    public List<PriceInterval> readPrices(Path pathFile) throws IOException{
        List<String> lines=Files.readAllLines(pathFile, StandardCharsets.UTF_8);
        List<PriceInterval> datesAndPrices=new ArrayList<>();

        for(int i=0;i<lines.size();i++){
            String line= lines.get(i);
            if(i==0 && line.startsWith("\uFEFF")){
            line=line.substring(1);}
            if(line.isBlank()){
                PriceInterval previousLine = datesAndPrices.get(datesAndPrices.size()-1);
                throw new IllegalArgumentException("There is empty row after" + previousLine.getStartTime().format(dataFormat));
            }
            try {
                    PriceInterval interval = parseLine(line);
                    if(!datesAndPrices.isEmpty()) {
                        PriceInterval previous = datesAndPrices.get(datesAndPrices.size() - 1);

                        if (!interval.getStartTime().toInstant().equals(previous.getEndTime().toInstant())) {
                            throw new IllegalArgumentException("There is a missing or repeated interval");
                        }
                    }
                    datesAndPrices.add(interval);

            } catch(IllegalArgumentException exception){
                PriceInterval prevLine = datesAndPrices.get(datesAndPrices.size()-1);
                throw new IllegalArgumentException("There is an invalid row after"+ prevLine.getStartTime().format(dataFormat),exception);
            }
            catch (java.time.DateTimeException exception){
                PriceInterval preLine=datesAndPrices.get(datesAndPrices.size()-1);
                throw new IllegalArgumentException("There is invalid date or time after this line"+ preLine.getStartTime().format(dataFormat),exception);
            }
        }
        if (datesAndPrices.isEmpty()){
            throw new IllegalArgumentException("The price list/csv file is empty.");
        }

        return datesAndPrices;


    }
}
