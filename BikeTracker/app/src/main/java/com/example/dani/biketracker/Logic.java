package com.example.dani.biketracker;

import android.annotation.SuppressLint;
import android.location.Location;
import android.support.annotation.NonNull;
import android.util.Log;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.annotations.Nullable;
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
import java.util.Objects;
import Core.Route;
import Core.Session;
import sensors.Synchro;

/**
 * Created by Dani on 13/09/18.
 */

public class Logic extends Thread {
    //Reference of my logic
    private static Logic ref = null;
    //Debugging string
    private static String TAG = "DEBUGGING";
    // --- Database management variables --- //

    //Reference to the firestore database
    private FirebaseFirestore db;
    // Boolean that confirms the creation of the session in the database
    private Boolean sessionInDatabase;


    // --- Session management variables --- //

    //Session arraylist
    private ArrayList<Session> sessions;
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
    //Reference route of the journey
    private Route route;
    //Current session ID
    private String id_session;
    //Current session time in millis
    private Long session_time;
    /** Indicates that the user is at least at 5 meters away or less of the route path **/
    private boolean onRoute;
    /** reference route's name **/
    private String routeName;

    // --- Location management variables --- //

    // My current location
    private Location myLoc;
    // Last Speed to calculate the acceleration
    private float lastSpeed;
    //CurrentAcceleration
    private float acceleration;

    //-- The ghost variables -- //
    private Ghost ghost;
    private int suggestion;
    private float gap;

    //-- The visible target for the user -- //
    private Location currentTarget;

    // -- Observer pattern to change the interface with the suggestion --- //
    private Synchro observer;

    // --- Bluetooth management variables --- //
    //private ComunicationBluetooth comunicationBluetooth;


    /**
     * Logic constructor that
     * initialize the session's array
     */
    private Logic() {
        myLoc = new Location(" ");
        currentTarget = myLoc;
        //Set the value to run thread
        isRunning = true;
        //Initialize the sessions array list
        sessions = new ArrayList<>();
        sessionInDatabase = false;
        ghost = new Ghost();
        suggestion = 0;
        gap = 0;
        observer = new Synchro();
    }


    /**
     * Use the singleton pattern to return the current instance of the Logic object if exist,
     * if not, create a new Logic object.
     *
     * @return Current instance of the Logic object.
     */
    public static Logic getInstance() {
        if (ref == null) ref = new Logic();
        return ref;
    }

    /**
     * Creates a new session in the arrayList of sessions,
     * references it in the currentSession Session object, initializes the time,
     * get the last location as the first location, of the session and set to
     * true the isInSession boolean.
     */
    public void startNewSession() {
        sessions.add(new Session(id_session, id_user, getStartTime()));
        currentSession = sessions.get(sessions.size() - 1);
        session_time = currentDateAndTime().getTimeInMillis();
        findJourney();
        acceleration = 0;
    }

    /**
     * Set to false the isInSession boolean and call the method
     * to update the current session in the data base.
     */
    public void closeSession() {
        isInSession = false;
        updateSessionInDataBase();
    }

