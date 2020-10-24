package com.youssefdirani.cause_and_help;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

class MapSetup extends FragmentActivity implements OnMapReadyCallback,
//https://android-developers.googleblog.com/2017/11/moving-past-googleapiclient_21.html
//https://stackoverflow.com/questions/57575770/com-google-android-gms-common-api-googleapiclient-is-deprecated
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        GoogleMap.OnMarkerClickListener, GoogleMap.OnMarkerDragListener,
        GoogleMap.OnCameraIdleListener, GoogleMap.InfoWindowAdapter, GoogleMap.OnInfoWindowClickListener {

    MainActivity activity;
    Toasting toasting;

    GoogleMap mMap;
    private static final long LOCATION_UPDATE_INTERVAL = 45 * 1000; //1 minute
    private static final long LOCATION_FASTEST_UPDATE_INTERVAL = 10 * 1000; //half a minute

    LatLngBounds oldScreen;

    MapSetup( MainActivity activity ) {
        this.activity = activity;
        this.toasting = activity.toasting;
        //DO NOT TRY TO ACCESS THIS SAME CLASS INSTANCE THIS WAY :  activity.mapSetup  this makes a crash I strongly believe. Besides you don't need to. "this" is enough for you
    }

/*
    @Override
    public void onCameraMoveStarted(int reason) {

    }
*/
    void askForMapReady() {
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) activity.getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment == null) {
            Log.i("Youssef", "mapFragment is null. I hope we won't enter here.");
        }
        mapFragment.getMapAsync(this); //https://developers.google.com/android/reference/com/google/android/gms/maps/MapFragment.html#getMapAsync(com.google.android.gms.maps.OnMapReadyCallback)
    }

    void changeMapType() {
        if( mMap.getMapType() != GoogleMap.MAP_TYPE_NORMAL ) {
            mMap.setMapType( GoogleMap.MAP_TYPE_NORMAL );
        } else {
            mMap.setMapType( GoogleMap.MAP_TYPE_HYBRID );
        }
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
    private double lat = 0;
    private double lng = 0;
    private float zoom;
    String referent = "";
    private GoogleApiClient mGoogleApiClient; //to get current location
    Marker electrotelMarker = null;
    Marker lastUserMarker = null; //null is a good indication
    Marker marker_pointOnCircle = null;
    Circle circle = null;
    double radius = 70;
    Circle lowerBound_circle = null;
    Circle upperBound_circle = null;
    double lowerBound_radius = 10;
    double upperBound_radius = 250;
    double upperBound_radius_backoff = 80; //make sure that 250 - 80 > 10

    MarkerOptions markerOptions_pointOnCircle;

    double getLat() {
        return lat;
    }
    double getLng() {
        return lng;
    }
    byte getRadius() {
        return (byte) Math.floor( radius );
    }


    private double cosine_pointOnCircleMarker = 0;
    private double sine_pointOnCircleMarker = 1;

    private final double geoScaleToMeter = 110754;
    private void getSineAndCosineOfPointOnCircleMarker() {
        if( marker_pointOnCircle == null || ( lat == 0 && lng == 0 ) ) {
            cosine_pointOnCircleMarker = 0;
            sine_pointOnCircleMarker = 1;
            return;
        }
        Location location_circleCenter = new Location("Youssef");
        location_circleCenter.setLatitude(lat);
        location_circleCenter.setLongitude(lng);

        Location location_pointOnCircle = new Location("Youssef");
        location_pointOnCircle.setLatitude( marker_pointOnCircle.getPosition().latitude );
        location_pointOnCircle.setLongitude( marker_pointOnCircle.getPosition().longitude );

        if( lat == marker_pointOnCircle.getPosition().latitude && lng == marker_pointOnCircle.getPosition().longitude ) {
            //my convention of not existing valid values
            cosine_pointOnCircleMarker = 0;
            sine_pointOnCircleMarker = 1;
            return;
        }

        double distance =  location_circleCenter.distanceTo(location_pointOnCircle);
        //Log.i("Youssef", "getting sine and cosine. distance is " + distance);
        double delta_y = marker_pointOnCircle.getPosition().latitude - lat;
        delta_y *= geoScaleToMeter;
        //Log.i("Youssef", "getting sine and cosine. delta_y is " + delta_y);
        double delta_x = marker_pointOnCircle.getPosition().longitude - lng;
        delta_x *= geoScaleToMeter;
        //Log.i("Youssef", "getting sine and cosine. delta_x is " + delta_x);
        cosine_pointOnCircleMarker = delta_x / distance;
        //Log.i("Youssef", "getting sine and cosine. cosine is " + cosine_pointOnCircleMarker);
        sine_pointOnCircleMarker = delta_y / distance;
        //Log.i("Youssef", "getting sine and cosine. sine is " + sine_pointOnCircleMarker);
    }

    private void refix_pointOnCircle() {
        if( marker_pointOnCircle != null ) {
            marker_pointOnCircle.remove();
        }

        //don't worry about sine and cosine in the following; they are simply set to a certain default value in case it's not valid
        double latitude_pointOnCircle = lat + sine_pointOnCircleMarker * radius / geoScaleToMeter; //the 110754 came here from https://stackoverflow.com/questions/1253499/simple-calculations-for-working-with-lat-lon-and-km-distance
        double longitude_pointOnCircle = lng + cosine_pointOnCircleMarker * radius / geoScaleToMeter;
        LatLng latLng_pointOnCircle = new LatLng(latitude_pointOnCircle, longitude_pointOnCircle);

        markerOptions_pointOnCircle.position( latLng_pointOnCircle )
                .title( (long) Math.floor(radius) + " متر");

        marker_pointOnCircle = mMap.addMarker( markerOptions_pointOnCircle );
        marker_pointOnCircle.setDraggable(true);
        marker_pointOnCircle.setIcon( BitmapDescriptorFactory.fromResource(R.drawable.reddot));
        if( !marker_pointOnCircle.isInfoWindowShown() ) {
            marker_pointOnCircle.showInfoWindow();
        }

    }

/*
    private Handler waitThenReturnMapFAB_ToNormal;
    private Runnable runnable_returnMapFAB_ToNormal;
    private boolean isAwaitingToReturnMapFAB_ToNormal = false;
*/

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.i("Youssef", "Map is ready now !");
        mMap = googleMap;
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMarkerDragListener(this);
        mMap.setOnCameraIdleListener(this);
        mMap.setOnInfoWindowClickListener(this);

        /*
        waitThenReturnMapFAB_ToNormal = new Handler();
        runnable_returnMapFAB_ToNormal = new Runnable() {
            public void run() {
                positionMapFAB("back to normal");
            }
        };
        */
        mMap.setOnInfoWindowCloseListener(new GoogleMap.OnInfoWindowCloseListener() {
            @Override
            public void onInfoWindowClose(Marker marker) {
                /*
                if( !marker.equals(lastUserMarker) ) {
                    isAwaitingToReturnMapFAB_ToNormal = true;
                    waitThenReturnMapFAB_ToNormal.postDelayed( runnable_returnMapFAB_ToNormal , 300 );
                }
                */
            }
        });
        mMap.getUiSettings().setTiltGesturesEnabled(false);
        //mMap.getUiSettings().setRotateGesturesEnabled(false);
        //mMap.getUiSettings().map

        mMap.setOnMapClickListener(
                new GoogleMap.OnMapClickListener() {
                    @Override
                    public void onMapClick(LatLng latLng) {
                        Log.i("Youssef", "map has been clicked.");
                        if( !isCircleAndCircleMarkerBeingDragged ) { //not needed, because this event is never called (I tested it) when marker_pointOnCircle had the focus and being dragged
                            if( marker_pointOnCircle != null ) {
                                marker_pointOnCircle.setVisible(false);
                                Log.i("Youssef", "pointOnCircle should had become invisible if already visibile.");
                            }
                            if (circle != null) {
                                circle.setStrokeWidth(3);
                                Log.i("Youssef", "circle's thickness should has been minimized");
                            }
                        }
                    }
                }
        );

        connectAttemptToGetCurrentLocation();
    }

    void changeMap() {
        if( referent.equalsIgnoreCase("user") ) {
            zoom = 17;
            //lat and lng have already been taken in onConnected(...)
            mMap.getUiSettings().setMapToolbarEnabled(false); //the user's current location
        } else if( referent.equalsIgnoreCase("electrotel") ) {
            lat = 33.4165429;
            lng = 35.41302;
            zoom = 16;
            mMap.getUiSettings().setMapToolbarEnabled( true );
        }

        LatLngBounds currentScreen = mMap.getProjection().getVisibleRegion().latLngBounds;
        LatLng latLng = new LatLng( lat, lng );

        if( referent.equalsIgnoreCase("user") ) {
            if( circle != null ) { //not the first time.
                circle.remove();
            }
            circle = mMap.addCircle( new CircleOptions()
                    .center(latLng)
                    .radius(radius)
                    .strokeColor(Color.RED).strokeWidth(3)
            );

            if( marker_pointOnCircle == null ) {
                markerOptions_pointOnCircle = new MarkerOptions();
            }
            getSineAndCosineOfPointOnCircleMarker();
            refix_pointOnCircle();
            marker_pointOnCircle.setVisible( false );

            if(lastUserMarker != null) { //not the first time
//              if( lastUserMarker.isVisible() ) { //no need for this test //Note that this does not indicate whether the marker is within the screen's viewport. It indicates whether the marker will be drawn if it is contained in the screen's viewport.
                lastUserMarker.remove();
                lastUserMarker = mMap.addMarker( new MarkerOptions().position(latLng).title("موقعك الآن") );
                //seeUserMarker();
            } else { //first time, we always want to see the location.
                lastUserMarker = mMap.addMarker( new MarkerOptions().position(latLng).title("موقعك الآن") );
                mMap.animateCamera( CameraUpdateFactory.newLatLngZoom( latLng , zoom ) );
            }
//              }
        } else if (referent.equalsIgnoreCase("electrotel") ) {
            if( electrotelMarker == null ) {
                electrotelMarker = mMap.addMarker( new MarkerOptions().position(latLng).title("محل أخيكم يوسف ديراني") );
                electrotelMarker.setDraggable(true);
                if (!electrotelMarker.isInfoWindowShown()) {
                    electrotelMarker.showInfoWindow();
                }
                electrotelMarker.setIcon( BitmapDescriptorFactory
                        .defaultMarker(BitmapDescriptorFactory.HUE_GREEN) ); //https://stackoverflow.com/questions/19076124/android-map-marker-color
            }
            //now to see if there's need to zoom higher
            if( lastUserMarker != null ) { //there is a user marker
                if( currentScreen.contains( lastUserMarker.getPosition() ) &&
                        currentScreen.contains( electrotelMarker.getPosition() ) ) {
                    //do nothing
                } else {
                    zoomOut();
                }
            } else {
                ///mMap.moveCamera( CameraUpdateFactory.newLatLng( latLng ) );
                mMap.animateCamera( CameraUpdateFactory.newLatLngZoom( latLng , zoom ) );
            }
        }
    }

    void seeUserMarker() {
        if( !isFirstTimeLocationReceived ) {
            LatLng latLng = new LatLng(lat, lng);
            LatLngBounds currentScreen = mMap.getProjection().getVisibleRegion().latLngBounds;
            if( currentScreen.contains( lastUserMarker.getPosition() ) ) {
                //don't do anything. The marker being shown is enough.
            } else {
                mMap.animateCamera( CameraUpdateFactory.newLatLng( latLng ) );
            }
        }
    }

    private void zoomOut() {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(lastUserMarker.getPosition());
        builder.include(electrotelMarker.getPosition());
        LatLngBounds bounds = builder.build();

        int width = activity.getResources().getDisplayMetrics().widthPixels;
        int height = activity.getResources().getDisplayMetrics().heightPixels;
        int padding = (int) (width * 0.10); // offset from edges of the map 10% of screen

        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding);

        mMap.animateCamera(cu);
    }

    void connectAttemptToGetCurrentLocation() {
        Log.i("Youssef", "entering Connect attempt to get current location.");
        /* There's something similar to GoogleApiClient in
        * https://stackoverflow.com/questions/22733661/googleapiclient-and-googleplayservicesclient-can-you-preserve-a-separation-of-c
        * which is GooglePlayServicesClient.
        * */

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(activity)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        //if (mGoogleApiClient != null) { //always true
        if( !mGoogleApiClient.isConnected() ) {
            Log.i("Youssef", "Connecting...");
            mGoogleApiClient.connect(); //this will usually mean to find move to last location.
        }
        //}
    }

    boolean isLocationReceived = false;
    boolean isFirstTimeLocationReceived = true; //just not to let toasting appear every time location is updated. I made use of this variable in onResume as well
    private boolean isCircleAndCircleMarkerBeingDragged = false;

    void updateLocation( Location location ) {
        //if we didn't enter here for some long time, like 20 seconds or even 1 minute, we might have to restart activity
        isLocationReceived = true;
        activity.userInteractionForLocationReceipt.setVariablesOnLocationUpdate();

        if( !isCircleAndCircleMarkerBeingDragged ) {
            lat = location.getLatitude();
            lng = location.getLongitude();
            referent = "user"; //this means somewhere in Lebanon.
            changeMap();
        }

        if( isFirstTimeLocationReceived ) {
            if (activity.isSalutsNotCasesSelected) {
                toasting.toastUp("لقد تم إيجاد موقعك بنجاح و انت تشاهد تحيات الناس الآن", Toast.LENGTH_SHORT);
            } else {
                toasting.toastUp("لقد تم إيجاد موقعك بنجاح و انت تشاهد قضايا الناس الآن", Toast.LENGTH_SHORT);
            }
            isFirstTimeLocationReceived = false;

            oldScreen = mMap.getProjection().getVisibleRegion().latLngBounds;
            //it can be interesting to request for data from server. I made it just for the case of resume after pause
            if( isGoingToRequestMarkersInNewScreen() ) {
                byte[] final_message_byte = activity.setRequestMessageToSend();
                activity.socketConnection.socketConnectionSetup(final_message_byte);
            }
        }
    }

    protected boolean isConnected = false;
    @Override
    public void onConnected( @Nullable final Bundle bundle ) {
        Log.i("Youssef", "Connected !");

        if (ActivityCompat.checkSelfPermission( activity, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission( activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                        PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission( activity, Manifest.permission.ACCESS_COARSE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions( activity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_COARSE_LOCATION}, 1); /*last parameter requestCode is
            * related to this app itself.
            * https://developer.android.com/reference/androidx/core/app/ActivityCompat.html#requestDragAndDropPermissions(android.app.Activity,%20android.view.DragEvent)
            */
        } else {
            Log.i("Youssef", "permissions are fine in onConnected");
            mapOperations();
        }

    }

    @SuppressLint("MissingPermission")
    void mapOperations() {
        //mLastLocation = LocationServices.FusedLocationApi.getLastLocation( mGoogleApiClient );
        FusedLocationProviderClient fusedLocationApi = LocationServices.getFusedLocationProviderClient(activity);
        /*another way is : https://developer.android.com/training/location/receive-location-updates#get-last-location
        //same as the second answer here https://stackoverflow.com/questions/46481789/android-locationservices-fusedlocationapi-deprecated
        // But I doubt it will work.
        */
        setLocationRequest( LOCATION_UPDATE_INTERVAL , LOCATION_FASTEST_UPDATE_INTERVAL );
        LocationUpdatesBroadcastReceiver.activity = activity;
        setPendingIntent();
        fusedLocationApi.requestLocationUpdates(locationRequest, pendingIntent);
        isConnected = true;
        /*After we have requested location, it's probably necessary to await for 30 seconds or 1 minute to
        * interact with the user in case no location has yet been updated.
        * For this I'll be monitoring isLocationReceived with a handle.*/
        if( !isLocationReceived && !activity.isFirstTime ) {
            activity.userInteractionForLocationReceipt.setUserInteraction_WaitForLocationReceipt();
        }
    }

    PendingIntent pendingIntent; //https://stackoverflow.com/questions/2808796/what-is-an-android-pendingintent

    private void setPendingIntent() {
        Intent intent = new Intent( activity, LocationUpdatesBroadcastReceiver.class);
        intent.setAction( LocationUpdatesBroadcastReceiver.ACTION_PROCESS_UPDATES );
        pendingIntent = PendingIntent.getBroadcast(activity, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private LocationRequest locationRequest;
    void setLocationRequest( long updateInterval , long fastestUpdateInterval ) {
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval( updateInterval );
        locationRequest.setFastestInterval( fastestUpdateInterval );
    }

    //I think it may happen that we lose connection in case e.g. the user turned off the location then turned it on.
    //So you will have to find a way to fix this in your service probably ............!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

    @Override
    public void onConnectionSuspended(int arg0) {
        Log.i("Youssef", "Connection is suspended :(");
        isConnected = false;
    }

    //check copyrights here ??
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        //https://developers.google.com/android/guides/google-api-client
        toasting.toast("هناك مشكلة ربما بGoogle Play Services - " +
                "هذا النوع من المشاكل قلّما يحصل و هو خلل في جهازك.", Toast.LENGTH_LONG);
        Log.i("Youssef", "Connection has failed :( :(");
         /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {
            final int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
                /*
                 * Thrown if Google Play services canceled the original
                 * PendingIntent
                 */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
            /*
             * If no resolution is available, display a dialog to the
             * user with the error.
             */
            Log.i("Youssef", "Location services connection failed with code " + connectionResult.getErrorCode());
        }
        isConnected = false;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        boolean allRequestsAreGranted = true;
        if (requestCode == 1) {
            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                int grantResult = grantResults[i];
                if (permission.equals(Manifest.permission.ACCESS_FINE_LOCATION) ||
                        permission.equals(Manifest.permission.ACCESS_COARSE_LOCATION) ||
                        permission.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        allRequestsAreGranted = false;
                    }
                }
            }
            if( allRequestsAreGranted ) {
                Log.i("Youssef", "in onRequestPermissionsResult");
                mapOperations();
            } else {
                //requestPermissions(new String[]{Manifest.permission.SEND_SMS}, PERMISSIONS_CODE);
                toasting.toast("عليك أن تعطي الإذن. يمكنك الذهاب إلى settings ثم apps" +
                        " و تختار هذا التطبيق و تعطي الإذن.", Toast.LENGTH_LONG);
                activity.finish();
            }
        }
    }
/*
    private void positionMapFAB( String placement ) {
        if( isAwaitingToReturnMapFAB_ToNormal ) { //usually I place this block outside this method but it's all ok.
            waitThenReturnMapFAB_ToNormal.removeCallbacks( runnable_returnMapFAB_ToNormal );
        }
        isAwaitingToReturnMapFAB_ToNormal = false;
        final FloatingActionButton fab_changeMap = activity.findViewById(R.id.fab_changemaplayer);
        if( fab_changeMap.getLayoutParams() instanceof ViewGroup.MarginLayoutParams ) {
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) fab_changeMap.getLayoutParams();
            int fab_margin = (int) activity.getResources().getDimension(R.dimen.fab_margin);
            int fab_margin_extra = (int) activity.getResources().getDimension(R.dimen.fab_margin_extra);
            int fab_margin_extra_extra = (int) activity.getResources().getDimension(R.dimen.fab_margin_extra_extra);
            if( placement.equals("above map toolbar") ) {
                p.setMargins(fab_margin, fab_margin, fab_margin_extra, fab_margin_extra_extra); //fab_margin is not necessary at all.
            } else if( placement.equals("back to normal") ) {
                p.setMargins( fab_margin , fab_margin , fab_margin , fab_margin_extra );
            }
            fab_changeMap.requestLayout();
        }
    }
*/
    @Override
    public boolean onMarkerClick( Marker marker ) {
        Log.i("Youssef", "inside onMarkerClick");
        if( marker.equals(electrotelMarker) ) {
            toasting.toastBottom("لتحمي علامة \"محل أخيك ديراني\"، إضغط طويلاً عليها",
                    Toast.LENGTH_LONG);
            mMap.getUiSettings().setMapToolbarEnabled(true);
            //positionMapFAB("above map toolbar");
        } else if (marker.equals(lastUserMarker) || marker.equals(marker_pointOnCircle)) {
            Log.i("Youssef", "Either lastUserMarker or marker_pointOnCircle has been clicked");
            mMap.getUiSettings().setMapToolbarEnabled(false);
        } else { //it has to be a marker gotten from the intermediate server
            activity.markersAction.updateLastCustomMarkerClicked( marker );
            return false;
        }

        if( marker.equals(lastUserMarker) ) {
            if (!marker_pointOnCircle.isInfoWindowShown()) {
                marker_pointOnCircle.showInfoWindow();
            }
            marker_pointOnCircle.setIcon( BitmapDescriptorFactory.fromResource(R.drawable.reddot));
            marker_pointOnCircle.setVisible(true);
            circle.setStrokeWidth(10);
        }

        return false; //don't know this value is returned to whom, but it's ok
    }

    @Override
    public void onMarkerDragStart(Marker marker) { //that's a long click
        Log.i("Youssef", "inside onMarkerDragStart");
        if (marker.equals(electrotelMarker)) {
            electrotelMarker.remove();
            electrotelMarker = null;
        }
        if (marker.equals(marker_pointOnCircle)) {
            isCircleAndCircleMarkerBeingDragged = true;

            if (!marker_pointOnCircle.isInfoWindowShown()) {
                marker_pointOnCircle.showInfoWindow();
            }

            LatLng latLng = new LatLng(lat, lng);
            if( lowerBound_circle != null ) { //not the first time.
                lowerBound_circle.remove();
            }
            lowerBound_circle = mMap.addCircle( new CircleOptions()
                    .center(latLng)
                    .radius( lowerBound_radius )
                    .strokeColor(Color.YELLOW).strokeWidth(5)
            );
            if( upperBound_circle != null ) { //not the first time.
                upperBound_circle.remove();
            }
            upperBound_circle = mMap.addCircle( new CircleOptions()
                    .center(latLng)
                    .radius( upperBound_radius )
                    .strokeColor(Color.YELLOW).strokeWidth(5)
            );

            circleTrackMarker(); //might be useful, although the genuine place pf this statement is in onMarkerDrag(...)
        }
    }

    private void circleTrackMarker() {
        Location location_circleCenter = new Location("Youssef");
        location_circleCenter.setLatitude(lat);
        location_circleCenter.setLongitude(lng);

        Location location_pointOnCircle = new Location("Youssef");
        location_pointOnCircle.setLatitude(marker_pointOnCircle.getPosition().latitude);
        location_pointOnCircle.setLongitude(marker_pointOnCircle.getPosition().longitude);

        double expectedRadius =  location_circleCenter.distanceTo(location_pointOnCircle); //on start dragging, the marker bumps forward - this is the expected location
        //respecting bounds
        if (expectedRadius < lowerBound_radius || expectedRadius > upperBound_radius) {
            if( expectedRadius > upperBound_radius ) { //to fix a sort of a natural bug
                circle.remove();
                radius = upperBound_radius - upperBound_radius_backoff;
                circle = mMap.addCircle(new CircleOptions()
                        .center(new LatLng(lat, lng))
                        .radius(radius)
                        .strokeColor(Color.RED).strokeWidth(5)
                );
            }
            getSineAndCosineOfPointOnCircleMarker();
            refix_pointOnCircle();
            removeTheTwoBoundaryCircles();
            return;
        }

        //tracking the circle to the marker
        circle.remove();
        circle = mMap.addCircle(new CircleOptions()
                .center(new LatLng(lat, lng))
                .radius(expectedRadius)
                .strokeColor(Color.RED).strokeWidth(5)
        );
        radius = expectedRadius;

        //set the title of the marker
        if (marker_pointOnCircle.isInfoWindowShown()) {
            marker_pointOnCircle.hideInfoWindow();
        }
        marker_pointOnCircle.setTitle( (long) Math.floor( radius ) + " متر");
        if (!marker_pointOnCircle.isInfoWindowShown()) {
            marker_pointOnCircle.showInfoWindow();
        }

    }

    @Override
    public void onMarkerDrag(Marker marker) {
        Log.i("Youssef", "onMarkerDrag - beginning");
        if( marker.equals( marker_pointOnCircle ) ) {
            //circle will track the marker_pointOnCircle, while respecting the lower and upper bounds
            getSineAndCosineOfPointOnCircleMarker();
            //refix_pointOnCircle();
            circleTrackMarker();
        }

        Log.i("Youssef", "onMarkerDrag - finishing");
    }

    private void removeTheTwoBoundaryCircles() {
        if (lowerBound_circle != null) { //not the first time.
            lowerBound_circle.remove();
        }
        if (upperBound_circle != null) { //not the first time.
            upperBound_circle.remove();
        }
    }

    @Override
    public void onMarkerDragEnd(Marker marker) {
        Log.i("Youssef" , "inside on marker drag end");
        if (marker.equals(marker_pointOnCircle)) {


            /*refixing the marker on circle is to fix a bug; I delete then recreate another one just like him.
            * getSineAndCosineOfPointOnCircleMarker(); //DO NOT CALL THIS HERE ! It's wrong. Sine and cosine are already gotten in onMarkerDrag
            */
            refix_pointOnCircle();

            //back to normal operation
            removeTheTwoBoundaryCircles();
            //accepting new locations change
            isCircleAndCircleMarkerBeingDragged = false;
        }
    }

    boolean isServicesConnected() {
        // Verifying the availability of Google Play services
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(activity);
        // If Google Play services is available
        return (ConnectionResult.SUCCESS == resultCode);
    }

    boolean requestMarkersInSameScreen() {
        LatLngBounds newScreen = mMap.getProjection().getVisibleRegion().latLngBounds;
        //changeSizeOrDisappearMarkers( newScreen );
        if( newScreen.northeast.latitude - newScreen.southwest.latitude < 0.35 ) {
            return true;
        } else {
            return false;
        }
    }

    String markersSize = "";
    private void changeSizeOrDisappearMarkers( LatLngBounds newScreen ) {
        if( newScreen.northeast.latitude - newScreen.southwest.latitude > 0.35 ) { //markers must all be hidden
            if( !markersSize.equals("hidden") ) {
                //hide them all
                activity.markersAction.hideAllMarkers();
                markersSize = "hidden";
            }
        } else if( newScreen.northeast.latitude - newScreen.southwest.latitude > 0.07 ) { //markers must be small
            if( !markersSize.equals("small") ) {
                //make them small
                activity.markersAction.sizeAllMarkers("small");
                markersSize = "small";
            }
        } else { //all markers must be the normal large size
            if( !markersSize.equals("normal") ) {
                //make them normal (large)
                activity.markersAction.sizeAllMarkers("normal");
                markersSize = "normal";
            }
        }
    }

    private boolean isGoingToRequestMarkersInNewScreen() { //this method also takes care of updating currentScreen
        LatLngBounds newScreen = mMap.getProjection().getVisibleRegion().latLngBounds;
        changeSizeOrDisappearMarkers( newScreen );
        /*
        Log.i("Youssef", "newScreen.northeast.latitude " + newScreen.northeast.latitude );
        Log.i("Youssef", "oldScreen.northeast.latitude " + oldScreen.northeast.latitude );
        Log.i("Youssef", "oldScreen.southwest.latitude " + oldScreen.southwest.latitude );
        Log.i("Youssef", "2 * oldScreen.northeast.latitude - oldScreen.southwest.latitude " +
                (double) (2 * oldScreen.northeast.latitude - oldScreen.southwest.latitude) );
        */
        Log.i("Youssef", "Inside isGoingToRequestMarkersInNewScreen().");

        if( !( newScreen.northeast.latitude > 2 * oldScreen.northeast.latitude - oldScreen.southwest.latitude ||
                newScreen.northeast.longitude > 2 * oldScreen.northeast.longitude - oldScreen.southwest.longitude ||
                newScreen.southwest.latitude < 2 * oldScreen.southwest.latitude - oldScreen.northeast.latitude ||
                newScreen.southwest.longitude < 2 * oldScreen.southwest.longitude - oldScreen.northeast.longitude ) ) {
            Log.i("Youssef", "We're still considered in the range of the old screen.");
        } else {
            Log.i("Youssef", "We've moved to a broader range than the old screen's.");
        }

        if( !(oldScreen.northeast.latitude - oldScreen.southwest.latitude > 2 * ( newScreen.northeast.latitude - newScreen.southwest.latitude ) ) ) {
            Log.i("Youssef", "Not going from big screen to small screen.");
        } else {
            Log.i("Youssef", "Going from big screen to small screen.");
        }

        if( !( newScreen.northeast.latitude - newScreen.southwest.latitude < 0.35 ) ) {
            Log.i("Youssef", "Our new screen is not small enough.");
        } else {
            Log.i("Youssef", "Our new screen is small enough.");
        }

        if (
                (
                ( newScreen.northeast.latitude > 2 * oldScreen.northeast.latitude - oldScreen.southwest.latitude ||
                newScreen.northeast.longitude > 2 * oldScreen.northeast.longitude - oldScreen.southwest.longitude ||
                newScreen.southwest.latitude < 2 * oldScreen.southwest.latitude - oldScreen.northeast.latitude ||
                newScreen.southwest.longitude < 2 * oldScreen.southwest.longitude - oldScreen.northeast.longitude ) /*this means the current
                screen has gone beyond the bigger virtual screen*/
                    ||
                    ( oldScreen.northeast.latitude - oldScreen.southwest.latitude > 2 * ( newScreen.northeast.latitude - newScreen.southwest.latitude ) ) /*this is a
                    * fix for starting map view which shows the map of the whole earth. And even more than that, if the user is scrolling a lot so it's not a bad deal.*/
                )
                        &&
                newScreen.northeast.latitude - newScreen.southwest.latitude < 0.35 //don't request markers if screen is too wide
        ) {
            oldScreen = newScreen;
            return true;
        } else {
            //oldScreen = newScreen;
            return false;
        }
    }



    @Override
    public void onCameraIdle() {
        Log.i("Youssef", "camera is idle");
        /*We enter here AFAIK when we already have a location update, so we're ready to setRequestMessageToSend()
        * but first let us check if the current screen has something really extra to the last old screen (which is currentScreen)
         */
        if( isLocationReceived ) { /*Usually not needed, since onCameraIdle is called after we already received our location.
            *currentScreen is not null as well (by consequence)
            */
            if( isGoingToRequestMarkersInNewScreen() ) {
                Log.i("Youssef", "It is going To Request Markers In New Screen.");
                byte[] final_message_byte =  activity.setRequestMessageToSend();
                activity.socketConnection.socketConnectionSetup( final_message_byte );
            }
        }
    }


    @Override
    public View getInfoWindow(Marker marker) {

        return null;
    }

    @Override
    public View getInfoContents(Marker marker) {

        return null;
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        Log.i("Youssef", "InfoWindow clicked");

    }

}
