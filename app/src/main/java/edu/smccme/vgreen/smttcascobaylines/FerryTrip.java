package edu.smccme.vgreen.smttcascobaylines;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by vgreen on 4/30/16.
 *
 * This is the info that comes back from a "trips" query to the web service.  If you need additional
 * info from the web service, please talk to vgreen.
 *
 */
public class FerryTrip {
    private int m_tripId;
    //private int m_serviceId;
    private String m_date; // change this to a Date if you like
    private String m_routeId;
    private String m_shapeId;
    private ArrayList<FerryStop> m_stops;

    public static String getFerryTripId(JSONObject jsObj) throws JSONException {
        return jsObj.getString("trip_id");
    }
    public static String getFerryTripDate(JSONObject jsObj) throws JSONException {
        return jsObj.getString("date");
    }

    // constructs itself from JSON by...MAGIC...
    public FerryTrip(JSONObject jsObj) {
        try {
            m_tripId = Integer.parseInt(jsObj.getString("trip_id"));
            //m_serviceId = Integer.parseInt(jsObj.getString("service_id"));
            m_date = jsObj.getString("date");
            m_routeId = jsObj.getString("route_id");
            m_shapeId = jsObj.getString("shape_id");
        } catch(JSONException e) {
            Log.d(FerryTrip.class.toString(), e.getMessage());
        }

        m_stops = new ArrayList<FerryStop>();  // see if you need this to be a HashMap instead...
    }

    public void addStopFromJSON(JSONObject jsObj) {
        try {
            FerryStop fs = new FerryStop(jsObj);
            m_stops.add(fs);
        } catch(JSONException e) {
            Log.d(FerryTrip.class.toString(), e.getMessage());
        }

    }

    public String getShapeId() {
        return m_shapeId;
    }

    public class FerryStop {
        private int m_stopId;
        private String m_stopSequence;
        private String m_departureTime;

        public FerryStop(JSONObject jsObj) throws JSONException {
            m_stopId = Integer.parseInt(jsObj.getString("stop_id"));
            m_stopSequence = jsObj.getString("stop_sequence");
            m_departureTime = jsObj.getString("departure_time");
        }

        public String getDepartureTime() {
            return m_departureTime;
        }

        public int getStopId() {
            return m_stopId;
        }

        public String getStopSequence() {
            return m_stopSequence;
        }
    }

}
