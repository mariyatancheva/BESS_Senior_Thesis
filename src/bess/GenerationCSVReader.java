package bess;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;

public class GenerationCSVReader {
    private static final ZoneId timeZone=ZoneId.of("Europe/Sofia");
    private static final  DateTimeFormatter startFormat=DateTimeFormatter.ofPattern("dd.MM.uuuu HH:mm").withResolverStyle(ResolverStyle.STRICT);
    private static final  DateTimeFormatter endFormat=DateTimeFormatter.ofPattern("dd.MM.uuuu HH:mm").withResolverStyle(ResolverStyle.STRICT);
    public GenerationRecord parseLine(String line){
        String[] columns=line.split(";",-1);
        if(columns.length !=2){
            throw new IllegalArgumentException("Each row must coontain a time interval and generation data.");
        }
        String[] interval =columns[0].split("-",-1);
        if(interval.length !=2){
            throw new IllegalArgumentException("The interval must have only start and end");
        }
        String startText=interval[0].trim();
        char marker ='\0';
        if(startText.endsWith("A")||startText.endsWith("B")){
            marker=startText.charAt(startText.length()-1);
            startText=startText.substring(0,startText.length()-1);
        }
        LocalDateTime localStart=LocalDateTime.parse(startText,startFormat);
        LocalTime localEnd=LocalTime.parse(interval[1].trim(),endFormat);
        if(!localEnd.equals(localStart.plusMinutes(15).toLocalTime())){
            throw new IllegalArgumentException("The interval must last 15 minutes.");
        }
        if(localStart.getMinute() %15 !=0){
            throw new IllegalArgumentException("Intervals must start at 00,15,30 or 45.");
        }
        List<ZoneOffset> offsets=timeZone.getRules().getValidOffsets(localStart);
        if(offsets.isEmpty()){
            throw new IllegalArgumentException("The start time does not exists becasue of the clock change,");
        }
        ZoneOffset offset;
        if(offsets.size()==2){
            if(marker=='A'){
                offset=offsets.get(0);
            }else if(marker=='B'){
                offset=offsets.get(1);
            }else {
                throw new IllegalArgumentException("The repeated interval must be market with A or B.");
            }
            }else { if(marker != '\0') {
                throw new IllegalArgumentException("A and B should be allowed only in repeated hours.");
        }
        offset=offsets.get(0);

            }
        Instant startTime= localStart.toInstant(offset);
        double energyInKWh=Double.parseDouble(columns[1].trim().replace(',','.'));
        return new GenerationRecord(startTime, energyInKWh);
        }
        public List<GenerationRecord> readGeneration(Path path)
            throws IOException{
        List<String> lines=Files.readAllLines(path, StandardCharsets.UTF_8);
        List<GenerationRecord> records=new ArrayList<>();
        for(int i=0; i<lines.size();i++){
            String line =lines.get(1);
            if(i==0 && line.startsWith("\uFEFF")){
                line=line.substring(1);
            }
            try{
                if(line.isBlank()){
                    throw new IllegalArgumentException("Empty row.");
                }
                GenerationRecord record=parseLine(line);
                if(!records.isEmpty()){
                    GenerationRecord previous=records.get(records.size()-1);
                    if(!record.getStartTime().equals(previous.getEndTime())){
                        throw new IllegalArgumentException("Missing or repeated intervals.");
                    }
                }
                records.add(record);
            } catch (IllegalArgumentException | DateTimeException exception){
                throw new IOException("Invalid generation data in line"+ (i+1)+ ":"+ exception.getMessage(),exception);
            }
        }
        if(records.isEmpty()){
            throw new IOException("The generation file is empty.");
        }
        return records;
    }

}
