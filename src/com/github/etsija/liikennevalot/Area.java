package com.github.etsija.liikennevalot;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Area {

	long _id;		// PK
	Date _time;
	String _name;
	
	// Constructors
	
	public Area() {
	}
	
	public Area(String name) {
		this._name = name;
	}
	
	public Area(long id, String name) {
		this._id = id;
		this._name = name;
	}
	
	// Setters
	
	public void setId(long id) {
		this._id = id;
	}
	
	public void setTime(Date time) {
		this._time = time;
	}
	
	public void setName(String name) {
		this._name = name;
	}
	
	// Getters
	
	public long getId() {
		return this._id;
	}
	
	public Date getTime() {
		return this._time;
	}
	
	public String getName() {
		return this._name;
	}
	
	// Other methods
	
	public String toString() {
		SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");  
		String strRet = _id + " | " + formatter.format(_time) + " | " 
					  + _name;
		return strRet;
	}
}