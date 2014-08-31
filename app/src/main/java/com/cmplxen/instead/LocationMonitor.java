package com.cmplxen.instead;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v4.content.WakefulBroadcastReceiver;
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
public class LocationMonitor  {
    public static final int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9999;

    public static boolean start(Context context) {

        // ASSERT(stopped)

        // Check that Google Play services is available
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);
        if (ConnectionResult.SUCCESS != resultCode) {
            Log.d("LocationMonitor", "Google Play services is not available.");
            return false;
        }

        // Create an alarm to periodically get location updates
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, LocationAlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
        am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 1000 * 10 * 1, pi); // Millisec * Second * Minute
        Log.d("LocationMonitor", "AlarmManager.setRepeating called");

        return true;
    }

    public static void stop(Context context) {

        // Cancel the location-monitor alarm
        Intent intent = new Intent(context, LocationMonitor.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(sender);
    }


}

class LocationAlarmReceiver extends WakefulBroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Put here YOUR code.
        Log.d("LaRcvr", "Intent received");
        Toast.makeText(context, "Alarm !!!!!!!!!!", Toast.LENGTH_LONG).show(); // For example

        Intent getLocationIntent = new Intent(context, LocationUpdateService.class);
        getLocationIntent.setAction(LocationUpdateService.GET_LOCATION);
        startWakefulService(context, getLocationIntent);
    }

}
