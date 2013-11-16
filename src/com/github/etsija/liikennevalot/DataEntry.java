package com.github.etsija.liikennevalot;

import android.location.Location;
import android.text.format.Time;

public class DataEntry {
	private Time time;
	private String type;
	private Location loc;
	private String addr;
	
	public DataEntry() {
		super();
	}
	
	public DataEntry(Time time, String type, Location loc, String addr) {
		this.time = time;
		this.type = type;
		this.loc = loc;
		this.addr = addr;
	}
	
	public String getTime() {
		return String.format("%02d", time.monthDay) + "."
			 + String.format("%02d", time.month) + "." 
			 + String.format("%04d", time.year) + " "
			 + String.format("%02d", time.hour) + ":" 
			 + String.format("%02d", time.minute) + ":"
			 + String.format("%02d", time.second);
	}
	
	public String getType() {
		return type;
	}
	public String getLocation() {
		return loc.getLatitude() + "," + loc.getLongitude() + ",0";
	}
	
	public String getAddress() {
		return addr;
	}
}
