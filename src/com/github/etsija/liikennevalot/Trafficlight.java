package com.github.etsija.liikennevalot;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Trafficlight {

	long _id;				// PK
	long _id_intersection;	// FK
	Date _time;
	String _geohash;
	double _latitude;
	double _longitude;
	String _light;
	
	// Constructors
	
	public Trafficlight() {
	}
	
	public Trafficlight(Date time, String light) {
		this._time = time;
		this._light = light;
	}
	
	public Trafficlight(long id, long id_intersection, Date time, String geohash, 
						double latitude, double longitude, String light) {
		this._id = id;
		this._id_intersection = id_intersection;
		this._time = time;
		this._geohash = geohash;
		this._latitude = latitude;
		this._longitude = longitude;
		this._light = light;
	}
	
	// Setters
	
	public void setId(long id) {
		this._id = id;
	}
	
	public void setIdIntersection(long id_intersection) {
		this._id_intersection = id_intersection;
	}
	
	public void setTime(Date time) {
		this._time = time;
	}
	
	public void setGeohash(String geohash) {
		this._geohash = geohash;
	}
	
	public void setLatitude(double latitude) {
		this._latitude = latitude;
	}
	
	public void setLongitude(double longitude) {
		this._longitude = longitude;
	}
	
	public void setLight(String light) {
		this._light = light;
	}
	
	// Getters
	
	public long getId() {
		return this._id;
	}
	
	public long getIdIntersection() {
		return this._id_intersection;
	}
	
	public Date getTime() {
		return this._time;
	}
	
	public String getGeohash() {
		return this._geohash;
	}
	
	public double getLatitude() {
		return this._latitude;
	}
	
	public double getLongitude() {
		return this._longitude;
	}
	
	public String getLight() {
		return this._light;
	}
	
	// Other methods
	
	public String toString() {
		SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
		String strRet = _id + " | "
	                  + _id_intersection + " | "
	                  + formatter.format(_time) + " | "
	                  + _geohash + " | "
	                  + _latitude + " | "
	                  + _longitude + " | "
	                  + _light;
		return strRet;
	}
}