package lvr.sidnapp;

/**
 * Created by Vasil on 03.02.2017.
 */

public class BeaconData {
    private int beaconIndex;
    private double peakFactor;
    private int peakPosition;

    public BeaconData(int beaconIndex, int peakPosition, double peakFactor) {
        this.beaconIndex = beaconIndex;
        this.peakPosition = peakPosition;
        this.peakFactor = peakFactor;
    }

    public int getBeaconIndex(){
        return this.beaconIndex;
    }

    public double getPeakFactor() {
        return this.peakFactor;
    }

    public int getPeakPosition() {
        return this.peakPosition;
    }
}
