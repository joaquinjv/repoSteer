package com.steer.geolocation;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.steer.geolocation.entities.PointOfSale;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements
            GoogleApiClient.ConnectionCallbacks,
            GoogleApiClient.OnConnectionFailedListener,
            LocationListener,
            OnMapReadyCallback,
            GoogleMap.OnMapClickListener,
            GoogleMap.OnMarkerClickListener,
            ResultCallback<Status> {

    private static final String TAG = MainActivity.class.getSimpleName();

    private GoogleMap map;
    private GoogleApiClient googleApiClient;
    private Location lastLocation;

    public static String PACKAGE_NAME;

    private TextView textLat, textLong;

    private MapFragment mapFragment;

    private static final String NOTIFICATION_MSG = "NOTIFICATION MSG";
    // Create a Intent send by the notification
    public static Intent makeNotificationIntent(Context context, String msg) {
        Intent intent = new Intent( context, MainActivity.class );
        intent.putExtra( NOTIFICATION_MSG, msg );
        return intent;
    }

    public String loadJSONFromAsset() {
        ArrayList<HashMap<String, String>> list = new ArrayList<HashMap<String, String>>();
        return null;
    }

    public String loadJSON() {
        String json = "";
        try {
            BufferedReader reader = new BufferedReader(new FileReader(this.getFileStreamPath("userInfo2.json").getPath()));
            String line;
            StringBuilder buffer = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }
            reader.close();
            json = buffer.toString();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return json;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textLat = (TextView) findViewById(R.id.lat);
        textLong = (TextView) findViewById(R.id.lon);
        PACKAGE_NAME = getApplicationContext().getPackageName();
        /*
        Gson gson = new Gson();
        try {
            //JSONObject obj = new JSONObject(loadJSON());
            JSONObject obj = new JSONObject(loadJSONFromAsset());
        } catch (JSONException e) {
            e.printStackTrace();
        }*/

        //Object pointOfSales = gson.fromJson(loadJSONFromAsset(), Object.class);

        // initialize GoogleMaps
        initGMaps();

        // create GoogleApiClient
        createGoogleApi();

    }

    // Create GoogleApiClient instance
    private void createGoogleApi() {
        Log.d(TAG, "createGoogleApi()");
        if ( googleApiClient == null ) {
            googleApiClient = new GoogleApiClient.Builder( this )
                    .addConnectionCallbacks( this )
                    .addOnConnectionFailedListener( this )
                    .addApi( LocationServices.API )
                    .build();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Call GoogleApiClient connection when starting the Activity
        googleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Disconnect GoogleApiClient when stopping Activity
        googleApiClient.disconnect();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate( R.menu.main_menu, menu );
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch ( item.getItemId() ) {
            case R.id.geofence: {
                startGeofence();
                return true;
            }
            case R.id.clear: {
                clearGeofence();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private final int REQ_PERMISSION = 999;

    // Check for permission to access Location
    private boolean checkPermission() {
        Log.d(TAG, "checkPermission()");
        // Ask for permission if it wasn't granted yet
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED );
    }

    // Asks for permission
    private void askPermission() {
        Log.d(TAG, "askPermission()");
        ActivityCompat.requestPermissions(
                this,
                new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
                REQ_PERMISSION
        );
    }

    // Verify user's response of the permission requested
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult()");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch ( requestCode ) {
            case REQ_PERMISSION: {
                if ( grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED ){
                    // Permission granted
                    getLastKnownLocation();

                } else {
                    // Permission denied
                    permissionsDenied();
                }
                break;
            }
        }
    }

    // App cannot work without the permissions
    private void permissionsDenied() {
        Log.w(TAG, "permissionsDenied()");
        // TODO close app and warn user
    }

    // Initialize GoogleMaps
    private void initGMaps(){
        mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    // Callback called when Map is ready
    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG, "onMapReady()");
        map = googleMap;
        map.setOnMapClickListener(this);
        map.setOnMarkerClickListener(this);

        this.setPointOfSales();

        /* Try Intent
        Uri gmmIntentUri = Uri.parse("google.navigation:q=-34.9302700,-57.9724603");
        //Uri gmmIntentUri = Uri.parse("http://maps.google.com/maps?q=loc:-34.9302700,-57.9724603");
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        //mapIntent.setClassName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity");
        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        }*/
    }

    @Override
    public void onMapClick(LatLng latLng) {
        Log.d(TAG, "onMapClick("+latLng +")");
        markerForGeofence(latLng);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        Log.d(TAG, "onMarkerClickListener: " + marker.getPosition() );
        return false;
    }

    private LocationRequest locationRequest;
    // Defined in mili seconds.
    // This number in extremely low, and should be used only for debug
    private final int UPDATE_INTERVAL =  1000;
    private final int FASTEST_INTERVAL = 900;

    // Start location Updates
    private void startLocationUpdates(){
        Log.i(TAG, "startLocationUpdates()");
        locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_INTERVAL)
                .setFastestInterval(FASTEST_INTERVAL);

        if ( checkPermission() )
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    //private void updateCameraBearing(GoogleMap googleMap, float bearing) {
    private void updateCameraBearing(float bearing) {
        if ( map == null) return;
        CameraPosition camPos = CameraPosition
                .builder(
                        map.getCameraPosition() // current Camera
                )
                .zoom(19)         //Establecemos el zoom en 19
                .tilt(70)
                .bearing(bearing)
                .build();
        map.animateCamera(CameraUpdateFactory.newCameraPosition(camPos));

    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "onLocationChanged ["+location+"]");
        lastLocation = location;
        writeActualLocation(location);
        //updateCameraBearing(location.getBearing());
    }

    // GoogleApiClient.ConnectionCallbacks connected
    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "onConnected()");
        getLastKnownLocation();
        recoverGeofenceMarker();
    }

    // GoogleApiClient.ConnectionCallbacks suspended
    @Override
    public void onConnectionSuspended(int i) {
        Log.w(TAG, "onConnectionSuspended()");
    }

    // GoogleApiClient.OnConnectionFailedListener fail
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.w(TAG, "onConnectionFailed()");
    }

    // Get last known location
    private void getLastKnownLocation() {
        Log.d(TAG, "getLastKnownLocation()");
        if ( checkPermission() ) {
            lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
            if ( lastLocation != null ) {
                Log.i(TAG, "LasKnown location. " +
                        "Long: " + lastLocation.getLongitude() +
                        " | Lat: " + lastLocation.getLatitude());
                //writeLastLocation();
                startLocationUpdates();
            } else {
                Log.w(TAG, "No location retrieved yet");
                startLocationUpdates();
            }
        }
        else askPermission();
    }

    private void writeActualLocation(Location location) {
        textLat.setText( "Lat: " + location.getLatitude() );
        textLong.setText( "Long: " + location.getLongitude() );

        markerLocation(new LatLng(location.getLatitude(), location.getLongitude()), location);
    }

    private void writeLastLocation() {
        writeActualLocation(lastLocation);
    }

    private Marker locationMarker;
    private void markerLocation(LatLng latLng, Location location) {
        Log.i(TAG, "markerLocation("+latLng+")");
        String title = latLng.latitude + ", " + latLng.longitude;
        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
                .title(title);
        if ( map!=null ) {
            if ( locationMarker != null )
                locationMarker.remove();
            locationMarker = map.addMarker(markerOptions);
            //float zoom = 14f;
            CameraPosition camPos = CameraPosition
                    .builder(
                            map.getCameraPosition() // current Camera
                    )
                    .bearing(location.getBearing())
                    .target(latLng)   //Centramos en mi ubicacion
                    .zoom(19)         //Establecemos el zoom en 19
                    //.bearing(45)      //Establecemos la orientación con el noreste arriba
                    .tilt(70)         //Bajamos el punto de vista de la cámara 70 grados
                    .build();
            CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(camPos);//newLatLngZoom(latLng, zoom);
            map.animateCamera(cameraUpdate);
        }
    }


    /* Original Code
    private Marker geoFenceMarker;
    private void markerForGeofence(LatLng latLng) {
        Log.i(TAG, "markerForGeofence("+latLng+")");
        String title = latLng.latitude + ", " + latLng.longitude;
        // Define marker options
        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                .title(title);
        if ( map!=null ) {
            // Remove last geoFenceMarker
            if (geoFenceMarker != null)
                geoFenceMarker.remove();

            geoFenceMarker = map.addMarker(markerOptions);

        }
    }*/

    private List<Marker> pointOfSalesMarkers = new ArrayList<Marker>();
    //private List<PointOfSale> pointOfSalesList = new ArrayList<PointOfSale>();
    private void setPointOfSales() {
        DatabaseReference dbRef =
                FirebaseDatabase.getInstance().getReference().child("pointOfSales");
        dbRef.addValueEventListener(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot ds : dataSnapshot.getChildren()) {
                            Log.e(TAG, "dataChange reading db");
                            // Get Post object and use the values to update the UI
                            PointOfSale pos = ds.getValue(PointOfSale.class);
                            MarkerOptions markerOptions = new MarkerOptions()
                                    .position(new LatLng(pos.getLatitude(), pos.getLongitude()))
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                                    .title(pos.getName())
                                    .snippet(pos.getDetails());
                            if (map != null) {
                                // Remove last geoFenceMarker
                                //if (geoFenceMarker != null)
                                //    geoFenceMarker.remove();
                                pointOfSalesMarkers.add(map.addMarker(markerOptions));
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.e(TAG, "Error reading db");
                    }
                }
        );

