package com.example.androidtest;

import androidx.annotation.NonNull;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class Locations extends RealmObject {
    /**
     * realm model object
     */
    @PrimaryKey
    private int id;
    private String lat;
    private String lng;
    private String timestamp;

    public Locations(int id,String lat, String lng, String timestamp) {
        this.id=id;
        this.lat = lat;
        this.lng = lng;
        this.timestamp = timestamp;
    }

    public String getLat(){
        return this.lat;
    }
    public String getLng(){
        return this.lng;
    }
    public String getTime(){
        return this.timestamp;
    }
    public int getId(){return this.id;}

    @NonNull
    @Override
    public String toString() {
        return "Lat: "+this.lat+"\nlng: "+this.lng+"\ntime: "+this.timestamp;
    }

    public Locations(){}

}
