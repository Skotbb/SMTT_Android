package edu.smccme.vgreen.smttcascobaylines;

import android.app.DownloadManager;
import android.content.Context;
import android.graphics.AvoidXfermode;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by vgreen on 4/30/16.
 *
 * This class keeps track of vehicle and schedule info.  Vehicle info is for ferries that are
 *  on a route, and is updated periodically (probably about every 15 seconds).  If your code
 *  is interested in knowing when the vehicles have been updated, register as a listener and
 *  implement the VehicleListener interface.
 *  See method list below.
 *
 *  Schedule info
 *  is static for now, coming out of a bunch of database tables that are hosted on the
 *  cnms275 server.
 *
 *  Schedule info is not stored in the ModelManager--it will just make a query to the
 *  database whenever somebody wants schedule info.  In order to use the query methods,
 *  you must agree to implement the ScheduleListener interface so you can be called back
 *  when the schedule info arrives from the database.
 *
 *  If you want something else added to this data model, please add it.  If you need a change
 *  to the web service (see FeedReader), please ask vgreen.
 *
 */
public class ModelManager {

    public enum QueryType {STOPS, ROUTE_SHAPES, TRIPS}
    public enum ResultType {RESULT_OK, RESULT_ERROR}

    public static final String QUERY_TRIP_DATE = "date";
    public static final String QUERY_TRIP_DEPART_PORT_ID = "departure_id";
    public static final String QUERY_TRIP_ARRIVE_PORT_ID = "arrival_id";

    public interface QueryListener {
        public void onQueryResult (int requestCode, ResultType resultCode, String jsonResult);
    }

    // Callers: pass a unique request code to startQueryForResult, and check it in onQueryResult
    // to make sure it matches.
    // See ModelTestingActivity.java for an example of usage.
    // startQueryForResult constructs URL parameters for the web service and then starts an
    // AsyncTask to handle the web service call.
    public void startQueryForResult(QueryListener requester, int requestCode, QueryType queryType, Bundle args) {
        String parameters = "";
        switch(queryType) {
            case STOPS:
                parameters = "?command=stops";
                break;
            case ROUTE_SHAPES:
                parameters = "?command=route_shapes";
                break;
            case TRIPS:
                parameters = "?command=trips";

                // pull out any more params from the Bundle, if it has been supplied
                if (args != null) {
                    String date = args.getString(QUERY_TRIP_DATE, "");
                    String dep = args.getString(QUERY_TRIP_DEPART_PORT_ID, "");
                    String arr = args.getString(QUERY_TRIP_ARRIVE_PORT_ID, "");

                    if (!date.isEmpty()) {
                        parameters += "&" + QUERY_TRIP_DATE + "=" + date;
                    }
                    if (!dep.isEmpty()) {
                        parameters += "&" + QUERY_TRIP_DEPART_PORT_ID + "=" + dep;
                    }
                    if (!arr.isEmpty()) {
                        parameters += "&" + QUERY_TRIP_ARRIVE_PORT_ID + "=" + arr;
                    }
                }


                break;
            default:
        }

        if (!parameters.isEmpty()) {
            QueryTask qt = new QueryTask(requester, requestCode, m_context);
            qt.execute(WEBSERVICE_DB_URL + parameters);
        }
        else {
            requester.onQueryResult(requestCode, ResultType.RESULT_ERROR, null);
        }

    }


    private static final String WEBSERVICE_DB_URL = "http://cnms275.smccme.edu/~valeriebgreen/Mobile_Apps/smtt.php";

    protected class QueryTask extends AsyncTask<String, Void, String> {

        private QueryListener m_requester;
        private int m_requestCode;
        private Context m_context;

        public QueryTask(QueryListener requester, int requestCode, Context context) {
            m_requester = requester;
            m_requestCode = requestCode;
            m_context = context;
        }

        @Override
        // This method happens in its own thread (not the UI thread).
        protected String doInBackground(String... params) {
            String result = "";
            try {
                FileIO fio = new FileIO(m_context);
                result = fio.downloadFile(params[0]);
            } catch (ConnectException e) {
                Log.d(ModelManager.class.toString(), e.getMessage());
            }

            return result;
        }

