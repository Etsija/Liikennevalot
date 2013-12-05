package com.github.etsija.liikennevalot;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.text.format.Time;

public class Intersection {

	long _id;		// PK
	long _id_area;	// FK
	Date _time;
	String _geohash;
	double _latitude;
	double _longitude;
	double _radius;
	String _address;
	
	// Constructors
	
	public Intersection() {
	}
	
	public Intersection(String geohash) {
		this._geohash = geohash;
	}
	
	public Intersection(long id_area, String geohash, double latitude, double longitude, double radius, String address) {
		this._id_area = id_area;
		this._geohash = geohash;
		this._latitude = latitude;
		this._longitude = longitude;
		this._radius = radius;
		this._address = address;
	}
	
	// Setters
	
	public void setId(long id) {
		this._id = id;
	}
	
	public void setIdArea(long id_area) {
		this._id_area = id_area;
	}
	
	public void setTime(Date time) {
		this._time = time;
	}
	
	@SuppressWarnings("deprecation")
	public void setTime(Time time) {
		Date date = new Date();
		date.setYear(time.year-1900);
		date.setMonth(time.month-1);
		date.setDate(time.monthDay);
		date.setHours(time.hour);
		date.setMinutes(time.minute);
		date.setSeconds(time.second);
		
		setTime(date);
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
	
	public void setRadius(double radius) {
		this._radius = radius;
	}
	
	public void setAddress(String address) {
		this._address = address;
	}
	
	// Getters
	
	public long getId() {
		return this._id;
	}
	
	public long getIdArea() {
		return this._id_area;
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
	
	public double getRadius() {
		return this._radius;
	}
	
	public String getAddress() {
		return this._address;
	}
	
	// Other methods
	
	public String toString() {
		SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
		String strRet = _id + " | "
	                  + _id_area + " | "
	                  //+ formatter.format(_time) + " | "
	                  + _geohash + " | "
	                  + _address + " | "
	                  + _latitude + " | "
	                  + _longitude + " | "
	                  + _radius;
		return strRet;
	}
}
