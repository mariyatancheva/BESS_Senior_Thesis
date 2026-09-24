package bess;

public class TradingResults {
    private void validateInputs(double energyMWh, double pricePerMWh){
        if(!Double.isFinite(energyMWh)|| energyMWh<0){
            throw new IllegalArgumentException("Energy must be positive number");
        }
        if(!Double.isFinite(pricePerMWh)){
            throw new IllegalArgumentException("Price must be finite number");
        }
    }
    public double calculalteChargingCost(double purchasedEnergy, double pricePerMWhEUR) {
        validateInputs(purchasedEnergy, pricePerMWhEUR);
        return purchasedEnergy * pricePerMWhEUR;
    }
    public double calculateRevenue(double soldEnergy, double pricePerMWhEUR){
        validateInputs(soldEnergy, pricePerMWhEUR);
        return soldEnergy*pricePerMWhEUR;
    }





}
