package edu.smccme.vgreen.smttcascobaylines;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public class ModelTestingActivity extends AppCompatActivity implements ModelManager.VehicleListener, ModelManager.QueryListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_model_testing);


        // get the data model going...
        ModelManager mMgr = ModelManager.getInstance(getApplicationContext());

        // register for vehicle updates
        mMgr.registerVehicleListener(this);
        // test the stops, route shapes, and a specific set of trips
        mMgr.startQueryForResult(this, 1234567, ModelManager.QueryType.STOPS, null);
        mMgr.startQueryForResult(this, 99999999, ModelManager.QueryType.ROUTE_SHAPES, null);

        // for a trip, you can optionally specify any of:
        // date (in format YYYYMMDD)
        // departure port (by numeric id)
        // arrival port (by numeric id)

        // say I want to get really specific:
        // show all the trips on 2016-05-02 between Portland and Great Diamond Island.
        Bundle b = new Bundle();
        b.putString(ModelManager.QUERY_TRIP_DATE, "20160502");
        b.putString(ModelManager.QUERY_TRIP_DEPART_PORT_ID, "2"); // vgreen note: get the stops first to see all ids
        b.putString(ModelManager.QUERY_TRIP_ARRIVE_PORT_ID, "4"); // ditto

        mMgr.startQueryForResult(this, 246800, ModelManager.QueryType.TRIPS, b);

    }

    @Override
    public void vehiclesChanged(ArrayList<VehicleInfo> vehicles) {
        for (int i=0; i<vehicles.size(); i++) {
            VehicleInfo vi = vehicles.get(i);
            Log.d(ModelTestingActivity.class.toString(), "received data for: " + vi.getVehicleId());
        }
    }

    @Override
    public void onQueryResult(int requestCode, ModelManager.ResultType resultCode, String jsonResult) {
        if (resultCode == ModelManager.ResultType.RESULT_OK) {
            if (requestCode == 1234567) {
                // do something awesome with the json data here
                Log.d(ModelTestingActivity.class.toString(), "STOPS: " + jsonResult);
            } else if (requestCode == 99999999) {
                // do something awesome with the json data here
                Log.d(ModelTestingActivity.class.toString(), "ROUTE SHAPES: " + jsonResult);
            }
            else if (requestCode == 246800) {
                // do something...
                Log.d(ModelTestingActivity.class.toString(), "TRIPS: " + jsonResult);

                // the following code converts the trip info into a map of FerryTrip objects.
                // You can decide whether you want to store this in the model or not.

                HashMap<String, FerryTrip> ferryTrips = new HashMap<String, FerryTrip>();  // keyed by the trip id + date
                try {
                    JSONArray jsArr = new JSONArray(jsonResult);
                    for (int i=0; i<jsArr.length(); i++) {
                        JSONObject obj = jsArr.getJSONObject(i);
                        String hashKey = FerryTrip.getFerryTripId(obj) + "#" + FerryTrip.getFerryTripDate(obj);
                        FerryTrip ft = ferryTrips.get(hashKey);
                        if (ft == null) {
                            // this is the first time we've seen this trip id in the list,
                            // so create a new object.
                            ft = new FerryTrip(obj);
                            ft.addStopFromJSON(obj);
                            ferryTrips.put(hashKey, ft);
                        }
                        else {
                            // just add another stop to the already existing FerryTrip.
                            ft.addStopFromJSON(obj);
                        }
                    }

                    // at this point, there should be a bunch of FerryTrip objects in the map,
                    // each with 1 or more stops.  Depending on the search parameters,
                    // all stops in a trip may not be available.
                    Log.d(ModelTestingActivity.class.toString(), ferryTrips.toString());

                } catch (JSONException e) {
                    Log.d(ModelTestingActivity.class.toString(), e.getMessage());
                }




            }
        }
    }
}
