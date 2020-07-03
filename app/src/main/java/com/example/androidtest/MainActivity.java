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
import android.view.View;
import android.widget.Button;
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

    private SwipeRefreshLayout swipeRefreshLayout;
    LocationsAdapter adapter;
    ArrayList<Locations> locations;


    private LocationRequest locationRequest;
    private Realm realm;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        text_location=(TextView)findViewById(R.id.text_location);
        Button clear = (Button) findViewById(R.id.clear);
        swipeRefreshLayout=(SwipeRefreshLayout)findViewById(R.id.swipe_layout);
        swipeRefreshLayout.setOnRefreshListener(this);
        recyclerView = (RecyclerView) findViewById(R.id.my_recycler_view);
        recyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);


        instance = this;
        Realm.init(this);
        realm = Realm.getDefaultInstance();


        locations=fillLocations();
        adapter=new LocationsAdapter(this,locations);
        recyclerView.setAdapter(adapter);




        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearAllLocations();
            }
        });




        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(swipeCallback());
        itemTouchHelper.attachToRecyclerView(recyclerView);

        checkPermissions();




    }

    public void checkPermissions(){
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


    public ItemTouchHelper.SimpleCallback swipeCallback(){
        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT | ItemTouchHelper.DOWN | ItemTouchHelper.UP) {

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                Toast.makeText(MainActivity.this, "on Move", Toast.LENGTH_SHORT).show();
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                if(locations.size()>0){
                    final int position = viewHolder.getAdapterPosition();

                    final int id=locations.get(position).getId();

                    Toast.makeText(MainActivity.this, "Deleted "+id, Toast.LENGTH_SHORT).show();
                    //Remove swiped item from list and notify the RecyclerView

                    //remove from database
                    realm.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            RealmResults<Locations> result = realm.where(Locations.class).equalTo("id",id).findAll();
                            result.deleteAllFromRealm();
                        }
                    });
                    locations.clear();
                    locations.addAll(fillLocations());
                    adapter=new LocationsAdapter(MainActivity.this,locations);
                    recyclerView.setAdapter(adapter);
                }

            }
        };
        return simpleItemTouchCallback;
    }


    private void clearAllLocations() {
        if(locations.size()>0){
            locations.clear();
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    RealmResults<Locations> result = realm.where(Locations.class).findAll();
                    result.deleteAllFromRealm();
                }
            });
            adapter=new LocationsAdapter(MainActivity.this,locations);
            adapter.notifyDataSetChanged();
            recyclerView.setAdapter(adapter);
            text_location.setText("No records.");
            Toast.makeText(this, "Cleaned", Toast.LENGTH_SHORT).show();
        }
        else
            Toast.makeText(this, "Nothing to clean", Toast.LENGTH_SHORT).show();
    }

    private void updateLocation() {
        buildLocationRequest();
        FusedLocationProviderClient fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
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
        locationRequest.setFastestInterval(30000); //every 30 seconds
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




    public ArrayList<Locations> fillLocations(){
        RealmResults<Locations> results=realm.where(Locations.class).sort("id", Sort.DESCENDING).findAll();
        return new ArrayList<>(results);
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