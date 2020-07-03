package com.example.androidtest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.LocationResult;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import io.realm.Realm;

public class LocationService extends BroadcastReceiver {

     public static  final String ACTION_PROCESS_UPDATE="com.example.androidtest.UPDATE_LOCATION";

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent!=null){

            final String action=intent.getAction();
            if(ACTION_PROCESS_UPDATE.equals(action)){
                LocationResult result= LocationResult.extractResult(intent);
                if(result!=null){
                    final Location location= result.getLastLocation();
                    final String lat=String.valueOf(location.getLatitude());
                    final String lng=String.valueOf(location.getLongitude());
                    final String timestamp=getCurrentTimeStamp();

                    try{


                        Realm realm = Realm.getDefaultInstance();
                        Number currentIdNum = realm.where(Locations.class).max("id");
                        //creating "AutoIncrement" primary key
                        int nextId;
                        if(currentIdNum == null) {
                            nextId = 1;
                        } else {
                            nextId = currentIdNum.intValue() + 1;
                        }
                        //saving
                        realm.beginTransaction();

                        //splitting strings
                        String substr=lat.substring(0,12);
                        String substr2=lng.substring(0,12);

                        Locations loc=new Locations(nextId,substr,substr2,timestamp);
                        String x="lat: "+loc.getLat()+" lng: "+loc.getLng();
                        MainActivity.getInstance().updateTVLocation("Current:\n"+x);


                        Toast.makeText(context, "new location: "+x, Toast.LENGTH_SHORT).show();

                        //saving to database
                        realm.insert(loc);
                        realm.commitTransaction();

                        updateChanges(context);
                    } catch (Exception e) {
                        Log.d("LocationService",e.getMessage());
                        //Toast.makeText(context, "Error "+e.getMessage().toString(), Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }

                }
            }
        }
    }

    private void updateChanges(Context context) {
        LocationsAdapter adapter;
        ArrayList<Locations> locs=MainActivity.getInstance().fillLocations();
        adapter=new LocationsAdapter(context,locs);
        MainActivity.getInstance().recyclerView.setAdapter(adapter);
        MainActivity.getInstance().locations.clear();
        MainActivity.getInstance().locations.addAll(locs);
        adapter.notifyDataSetChanged();
    }

    public static String getCurrentTimeStamp(){
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String currentDateTime = dateFormat.format(new Date()); // Find todays date
            return currentDateTime;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
