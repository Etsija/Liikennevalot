package com.github.etsija.liikennevalot;

public class Intersection {

	int _id;		// PK
	int _id_area;	// FK
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
	
	public Intersection(int id, int id_area, String geohash, double latitude, double longitude, double radius, String address) {
		this._id = id;
		this._id_area = id_area;
		this._geohash = geohash;
		this._latitude = latitude;
		this._longitude = longitude;
		this._radius = radius;
		this._address = address;
	}
	
	// Setters
	
	public void setId(int id) {
		this._id = id;
	}
	
	public void setIdArea(int id_area) {
		this._id_area = id_area;
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
	
	public int getId() {
		return this._id;
	}
	
	public int getIdArea() {
		return this._id_area;
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
		String strRet = _id + ": "
	                  + _id_area + " "
	                  + _geohash + " "
	                  + _address + " "
	                  + _latitude + " "
	                  + _longitude + " "
	                  + _radius;
		return strRet;
	}
}
