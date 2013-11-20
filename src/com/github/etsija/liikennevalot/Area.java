package com.github.etsija.liikennevalot;

public class Area {

	int _id;		// PK
	String _name;
	
	// Constructors
	
	public Area() {
	}
	
	public Area(String name) {
		this._name = name;
	}
	
	public Area(int id, String name) {
		this._id = id;
		this._name = name;
	}
	
	// Setters
	
	public void setId(int id) {
		this._id = id;
	}
	
	public void setName(String name) {
		this._name = name;
	}
	
	// Getters
	
	public int getId() {
		return this._id;
	}
	
	public String getName() {
		return this._name;
	}
	
	// Other methods
	
	public String toString() {
		String strRet = _id + ": " + _name;
		return strRet;
	}
}