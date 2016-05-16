package edu.smccme.vgreen.smttcascobaylines;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.vision.barcode.Barcode;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class FilterActivity extends AppCompatActivity
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        LocationListener, AdapterView.OnItemSelectedListener, ModelManager.QueryListener, View.OnClickListener {
    // ********************* Added by Liam Hand ******************
    private SharedPreferences savedValues;
    private final String savedArrivalValue = "savedArrivalValue";
    private final String savedDepartureValue = "savedDepartureValue";
    private final String savedBundleArrival = "savedBundleArrival";
    private final String savedBundleDeparture = "savedBundleDeparture";
    private final String savedBundleDate = "savedBundleDate";

    // create some global variables
    Context context;
    private HashMap<String, FerryTrip> ferryTrips;
    private Button noFilterButton;
    private Button submitButton;
    private CalendarView calendar;
    private Spinner departureSpinner;
    private Spinner arrivalSpinner;
    private EditText dateText;
    //ArrayList<Stop> stops = new ArrayList<>();
    ArrayList<Object> trips = new ArrayList<>(); // trips
    //ArrayList<Routes> routes = new ArrayList<>();
    ArrayList<Object> boats = new ArrayList<>(); // boats

    // some String variables to use to filter the data.  (Added on 5-6-2016 by Liam Hand)
    private String arrivalValue = "NO_FILTER_NEEDED";
    private String departureValue = "NO_FILTER_NEEDED";
    private String bundleDate = "NOTHING_SELECTED";
    private String bundleArrival = "NOTHING_SELECTED";
    private String bundleDeparture = "NOTHING_SELECTED";
    // ************************************************************

    private static final String DEBUG_TAG = "Filter_Activity";

    protected GoogleApiClient mClient;
    protected LocationRequest mLocationRequest;

    protected Location mMyLocation;
    protected Port mMyPort;
    protected PortManager mMyPortManager;

    protected ModelManager mMgr;

    //Location Update Variables
    private int mUpdateInterval = 30 * 1000;
    private int mFastestUpdateInterval = 10 * 1000;

    //Test TextView to check Location
    private TextView mClosestPortTV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter);

        //*********************** Added by Liam Hand ***************************

        // set the spinner
        departureSpinner = (Spinner) findViewById(R.id.departureSpinner);
        // attach the array
        ArrayAdapter<CharSequence> departureAdapter = ArrayAdapter.createFromResource(this,
                R.array.departureOptions, android.R.layout.simple_spinner_item);
        // departureAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);


        // attach the spinner
        departureSpinner.setAdapter(departureAdapter);
        // set the listener to the spinners and buttons
        departureSpinner.setOnItemSelectedListener(this);


        // set the spinner
        arrivalSpinner = (Spinner) findViewById(R.id.arrivalSpinner);
        // attach the array
        ArrayAdapter<CharSequence> arrivalAdapter = ArrayAdapter.createFromResource(this,
                R.array.arrivalOptions, android.R.layout.simple_spinner_item);
        // departureAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // attach the spinner
        arrivalSpinner.setAdapter(arrivalAdapter);
        // set the listener to the spinners and buttons
        arrivalSpinner.setOnItemSelectedListener(this);

        // initialize the submit button
        submitButton = (Button) findViewById(R.id.submitFilterButton);
        submitButton.setOnClickListener(this);
        // initialize the no filter needed button
        noFilterButton = (Button) findViewById(R.id.noFilterNeededButton);
        noFilterButton.setOnClickListener(this);

        // set the calendar
        initializeCalendar();

        // get the Saved Preferences
        savedValues = getSharedPreferences("SavedValues", MODE_PRIVATE);


        //**********************************************************

        //Get the instance of ModelManager
        mMgr = ModelManager.getInstance(this);
        //Get the instance of PortManager, giving access to the list of ports.
        mMyPortManager = PortManager.getInstance();

        setClient();

        //Initialize Widgets.
        mClosestPortTV = (TextView) findViewById(R.id.closest_port_TV);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mClient.connect();
    }

    protected void setClient() {
        mClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(mUpdateInterval);
        mLocationRequest.setFastestInterval(mFastestUpdateInterval);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    protected void updateForLocation() {
        if (mMyPort != null) {
            mClosestPortTV.setText(mMyPort.getFullLabel());
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(DEBUG_TAG, "Connected to Services");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling

            Toast.makeText(this, "No permissions", Toast.LENGTH_LONG).show();
        }
        //Pull first location (last known)
        mMyLocation = LocationServices.FusedLocationApi.getLastLocation(mClient);
        createLocationRequest();
        //Use ModelManager to manage locations.
        if (mMyLocation != null) {
            mMgr.createMyLocationManager(mMyLocation);
            mMyPort = mMgr.getMyPort();
            //Test TextView for checking Location
            updateForLocation();
        } else {
            Toast.makeText(this, R.string.GPS_no_loc_avail, Toast.LENGTH_SHORT).show();
        }

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mClient, mLocationRequest, this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mClient.disconnect();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Toast.makeText(this, R.string.GPS_no_serv, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLocationChanged(Location location) {
        mMyLocation = location;
        mMgr.setMyLocation(location);
        mMyPort = mMgr.getMyPort();
        //Test TextView for checking coordinates
        updateForLocation();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(this, R.string.GPS_cannot_con, Toast.LENGTH_SHORT).show();
    }

    /**
     * Added by Liam Hand
     * This method will determine which item was selected from the respective spinners and set
     * the global variables for the query.
     *
     * @param parent
     * @param view
     * @param position
     * @param id
     */
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent.getId() == R.id.arrivalSpinner) {
            arrivalValue = parent.getItemAtPosition(position).toString();
            switch (arrivalValue) {
                case "Arrival Locations":
                    bundleArrival = "NOTHING_SELECTED"; // "If I learned anything as a businessman, it's redundancy" -- Malcolm Merlyn
                    // Toast.makeText(this, bundleArrival, Toast.LENGTH_SHORT).show();
                    break;
                case "PK, Peaks Island":
                    bundleArrival = "0";
                    // Toast.makeText(this, bundleArrival, Toast.LENGTH_SHORT).show();
                    break;
                case "109, Portland":
                    bundleArrival = "1";
                    // Toast.makeText(this, bundleArrival, Toast.LENGTH_SHORT).show();
                    break;
                case "LD, Little Diamond Island":
                    bundleArrival = "2";
                    // Toast.makeText(this, bundleArrival, Toast.LENGTH_SHORT).show();
                    break;
                case "GD, Great Diamond Island":
                    bundleArrival = "3";
                    // Toast.makeText(this, bundleArrival, Toast.LENGTH_SHORT).show();
                    break;
                case "DC, Diamond Cove":
                    bundleArrival = "4";
                    // Toast.makeText(this, bundleArrival, Toast.LENGTH_SHORT).show();
                    break;
                case "LO, Long Island":
                    bundleArrival = "5";
                    // Toast.makeText(this, bundleArrival, Toast.LENGTH_SHORT).show();
                    break;
                case "CH, Chebeague Island":
                    bundleArrival = "6";
                    // Toast.makeText(this, bundleArrival, Toast.LENGTH_SHORT).show();
                    break;
                case "CF, Cliff Island":
                    bundleArrival = "7";
                    // Toast.makeText(this, bundleArrival, Toast.LENGTH_SHORT).show();
                    break;
                case "BI, Bailey Island":
                    bundleArrival = "8";
                    // Toast.makeText(this, bundleArrival, Toast.LENGTH_SHORT).show();
                    break;
            }
        } else if (parent.getId() == R.id.departureSpinner) {
            departureValue = parent.getItemAtPosition(position).toString();
            switch (departureValue) {
                case "Departure Locations":
                    bundleDeparture = "NOTHING_SELECTED"; // "If I learned anything as a businessman, it's redundancy" -- Malcolm Merlyn
                    // Toast.makeText(this, bundleDeparture, Toast.LENGTH_SHORT).show();
                    break;
                case "PK, Peaks Island":
                    bundleDeparture = "0";
                    // Toast.makeText(this, bundleDeparture, Toast.LENGTH_SHORT).show();
                    break;
                case "109, Portland":
                    bundleDeparture = "1";
                    // Toast.makeText(this, bundleDeparture, Toast.LENGTH_SHORT).show();
                    break;
                case "LD, Little Diamond Island":
                    bundleDeparture = "2";
                    // Toast.makeText(this, bundleDeparture, Toast.LENGTH_SHORT).show();
                    break;
                case "GD, Great Diamond Island":
                    bundleDeparture = "3";
                    // Toast.makeText(this, bundleDeparture, Toast.LENGTH_SHORT).show();
                    break;
                case "DC, Diamond Cove":
                    bundleDeparture = "4";
                    // Toast.makeText(this, bundleDeparture, Toast.LENGTH_SHORT).show();
                    break;
                case "LO, Long Island":
                    bundleDeparture = "5";
                    // Toast.makeText(this, bundleDeparture, Toast.LENGTH_SHORT).show();
                    break;
                case "CH, Chebeague Island":
                    bundleDeparture = "6";
                    // Toast.makeText(this, bundleDeparture, Toast.LENGTH_SHORT).show();
                    break;
                case "CF, Cliff Island":
                    bundleDeparture = "7";
                    // Toast.makeText(this, bundleDeparture, Toast.LENGTH_SHORT).show();
                    break;
                case "BI, Bailey Island":
                    bundleDeparture = "8";
                    // Toast.makeText(this, bundleDeparture, Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    public void initializeCalendar() {
        // set up the calendar view
        calendar = (CalendarView) findViewById(R.id.calendarView);

        calendar.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            String newMonth = "";
            @Override
            public void onSelectedDayChange(CalendarView calendarView, int year, int month, int day) {
                Toast.makeText(getApplicationContext(), "Selected Date:\n" + "Day = " + day + "\n" +
                        "Month = " + month + "\n" + "Year = " + year, Toast.LENGTH_LONG).show();

                // because the months go from 0-11 in android studios, we need to add one to the
                // number that represents the month
                month = month + 1;
                if(month < 10){
                    newMonth = 0 + "" + month;
                }
                else if (day >= 10){
                    newMonth = month + "";
                }

                bundleDate = year + "" + newMonth + "" + day;

            }
        });
    }

    /**
     * This method will be called to start the query for the data...
     */
    public void startTheQuery() {
        Bundle b = new Bundle();
        b.putString(ModelManager.QUERY_TRIP_DATE, bundleDate);
        b.putString(ModelManager.QUERY_TRIP_DEPART_PORT_ID, bundleDeparture); // vgreen note: get the stops first to see all ids
        b.putString(ModelManager.QUERY_TRIP_ARRIVE_PORT_ID, bundleArrival); // ditto

        mMgr.startQueryForResult(this, 246800, ModelManager.QueryType.TRIPS, b);

    }

    /**
     * This method was taken from the ModelTestingActivity and reads in the trips.  From there it
     * will convert them to objects and we can then send that list of objects to the ModelManager
     *
     * @param requestCode
     * @param resultCode
     * @param jsonResult
     */

    @Override
    public void onQueryResult(int requestCode, ModelManager.ResultType resultCode, String jsonResult) {
        if (requestCode == 246800) {
            // do something...
            Log.d(ModelTestingActivity.class.toString(), "TRIPS: " + jsonResult);

            // the following code converts the trip info into a map of FerryTrip objects.
            // You can decide whether you want to store this in the model or not.

            ferryTrips = new HashMap<String, FerryTrip>();  // keyed by the trip id + date
            try {
                JSONArray jsArr = new JSONArray(jsonResult);
                for (int i = 0; i < jsArr.length(); i++) {
                    JSONObject obj = jsArr.getJSONObject(i);
                    String hashKey = FerryTrip.getFerryTripId(obj) + "#" + FerryTrip.getFerryTripDate(obj);
                    FerryTrip ft = ferryTrips.get(hashKey);
                    if (ft == null) {
                        // this is the first time we've seen this trip id in the list,
                        // so create a new object.
                        ft = new FerryTrip(obj);
                        ft.addStopFromJSON(obj);
                        ferryTrips.put(hashKey, ft);
                    } else {
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

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.submitFilterButton) {

            if (bundleArrival.equals(bundleDeparture)){
                Toast.makeText(this, "Caution: You have selected the same ports for both departure" +
                        " and arrival.", Toast.LENGTH_SHORT).show();
            }
            // if the bundle is complete, send out the query and then tell the Model Manager what the
            // filtered data is
            if (bundleArrival != "NOTHING_SELECTED" && bundleDeparture != "NOTHING_SELECTED"
                    && bundleDate != "NOTHING_SELECTED") {
                startTheQuery();
                mMgr.setFilteredMap(ferryTrips);

                Intent intent = new Intent(v.getContext(), ScheduleActivity.class);
                startActivity(intent);

            }
            if (bundleArrival == "NOTHING_SELECTED") {
                Toast.makeText(this, "Please select an arrival port. ", Toast.LENGTH_SHORT).show();
            }
            if (bundleDeparture == "NOTHING_SELECTED") {
                Toast.makeText(this, "Please select a departure port. ", Toast.LENGTH_SHORT).show();
            }
            if (bundleDate == "NOTHING_SELECTED") {
                Toast.makeText(this, "Please select a date. ", Toast.LENGTH_SHORT).show();
            }
        }
        else if (v.getId()== R.id.noFilterNeededButton){
            // start the query with no parameters to get all of the trips
            Bundle b = new Bundle();
            b.putString(ModelManager.QUERY_TRIP_DATE, "");  // blank String for all trips
            b.putString(ModelManager.QUERY_TRIP_DEPART_PORT_ID, ""); // blank String for all trips
            b.putString(ModelManager.QUERY_TRIP_ARRIVE_PORT_ID, ""); // blank String for all trips

            mMgr.startQueryForResult(this, 246800, ModelManager.QueryType.TRIPS, b);

            //mMgr.setFilteredMap(ferryTrips);

            Intent intent = new Intent(v.getContext(), ScheduleActivity.class);
            startActivity(intent);

        }
    }

    /**
     * Save the variables when the activity changes or the orientation changes
     * @param savedInstanceState

    public void onSaveInstanceState(Bundle savedInstanceState){
    savedInstanceState.putString(savedArrivalValue, arrivalValue);
    savedInstanceState.putString(savedDepartureValue, departureValue);
    savedInstanceState.putString(savedBundleDate, bundleDate);
    savedInstanceState.putString(savedBundleArrival, bundleArrival);
    savedInstanceState.putString(savedBundleDeparture, bundleDeparture);
    }

    /**
     * Restore the values to their saved states once this activity resumes
     * @param savedInstanceState

    public void onRestoreInstanceState(Bundle savedInstanceState){
    super.onRestoreInstanceState(savedInstanceState);
    arrivalValue = savedInstanceState.getString(savedArrivalValue);
    departureValue = savedInstanceState.getString(savedDepartureValue);
    bundleDate = savedInstanceState.getString(savedBundleDate);
    bundleArrival = savedInstanceState.getString(savedBundleArrival);
    bundleDeparture = savedInstanceState.getString(savedBundleDeparture);
    Toast.makeText(this, arrivalValue + " " +  departureValue + " " +  bundleDate + " " + bundleArrival + " "+  bundleDeparture,
    Toast.LENGTH_SHORT).show();
    }
     */
    /**
     * This method will save the variables when the device changes orientation or the user switches activities
     */
    @Override
    public void onPause(){
        // save the variables
        SharedPreferences.Editor editor = savedValues.edit();
        editor.putString(savedArrivalValue, arrivalValue);
        editor.putString(savedDepartureValue, departureValue);
        editor.putString(savedBundleDate, bundleDate);
        editor.putString(savedBundleArrival, bundleArrival);
        editor.putString(savedBundleDeparture, bundleDeparture);
        editor.commit();

        super.onPause();
    }
    /**
     * This method will get the variables when the activity restarts
     */
    @Override
    public void onResume(){
        super.onResume();
        // get the values back into their correct variables
        arrivalValue = savedValues.getString(savedArrivalValue, "");
        departureValue = savedValues.getString(savedDepartureValue, "");
        bundleDate = savedValues.getString(savedBundleDate, "");
        bundleArrival = savedValues.getString(savedBundleArrival, "");
        bundleDeparture = savedValues.getString(savedBundleDeparture, "");
        // Toast.makeText(this, arrivalValue + " " +  departureValue + " " +  bundleDate + " " + bundleArrival + " "+  bundleDeparture,
        //     Toast.LENGTH_SHORT).show();

    }
}

