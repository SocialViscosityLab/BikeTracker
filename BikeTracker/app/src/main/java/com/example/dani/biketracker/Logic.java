package com.example.dani.biketracker;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.location.Location;
import android.location.LocationManager;
import android.support.annotation.NonNull;
import android.util.Log;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.annotations.Nullable;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;

/**
 * Created by Dani on 13/09/18.
 */

public class Logic extends Thread{
    //Reference of my logic
    private static Logic ref=null;
    //Debugging string
    private static String TAG = "DEBUGGING";



    // --- Database management variables --- //

    //Reference to the firestore database
    private FirebaseFirestore db;
    //Firebase user
    private FirebaseUser user;
    // Boolean that confirms the creation of the session in the database
    private Boolean sessionInDatabase;


    // --- Session management variables --- //

    //Session arraylist
    private ArrayList<Session>sessions;
    //Ref of the current session
    private Session currentSession;
    //Boolean to know if is currently creating data for a session
    private boolean isInSession;
    //Loop state
    private boolean isRunning;
    //Current user ID
    private String id_user;
    //Current journey ID
    private String id_journey;
    //Current session ID
    private String id_session;
    //Current session time in millis
    private Long session_time;


    // --- Location management variables --- //

    //Location manager
    private LocationManager lm;
    // My current location
    private Location myLoc;
    // Last locations: To temporary calculate the speed
    private Location lastLoc;
    // Last Speed to calculate the acceleration
    private float lastSpeed;
    //CurrentAcceleration
    private float acceleration;

    //-- The ghost variables -- //
    private Ghost ghost;


    // --- Bluetooth management variables --- //
    private ComunicationBluetooth comunicationBluetooth;


    /**
     * Logic constructor that
     * initialize the session's array
     *
     */
    private Logic(){
        myLoc = new Location(" ");
        //Set the value to run thread
        isRunning =  true;
        //Initialize the sessions arraylist
        sessions = new ArrayList<Session>();

        sessionInDatabase = false;

        ghost = new Ghost();

    }


    /**
     * Use the singleton pattern to return the current instance of the Logic object if exist,
     * if not, create a new Logic object.
     *
     * @return Current instance of the Logic object.
     */
    public static Logic getInstance(){
        if(ref == null) ref = new Logic();
        return ref;
    }


    /**
     * Ask for the current system time and format it as YYYY/MM/dd - HH:mm:ss.
     * This time will be replaced by the server for the server time.
     *
     * @return Current time with the format YYYY/MM/dd - HH:mm:ss as a string.
     */
    private String getStartTime(){
        @SuppressLint("SimpleDateFormat") DateFormat formatter = new SimpleDateFormat("YYYY/MM/dd - HH:mm:ss");
        return  String.valueOf(formatter.format(currentDateAndTime().getTime()));
    }


    /**
     * Ask for an instance of the system's calendar.
     *
     * @return Current date in form of a time manager object.
     */
    private Calendar currentDateAndTime(){
        return Calendar.getInstance();
    }


    /**
     * Creates a new session in the arrayList of sessions,
     * references it in the currentSession Session object, initializes the time,
     * get the last location as the first location, of the session and set to
     * true the isInSession boolean.
     */
    public void startNewSession(){
        sessions.add(new Session(id_session, id_user, getStartTime()));
        currentSession = sessions.get(sessions.size()-1);
        session_time = currentDateAndTime().getTimeInMillis();
        findJourney();
        lastLoc = myLoc;
        isInSession = true;
        acceleration = 0;
        followGhost();

    }


    /**
     * Set to false the isInSession boolean and call the method
     * to update the current session in the data base.
     */
    public void closeSession(){
        isInSession = false;
        updateSessionInDataBase();
    }


