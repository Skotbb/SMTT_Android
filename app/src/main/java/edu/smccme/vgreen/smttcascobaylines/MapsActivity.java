package edu.smccme.vgreen.smttcascobaylines;

import android.graphics.Color;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MapsActivity extends AppCompatActivity
        implements OnMapReadyCallback, ModelManager.VehicleListener, ModelManager.QueryListener {

    private GoogleMap m_map;
    private ModelManager m_mgr;
    private ArrayList<Marker> m_ferryMarkers;
    private ArrayList<VehicleInfo> m_activeFerries;
    private HashMap<String, ArrayList<FerryTrip>> m_ferryTrips;

    private static final int STOPS_REQUEST_ID = 1234567; // whatever
    private static final int SHAPES_REQUEST_ID = 8675309; //Jenny, I got your number
    private static final int TRIPS_REQUEST_ID = 1011;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        m_mgr = ModelManager.getInstance(getApplicationContext());
        m_ferryMarkers = new ArrayList<>();
        m_activeFerries = new ArrayList<>();
        m_ferryTrips = new HashMap<>();
    }

    @Override
    protected void onDestroy() {
        // clear the markers to prevent a memory leak
        m_ferryMarkers.clear();
        m_map.clear();
        super.onDestroy();
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        m_map = googleMap;

        // allow zoom controls and gestures

        //  move the camera
        LatLng fortGorges = new LatLng(43.662498, -70.221609);

        //Set all the ports to be included in the Map Bounds
        PortManager mPorts = PortManager.getInstance();
        List<Port> mPortList = new ArrayList<>();
        mPortList = mPorts.getPorts();
        //Build LatLng objects from ports
        List<LatLng> mPortLatLng = new ArrayList<>();
        for (Port port : mPortList) {
            LatLng portLoc = new LatLng(port.getPortLat(), port.getPortLon());
            mPortLatLng.add(portLoc);
        }

        LatLngBounds mMapBounds =
                LatLngBounds.builder().
                        include(mPortLatLng.get(0))
                        .include(mPortLatLng.get(1))
                        .include(mPortLatLng.get(2))
                        .include(mPortLatLng.get(3))
                        .include(mPortLatLng.get(4))
                        .include(mPortLatLng.get(5))
                        .include(mPortLatLng.get(6))
                        .include(mPortLatLng.get(7))
                        .include(mPortLatLng.get(8))
                        .build();

        // various CameraUpdate objects control the camera
        // see:
        // https://developers.google.com/maps/documentation/android-api/views
        m_map.moveCamera(CameraUpdateFactory.newLatLngBounds(mMapBounds, 50));

        // get the ports from the model
        m_mgr.startQueryForResult(this, STOPS_REQUEST_ID, ModelManager.QueryType.STOPS, null);

        // register to get vehicle updates.
        m_mgr.registerVehicleListener(this);

        //getTripsInProgress();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_schedule) {
            return true;
        } else if (id == R.id.action_map) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    // QueryListener


    @Override
    public void onQueryResult(int requestCode, ModelManager.ResultType resultCode, String jsonResult) {
        if (resultCode == ModelManager.ResultType.RESULT_OK) {
            if (requestCode == STOPS_REQUEST_ID) {
                Log.d(MapsActivity.class.toString(), "STOPS json: " + jsonResult);

                // TODO: show each stop on the map
                try {
                    JSONArray stopsArray = new JSONArray(jsonResult);
                    for (int i = 0; i < stopsArray.length(); i++) {
                        JSONObject obj = stopsArray.getJSONObject(i);
                        LatLng port = new LatLng(Double.parseDouble(obj.getString("stop_lat")), Double.parseDouble(obj.getString("stop_lon")));
                        m_map.addMarker(new MarkerOptions()
                                .position(port)
                                .title(obj.getString("stop_name"))
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.btn_circle_selected))
                        );

                    }
                } catch (JSONException e) {
                    Log.d(MapsActivity.class.toString(), e.getMessage());
                }
            }//Query for STOPS
            if (requestCode == TRIPS_REQUEST_ID) {
                Log.d(MapsActivity.class.toString(), "TRIPS json :" + jsonResult);

                try {
                    JSONArray tripsArray = new JSONArray(jsonResult);
                    if (!m_activeFerries.isEmpty()) {
                        for (int j = 0; j < m_activeFerries.size(); j++) { //For each Ferry
                            for (int i = 0; i < tripsArray.length(); i++) {
                                JSONObject obj = tripsArray.getJSONObject(i);
                                String routeId = obj.getString("route_id");
                                //Check if its trip matches TripOBJ
                                if (m_activeFerries.get(j).getRouteId().equalsIgnoreCase(routeId)) {
                                    //if it does, and we don't have an active trip
                                    //for it, add it to the HashMap.
                                    if (!m_ferryTrips.containsKey(routeId)) {
                                        ArrayList<FerryTrip> temp = new ArrayList<>();
                                        temp.add(new FerryTrip(obj));
                                        m_ferryTrips.put(routeId, temp);
                                    }else{
                                        m_ferryTrips.get(routeId).add(new FerryTrip(obj));
                                    }
                                }
                            }


                        }
                    }
                } catch (JSONException e) {
                    Log.d(MapsActivity.class.toString(), e.getMessage());
                }
            }// Query for TRIPS
            if (requestCode == SHAPES_REQUEST_ID) {
                Log.d(MapsActivity.class.toString(), "SHAPES json: " + jsonResult);

                try {
                    JSONArray shapesArray = new JSONArray(jsonResult);
                    List<LatLng> pointList = new ArrayList<>();

                    //if shape Object matches active trip shape_id
                    for (Map.Entry<String, ArrayList<FerryTrip>> entry : m_ferryTrips.entrySet()) {
                        String key = entry.getKey();
                        ArrayList<FerryTrip> current = m_ferryTrips.get(key);
                        for(FerryTrip curTrip : current) {
                            for (int i = 0; i < shapesArray.length(); i++) {//Check for each shape in array
                                JSONObject obj = shapesArray.getJSONObject(i);
                                //if FerryTrip shapeId equals current shape object
                                //add it to the array
                                if(obj.getString("shape_id") != null) {
                                    String shapeId = obj.getString("shape_id");
                                }
                                if (curTrip.getShapeId().equalsIgnoreCase(obj.getString("shape_id"))) {
                                    LatLng shapePoint = new LatLng(
                                            Double.parseDouble(obj.getString("shape_pt_lat")),
                                            Double.parseDouble(obj.getString("shape_pt_lon")));

                                    pointList.add(shapePoint);
                                }
                            }
                        }
                    }
                    PolylineOptions shapeOptions = new PolylineOptions()
                            .addAll(pointList)
                            .width(3)
                            .color(Color.BLUE);
                    m_map.addPolyline(shapeOptions);

                } catch (JSONException e) {
                    Log.d(MapsActivity.class.toString(), e.getMessage());
                }
            }//Query for SHAPES
        }
    }

    // VehicleListener
    @Override
    public void vehiclesChanged(ArrayList<VehicleInfo> vehicles) {
        // remove all existing ferry markers from the map
        ArrayList<Marker> testList = new ArrayList<>();
        testList = m_ferryMarkers;
        for (int i = 0; i < m_ferryMarkers.size(); i++) {
            Marker m = m_ferryMarkers.get(i);
            m.remove();
        }
        m_activeFerries = vehicles;
        for (int i = 0; i < vehicles.size(); i++) {
            VehicleInfo vi = vehicles.get(i);
            Log.d(MapsActivity.class.toString(), "adding vehicle " + vi.getVehicleId());
            LatLng boatCoords = new LatLng(vi.getLatitude(), vi.getLongitude());
            Marker m = m_map.addMarker(new MarkerOptions()
                    .position(boatCoords)
                    .title(vi.getVehicleId())  // ?
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ferry))
            );
            m_ferryMarkers.add(m);
        }
        //Check if the lists are still the same
        m_activeFerries = vehicles;
        getTripsInProgress();

    }

    protected void getTripsInProgress() {
        Bundle bundle = new Bundle();
        bundle.putString(ModelManager.QUERY_TRIP_DATE, "20160516");
        m_mgr.startQueryForResult(this, TRIPS_REQUEST_ID, ModelManager.QueryType.TRIPS, bundle);

        // get the shapes from the model
        m_mgr.startQueryForResult(this, SHAPES_REQUEST_ID, ModelManager.QueryType.ROUTE_SHAPES, null);
    }
}
