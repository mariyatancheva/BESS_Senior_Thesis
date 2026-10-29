package bess;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class SpeculationCSVReader {
    public SpeculationRecord parseLine(String line){
        String[] columns=line.split(";",-1);
        if(columns.length !=4){
            throw new IllegalArgumentException("Each speculation row must contain four columns");
        }
        Instant startTime=Instant.parse(columns[0].trim());
        double imbalancePrice= Double.parseDouble(columns[1].trim());
        double systemImbalance= Double.parseDouble(columns[2].trim());
        double dayAheadPrice= Double.parseDouble(columns[3].trim());
        return new SpeculationRecord(startTime,imbalancePrice,systemImbalance,dayAheadPrice);
    }
    public List<SpeculationRecord> readSpeculation(Path path)
        throws IOException{
        List<String> lines=Files.readAllLines(path, StandardCharsets.UTF_8);
        if(lines.isEmpty()){
            throw new IOException("The speculation file is empty.");
        }
        String header= lines.get(0);
        if(header.startsWith("\uFEFF")){
            header=header.substring(1);
        }
        String[] headers=header.split(";",-1);
        if(headers.length !=4 || !headers[0].trim().equals("Start time") || !headers[1].trim().equals("imbalancePriceEURPerMWh")|| !headers[2].trim().equals("systemImbalanceMWh")|| !headers[3].trim().equals("dayAheadPriceEURPerMWh")){
            throw new IOException("Unexpected speculation CSV columns.");
        }
        List<SpeculationRecord> records=new ArrayList<>();
        for(int i=1;i<lines.size();i++){
            String line=lines.get(i);
            try{
                if(line.isBlank()){
                    throw new IllegalArgumentException("Empty row.");
                }
                SpeculationRecord record= parseLine(line);
                if(!records.isEmpty()){
                    SpeculationRecord previous=records.get(records.size()-1);
                    if(!record.getStartTime().equals(previous.getEndTime())){
                        throw new IllegalArgumentException("Missing, repeated or out-of-order interval.");
                    }
                }
                records.add(record);
            } catch (IllegalArgumentException | DateTimeException exception){
                throw new IOException("Invalid speculation data at line "+(i+1)+ ":"+ exception.getMessage(),exception);
            }
        }
        if(records.isEmpty()){
            throw new IOException("The speculation file contains no data records.");
        }
        return records;
    }
}