    /**
     * Manage the thread to create a data point
     * every second since the current session started until
     * it finishes.
     * TODO: define if the session should automatically stop if the ghost the last route point and is not looped
     */
    public void run() {
        while (isRunning) {

            try {
                if (isInSession) {
                    //add new data_points to the last session created every second
                    long current_session_time = currentDateAndTime().getTimeInMillis() - session_time;

                    float speed = myLoc.getSpeed();

                    //Acceleration calculation
                    acceleration = speed - lastSpeed;

                    currentSession.addNewDataPoint(current_session_time, myLoc.getLongitude(), myLoc.getLatitude(), calculateSuggestion(), speed, acceleration);

                    updateCurrentPosition();

                    gap = route.getAtoBDistance(ghost.getPosition(), myLoc);
                    Log.d(TAG, "You are at " + gap + " meters away from the ghost.");

                    lastSpeed = speed;
                }

                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    /* ---------- Methods to process and get information -----------------*/


    /**
     * Ask for the current system time and format it as YYYY/MM/dd - HH:mm:ss.
     * This time will be replaced by the server for the server time.
     * @return Current time with the format YYYY/MM/dd - HH:mm:ss as a string.
     */

    private String getStartTime() {
        @SuppressLint("SimpleDateFormat") DateFormat formatter = new SimpleDateFormat("YYYY/MM/dd - HH:mm:ss");
        return String.valueOf(formatter.format(currentDateAndTime().getTime()));
    }


    /**
     * Ask for an instance of the system's calendar.
     *
     * @return Current date in form of a time manager object.
     */
    private Calendar currentDateAndTime() {
        return Calendar.getInstance();
    }

    /**
     * Method to calculate the suggestion given the distance
     * between the ghost and the user in the route
     */
    private int calculateSuggestion(){
        float gap = route.getAtoBDistance(ghost.getPosition(), myLoc);

        float bikeLength = 2;
        // the selected value
        float desiredProximity = 4;
        //The value calculated by the live time of the green wave and the speed of the ghost
        float greenWaveTime = 5;
        float greenWaveLength = (float) (greenWaveTime * ghost.getSpeed());
        float errorRange = 1.5f;

        if(gap > 0){
            if(gap <= (bikeLength + desiredProximity + errorRange) && gap >= (bikeLength + desiredProximity - errorRange)){
                suggestion=0;
            }else{
                suggestion = 1;
            }
        }else{
            suggestion=-1;
        }

        observer.updateChange();

        return suggestion;
    }

    /** Method that define what target to show depending on the current position and the ghost position.
     * if the ghost in in the same segment than the user, the target will be the ghost
     * If the ghost is not in the same segment than the user, the target will be the closer corner to the ghost in the user's segment
     */
    public Location getTarget(){
        if(isInSession){
            currentTarget = ghost.getPosition();
            //Log.d(TAG, "Current  segment: "+ route.getCurrentSegment());
            //Log.d(TAG, "Ghost  segment: "+ route.getGhostSegment());
            if((float)route.getProjection().get("distance") > 5){
                currentTarget = (Location) route.getProjection().get("projected_loc");
                onRoute = false;
            }else{
                onRoute = true;
                if(route.getCurrentSegment() == route.getGhostSegment()){
                    currentTarget = ghost.getPosition();
                }else if(suggestion < 0){
                    currentTarget = route.getCurrentSegment().getStart();
                }else if(suggestion > 0){
                    currentTarget = route.getCurrentSegment().getEnd();
                }
            }

        }
        return currentTarget;
    }

    /* ---------- DataBase set up methods -----------------*/


    /**
     * Set the session's user id from the firebase's user
     * that previously logged in
     *
     * @param user User that was logged in the fireBase interface
     */
    public void setUser(FirebaseUser user) {
        id_user = user.getUid();
    }

    /**
     * Set the session's user id from the firebase's user
     * that previously logged in
     */
    public void firestoreSetup() {
        db = FirebaseFirestore.getInstance();

        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();
        db.setFirestoreSettings(settings);
    }


    /* ---------- Methods to consult the DataBase -----------------*/

    /**
     * Method that set the journey's id by reading all
     * the current journeys in the database and identifying
     * last one
     * TODO: It might be useful to find the active journey with a attribute
     */
    private void findJourney() {
        db.collection("journeys")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @SuppressLint("DefaultLocale")
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            int tempId = 0;

                            //Journey's reference route
                            DocumentReference refRoute = null;

                            for (QueryDocumentSnapshot document : Objects.requireNonNull(task.getResult())) {
                                int id = Integer.parseInt(document.getId());
                                if (id > tempId) {
                                    tempId = id;
                                    refRoute = (DocumentReference) document.getData().get("reference_route");
                                }
                            }
                            id_journey = String.format("%05d", tempId);

                            getReferenceRoute(Objects.requireNonNull(refRoute));
                            getSessionId();
                        } else {
                            Log.w(TAG, "Error getting documents.", task.getException());
                        }
                    }
                });
    }

    /**
     * Method that create the object Route with the boolean of the loop state
     */
    private void getReferenceRoute(final DocumentReference refRoute) {
        refRoute.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                boolean loop = (boolean) Objects.requireNonNull(documentSnapshot.getData()).get("loop");
                routeName = documentSnapshot.getId();

                route = new Route(loop);
                getReferenceRoutePoints(refRoute);
            }
        });
    }

    /**
     * Method to set the data points
     * of the reference route from the journey
     *
     * @param refRoute fireStore reference object to get the reference route details
     */

    private void getReferenceRoutePoints(DocumentReference refRoute) {
        refRoute.collection("position_points")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            Location positionPoints[] = new Location[Objects.requireNonNull(task.getResult()).size()];

                            int index = 0;

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Location tempL = new Location("");

                                tempL.setLatitude((double) document.getData().get("latitude"));
                                tempL.setLongitude((double) document.getData().get("longitude"));
                                positionPoints[index] = tempL;
                                index++;
                            }
                            route.setRoutePoints(positionPoints);
                            followGhost();
                            isInSession = true;

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
     */
    private void getSessionId() {
        db.collection("journeys").document(id_journey).collection("sessions")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @SuppressLint("DefaultLocale")
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
     * Check the current position on the fireStore database
     * every second to process the needed suggestion.
     */
    private void followGhost() {
        Log.w(TAG, "Start following the ghost");
        final DocumentReference docRef = db.collection("journeys").document(id_journey).collection("sessions").document("00000");
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

                    if (ghostPositionDoc != null) {
                        Location tempL = new Location("");
                        tempL.setLatitude((double) ghostPositionDoc.get("latitude"));
                        tempL.setLongitude((double) ghostPositionDoc.get("longitude"));
                        Double speed = 0d;
                        Double acceleration = 0d;

                        if (ghostPositionDoc.get("speed") instanceof Long) {
                            Long tempS = (Long) ghostPositionDoc.get("speed");
                            speed = tempS.doubleValue();
                        } else {
                            speed = (Double) ghostPositionDoc.get("speed");

                        }
                        if (ghostPositionDoc.get("acceleration") instanceof Long) {
                            Long tempA = (Long) ghostPositionDoc.get("acceleration");
                            acceleration = tempA.doubleValue();

                        } else {
                            acceleration = (Double) ghostPositionDoc.get("acceleration");
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


    /* ---------- Methods to write in the data base -----------------*/

    /**
     * Create the basic information of the new session
     */
    private void setNewSession() {
        db.collection("journeys").document(id_journey).collection("sessions").document(id_session).set(currentSession.getSessionDoc());
        sessionInDatabase = true;
    }

    /**
     * Create the basic information of the new session
     */
    private void updateCurrentPosition() {
        if (sessionInDatabase) {
            db.collection("journeys").document(id_journey).collection("sessions").document(id_session).update(currentSession.getCurrentPosition());
        }
    }

    /**
     * Add a new document with the information of the session
     * with a numeric id consecutive to the
     * last id founded in the sessions of the last founded
     * journey.
     */
    private void updateSessionInDataBase() {
        db.collection("journeys").document(id_journey).collection("sessions").document(id_session).update(currentSession.getDataPointsDocument());
    }

    public void setMyLoc(Location myLoc) {
        this.myLoc = myLoc;
    }

    public ArrayList<Session> getSessions() {
        return sessions;
    }

    public boolean isInSession() {
        return isInSession;
    }

    public Synchro getObserver(){ return observer; }

    public int getSuggestion(){
        return suggestion;
    }
    public float getGap(){
        return gap;
    }
    public Location getMyLoc(){ return myLoc; }
    public boolean isOnRoute(){ return onRoute; }
    public String getRouteName(){ return routeName; }

    //public void setBluetoothConnection(BluetoothDevice device){ comunicationBluetooth = new ComunicationBluetooth(device); }
}