        @Override
        // this method happens on the UI thread.
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            m_requester.onQueryResult(m_requestCode, ResultType.RESULT_OK, s);
        }
    }




    // this should really be bundled into the QueryListener above,
    // but to make Jeremy's life easier, it will automatically update here instead.

    public interface VehicleListener {
        public void vehiclesChanged(ArrayList<VehicleInfo> vehicles);
    }

    public void registerVehicleListener(VehicleListener listener) {
        if (m_vListeners.indexOf(listener) == -1 ) {
            m_vListeners.add(listener);
        }
    }

    public void unregisterVehicleListener(VehicleListener listener) {
        int index = m_vListeners.indexOf(listener);
        if (index != -1) {
            m_vListeners.remove(index);
        }
    }

    private ArrayList<VehicleListener> m_vListeners;

    // Vehicle info is stored.  Schedule info is not.
    private ArrayList<VehicleInfo> m_vehicles;

    private Context m_context;
    private DBCommunicator m_dbc;


    // singleton
    private ModelManager(Context ctx) {
        m_vListeners = new ArrayList<VehicleListener>();
        m_vehicles = new ArrayList<VehicleInfo>();
        m_context = ctx;

        m_dbc = new DBCommunicator();
    }

    private static ModelManager s_instance = null;

    public static ModelManager getInstance(Context ctx) {
        if (s_instance == null) {
            s_instance = new ModelManager(ctx);
        }
        return s_instance;
    }


    // VehicleListener notification
    // Sochenda: changed the method from private to protected
    // to notify listeners if the vehicles update from the adapter

    protected void notifyVehicleListeners() {
        // I had an idea that it is important to give each listener a copy of the array,
        // not a reference, but now I can't remember why.
        Iterator<VehicleListener> it = m_vListeners.iterator();
        while (it.hasNext()) {
            VehicleListener vl = it.next();
            vl.vehiclesChanged(m_vehicles);
        }
    }

    protected void updateVehicleInfo(ArrayList<VehicleInfo> newList) {
        Log.d(ModelManager.class.toString(), "Updating vehicle info array on UI thread");
        m_vehicles = newList;

        // let listeners know
        notifyVehicleListeners();
    }

    // ********************************************
    // GPS and Location Helpers

    private MyLocationManager mMyLocationManager;

    private class MyLocationManager{
        private Location mMyLocation;
        private Port mMyPort;

        private PortManager mPortManager;

        private MyLocationManager(Location loc){
            mPortManager = PortManager.getInstance();

            mMyLocation = new Location(loc);
            mMyPort = getMyPort();
        }

        private Location getMyLocation() {
            return mMyLocation;
        }

        private void setMyLocation(Location myLocation) {
            mMyLocation = myLocation;
        }

        private Port getMyPort() {
            float diff=0,
                    closest=0;
            List<Port> portList = mPortManager.getPorts();
            Port port, closestPort;
            Location loc;

            loc = new Location("default");
            port = portList.get(0);
            loc.setLatitude(port.getPortLat());
            loc.setLongitude(port.getPortLon());
            diff = mMyLocation.distanceTo(loc);
            closest = diff;
            closestPort = port;

            for(int i = 1; i < portList.size(); i++){
                //Check distance to all ports
                port = portList.get(i);
                loc.setLatitude(port.getPortLat());
                loc.setLongitude(port.getPortLon());
                diff = mMyLocation.distanceTo(loc);
                //Check if it's closer than the last
                if(diff < closest){
                    closest = diff;
                    closestPort = port;
                }
            }
            return closestPort;
        }

    }

    public void createMyLocationManager(Location loc){
        if(mMyLocationManager == null){
            mMyLocationManager = new MyLocationManager(loc);
        }
    }


    public Location getMyLocation() {
        if(mMyLocationManager != null){
            return mMyLocationManager.getMyLocation();
        }

        return null;
    }

    public void setMyLocation(Location newLoc){
        if(mMyLocationManager != null){
            mMyLocationManager.setMyLocation(newLoc);
        }
    }

    public Port getMyPort(){
        if(mMyLocationManager != null){
            return mMyLocationManager.getMyPort();
        }

        return null;
    }


    // ******************************************
    // Web service communication

    // this could be moved out of here if other things end up needing it.
    protected class Route {
        private String m_id;
        private String m_name;
        private String m_color;

        public Route(String id, String name, String color) {
            m_id = id;
            m_name = name;
            m_color = color;
        }

        public String getColor() {
            return m_color;
        }

        public String getId() {
            return m_id;
        }

        public String getName() {
            return m_name;
        }
    }


    // drop this class into any place you need it...
    protected class DBCommunicator {

        protected static final String FEED_URL = "http://66.63.112.139/gtfsrt-cascobay/vehicles";
        protected static final String ROUTES_QUERY = "http://smttracker.southportland.org/bustime/api/v3/getroutes?key=b6ueFqGkcmmCPNXE8ehjFTs2H&rtpidatafeed=Casco+Bay+Lines&format=json";
        protected static final String VEHICLES_QUERY = "http://smttracker.southportland.org/bustime/api/v3/getvehicles?key=b6ueFqGkcmmCPNXE8ehjFTs2H&format=json&rt=";

        // this class will grab the real-time GTFS feed for the ferry boats
        // every 15 seconds

        protected ArrayList<Route> m_routes;

        protected Timer m_timer;

        //private FeedMessage m_feed = null;

        // During the whole lifetime of the DBCommunicator, it will be polling.
        public DBCommunicator() {
            m_routes = new ArrayList<Route>();
            Log.d(ModelManager.class.toString(), "starting DBCommunicator");
            getRoutes(); // set up the routes so we can get the vehicles that are on each route
            startTimer();
        }

        // vgreen TODO: is this a leak?  finalize() is never called because its
        // parent is static, so the DBCommunicator instance is not
        // going out of scope.  Weirdly, it is not going out of scope even when
        // the app goes away.
        // Therefore, the Timer is never being stopped.
        protected void finalize() {
            Log.d(ModelManager.class.toString(), "stopping DBCommunicator");
            stopTimer();
        }


// This fires off an AsyncTask to get the Route info.  We need this because the JSON vehicle info
        // requires us to send a list of route ids.  Yes, I know...the routes are DB,IB,PK...but
        // this is the "right" way to do it...
        // This only happens once, on startup.
        protected void getRoutes() {

            AsyncTask<String, Void, String> getRoutesTask = new AsyncTask<String, Void, String>() {
                @Override
                protected String doInBackground(String... params) {
                    String routesJSON = "";
                    try {
                        FileIO fio = new FileIO(m_context);
                        routesJSON = fio.downloadFile(params[0]);
                    } catch (ConnectException e) {
                        Log.d(ModelManager.class.toString(), e.getMessage());
                    }
                    return routesJSON;
                }

                @Override
                protected void onPostExecute(String s) {
                    super.onPostExecute(s); // necessary?  I don't think so...tbd vgreen

                    // convert the JSON into a bunch of Route objects and stash in our list.
                    try {
                        m_routes.clear();
                        JSONObject respObj = new JSONObject(s);
                        JSONObject bustimeResp = respObj.getJSONObject("bustime-response");
                        JSONArray routes = bustimeResp.getJSONArray("routes");
                        for (int i=0; i<routes.length(); i++) {
                            JSONObject route = routes.getJSONObject(i);
                            Route r = new Route(route.getString("rt"), route.getString("rtnm"), route.getString("rtclr"));
                            m_routes.add(r);
                        }
                    } catch (JSONException e) {
                        Log.d(ModelManager.class.toString(), e.getMessage());
                    }

                }
            };

            getRoutesTask.execute(ROUTES_QUERY);


        }


        // We're delaying the timer here to let the routes populate first.  At worst there will be
        // a blank vehicle list for a second, but in all likelihood that will never occur.
        protected void startTimer() {
            TimerTask tt = new TimerTask() {
                @Override
                public void run() {
                    try {
                        // create a FileIO to read from the web service to find vehicles for each route.
                        FileIO fio = new FileIO(m_context);
                        // construct the route list
                        String routeValues = "";
                        for (int i = 0; i < m_routes.size(); i++) {
                            routeValues += m_routes.get(i).getId() + ",";
                        }
                        // trim trailing comma (may not be important)
                        if (routeValues.length() > 0) {
                            routeValues = routeValues.substring(0, routeValues.length() - 1);
                        }

                        String vehicleJSON = fio.downloadFile(VEHICLES_QUERY + routeValues);

                        ArrayList<VehicleInfo> parsedVehicles = jsonToVehicles(vehicleJSON);

                        // put this on the UI thread to update the ModelManager
                        VehicleUpdateRunnable vRun = new VehicleUpdateRunnable(parsedVehicles);
                        Handler h = new Handler(Looper.getMainLooper());
                        h.post(vRun);

                    } catch (ConnectException e) {
                        Log.d(ModelManager.class.toString(), e.getMessage());
                    }
                }
            };

            // actually start the Timer here.
            if (m_timer == null) {
                m_timer = new Timer(true); // true == "isDaemon"
                int delay = 10 * 1000; // fire the first one after the routes come back
                int interval = 15 * 1000; // check every 15 seconds (15000 milliseconds).
                m_timer.schedule(tt, delay, interval);
            }

        }


        protected void stopTimer() {
            if (m_timer != null) {
                m_timer.cancel();
                m_timer = null;
            }
        }

        // helper method for converting to vehicle info array

        ArrayList<VehicleInfo> jsonToVehicles(String json) {
            ArrayList<VehicleInfo> vehicles = new ArrayList<VehicleInfo>();

            try {
                JSONObject busResp = new JSONObject(json);
                busResp = busResp.getJSONObject("bustime-response");
                JSONArray vArray = busResp.getJSONArray("vehicle");
                for (int i=0; i<vArray.length(); i++) {
                    JSONObject cur = vArray.getJSONObject(i);
                    VehicleInfo vinfo = new VehicleInfo(
                            cur.getString("vid"),
                            cur.getString("tmstmp"),
                            cur.getString("rt"),
                            Double.parseDouble(cur.getString("lat")),
                            Double.parseDouble(cur.getString("lon")),
                            Integer.parseInt(cur.getString("hdg")),
                            cur.getString("des"),
                            cur.getBoolean("dly"),
                            Integer.parseInt(cur.getString("tatripid"))
                    );
                    vehicles.add(vinfo);
                }

            } catch (JSONException e) {
                Log.d(ModelManager.class.toString(), e.getMessage());
            }

            return vehicles;
        }



        // post the vehicle info array to the UI thread.
        private class VehicleUpdateRunnable implements Runnable {

            private ArrayList<VehicleInfo> m_vehicles;

            public VehicleUpdateRunnable(ArrayList<VehicleInfo> vehicles) {
                m_vehicles = vehicles;
            }

            @Override
            public void run() {
                ModelManager.this.updateVehicleInfo(this.m_vehicles);
            }
        }

    }



}
