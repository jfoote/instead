package com.cmplxen.instead;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

/**
 * Created by user0 on 8/17/14.
 */
public class LocationMonitor implements
        LocationListener,
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener {
    public static final int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9999;
    Service mSuggestionService;

    // A request to connect to Location Services
    private LocationRequest mLocationRequest;

    // Stores the current instantiation of the location client in this object
    private LocationClient mLocationClient;

    public LocationMonitor(Service service) {
        mSuggestionService = service;
    }

    public boolean start() {

        // ASSERT(stopped)

        // Check that Google Play services is available
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(
                this.mSuggestionService);
        if (ConnectionResult.SUCCESS != resultCode) {
            Log.d("LocationMonitor", "Google Play services is not available.");
            return false;
        }

        // Create a location API request to sign up for location updates
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setInterval(5 * 1000); //TODO: * 5); // 5 minutes
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setFastestInterval(5 * 1000); // TODO: 1 minute

        // Create a new location client, using the enclosing class to handle callbacks.
        mLocationClient = new LocationClient(mSuggestionService, this, this);
        mLocationClient.connect();
        Log.d("LocationMonitor::start", "location client connection initiated");

        return true;
    }

    public void stop() {
        // If the client is connected
        if (mLocationClient.isConnected()) {
            mLocationClient.removeLocationUpdates(this);
            Log.d("LocationMonitor::stop", "location updates stopped");
        }

        // After disconnect() is called, the client is considered "dead".
        mLocationClient.disconnect();
    }

    /*
 * Handle results returned to this Activity by other Activities started with
 * startActivityForResult(). In particular, the method onConnectionFailed() in
 * LocationUpdateRemover and LocationUpdateRequester may call startResolutionForResult() to
 * start an Activity that handles Google Play services problems. The result of this
 * call returns here, to onActivityResult.
 */

    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

        // Choose what to do based on the request code
        switch (requestCode) {

            // If the request code matches the code sent in onConnectionFailed
            case LocationMonitor.CONNECTION_FAILURE_RESOLUTION_REQUEST :

                switch (resultCode) {
                    case Activity.RESULT_OK:

                        Log.d("LocationMonitor::onActivityResult", "Connection issue resolved");
                        break;

                    default:
                        Log.w("LocationMonitor::onActivityResult", "Connection issue NOT resolved");
                        break;
                }

                // If any other request code was received
            default:
                // Report that this Activity received an unknown requestCode
                Log.d("LocationMonitor::onActivityResult", "Unknown request code:" + requestCode);

                break;
        }
    }

    /*
    * Called by Location Services if the attempt to
    * Location Services fails.
    */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

        /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {
            try {

                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(new Activity(), // TODO: this is wrong; maybe pass settings activity in here and use that instead
                        LocationMonitor.CONNECTION_FAILURE_RESOLUTION_REQUEST);

                /*
                * Thrown if Google Play services canceled the original
                * PendingIntent
                */

            } catch (IntentSender.SendIntentException e) {

                // Log the error
                e.printStackTrace();
            }
        } else {

            // If no resolution is available, display a dialog to the user with the error.
            Log.w("LocationManager::onConnectionFailed", "Connection issue has no resolution");
        }
    }

    /*
 * Called by Location Services when the request to connect the
 * client finishes successfully. At this point, you can
 * request the current location or start periodic updates
 */
    @Override
    public void onConnected(Bundle bundle) {
        Log.d("LocationManager::onConnected", "connected");
        mLocationClient.requestLocationUpdates(mLocationRequest, this);
    }

    @Override
    public void onDisconnected() {
        Log.d("LocationManager::onDisconnected", "disconnected");
        // TODO: maybe notify the settings activity so it can de-check the location updates box,
        // or maybe make sure we are disconnected before we call the suggestion service "stopped"
    }

    /**
     * Report location updates to the UI.
     *
     * @param location The updated location.
     */
    @Override
    public void onLocationChanged(Location location) {
        Log.d("LocationManager::onLocationChanged", "lat=" + location.getLatitude() + ",long=" +
            location.getLongitude());

        Toast.makeText(mSuggestionService.getApplicationContext(), "LocMgr:lat=" + location.getLatitude() + ",long=" + location.getLongitude(), Toast.LENGTH_SHORT).show();

        Context context = mSuggestionService.getApplicationContext();
        Intent seenIntent = new Intent(context, NetworkService.class);
        seenIntent.setAction(NetworkService.GET_PLACES_ACTION);
        seenIntent.putExtra(NetworkService.GET_PLACES_LATITUDE, location.getLatitude());
        seenIntent.putExtra(NetworkService.GET_PLACES_LONGITUDE, location.getLongitude());
        context.startService(seenIntent);
    }
}