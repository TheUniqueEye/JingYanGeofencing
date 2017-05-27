package cs190i.cs.ucsb.edu.jingyangeofencing;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.JsonToken;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import static java.lang.Thread.sleep;

public class MapsActivity extends FragmentActivity
        implements OnMapReadyCallback,
        GoogleMap.OnMapLoadedCallback,
        GoogleMap.OnInfoWindowClickListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, ResultCallback<Status> {

    private GoogleMap mMap;
    private Marker currentMarker;
    private LatLng currentLocation;
    private LatLng iniLocation;
    private GoogleApiClient googleApiClient;
    private PendingIntent geoFencePendingIntent;
    private final int GEOFENCE_REQ_CODE = 0;

    private static final String PLACES_API_BASE = "https://maps.googleapis.com/maps/api/place/nearbysearch";
    private static final String OUT_JSON = "/json";
    private static final String LOCATION = "?location=";
    private static final String RADIUS = "&radius=";
    private static final String KEY = "&key=";
    private static final String DETAILED_PLACES_API_BASE = "https://maps.googleapis.com/maps/api/place/details";
    private static final String PLACE_ID = "?placeid=";
    private static final int mRadius = 500;
    private static final String mKey = "mKey";
    private static final float GEOFENCE_RADIUS_IN_METERS = 25.0f;
    private static final long GEOFENCE_EXPIRATION_IN_MILLISECONDS = 60 * 60 * 1000;

    ArrayList<HashMap<String, String>> resultList;
    ArrayList<Geofence> mGeofenceList;
    String place_id_current = null;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        resultList = new ArrayList<>();
        mGeofenceList = new ArrayList<>();
        // create GoogleApiClient
        createGoogleApi();
    }

    // -----------------------------------------------------------------------------------------------

    // Create GoogleApiClient instance
    private void createGoogleApi() {
        Log.d("createGoogleApi", "createGoogleApi()");
        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
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

    // -----------------------------------------------------------------------------------------------

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
        mMap = googleMap;

        // Add a marker on Campus and move the camera
        iniLocation = new LatLng(34.4140, -119.8489);

        if (getCurrentLocation() != null) {
            iniLocation = getCurrentLocation();
        }

        currentMarker = mMap.addMarker(new MarkerOptions()
                .position(iniLocation)
                .title("Current Location")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))); // customize color
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(iniLocation, 17.0f));
        mMap.setOnMapLoadedCallback(this);
        mMap.setOnInfoWindowClickListener(this);

    }

    @Override
    public void onMapLoaded() {
        //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 17.0f));

        // send request and get result - on new Thread
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d("Run new Thread", "Run new Thread");

                    // TODO set [changed current location] to request
                    //currentLocation = getCurrentLocation();
                    requestGetNearbyPlace(iniLocation);

                    // add marker - on Main Thread
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Log.d("Run UI Thread", "Run UI Thread");
                            // add marker to nearby places
                            for (int i = 0; i < resultList.size(); i++) {
                                LatLng pos = new LatLng(Double.parseDouble(resultList.get(i).get("lat")),
                                        Double.parseDouble(resultList.get(i).get("lng")));
                                MarkerOptions mo = new MarkerOptions()
                                        .position(pos)
                                        .title(resultList.get(i).get("name"))
                                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                                Marker geoFenceMarker = mMap.addMarker(mo);
                                drawGeofence(geoFenceMarker);

                                //Build new GeoFences
                                mGeofenceList.add(createGeofence(resultList.get(i).get("name"),
                                        resultList.get(i).get("lat"),
                                        resultList.get(i).get("lng")));
                                GeofencingRequest request = createGeofenceRequest(mGeofenceList.get(i));
                                addGeofence(request);
                            }
                            Log.d("mGeofenceList size", "" + mGeofenceList.size());
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    // -----------------------------------------------------------------------------------------------

    // Create a Geofence Request
    private GeofencingRequest createGeofenceRequest(Geofence geofence) {
        return new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build();
    }

    // Create a Geofence
    private Geofence createGeofence(String geofence_id, String lat, String lng) {
        return new Geofence.Builder()
                .setRequestId(geofence_id)
                .setCircularRegion(Double.parseDouble(lat), Double.parseDouble(lng), GEOFENCE_RADIUS_IN_METERS)
                .setExpirationDuration(GEOFENCE_EXPIRATION_IN_MILLISECONDS)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();
    }

    // draw geoFenceLimits
    private void drawGeofence(Marker geoFenceMarker) {
        //Log.d("drawGeofence","drawGeofence");
        CircleOptions circleOptions = new CircleOptions()
                .center( geoFenceMarker.getPosition())
                .strokeColor(Color.argb(100, 150,150,150) )
                .fillColor( Color.argb(100, 150,150,150) )
                .radius( GEOFENCE_RADIUS_IN_METERS );
        mMap.addCircle( circleOptions );
    }

    private PendingIntent createGeofencePendingIntent() {
        if (geoFencePendingIntent != null) {
            return geoFencePendingIntent;
        }
        Intent intent = new Intent(this, GeofenceTrasitionService.class);
        return PendingIntent.getService(
                this, GEOFENCE_REQ_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    // Add the created GeofenceRequest to the device's monitoring list
    private void addGeofence(GeofencingRequest request) {
        if (checkPermission()) {
            LocationServices.GeofencingApi.addGeofences(
                    googleApiClient,
                    request,
                    createGeofencePendingIntent()
            ).setResultCallback(MapsActivity.this);
        }
    }

    static Intent makeNotificationIntent(Context geofenceService, String msg) {
        Log.d("msg",msg);
        return new Intent(geofenceService,MapsActivity.class);
    }

    private boolean checkPermission() {
        // Ask for permission if it wasn't granted yet
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED );
    }

    // -----------------------------------------------------------------------------------------------

    // Check for permission to access Location
    public LatLng getCurrentLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        final int REQ_PERMISSION = 1234;

        if (ActivityCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Location loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            if (loc == null) {
                // fall back to network if GPS is not available
                loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
            if (loc != null) {
                double myLat = loc.getLatitude();
                double myLng = loc.getLongitude();
                LatLng currLoc = new LatLng(myLat, myLng);

                // location changed >[currentLocation]

                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER, 0, 0,
                        new LocationListener() {
                            public void onLocationChanged(Location location) {
                                // code to run when user's location changes
                                currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                                Toast.makeText(getApplicationContext(), "Move to " + location.getLatitude() + " " + location.getLongitude(), Toast.LENGTH_SHORT).show();

                                // create path
                                mMap.addCircle(new CircleOptions()
                                        .center(currentLocation)
                                        .radius(2)
                                        .strokeColor(Color.TRANSPARENT)
                                        .fillColor(Color.BLACK));
                                // move current Marker
                                currentMarker.setPosition(currentLocation);

                            }

                            public void onStatusChanged(String prov, int stat, Bundle b) {
                            }

                            public void onProviderEnabled(String provider) {
                            }

                            public void onProviderDisabled(String provider) {
                            }
                        }
                );
                return currLoc;
            }
        } else {
            Toast.makeText(getApplicationContext(), "Missing permissions to access location.", Toast.LENGTH_SHORT).show();
            // set app to ask for permissions
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQ_PERMISSION);
            return null;
        }
        return null;
    }

    // -----------------------------------------------------------------------------------------------

    public boolean requestGetNearbyPlace(LatLng l) throws IOException, JSONException {

        //https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=-33.8670522,151.1957362&radius=500&key=my_key

        StringBuilder sb = new StringBuilder(PLACES_API_BASE);
        sb.append(OUT_JSON);
        sb.append(LOCATION);
        sb.append(l.latitude + "," + l.longitude);
        sb.append(RADIUS);
        sb.append(mRadius);
        sb.append(KEY);
        sb.append(mKey);

        URL url = new URL(sb.toString());
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        InputStreamReader in = new InputStreamReader(connection.getInputStream());
        int read;
        char[] buff = new char[1024];
        StringBuilder jsonResults = new StringBuilder();
        while ((read = in.read(buff)) != -1) {
            jsonResults.append(buff, 0, read);
        }

        JSONObject jsonObject = new JSONObject(jsonResults.toString());
        Log.d("jsonObject", "" + jsonObject.toString());

        // Getting JSON Array node
        JSONArray results = jsonObject.getJSONArray("results");

        // looping through All results
        for (int i = 0; i < results.length(); i++) {
            JSONObject r = results.getJSONObject(i);

            JSONObject location = r.getJSONObject("geometry").getJSONObject("location");
            String lat = location.getString("lat");
            String lng = location.getString("lng");

            String place_id = r.getString("place_id");
            String name = r.getString("name");

            // tmp hash map for single contact
            HashMap<String, String> result = new HashMap<>();

            // adding each child node to HashMap key => value
            result.put("place_id", place_id);
            result.put("name", name);
            result.put("lat", lat);
            result.put("lng", lng);

            // adding contact to contact list
            resultList.add(result);
        }
        Log.d("resultList size", "" + resultList.size());
        return true;
    }

    // -----------------------------------------------------------------------------------------------

    @Override
    public void onInfoWindowClick(Marker marker) {

        for (int i = 0; i < resultList.size(); i++) {
            String place_name = resultList.get(i).get("name");
            if (place_name.equals(marker.getTitle())) {
                place_id_current = resultList.get(i).get("place_id");
            }
        }
        //Log.d("place_id", "" + place_id);
        Toast.makeText(this, "Info window :" + place_id_current,
                Toast.LENGTH_SHORT).show();

        // send request and get result - on new Thread
        Thread thread_d = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    openDetailPlaceWebBrowser(place_id_current);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        thread_d.start();
    }

    public void openDetailPlaceWebBrowser(String place_id) throws IOException, JSONException {

        // https://maps.googleapis.com/maps/api/place/details/json?placeid=ChIJN1t_tDeuEmsRUsoyG83frY4&key=YOUR_API_KEY

        StringBuilder sb_d = new StringBuilder(DETAILED_PLACES_API_BASE);
        sb_d.append(OUT_JSON);
        sb_d.append(PLACE_ID);
        sb_d.append(place_id);
        sb_d.append(KEY);
        sb_d.append(mKey);

        URL url_d = new URL(sb_d.toString());

        HttpURLConnection connection_d = (HttpURLConnection) url_d.openConnection();
        InputStreamReader in_d = new InputStreamReader(connection_d.getInputStream());
        int read_d;
        char[] buff_d = new char[1024];
        StringBuilder jsonResults_d = new StringBuilder();
        while ((read_d = in_d.read(buff_d)) != -1) {
            jsonResults_d.append(buff_d, 0, read_d);
        }

        JSONObject jsonObject_d = new JSONObject(jsonResults_d.toString());
        //Log.d("jsonObject detail place", "" + jsonResults_d.toString());

        // Getting JSON Array node
        JSONObject results_d = jsonObject_d.getJSONObject("result");
        String url_google = results_d.getString("url");

        Intent intent = new Intent(this, WebViewActivity.class); // open web-browser page in app
        intent.putExtra("DETAIL_PLACE_URL", url_google);
        Log.d("SEND_DETAIL_PLACE_URL", "" + url_google);
        startActivity(intent);

        // open external browser
//        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url_google.toString()));
//        startActivity(browserIntent);

    }

    // -----------------------------------------------------------------------------------------------

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i("onConnected", "onConnected()");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.w("onConnectionSuspended", "onConnectionSuspended()");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.w("onConnectionFailed", "onConnectionFailed()");
    }

    @Override
    public void onResult(@NonNull Status status) {
        Log.e("onResult", "onResult()");
    }
}
