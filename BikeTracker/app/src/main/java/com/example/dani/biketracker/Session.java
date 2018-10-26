package com.example.dani.biketracker;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Session {
    private String id_session;
    private String id_user;
    private String start_time;
    private ArrayList <DataPoint> data_points;
    private JSONObject jsonSession;
    private JSONArray jsonDataPointsArray;

    public Session(String id_session, String id_user, String start_time) {
        this.id_session = id_session;
        this.id_user = id_user;
        this.start_time = start_time;
        data_points =  new ArrayList<DataPoint>();
        jsonSession = new JSONObject();
        jsonDataPointsArray = new JSONArray();

    }

    public String getId_session() {
        return id_session;
    }

    public String getId_user() {
        return id_user;
    }

    public String getStart_time() {
        return start_time;
    }

    public ArrayList<DataPoint> getData_points() {
        return data_points;
    }
    public void addNewDataPoint(long time, double longitud, double latitud, int suggestion, int speed, int acceleration){
        data_points.add(new DataPoint(time, longitud, latitud, suggestion, speed, acceleration));
    }

    public Map<String, Object> getSessionDocument(){
        // Create a new user with a first and last name
        Map<String, Object> sessionDoc = new HashMap<>();
        sessionDoc.put("id_user", id_user);
        sessionDoc.put("start_time", start_time);

        Map<String, Object> dataPointsDoc = new HashMap<>();
        for( int i = 0; i<data_points.size(); i++){
        DataPoint temp = data_points.get(i);
            dataPointsDoc.put(temp.getID(i), temp.getDataPointDocument());
        }
        sessionDoc.put("data_points", dataPointsDoc);


        return sessionDoc;

    }

    public JSONObject getJsonSession() {
        return jsonSession;
    }

    public String getStringOfSession()  {
        try {
            jsonSession.put("id_session", id_session);
            jsonSession.put("id_user", id_user);
            jsonSession.put("start_time", start_time);
            for(DataPoint i : data_points){
                JSONObject jot  = new JSONObject();
                jot.put("time", i.getTime());
                jot.put("longitude", i.getLongitud());
                jot.put("latitude", i.getLatitud());
                jot.put("suggestion", i.getSuggestion());
                jot.put("speed", i.getSpeed());
                jot.put("acceleration", i.getAcceleration());
                jsonDataPointsArray.put(jot);
            }
            jsonSession.put("data_points",jsonDataPointsArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonSession.toString();
    }
}