//      ######BORRAR ..SE AGREGAN LOS PUNTOS DE VENTA DESDE LA BASE
/*
        pointOfSalesList.add(new PointOfSale(
            "Kiosko Zodiaco","Horario: 9hs - 14hs, 16hs - 20hs","-34.927244", "-57.964372"));
        pointOfSalesList.add(new PointOfSale(
            "Alamacen del Barrio","Horario: 9:30hs - 13:30hs, 17hs - 20:30hs","-34.928245","-57.970325"));
        pointOfSalesList.add(new PointOfSale(
            "Locutorio El Sol","Horario: 7hs - 21hs","-34.918198", "-57.967283"));


        dbRef.child("pointOfSales").push().setValue(pointOfSalesList.get(0));
        dbRef.child("pointOfSales").push().setValue(pointOfSalesList.get(1));
        dbRef.child("pointOfSales").push().setValue(pointOfSalesList.get(2));

        for (int i = 0; i < pointOfSalesList.size(); i++){
            // Define marker options
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(new LatLng(pointOfSalesList.get(i).getLatitude(), pointOfSalesList.get(i).getLongitude()))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                    .title(pointOfSalesList.get(i).getName())
                    .snippet(pointOfSalesList.get(i).getDetails());
            if ( map!=null ) {
                // Remove last geoFenceMarker
                //if (geoFenceMarker != null)
                //    geoFenceMarker.remove();
                pointOfSalesMarkers.add(map.addMarker(markerOptions));

            }
        }
*/

    }

    private List<Marker> geoFenceMarkers = new ArrayList<Marker>();
    private void markerForGeofence(LatLng latLng) {
        Log.i(TAG, "markerForGeofence("+latLng+")");
        String title = latLng.latitude + ", " + latLng.longitude;
        // Define marker options
        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                .title(title);
        if ( map!=null ) {
            // Remove last geoFenceMarker
            //if (geoFenceMarker != null)
            //    geoFenceMarker.remove();
            geoFenceMarkers.add(map.addMarker(markerOptions));

        }
    }

    // Start Geofence creation process
    private void startGeofence() {
        Log.i(TAG, "startGeofence()");
        //if( geoFenceMarker != null ) {
        if( geoFenceMarkers.size() > 0 ) {
            for (int i = 0; i < geoFenceMarkers.size(); i++){
                Geofence geofence = createGeofence(geoFenceMarkers.get(i).getPosition(), GEOFENCE_RADIUS );
                GeofencingRequest geofenceRequest = createGeofenceRequest( geofence );
                addGeofence( geofenceRequest );
                //addGeofenceParking( geofenceRequest );
            }
        } else {
            Log.e(TAG, "Geofence marker is null");
        }
    }


    private static final long GEO_DURATION = 60 * 60 * 1000;
    private static final String GEOFENCE_REQ_ID = "My Geofence";
    private static final float GEOFENCE_RADIUS = 1000.0f; // in meters

    // Create a Geofence
    private Geofence createGeofence( LatLng latLng, float radius ) {
        Log.d(TAG, "createGeofence");
        return new Geofence.Builder()
                .setRequestId(GEOFENCE_REQ_ID)
                .setCircularRegion( latLng.latitude, latLng.longitude, radius)
                .setExpirationDuration( GEO_DURATION )
                .setTransitionTypes( Geofence.GEOFENCE_TRANSITION_ENTER
                        | Geofence.GEOFENCE_TRANSITION_EXIT )
                .build();
    }

    // Create a Geofence Request
    private GeofencingRequest createGeofenceRequest( Geofence geofence ) {
        Log.d(TAG, "createGeofenceRequest");
        Toast.makeText(MainActivity.this, "En zona de estacionamiento Detected",Toast.LENGTH_SHORT).show();
        Animation slideUp = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_up);
        View b = findViewById(R.id.showMessage);
        b.startAnimation(slideUp);
        b.setVisibility(View.VISIBLE);
        return new GeofencingRequest.Builder()
                .setInitialTrigger( GeofencingRequest.INITIAL_TRIGGER_ENTER )
                .addGeofence( geofence )
                .build();
    }

    private PendingIntent geoFencePendingIntent;
    private final int GEOFENCE_REQ_CODE = 0;
    private PendingIntent createGeofencePendingIntent() {
        Log.d(TAG, "createGeofencePendingIntent");
        if ( geoFencePendingIntent != null )
            return geoFencePendingIntent;

        Intent intent = new Intent( this, GeofenceTrasitionService.class);
        return PendingIntent.getService(
                this, GEOFENCE_REQ_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT );
    }

    // Add the created GeofenceRequest to the device's monitoring list
    private void addGeofence(GeofencingRequest request) {
        Log.d(TAG, "addGeofence");
        if (checkPermission())
            LocationServices.GeofencingApi.addGeofences(
                    googleApiClient,
                    request,
                    createGeofencePendingIntent()
            ).setResultCallback(this);
    }

    @Override
    public void onResult(Status status) {
        Log.i(TAG, "onResult: " + status);
        if ( status.isSuccess() ) {
            saveGeofence();
            drawGeofence();
        } else {
            // inform about fail
        }
    }

    // Draw Geofence circle on GoogleMap
    //private Circle geoFenceLimits;
    private List<Circle> geoFenceLimits = new ArrayList<>();
    private void drawGeofence() {
        Log.d(TAG, "drawGeofence()");

        if ( geoFenceLimits.size() > 0) {
            for (int i = 0; i < geoFenceLimits.size(); i++) {
                geoFenceLimits.get(i).remove();
            }
            geoFenceLimits = new ArrayList<Circle>();
        }
        for (int i = 0; i < geoFenceMarkers.size(); i++){
            CircleOptions circleOptions = new CircleOptions()
                    .center(geoFenceMarkers.get(i).getPosition())
                    //.strokeColor(Color.argb(50, 70,70,70))
                    //.fillColor( Color.argb(100, 150,150,150) )
                    //.fillColor(Color.TRANSPARENT)
                    //.radius( GEOFENCE_RADIUS );
                    .radius( GEOFENCE_RADIUS )
                    .fillColor(Color.TRANSPARENT)
                    .strokeColor(Color.TRANSPARENT)
                    .strokeWidth(0);
            geoFenceLimits.add(map.addCircle(circleOptions));
        }

        Polygon polygon2 = map.addPolygon(new PolygonOptions()
                .add(new LatLng(-34.927647, -57.973754), new LatLng( -34.933120, -57.979907),
                        new LatLng(-34.943369, -57.966196),new LatLng(-34.937715, -57.960064))
                .strokeColor(Color.BLUE)
                .fillColor(Color.parseColor("#51000000")).strokeWidth(2));


    }
    private void drawGeofence2() {
        /*
        Log.d(TAG, "drawGeofence()");
        LatLng l1 = new LatLng(-34.917636, -57.971065);
        LatLng l2 = new LatLng(-34.917636, -57.971065);
        LatLng l3 = new LatLng(-34.917636, -57.971065);
        CircleOptions circleOptions = new CircleOptions()
                .center(geoFenceMarkers.get(i).getPosition())
                .strokeColor(Color.argb(50, 70,70,70))
                .fillColor( Color.argb(100, 150,150,150) )
                .radius( GEOFENCE_RADIUS );
        geoFenceLimits.add(map.addCircle(circleOptions));
        */

    }

    private final String KEY_GEOFENCE_LAT = "GEOFENCE LATITUDE";
    private final String KEY_GEOFENCE_LON = "GEOFENCE LONGITUDE";

    // Saving GeoFence marker with prefs mng
    private void saveGeofence() {
        Log.d(TAG, "saveGeofence()");
        SharedPreferences sharedPref = getPreferences( Context.MODE_PRIVATE );
        SharedPreferences.Editor editor = sharedPref.edit();

        for (int i = 0; i < geoFenceMarkers.size(); i++){
            editor.putLong( KEY_GEOFENCE_LAT, Double.doubleToRawLongBits(geoFenceMarkers.get(0).getPosition().latitude ));
            editor.putLong( KEY_GEOFENCE_LON, Double.doubleToRawLongBits(geoFenceMarkers.get(0).getPosition().longitude ));
            editor.apply();
        }

    }

    // Recovering last Geofence marker
    private void recoverGeofenceMarker() {
        Log.d(TAG, "recoverGeofenceMarker");
        SharedPreferences sharedPref = getPreferences( Context.MODE_PRIVATE );

        if ( sharedPref.contains( KEY_GEOFENCE_LAT ) && sharedPref.contains( KEY_GEOFENCE_LON )) {
            double lat = Double.longBitsToDouble( sharedPref.getLong( KEY_GEOFENCE_LAT, -1 ));
            double lon = Double.longBitsToDouble( sharedPref.getLong( KEY_GEOFENCE_LON, -1 ));
            LatLng latLng = new LatLng( lat, lon );
            markerForGeofence(latLng);
            drawGeofence();
        }
    }

    // Clear Geofence
    private void clearGeofence() {
        Log.d(TAG, "clearGeofence()");
        LocationServices.GeofencingApi.removeGeofences(
                googleApiClient,
                //createGeofencePendingIntent()
                createGeofenceParkingIntent()
        ).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(Status status) {
                if ( status.isSuccess() ) {
                    // remove drawing
                    removeGeofenceDraw();
                }
            }
        });
    }

    //TODO
    // Add the created GeofenceRequest to the device's monitoring list
    private PendingIntent geofenceParkingIntent;
    private PendingIntent createGeofenceParkingIntent() {
        Log.d(TAG, "createGeofenceParkingIntent");
        if ( geoFencePendingIntent != null )
            return geoFencePendingIntent;

        Intent intent = new Intent( this, GeofenceParkingService.class);
        return PendingIntent.getService(
                this, GEOFENCE_REQ_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT );
    }

    // Add the created GeofenceRequest to the device's monitoring list
    private void addGeofenceParking(GeofencingRequest request) {
        Log.d(TAG, "addGeofence");
        if (checkPermission())
            LocationServices.GeofencingApi.addGeofences(
                    googleApiClient,
                    request,
                    createGeofenceParkingIntent()
            ).setResultCallback(this);
    }

    private GeofencingRequest createGeofenceParkingRequest( Geofence geofence ) {
        Log.d(TAG, "createGeofenceParkingRequest");
        return new GeofencingRequest.Builder()
                .setInitialTrigger( GeofencingRequest.INITIAL_TRIGGER_ENTER )
                .addGeofence( geofence )
                .build();
    }

    public void showMessage(View view) {
        Snackbar mySnackbar = Snackbar.make(findViewById(R.id.coordinatorLayout),
                "Estacionar?", Snackbar.LENGTH_LONG);
        EstacionarListener parkingListener = new EstacionarListener();
        //mySnackbar.setAction("Aceptar", parkingListener);
        mySnackbar.setAction("Si", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Animation slideDown = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_down);
                View b = findViewById(R.id.showMessage);
                b.startAnimation(slideDown);
                b.setVisibility(View.GONE);

                Geofence geofence = createGeofence(geoFenceMarkers.get(0).getPosition(), GEOFENCE_RADIUS );
                GeofencingRequest geofenceRequest = createGeofenceParkingRequest( geofence );
                addGeofenceParking( geofenceRequest );
            }
        });
        mySnackbar.show();

    }

    private void removeGeofenceDraw() {
        Log.d(TAG, "removeGeofenceDraw()");
        if (geoFenceMarkers.size() > 0){
            for (int i = 0; i < geoFenceMarkers.size(); i++) {
                geoFenceMarkers.get(i).remove();
            }
            geoFenceMarkers = new ArrayList<Marker>();
        }
        if ( geoFenceLimits.size() > 0){
            for (int i = 0; i < geoFenceLimits.size(); i++){
                geoFenceLimits.get(i).remove();
            }
            geoFenceLimits = new ArrayList<Circle>();
        }

        //if ( geoFenceLimits != null )
        //    geoFenceLimits.remove();
    }

}
