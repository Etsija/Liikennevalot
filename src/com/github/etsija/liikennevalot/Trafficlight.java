package com.github.etsija.liikennevalot;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Trafficlight {

	long _id;				// PK
	long _id_intersection;	// FK
	Date _time;
	String _light;
	
	// Constructors
	
	public Trafficlight() {
	}
	
	public Trafficlight(Date time, String light) {
		this._time = time;
		this._light = light;
	}
	
	public Trafficlight(long id, long id_intersection, Date time, String light) {
		this._id = id;
		this._id_intersection = id_intersection;
		this._time = time;
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
	
	public String getLight() {
		return this._light;
	}
	
	// Other methods
	
	public String toString() {
		SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
		String strRet = _id + " | "
	                  + _id_intersection + " | "
	                  + formatter.format(_time) + " | "
	                  + _light;
		return strRet;
	}
}