    /**
     * Manage the thread to create a data point
     * every second since the current session started until
     * it finishes.
     * TODO: get the speed and the acceleration from a sensor and not this calculation
     */
    public void run(){
        while (isRunning){

            try {
                if(isInSession){
                    //add new data_points to the last session created every second
                    long current_session_time = currentDateAndTime().getTimeInMillis()-session_time;

                    //Temporal Speed calculation
                    float speed = myLoc.distanceTo(lastLoc);

                    //Acceleration calculation
                    acceleration = speed - lastSpeed;

                    currentSession.addNewDataPoint(current_session_time,myLoc.getLongitude(),myLoc.getLatitude(), ghost.suggest(myLoc), speed, acceleration);
                    updateCurrentPosition();
                    lastLoc = myLoc;
                    lastSpeed = speed;
                }

                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * Set the session's user id from the firebase's user
     * that previously logged in
     * @param user
     */
    public void setUser(FirebaseUser user){
        this.user = user;
        id_user = user.getUid();
    }


    /**
     * Set the session's user id from the firebase's user
     * that previously logged in
     */
    //Method to connect with the database
    public void firestoreSetup() {
        db = FirebaseFirestore.getInstance();

        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();
        db.setFirestoreSettings(settings);
    }

    /**
     * Method that set the journey's id by reading all
     * the current journeys in the database and identifying
     * last one
     * TODO: It might be useful to find the active journey with a attribute
     */
    public void findJourney(){
        db.collection("journeys")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            int tempId = 0;
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                int id = Integer.parseInt(document.getId());
                                if(id > tempId){
                                    tempId = id;
                                }
                            }
                            id_journey = String.format("%05d", tempId);
                            getSessionId();
                        } else {
                            Log.w(TAG, "Error getting documents.", task.getException());
                        }
                    }
                });
    }


    /**
     * Method that set the session's id by reading all
     * the current sessions in the database, identifying
     * last one and finding the next consecutive numerical id
     * TODO: This method should be called just before writing the session to avoid overlapping
     */
    public void getSessionId(){
        db.collection("journeys").document(id_journey).collection("sessions")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            id_session = String.format("%05d", task.getResult().size());
                            setNewSession();
                        } else {
                            Log.w(TAG, "Error getting documents.", task.getException());
                        }
                    }
                });
    }

    /**
     * Create the basic information of the new session
     */
    public void setNewSession(){
        db.collection("journeys").document(id_journey).collection("sessions").document(id_session).set(currentSession.getSessionDoc());
        sessionInDatabase = true;
    }

    /**
     * Create the basic information of the new session
     */
    public void updateCurrentPosition(){
        if(sessionInDatabase) {
            db.collection("journeys").document(id_journey).collection("sessions").document(id_session).update(currentSession.getCurrentPosition());
        }
    }

    /**
     * Add a new document with the information of the session
     * with a numeric id consecutive to the
     * last id founded in the sessions of the last founded
     * journey.
     */
    public void updateSessionInDataBase(){

        db.collection("journeys").document(id_journey).collection("sessions").document(id_session).update(currentSession.getDataPointsDocument());

                /*
                addOnSuccessListener(new OnSuccessListener<DocumentChange>() {

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
    }

    /**
     * Check the current position on the firestore database
     * every second to process the needed suggestion.
     */
    public void followGhost(){
        final DocumentReference docRef = db.collection("journeys").document("0").collection("sessions").document("00000");
        docRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot snapshot,
                                @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e);
                    return;
                }

                if (snapshot != null && snapshot.exists()) {
                    Map<String, Object> ghostPositionDoc = (Map<String, Object>) snapshot.getData().get("current_ghost_position");

                    if(ghostPositionDoc != null) {
                        //Log.d(TAG, "Ghost: time = " + ghostPositionDoc.get("time"));
                        Location tempL = new Location("");
                        tempL.setLatitude((double) ghostPositionDoc.get("latitude"));
                        tempL.setLongitude((double)ghostPositionDoc.get("longitude"));
                        Double speed = 0d;
                        Double acceleration = 0d;

                        if(ghostPositionDoc.get("speed") instanceof Long){
                            Long tempS = (Long) ghostPositionDoc.get("speed");
                            speed = tempS.doubleValue();
                        }else{
                            speed = (Double) ghostPositionDoc.get("speed");

                        }
                        if(ghostPositionDoc.get("acceleration") instanceof Long){
                            Long tempA = (Long) ghostPositionDoc.get("acceleration");
                            acceleration = tempA.doubleValue();

                        }else {
                            acceleration= (Double) ghostPositionDoc.get("acceleration");
                        }

                        ghost.updateGhost(tempL, speed, acceleration);
                        //Log.d(TAG, "Ghost: Latitude = " + ghost.getPosition().getLatitude() + ", Longitude = " + ghost.getPosition().getLongitude() + ", Speed = " + ghost.getSpeed() + " , Acceleration = " + ghost.getAcceleration());
                    }

                } else {
                    Log.d(TAG, "Current data: null");
                }
            }
        });
    }
    public void setMyLoc(Location myLoc){ this.myLoc=myLoc; }

    public void setLocationManager(LocationManager lm){ this.lm = lm; }

    public ArrayList<Session> getSessions() { return sessions; }

    public boolean isInSession() { return isInSession; }

    public void setBluetoothConnection(BluetoothDevice device){ comunicationBluetooth = new ComunicationBluetooth(device); }
}
