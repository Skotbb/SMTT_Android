package edu.smccme.vgreen.smttcascobaylines;

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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class MapsActivity extends AppCompatActivity
        implements OnMapReadyCallback, ModelManager.VehicleListener, ModelManager.QueryListener {

    private GoogleMap m_map;
    private ModelManager m_mgr;
    private ArrayList<Marker> m_ferryMarkers;

    private static final int STOPS_REQUEST_ID = 1234567; // whatever

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
    }

    @Override
    protected void onDestroy() {
        // clear the markers to prevent a memory leak
        m_ferryMarkers.clear();
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

        // various CameraUpdate objects control the camera
        // see:
        // https://developers.google.com/maps/documentation/android-api/views
        m_map.moveCamera(CameraUpdateFactory.newLatLngZoom(fortGorges, 12.0f));


        // get the ports from the model
        m_mgr.startQueryForResult(this, STOPS_REQUEST_ID, ModelManager.QueryType.STOPS, null);

        // register to get vehicle updates.
        m_mgr.registerVehicleListener(this);
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
        }
        else if (id == R.id.action_map) {
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
                    for (int i=0; i<stopsArray.length(); i++) {
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
            }
        }
    }

    // VehicleListener
    @Override
    public void vehiclesChanged(ArrayList<VehicleInfo> vehicles) {
        // remove all existing ferry markers from the map
        for (int i=0; i<m_ferryMarkers.size(); i++) {
            Marker m = m_ferryMarkers.get(i);
            m.remove();
        }

        for (int i=0; i<vehicles.size(); i++) {
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
    }
}
