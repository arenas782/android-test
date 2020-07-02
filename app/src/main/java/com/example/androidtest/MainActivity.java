package com.example.androidtest;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;


import android.provider.Settings;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;


import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.util.ArrayList;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {

    static MainActivity instance;
    public static MainActivity getInstance() {
        return instance;
    }


    TextView text_location;

     RecyclerView recyclerView;

    private RecyclerView.LayoutManager layoutManager;
    SwipeRefreshLayout swipeRefreshLayout;

    ArrayList<String> locations;
    LocationsAdapter adapter;



    LocationRequest locationRequest;
    FusedLocationProviderClient fusedLocationProviderClient;

    Realm realm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        text_location=(TextView)findViewById(R.id.text_location);
        instance = this;
        Realm.init(this);
        realm = Realm.getDefaultInstance();
        locations=fillLocations();

        swipeRefreshLayout=(SwipeRefreshLayout)findViewById(R.id.swipe_layout);
        swipeRefreshLayout.setOnRefreshListener(this);


        recyclerView = (RecyclerView) findViewById(R.id.my_recycler_view);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        adapter=new LocationsAdapter(this,locations);
        recyclerView.setAdapter(adapter);


        BroadcastReceiver receiver=new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.d("Action",action);
                Bundle b=intent.getExtras();

            }
        };




        final RealmResults<Locations> records = realm.where(Locations.class).findAll();
        records.size(); // => 0 because no dogs have been added to the Realm yet
        Log.d("Main","Total records: "+records.size());
        //mAdapter = new RVAdapter(myDataset);
        //recyclerView.setAdapter(mAdapter);

        Dexter.withActivity(this).withPermission(Manifest.permission.ACCESS_FINE_LOCATION).withListener(
                new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        updateLocation();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        Toast.makeText(MainActivity.this, "Please give permissions to app", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {

                    }
                }
        ).check();

    }

    private void updateLocation() {
        buildLocationRequest();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, getPendintIntent());
    }

    private PendingIntent getPendintIntent() {
        Intent intent =new Intent(this,LocationService.class);
        intent.setAction(LocationService.ACTION_PROCESS_UPDATE);
        return PendingIntent.getBroadcast(this,0,intent,PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void buildLocationRequest() {
        locationEnabled();
        locationRequest=new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(60000); //every minute
        //locationRequest.setFastestInterval(30000); //every 30 seconds
        locationRequest.setSmallestDisplacement(10f);
        LocationSettingsRequest.Builder builder=new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
        builder.setAlwaysShow(true);


    }



    public void updateTVLocation(final String texto){
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                text_location.setText(texto);
            }
        });
    }


    /**
     * check if gps is active, otherwise prompts user to enable it
     */
    private void locationEnabled () {
        LocationManager lm = (LocationManager)
                getSystemService(Context. LOCATION_SERVICE ) ;
        boolean gps_enabled = false;
        boolean network_enabled = false;
        try {
            gps_enabled = lm.isProviderEnabled(LocationManager. GPS_PROVIDER ) ;
        } catch (Exception e) {
            e.printStackTrace() ;
        }
        try {
            network_enabled = lm.isProviderEnabled(LocationManager. NETWORK_PROVIDER ) ;
        } catch (Exception e) {
            e.printStackTrace() ;
        }
        if (!gps_enabled && !network_enabled) {
            new AlertDialog.Builder(MainActivity. this )
                    .setMessage( "Please enable GPS for location tracking" )
                    .setPositiveButton( "Settings" , new
                            DialogInterface.OnClickListener() {
                                @Override
                                public void onClick (DialogInterface paramDialogInterface , int paramInt) {
                                    startActivity( new Intent(Settings. ACTION_LOCATION_SOURCE_SETTINGS )) ;
                                }
                            })
                    .setNegativeButton( "Cancel" , null )
                    .show() ;
        }
    }


    @Override
    public void onResume() {
        // ... calling super.onResume(), etc...
        // Perform the Realm database query
        super.onResume();
        locations=fillLocations();
        adapter.notifyDataSetChanged();
    }

    public ArrayList<String> fillLocations(){
        ArrayList<String> locations=new ArrayList<>();
        RealmResults<Locations> results=realm.where(Locations.class).sort("id", Sort.DESCENDING).findAll();

        for(Locations loc:results)
        {
            locations.add(loc.toString());
        }

        return locations;
    }

    @Override
    public void onRefresh() {
        locations.clear();
        locations.addAll(fillLocations());
        adapter=new LocationsAdapter(this,locations);
        recyclerView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        swipeRefreshLayout.setRefreshing(false);
    }
}