package edu.smccme.vgreen.smttcascobaylines;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
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
import android.widget.SpinnerAdapter;
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
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class FilterActivity extends AppCompatActivity
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        LocationListener, AdapterView.OnItemSelectedListener, Spinner.OnItemClickListener,
        ModelManager.QueryListener, View.OnClickListener, DatePickerFragment.DatePickerListener {
    // ********************* Added by Liam Hand ******************
    private SharedPreferences savedValues;
    private final String savedArrivalValue = "savedArrivalValue";
    private final String savedDepartureValue = "savedDepartureValue";
    private final String savedBundleArrival = "savedBundleArrival";
    private final String savedBundleDeparture = "savedBundleDeparture";
    private final String savedBundleDate = "savedBundleDate";
    private final String DIALOGE_DATE = "DialogDate";

    private final int TRIPS_REQUEST_ID = 923;
    private static final int REQUEST_DATE = 0;

    // create some global variables
    Context context;
    private HashMap<String, FerryTrip> ferryTrips;
    //private Button noFilterButton;
    private Button submitButton;
    private CalendarView calendar;
    private Spinner departureSpinner;
    private ArrayAdapter<CharSequence> departureAdapter;
    private Spinner arrivalSpinner = null;
    ArrayAdapter<String> arrivalAdapter = null;
    private EditText dateText;
    //ArrayList<Stop> stops = new ArrayList<>();
    HashSet<String> availPorts = new HashSet<>(); // trips
    //ArrayList<Routes> routes = new ArrayList<>();
    ArrayList<Object> boats = new ArrayList<>(); // boats
    int itemSelected = 99;
    private Date mSelectedDate;

    // some String variables to use to filter the data.  (Added on 5-6-2016 by Liam Hand)
    private String arrivalValue = "NO_FILTER_NEEDED";
    private String departureValue = "NO_FILTER_NEEDED";
    private String bundleDate = "NOTHING_SELECTED";
    private String bundleArrival = "NOTHING_SELECTED";
    private String bundleDeparture = "NOTHING_SELECTED";
    // ************************************************************

    private static final String DEBUG_TAG = "Filter_Activity";

    private Calendar mCalendar;
    private String mMonth, mDay, mYear;

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
    private TextView mChooseDateTV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter);

        mCalendar = new GregorianCalendar();

        mYear = String.valueOf(mCalendar.get(Calendar.YEAR));
        mMonth = String.valueOf(mCalendar.get(Calendar.MONTH));
        if (mMonth.length() == 1) {
            mMonth = "0" + mMonth;
        }
        mDay = String.valueOf(mCalendar.get(Calendar.DAY_OF_MONTH));
        if (mDay.length() == 1) {
            mDay = "0" + mDay;
        }

        //*********************** Added by Liam Hand ***************************
        //*****************Modified by Scott Thompson**********************************

        // set the spinner
        departureSpinner = (Spinner) findViewById(R.id.departureSpinner);
        // attach the array
        departureAdapter = ArrayAdapter.createFromResource(this,
                R.array.locationOptions, android.R.layout.simple_spinner_item);
        // departureAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        arrivalSpinner = (Spinner) findViewById(R.id.arrivalSpinner);
        arrivalSpinner.setOnItemSelectedListener(this);

        // attach the spinner
        departureSpinner.setAdapter(departureAdapter);
        // set the listener to the spinners and buttons
        departureSpinner.setOnItemSelectedListener(this);

        if (mMyPort != null) {
            String portName = mMyPort.getFullLabel();
            int matchedLoc = departureAdapter.getPosition(portName);

            departureSpinner.setSelection(matchedLoc);
        }
        mChooseDateTV = (TextView) findViewById(R.id.choose_date_TV);

        mChooseDateTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager manager = getSupportFragmentManager();
                DatePickerFragment dialog = DatePickerFragment.newInstance(mCalendar.getTime());

                dialog.show(manager, DIALOGE_DATE);
            }


        });

        // set the spinner


        // initialize the submit button
        submitButton = (Button) findViewById(R.id.submitFilterButton);
        submitButton.setOnClickListener(this);

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
    } //END onCreate

    private void setArrivalSpinner(List<String> newList) {

        //Convert the Port ID to the full label
        List<String> displayList = new ArrayList<>();
        for (String id : newList) {
            if (id.length() < 2) {
                Port curr = mMyPortManager.getPortById(Integer.parseInt(id));
                displayList.add(curr.getFullLabel());
            } else {
                if (!id.equals(null)) {
                    int pId = Integer.parseInt(mMyPortManager.getPortIdByLabel(id));
                    displayList.add(mMyPortManager.getPortById(pId).getFullLabel());
                }
            }
        }

        // attach the array
        arrivalAdapter = new ArrayAdapter<String>(getApplicationContext(),
                android.R.layout.simple_spinner_item, displayList);

        // departureAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // attach the spinner
        arrivalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_item);
        arrivalSpinner.setAdapter(arrivalAdapter);
        // set the listener to the spinners and buttons

        arrivalAdapter.notifyDataSetChanged();

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
            Toast.makeText(getApplicationContext(), mMyPort.getPortLat() + "," + mMyPort.getPortLon(), Toast.LENGTH_SHORT);


            String portName = mMyPort.getFullLabel();
            int matchedLoc = departureAdapter.getPosition(portName);

            departureSpinner.setSelection(matchedLoc);

        }
    }

    private void setAvailablePorts() {
        if (departureSpinner != null) {
            String depart = String.valueOf(departureSpinner.getSelectedItem());
            depart = mMyPortManager.getPortIdByLabel(depart);

            departureValue = depart;
            //if Leaving from Portland, all options are available
            if (departureValue.equalsIgnoreCase("2")) {
                List<Port> temp = new ArrayList<>();
                List<String> names = new ArrayList<>();
                temp.addAll(mMyPortManager.getPorts());

                for (int i = 0; i < temp.size(); i++) {
                    names.add(temp.get(i).getFullLabel());
                }

                setArrivalSpinner(names);

            } else {
                //Query for ports attached to departure port
                Bundle bundle = new Bundle();
                bundle.putString(ModelManager.QUERY_TRIP_DATE, mYear + mMonth + mDay);
                //bundle.putString(ModelManager.QUERY_TRIP_DEPART_PORT_ID, depart);
                mMgr.startQueryForResult(this, TRIPS_REQUEST_ID, ModelManager.QueryType.TRIPS, bundle);
                //Use the list to convert the ArrivalSpinner to applicable ports
                if (availPorts != null) {
                    setArrivalSpinner(new ArrayList<String>(availPorts));
                }
            }

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
     * Modified by Scott Thompson
     * This method will determine which item was selected from the respective spinners and set
     * the global variables for the query.
     *
     * @param parent
     * @param view
     * @param position
     * @param id
     */
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (itemSelected != position) {
            setAvailablePorts();
            itemSelected = position;
        }
        if (parent.getId() == R.id.arrivalSpinner) {
            arrivalSpinner.setSelection(position);
        }


    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

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

        if (requestCode == TRIPS_REQUEST_ID) {
            Log.d(FilterActivity.class.toString(), jsonResult);

            //Create a set for Routes that include port of departure
            HashSet<String> routeSet = new HashSet<>();
            try {
                JSONArray jsArr = new JSONArray(jsonResult);
                departureValue = mMyPortManager.getPortIdByLabel(departureValue);
                for (int i = 0; i < jsArr.length(); i++) {
                    JSONObject obj = jsArr.getJSONObject(i);
                    if (departureValue.equalsIgnoreCase(obj.getString("stop_id"))) {
                        routeSet.add(obj.getString("route_id"));
                    }
                }
                //Check each Route in the set for attached ports.
                for (String route : routeSet) {
                    for (int i = 0; i < jsArr.length(); i++) {
                        JSONObject obj = jsArr.getJSONObject(i);
                        if (obj.getString("route_id").equalsIgnoreCase(route)) {
                            availPorts.add(obj.getString("stop_id"));
                        }
                    }
                }

            } catch (JSONException e) {
                Log.d(ModelTestingActivity.class.toString(), e.getMessage());
            }


        }

    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.submitFilterButton) {
            ModelManager.ScheduleFilter filter = ModelManager.getFilter();
            boolean hasPOD = false,
                    hasDate = false;
            //Check for Null Values
            if (arrivalSpinner.getSelectedItem() == null) {
                Toast.makeText(FilterActivity.this, "Select a Port of Arrival", Toast.LENGTH_SHORT).show();
            } else {
                String temp = arrivalSpinner.getSelectedItem().toString();
                Log.d("TESTIES", temp);
                filter.setPoa(temp);

                hasPOD = true;
            }

            if(mSelectedDate == null){
                Toast.makeText(FilterActivity.this, "Please select a date", Toast.LENGTH_SHORT).show();
            }else {
                String dateStuff = mChooseDateTV.getText().toString();

                filter.setSearchDate(mSelectedDate);
                filter.setDateString(dateStuff);
                Log.d("TESTIES", dateStuff);

                hasDate = true;
            }

            String temp = departureSpinner.getSelectedItem().toString();
            Log.d("TESTIES", temp);
            filter.setPod(temp);



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
    public void onPause() {
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
    public void onResume() {
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

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

    }

    @Override
    public void onDateChosen(Date date) {
        String dateString;
        String year;
        String month;
        String day;

        Calendar cal;
        cal = new GregorianCalendar();
        cal.setTime(date);

        year = String.valueOf(cal.get(Calendar.YEAR));
        month = String.valueOf(cal.get(Calendar.MONTH)+1);
        if (month.length() == 1) {
            month = "0" + month;
        }
        day = String.valueOf(cal.get(Calendar.DAY_OF_MONTH));
        if (day.length() == 1) {
            day = "0" + day;
        }

        mSelectedDate = date;

        dateString = year + month + day;

        mChooseDateTV.setText(cal.get(Calendar.DAY_OF_WEEK) + ", " + month +" "+ day +" "+ year);
    }
}

