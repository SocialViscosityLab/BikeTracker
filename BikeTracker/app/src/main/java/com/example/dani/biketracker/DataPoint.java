package com.example.dani.biketracker;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class DataPoint {
    private long time;
    private double longitud;
    private double latitud;
    //The suggestion can be -1, to stop, 0 to keep the velocity or 1 to accelerate
    private int suggestion;
    private int speed;
    private int acceleration;

    public DataPoint(long time, double longitud, double latitud, int suggestion, int speed, int acceleration){
        this.time = time;
        this.longitud = longitud;
        this.latitud = latitud;
        this.suggestion = suggestion;
        this.speed = speed;
        this.acceleration = acceleration;
    }

    public Map<String, Object> getDataPointDocument(){
        // Create a new user with a first and last name
        Map<String, Object> dataPointDoc = new HashMap<>();
        dataPointDoc.put("longitud", longitud);
        dataPointDoc.put("latitud", latitud);
        dataPointDoc.put("suggestion", suggestion);
        dataPointDoc.put("speed", speed);
        dataPointDoc.put("acceleration", acceleration);
        dataPointDoc.put("time", time);


        return dataPointDoc;

    }
    public String getID(int i) {
        String id = String.format("%05d", i);
        return id;
    }

    public long getTime() {
        return time;
    }

    public double getLongitud() {
        return longitud;
    }

    public double getLatitud() {
        return latitud;
    }

    public int getSuggestion() {
        return suggestion;
    }

    public int getSpeed() {
        return speed;
    }

    public int getAcceleration() {
        return acceleration;
    }
}
