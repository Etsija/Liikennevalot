package com.github.etsija.liikennevalot;

import android.location.Location;

public class Vehicle {

	private double  _latitude;
	private double  _longitude;
	private float   _speed;
	private long    _id_intersection;
	private double  _oldDistance;
	private double  _distance;
	private boolean _enterIntersection, _insideIntersection, _exitIntersection;
	int stopCount;
	boolean suggestGreen;
	Status status;
	
	public enum Status { OUTSIDE_INTERSECTION, ENTER_INTERSECTION, AT_INTERSECTION,	EXIT_INTERSECTION }
	
	public Vehicle() {
		this._id_intersection = -1;
		this._oldDistance = 3000.0;
		this._enterIntersection = false;
		this._insideIntersection = false;
		this._exitIntersection = false;
		this.stopCount = 0;
		this.suggestGreen = false;
		this.status = Status.OUTSIDE_INTERSECTION;
	}
	
	public Vehicle(Location location) {
		this._latitude = location.getLatitude();
		this._longitude = location.getLongitude();
		this._speed = location.getSpeed() * 3.6f;
		this._id_intersection = -1;
		this._oldDistance = 3000.0;
		this._enterIntersection = false;
		this._insideIntersection = false;
		this._exitIntersection = false;
		this.stopCount = 0;
		this.suggestGreen = false;
		this.status = Status.OUTSIDE_INTERSECTION;
	}

	// Setters
	
	public void setLatitude(double latitude) {
		this._latitude = latitude;
	}
	
	public void setLongitude(double longitude) {
		this._longitude = longitude;
	}
	
	public void setSpeed(float speed) {
		this._speed = speed * 3.6f;
	}
	
	public void setIntersectionId(long intersectionId) {
		this._id_intersection = intersectionId;
	}
	
	public void setOldDistance(double oldDistance) {
		this._oldDistance = oldDistance;
	}
	
	public void setDistance(double distance) {
		this._distance = distance;
	}
	
	// Distance of the vehicle to an intersection
	public void setDistance(Intersection intersection) {
		double startLat = this.getLatitude();
		double startLon = this.getLongitude();
		double endLat = intersection.getLatitude();
		double endLon = intersection.getLongitude();
		
		float[] results = new float[3];

		Location.distanceBetween(startLat, startLon, endLat, endLon, results);
		this.setDistance((double)results[0]);
	}
	
	public void setEnterIntersection(boolean enterIntersection) {
		this._enterIntersection = enterIntersection;
	}
	
	public void setInsideIntersection(boolean insideIntersection) {
		this._insideIntersection = insideIntersection;
	}
	
	public void setExitIntersection(boolean exitIntersection) {
		this._exitIntersection = exitIntersection;
	}
	
	// Getters
	
	public double getLatitude() {
		return this._latitude;
	}
	
	public double getLongitude() {
		return this._longitude;
	}
	
	public float getSpeed() {
		return this._speed;
	}
	
	public long getIntersectionId() {
		return this._id_intersection;
	}
	
	public double getOldDistance() {
		return this._oldDistance;
	}
	
	public double getDistance() {
		return this._distance;
	}
	
	public boolean getEnterIntersection() {
		return this._enterIntersection;
	}
	
	public boolean getInsideIntersection() {
		return this._insideIntersection;
	}
	
	public boolean getExitIntersection() {
		return this._exitIntersection;
	}
	
	// Other methods

	// Vehicle info for logging
	public String toString() {
		String strRet = _latitude + " | "
	                  + _longitude + " | "
	                  + _speed + " | "
	                  + _oldDistance + " | "
	                  + _distance + " | "
	                  + status;
		return strRet;
	}
	
	// How long has the vehicle been standing still at intersection?
	// If more than 4 seconds -> do not suggest a green light at border
	public String checkStop() {
		String strStopped;
		
		if (getSpeed() < 2) {
			stopCount++;
			strStopped = "PYSÄHTYNYT";
			if (stopCount > 4) {
				suggestGreen = false;
			}
		} else {
			//stopCount = 0;
			strStopped = "LIIKKEELLÄ";
		}
		return strStopped;
	}
}
