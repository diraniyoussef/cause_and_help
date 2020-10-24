package com.youssefdirani.cause_and_help;

//check copyrights here https://github.com/android/location-samples/tree/df8fa498cdd859aa5c49fd7d0dc3d329c6e01591/LocationUpdatesPendingIntent/app/src/main/java/com/google/android/gms/location/sample/locationupdatespendingintent

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.location.LocationResult;

import java.util.List;

public class LocationUpdatesBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "Youssef-LUBR";
    static MainActivity activity;

    static final String ACTION_PROCESS_UPDATES =
            "package com.youssefdirani.cause_and_help.action" +
                    ".PROCESS_UPDATES";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG,"Broadcast receiver has received something !");
        if (intent != null) {
            final String action = intent.getAction();
            if( ACTION_PROCESS_UPDATES.equals(action) ) {
                LocationResult result = LocationResult.extractResult(intent);
                if (result != null) {
                    List<Location> locations = result.getLocations();
                    for (final Location location: locations) {
                        //        Activity activity = (Activity) context;
/*
                        activity.runOnUiThread(new Runnable() {
                                    public void run() {
                                        //Do something on UiThread
                                        activity.updateLocation( location );
                                    }
                                });
*/
                        if( activity != null ) {
                            if( activity.mapSetup.isConnected ) {
                                activity.mapSetup.updateLocation(location);
                            }
                        }

                        Log.i(TAG,"A location !");
                       // updateLocation(location);

                    }
                }
            }
        }
    }
}
