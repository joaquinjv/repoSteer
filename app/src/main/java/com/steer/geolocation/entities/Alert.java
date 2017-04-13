package com.steer.geolocation.entities;

/**
 * Created by pedro on 18/10/16.
 */

public class Alert {

    private String Description;
    private float Latitude;
    private float Longitude;

    public Alert() {
    }

    public Alert(String description, float latitude, float longitude) {
        Description = description;
        Latitude = latitude;
        Longitude = longitude;
    }

    public String getDescription() {
        return Description;
    }

    public void setDescription(String description) {
        Description = description;
    }

    public float getLatitude() {
        return Latitude;
    }

    public void setLatitude(float latitude) {
        Latitude = latitude;
    }

    public float getLongitude() {
        return Longitude;
    }

    public void setLongitude(float longitude) {
        Longitude = longitude;
    }
}
