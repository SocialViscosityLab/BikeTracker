package com.example.dani.biketracker;

import android.location.Location;

public class Ghost {
    private double speed;
    private double acceleration;
    private Location position;


    /**
     * Constructor that initialize the Ghost's location
     */
    public Ghost(){
        position = new Location(" ");
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

}
