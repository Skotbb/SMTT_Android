package edu.smccme.vgreen.smttcascobaylines;

/**
 * Created by vgreen on 4/30/16.
 *
 * If you want other stuff added to VehicleInfo, please add it, or ask vgreen.
 */
public class VehicleInfo {
    private String m_vehicleId;
    private String m_timestamp;
    private String m_routeId;
    private double m_latitude;
    private double m_longitude;
    private int m_heading;
    private String m_destination;
    private boolean m_delayed;
    private int m_tripId;

    public VehicleInfo(
            String vehicleId,
            String timestamp,
            String routeId,
            double latitude,
            double longitude,
            int heading,
            String destination,
            boolean delayed,
            int tripId
    ) {
        m_vehicleId = vehicleId;
        m_timestamp = timestamp;
        m_routeId = routeId;
        m_latitude = latitude;
        m_longitude = longitude;
        m_heading = heading;
        m_destination = destination;
        m_delayed = delayed;
        m_tripId = tripId;
    }

    // read only


    public boolean isDelayed() {
        return m_delayed;
    }

    public String getDestination() {
        return m_destination;
    }

    public int getHeading() {
        return m_heading;
    }

    public double getLatitude() {
        return m_latitude;
    }

    public double getLongitude() {
        return m_longitude;
    }

    public String getRouteId() {
        return m_routeId;
    }

    public String getTimestamp() {
        return m_timestamp;
    }

    public int getTripId() {
        return m_tripId;
    }

    public String getVehicleId() {
        return m_vehicleId;
    }
}
