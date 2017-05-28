package com.example.tcoon.location2;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.heatmaps.Gradient;
import com.google.maps.android.heatmaps.HeatmapTileProvider;
import com.google.maps.android.heatmaps.WeightedLatLng;

import java.util.ArrayList;

public class MainActivity   extends AppCompatActivity
                            implements OnMapReadyCallback {

    private boolean f = false;
    private LocationManager locationManager;
    private GoogleMap map;
    DBHelper dbHelper;
    ContentValues cv;
    SQLiteDatabase db;
    private ArrayList<WeightedLatLng> weightedPointList;
    private HeatmapTileProvider heatmapTileProvider;
    private TileOverlay tileOverlay;
    TextView curValue;
    int N, M;
    final String LOG_TAG = "myLogs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (CheckPermissions.check(this))
            this.init();
        else
            this.finish();
    }

    private void init(){
        MapFragment mapFragment = (MapFragment)
                getFragmentManager().findFragmentById(R.id.mapFragment);
        mapFragment.getMapAsync(this);
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        weightedPointList = new ArrayList<>();
        dbHelper = new DBHelper(this);
        cv = new ContentValues();
        db = dbHelper.getWritableDatabase();
        curValue = (TextView) findViewById(R.id.curValues);
        Cursor c = db.query("points", null, null, null, null, null, null);
        N = c.getCount();
        M = 0;
        curValue.setText(Integer.toString(N) + " " + Integer.toString(M));
    }

    public void runButtonClick(View v){
        ImageButton runButton = (ImageButton) findViewById(R.id.runButton);
        if (this.f){
            runButton.setImageResource(R.drawable.ic_media_play);
            this.f = false;
            this.stop();
        }else{
            runButton.setImageResource(R.drawable.ic_media_pause);
            this.f = true;
            this.start();
        }
    }

    public void updButtonClick(View v){
        heatmapTileProvider.setWeightedData(weightedPointList);
        tileOverlay.clearTileCache();
        M = 0;
        curValue.setText(Integer.toString(N) + " " + Integer.toString(M));
        Toast.makeText(this,"Updated",Toast.LENGTH_SHORT).show();
    }

    private void start(){
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000,15,locationListener);
            Toast.makeText(this,"Start collecting",Toast.LENGTH_SHORT).show();
        }else Toast.makeText(this, "access denied", Toast.LENGTH_SHORT).show();

    }

    private void stop(){
        locationManager.removeUpdates(locationListener);
        Toast.makeText(this,"Stop collecting",Toast.LENGTH_SHORT).show();
    }

    public void clearData(View v){
        weightedPointList.clear();
        weightedPointList.add(new WeightedLatLng(new LatLng(0,0), 0));
        heatmapTileProvider.setWeightedData(weightedPointList);
        tileOverlay.clearTileCache();
        db.delete("points",null,null);
        Cursor c = db.query("points", null, null, null, null, null, null);
        N = c.getCount();
        M = 0;
        curValue.setText(Integer.toString(N) + " " + Integer.toString(M));
        Toast.makeText(this,"Data clered",Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            map.setMyLocationEnabled(true);
        }
        map.getUiSettings().setCompassEnabled(true);
        map.getUiSettings().setZoomControlsEnabled(true);

        Cursor c = db.query("points", null, null, null, null, null, null);
        if (c.moveToFirst()){
            int latColIndex = c.getColumnIndex("Lat");
            int lngColIndex = c.getColumnIndex("Lng");
            int wghColIndex = c.getColumnIndex("Weight");
            do{
                double lat = c.getDouble(latColIndex);
                double lng = c.getDouble(lngColIndex);
                double weight = c.getDouble(wghColIndex);
                LatLng ll = new LatLng(lat,lng);
                weightedPointList.add(new WeightedLatLng(ll,weight));
            } while (c.moveToNext());
        }
        if (weightedPointList.isEmpty())
            weightedPointList.add(new WeightedLatLng(new LatLng(0,0),0.1));
        heatmapTileProvider = new HeatmapTileProvider.Builder().weightedData(weightedPointList).build();
        heatmapTileProvider.setRadius(20);
        heatmapTileProvider.setOpacity(0.5);
        int[] colors = {
                Color.rgb(0, 0, 255), // blue
                Color.rgb(255, 0, 0)    // red
        };

        float[] startPoints = {
                0.1f, 10f
        };
        Gradient gradient = new Gradient(colors, startPoints);
        heatmapTileProvider.setGradient(gradient);
        map.clear();
        tileOverlay = map.addTileOverlay(new TileOverlayOptions().tileProvider(heatmapTileProvider));
        tileOverlay.setVisible(true);

        if (locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) != null){
            LatLng pos = new LatLng(
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER).getLatitude(),
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER).getLongitude());
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(pos,15));
        }
    }

    LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            LatLng cll = new LatLng(location.getLatitude(),location.getLongitude());
            String latitudeMin = Double.toString(location.getLatitude() - 0.00001);
            String longitudeMin = Double.toString(location.getLongitude() - 0.001);
            String latitudeMax = Double.toString(location.getLatitude() + 0.00001);
            String longitudeMax = Double.toString(location.getLongitude() + 0.001);
            String args[] = {latitudeMin, latitudeMax, longitudeMin, longitudeMax};
            String sql = "select * from points where Lat > ? and Lat < ? and Lng > ? and Lng < ?";
            Cursor c = db.rawQuery(sql, args);
            double weight = 0.1;
            if (c.moveToFirst()){
                do{
                    if (weight == 10) break;
                    weight+=0.05;
                }while (c.moveToNext());
            }
            weightedPointList.add(new WeightedLatLng(
                    cll,weight)
            );
            cv.put("Lat", location.getLatitude());
            cv.put("Lng", location.getLongitude());
            cv.put("Weight", weight);

            db.insert("points",null,cv);
            N++;
            M++;
            curValue.setText(Integer.toString(N) + " " + Integer.toString(M));
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };

    class DBHelper extends SQLiteOpenHelper {

        public DBHelper(Context context) {
            super(context, "myDB", null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("create table points ("
                    + "id integer primary key autoincrement,"
                    + "Lat double,"
                    + "Lng double,"
                    + "Weight double" + ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }
    }
}
