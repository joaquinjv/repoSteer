package com.steer.geolocation.entities;

/**
 * Created by mirrorlink on 4/2/17.
 */

public class PointOfSale {

    private Double latitude;

    private Double longitude;

    private String name;

    private String details;

    public PointOfSale(String name, String details, String latitude, String longitude) {
        this.latitude = Double.parseDouble(latitude);
        this.longitude = Double.parseDouble(longitude);
        this.name = name;
        this.details = details;
    }

    public PointOfSale() {
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
