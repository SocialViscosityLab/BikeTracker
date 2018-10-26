package com.example.dani.biketracker;

import android.annotation.SuppressLint;
import android.location.Location;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;


/**
 * Created by Dani on 13/09/18.
 */

public class Logic extends Thread{
    // My current location
    private Location myLoc;
    //Reference of my logic
    private static Logic ref=null;
    //Reference context
    private DatabaseReference dbRef;
    private FirebaseFirestore db;
    //Firebase user
    private FirebaseUser user;

    //Session arraylist
    private ArrayList<Session>sessions;
    //Ref of the current session
    private Session currentSession;
    //Is connected with the Firebase database
    private  boolean isConnected;
    //Boolean to know if is currently creating data for a session
    private boolean isInSession;
    //Loop state
    private boolean isRunning;
    //Current user ID
    private String id_user;
    //Current session ID TODO: ask for the last id in the database to know the next id to use
    private int id_session;
    //Current session time in millis
    private Long session_time;

    private static String TAG = "DEBUGGING";



    @SuppressLint("DefaultLocale")
    private Logic(){
        myLoc = new Location(" ");
        //id_user = String.format("%05d", 1);

        //Set the value to run thread
        isRunning =  true;
        //Initialize the sessions arraylist
        sessions = new ArrayList<Session>();

    }


    public static Logic getInstance(){
        if(ref == null) ref = new Logic();
        return ref;
    }

    //return the current time with the format YYYY/MM/dd - HH:mm:ss
    private String getStartTime(){
        @SuppressLint("SimpleDateFormat") DateFormat formatter = new SimpleDateFormat("YYYY/MM/dd - HH:mm:ss");
        return  String.valueOf(formatter.format(currentDateAndTime().getTime()));
        //TODO: this time should be replaced by the server absolute time
    }

    //Return the current date and work as a time manager
    private Calendar currentDateAndTime(){
        return Calendar.getInstance();
    }

    //Creates a new session in the arrayList sessions and start the counter
    public void startNewSession(){
        @SuppressLint("DefaultLocale") String string_session_id = String.format("%05d", id_session);
        sessions.add(new Session(string_session_id, id_user, getStartTime()));
        currentSession = sessions.get(sessions.size()-1);
        session_time = currentDateAndTime().getTimeInMillis();
        isInSession = true;
    }

    //Close the current session
    public void closeSession(){
        Log.d("debugging", currentSession.getStringOfSession());
        isInSession = false;
        updateSessionInDataBase();
    }

    public void run(){
        while (isRunning){
            try {
                if(isInSession){
                    //add new data_points to the last session created every second
                    //add new data_points to the last session created every second
                    long current_session_time = currentDateAndTime().getTimeInMillis()-session_time;
                    currentSession.addNewDataPoint(current_session_time,myLoc.getLongitude(),myLoc.getLatitude(), 0, 0, 0);
                }

                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void setUser(FirebaseUser user){
        this.user = user;
        id_user = user.getUid();
    }



    public void firestoreSetup() {
        // [START get_firestore_instance]
        db = FirebaseFirestore.getInstance();
        // [END get_firestore_instance]

        // [START set_firestore_settings]
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();
        db.setFirestoreSettings(settings);
        // [END set_firestore_settings]
    }

    public void readFirestore(){
        db.collection("routes")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Log.d(TAG, document.getId() + " => " + document.getData());
                            }
                        } else {
                            Log.w(TAG, "Error getting documents.", task.getException());
                        }
                    }
                });
    }

    public  DatabaseReference getFDB(){
        // Write a message to the database
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("message");

        myRef.setValue("Hello, World!");

        return myRef;
    }

    public void updateSessionInDataBase(){


// Add a new document with a generated ID
        db.collection("sessions")
                .add(currentSession.getSessionDocument())
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error adding document", e);
                    }
                });
/*
// Add a new document with a generated ID
        db.collection("Sessions")
                .add(currentSession)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error adding document", e);
                    }
                });
*/
/*
            final String path = "sessions";
            dbRef = FirebaseDatabase.getInstance().getReference(path);
            dbRef.setValue(sessions.get(sessions.size()-1));
  */

    }
    public void setDataBase(DatabaseReference dbRef){
        Log.d("debugging", "DB SETTED");

        isConnected = true;
        this.dbRef = dbRef;

    }

    public Location getMyLoc(){
        return myLoc;
    }
    public void setMyLoc(Location myLoc){this.myLoc = myLoc;}

    public ArrayList<Session> getSessions() {
        return sessions;
    }

    public boolean isInSession() {
        return isInSession;
    }
}
