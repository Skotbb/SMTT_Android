package edu.smccme.vgreen.smttcascobaylines;

/**
 * Created by Scott on 5/1/2016.
 */
public class Port {
    private int portId;
    private String shortLabel,
            fullLabel;
    double portLat,
        portLon;

    public Port(int portId, String shortLabel, String fullLabel, double portLat, double portLon) {
        this.portId = portId;
        this.shortLabel = shortLabel;
        this.fullLabel = fullLabel;
        this.portLat = portLat;
        this.portLon = portLon;
    }

    public int getPortId() {
        return portId;
    }

    public String getShortLabel() {
        return shortLabel;
    }

    public String getFullLabel() {
        return fullLabel;
    }

    public double getPortLat() {
        return portLat;
    }

    public double getPortLon() {
        return portLon;
    }
}
