package com.github.etsija.liikennevalot;

import android.text.format.Time;

public class Trafficlight {

	int _id;				// PK
	int _id_intersection;	// FK
	Time _time;
	String _light;
	
	// Constructors
	
	public Trafficlight() {
	}
	
	public Trafficlight(Time time, String light) {
		this._time = time;
		this._light = light;
	}
	
	public Trafficlight(int id, int id_intersection, Time time, String light) {
		this._id = id;
		this._id_intersection = id_intersection;
		this._time = time;
		this._light = light;
	}
	
	// Setters
	
	public void setId(int id) {
		this._id = id;
	}
	
	public void setIdIntersection(int id_intersection) {
		this._id_intersection = id_intersection;
	}
	
	public void setTime(Time time) {
		this._time = time;
	}
	
	public void setLight(String light) {
		this._light = light;
	}
	
	// Getters
	
	public int getId() {
		return this._id;
	}
	
	public int getIdIntersection() {
		return this._id_intersection;
	}
	
	public Time getTime() {
		return this._time;
	}
	
	public String getLight() {
		return this._light;
	}
	
	// Other methods
	
	public String timeToString() {
		String strRet = String.format("%02d", _time.monthDay) + "."
			          + String.format("%02d", _time.month) + "." 
			          + String.format("%04d", _time.year) + " "
				      + String.format("%02d", _time.hour) + ":" 
				      + String.format("%02d", _time.minute) + ":"
				      + String.format("%02d", _time.second);
		return strRet;
	}
	
	public String toString() {
		String strRet = _id + ": "
	                  + _id_intersection + " "
	                  + timeToString() + " "
	                  + _light;
		return strRet;
	}
}