package com.example.dani.biketracker;

import android.location.Location;

public class Ghost {
    private double speed;
    private double acceleration;
    private Location position;
    private Location currentGoalLocation;


    /**
     * Constructor that initialize the Ghost's location
     */
    public Ghost(){
        position = new Location(" ");
        currentGoalLocation = new Location("");

        //TODO Replace this temporal goal location
        // Test to decrease the speed
        currentGoalLocation.setLongitude(-88.23411);
        currentGoalLocation.setLatitude(40.102144);

        // Test to decrease the speed
        //currentGoalLocation.setLongitude(-88.222275);
        //currentGoalLocation.setLatitude(40.113543);
    }

    /**
     * Method to update the attributes of the ghost
     * from the dataBase
     */
    public void updateGhost(Location position, double speed, double acceleration){
        this.position.setLongitude(position.getLongitude());
        this.position.setLatitude(position.getLatitude());
        this.speed = speed;
        this.acceleration = acceleration;
    }


    /**
     * Method to calculate the suggestion given to the user
     * comparing its position to the ghost's position.
     * @param followerLocation
     * @return suggestion
     */
    public int suggest(Location followerLocation){
        int suggestion = 4;
        if(followerLocation.distanceTo(currentGoalLocation)>position.distanceTo(currentGoalLocation)){
            if(followerLocation.distanceTo(position)<=5.8 && followerLocation.distanceTo(position)>=4.8){
                suggestion=0;
            }else{
                suggestion = 1;
            }
        }else{
            suggestion=-1;
        }
        return suggestion;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public double getAcceleration() {
        return acceleration;
    }

    public void setAcceleration(double acceleration) {
        this.acceleration = acceleration;
    }

    public Location getPosition() {
        return position;
    }

    public void setPosition(Location position) {
        this.position = position;
    }

    public Location getCurrentGoalLocation() {
        return currentGoalLocation;
    }

    public void setCurrentGoalLocation(Location currentGoalLocation) {
        this.currentGoalLocation = currentGoalLocation;
    }
}
