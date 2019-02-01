package Core;

import android.annotation.SuppressLint;
import java.util.HashMap;
import java.util.Map;


public class DataPoint {

    /**Current register time of the user*/
    private long time;

    /**Current register longitude of the user*/
    private double longitude;

    /**Current register latitude of the user*/
    private double latitude;

    /**Suggestion to accelerate, stop or keep the current speed. It be -1, to stop, 0 to keep the velocity or 1 to accelerate*/
    private int suggestion;

    /**Current register speed of the user*/
    private float speed;

    /**Current register acceleration of the user*/
    private float acceleration;


    public DataPoint(long time, double longitude, double latitude, int suggestion, float speed, float acceleration){
        this.time = time;
        this.longitude = longitude;
        this.latitude = latitude;
        this.suggestion = suggestion;
        this.speed = speed;
        this.acceleration = acceleration;
    }

    /** Method to get a map with the structure of the
     * dataBase document for a DaraPoint
     * */
    public Map<String, Object> getDataPointDocument(){
        Map<String, Object> dataPointDoc = new HashMap<>();
        dataPointDoc.put("longitude", longitude);
        dataPointDoc.put("latitude", latitude);
        dataPointDoc.put("suggestion", suggestion);
        dataPointDoc.put("speed", speed);
        dataPointDoc.put("acceleration", acceleration);
        dataPointDoc.put("time", time);

        return dataPointDoc;

    }

    /** Get the string of the datapoint
     * with the 5-digits format
     * */
    public String getID(int i) {
        @SuppressLint("DefaultLocale") String id = String.format("%05d", i);
        return id;
    }

    public long getTime() {
        return time;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public int getSuggestion() {
        return suggestion;
    }

    public float getSpeed() {
        return speed;
    }

    public float getAcceleration() {
        return acceleration;
    }
}
