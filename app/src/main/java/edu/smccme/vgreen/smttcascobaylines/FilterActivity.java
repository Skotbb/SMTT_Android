package edu.smccme.vgreen.smttcascobaylines;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;

public class FilterActivity extends AppCompatActivity
implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        LocationListener{
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

    protected void setClient(){
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

    protected void updateForLocation(){
        if(mMyPort != null) {
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
        if(mMyLocation != null) {
            mMgr.createMyLocationManager(mMyLocation);
            mMyPort = mMgr.getMyPort();
            //Test TextView for checking Location
            updateForLocation();
        }else{
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
}
