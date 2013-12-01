package com.github.etsija.liikennevalot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.jgeohash.GeoHashUtils;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.util.Log;

@SuppressLint("SimpleDateFormat")
public class DatabaseHelper extends SQLiteOpenHelper {

	// Tag for the logging
	private static final String LOG = "SQLITE";
	
	// Db version
	private static final int DB_VERSION = 1;
	
	// Db name
	private static final String DB_NAME = "liikennevalot.sqlite";
	
	// Table creation SQL statements
	
	// Table "area"
	// Note: no two areas can have the same name!
	private static final String CREATE_TABLE_AREA = 
		"CREATE TABLE area(id INTEGER PRIMARY KEY AUTOINCREMENT, "
						+ "timestamp DATETIME NOT NULL DEFAULT (datetime('now','localtime')), "
						+ "name TEXT UNIQUE)";
	
	// Table "intersection"
	private static final String CREATE_TABLE_INTERSECTION = 
		"CREATE TABLE intersection(id INTEGER PRIMARY KEY AUTOINCREMENT, "
								+ "id_area INTEGER NOT NULL, "
								+ "timestamp DATETIME NOT NULL DEFAULT (datetime('now','localtime')), "
								+ "geohash TEXT NOT NULL, "
								+ "latitude REAL NOT NULL, "
								+ "longitude REAL NOT NULL, "
								+ "radius REAL, "
								+ "address TEXT, "
								+ "FOREIGN KEY(id_area) REFERENCES area(id) ON DELETE CASCADE)";

	private static final String CREATE_TABLE_TRAFFICLIGHT = 
		"CREATE TABLE trafficlight(id INTEGER PRIMARY KEY AUTOINCREMENT, "
								+ "id_intersection INTEGER NOT NULL, "
								+ "timestamp DATETIME NOT NULL DEFAULT (datetime('now','localtime')), "
								+ "geohash TEXT NOT NULL, "
								+ "latitude REAL NOT NULL, "
								+ "longitude REAL NOT NULL, "
								+ "light TEXT, "
								+ "FOREIGN KEY(id_intersection) REFERENCES intersection(id) ON DELETE CASCADE)";

	// Default constructor: create the db to ext SD, in application folder!
	public DatabaseHelper(Context context) {
		//super(context, DB_NAME, null, DB_VERSION);
		super(context, context.getExternalFilesDir(null).getAbsolutePath() + "/" + DB_NAME, null, DB_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		// Create required tables if they do not exist
		db.execSQL(CREATE_TABLE_AREA);
		db.execSQL(CREATE_TABLE_INTERSECTION);
		db.execSQL(CREATE_TABLE_TRAFFICLIGHT);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// On upgrade drop existing old tables
		db.execSQL("DROP TABLE IF EXISTS area");
		db.execSQL("DROP TABLE IF EXISTS intersection");
		db.execSQL("DROP TABLE IF EXISTS trafficlight");
		
		// Create new ones
		onCreate(db);
	}

	///////////////////////////////////////////////////////////////////////////
	// CRUD operations
	///////////////////////////////////////////////////////////////////////////
	
	///////////////////////////////////////////////////////////////////////////
	// Table "area"
	///////////////////////////////////////////////////////////////////////////
	
	// Create a new row
	public long createArea(Area area) {
		SQLiteDatabase db = this.getWritableDatabase();
		
		// Insert data
		ContentValues values = new ContentValues();
		values.put("name", area.getName());
		
		// Insert the new row to table
		long id = db.insert("area", null, values);
		return id;
	}
		
	// Fetch a row based on area id
	@SuppressLint("SimpleDateFormat")
	public Area getArea(long id) {
		Area retArea = new Area();
		SQLiteDatabase db = this.getReadableDatabase();
		
		String sqlQuery = "SELECT * FROM area WHERE id = " + id;
		//Log.d(LOG, "getArea(id): " + sqlQuery);
		
		Cursor c = db.rawQuery(sqlQuery, null);
		if (c.moveToFirst()) {
			retArea.setId(c.getLong(c.getColumnIndex("id")));
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			try {
				String strTemp = c.getString(c.getColumnIndex("timestamp"));
				Date date = sdf.parse(strTemp);
				retArea.setTime(date);
			} catch (ParseException e) {
				e.printStackTrace();
			}
			retArea.setName(c.getString(c.getColumnIndex("name")));
		}
		return retArea;
	}

	// Fetch a row based on the name of the area
	@SuppressLint("SimpleDateFormat")
	public Area getArea(String name) {
		Area retArea = new Area("NOT_IN_DB");
		SQLiteDatabase db = this.getReadableDatabase();
		
		String sqlQuery = "SELECT * FROM area WHERE name = '" + name + "'";
		//Log.d(LOG, "getArea(name): " + sqlQuery);
		
		Cursor c = db.rawQuery(sqlQuery, null);
		if (c.moveToFirst()) {
			retArea.setId(c.getLong(c.getColumnIndex("id")));
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			try {
				String strTemp = c.getString(c.getColumnIndex("timestamp"));
				Date date = sdf.parse(strTemp);
				retArea.setTime(date);
			} catch (ParseException e) {
				e.printStackTrace();
			}
			retArea.setName(c.getString(c.getColumnIndex("name")));
		}
		return retArea;
	}
	
	// Fetch the whole table into a list
	@SuppressLint("SimpleDateFormat")
	public List<Area> getAllAreas() {
		List<Area> retAreas = new ArrayList<Area>();
		SQLiteDatabase db = this.getReadableDatabase();

		String sqlQuery = "SELECT * FROM area";
		//Log.d(LOG, "getAllAreas(): " + sqlQuery);
		
		Cursor c = db.rawQuery(sqlQuery, null);
		if (c.moveToFirst()) {
			do {
				Area retArea = new Area();
				retArea.setId(c.getLong(c.getColumnIndex("id")));
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				try {
					String strTemp = c.getString(c.getColumnIndex("timestamp"));
					Date date = sdf.parse(strTemp);
					retArea.setTime(date);
				} catch (ParseException e) {
					e.printStackTrace();
				}
				retArea.setName(c.getString(c.getColumnIndex("name")));
				retAreas.add(retArea);
			} while (c.moveToNext());
		}
		return retAreas;
	}

	public int updateArea(Area area) {
		SQLiteDatabase db = this.getWritableDatabase();
		
		ContentValues values = new ContentValues();
		values.put("name", area.getName());
		
		// Update a row
		return db.update("area", values, "id = ?", new String[] { String.valueOf(area.getId()) });
	}
	
	// NOTE: Deletions of areas SHOULD cascade into intersections and trafficlights (needs to be tested)
	
	// Delete a row based on id
	public void deleteArea(long id) {
		SQLiteDatabase db = this.getWritableDatabase();
		db.delete("area", "id = ?", new String[] { String.valueOf(id) });
	}
		
	// Delete a row based on area name
	public void deleteArea(String name) {
		SQLiteDatabase db = this.getWritableDatabase();
		//db.delete("area", "name = ?", new String[] { String.valueOf(name) });
		db.delete("area", "name = ?", new String[] { name });
	}
	
	// Delete the newest record from area table
	public void deleteNewestArea() {
		SQLiteDatabase db = this.getWritableDatabase();
		
		String sqlQuery = "SELECT * FROM area ORDER BY timestamp DESC LIMIT 1";
		//Log.d(LOG, "deleteNewestArea(): " + sqlQuery);
		
		Cursor c = db.rawQuery(sqlQuery, null);
		if (c.moveToFirst()) {
			deleteArea(c.getLong(c.getColumnIndex("id")));
		}
	}
		
	///////////////////////////////////////////////////////////////////////////
	// Table "intersection"
	///////////////////////////////////////////////////////////////////////////
	
	// Create a new row.  An Area object needs to be passed for the relation between the tables
	public long createIntersection(Area area, Intersection intersection) {
		SQLiteDatabase db = this.getWritableDatabase();
		
		// Insert data
		ContentValues values = new ContentValues();
		values.put("id_area", area.getId());  // FK
		values.put("geohash", intersection.getGeohash());
		values.put("latitude", intersection.getLatitude());
		values.put("longitude", intersection.getLongitude());
		values.put("radius", intersection.getRadius());
		values.put("address", intersection.getAddress());	
		
		// Insert the new row to table
		long id = db.insert("intersection", null, values);
		return id;
	}

	// Fetch a row based on id
	@SuppressLint("SimpleDateFormat")
	public Intersection getIntersection(long id) {
		Intersection retIntersection = new Intersection();
		SQLiteDatabase db = this.getReadableDatabase();
		
		String sqlQuery = "SELECT * FROM intersection WHERE id = " + id;
		//Log.d(LOG, sqlQuery);
		
		Cursor c = db.rawQuery(sqlQuery, null);
		if (c.moveToFirst()) {
			retIntersection.setId(c.getLong(c.getColumnIndex("id")));
			retIntersection.setIdArea(c.getLong(c.getColumnIndex("id_area")));
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			try {
				String strTemp = c.getString(c.getColumnIndex("timestamp"));
				Date date = sdf.parse(strTemp);
				retIntersection.setTime(date);
			} catch (ParseException e) {
				e.printStackTrace();
			}
			retIntersection.setGeohash(c.getString(c.getColumnIndex("geohash")));
			retIntersection.setLatitude(c.getDouble(c.getColumnIndex("latitude")));
			retIntersection.setLongitude(c.getDouble(c.getColumnIndex("longitude")));
			retIntersection.setRadius(c.getDouble(c.getColumnIndex("radius")));
			retIntersection.setAddress(c.getString(c.getColumnIndex("address")));
		}
		return retIntersection;
	}

	// Fetch a row based on the geohash of the intersection coordinates
	@SuppressLint("SimpleDateFormat")
	public Intersection getIntersection(String geohash) {
		Intersection retIntersection = new Intersection();
		SQLiteDatabase db = this.getReadableDatabase();
		
		String sqlQuery = "SELECT * FROM intersection WHERE geohash = '" + geohash + "'";
		//Log.d(LOG, sqlQuery);
		
		Cursor c = db.rawQuery(sqlQuery, null);
		if (c.moveToFirst()) {
			retIntersection.setId(c.getLong(c.getColumnIndex("id")));
			retIntersection.setIdArea(c.getLong(c.getColumnIndex("id_area")));
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			try {
				String strTemp = c.getString(c.getColumnIndex("timestamp"));
				Date date = sdf.parse(strTemp);
				retIntersection.setTime(date);
			} catch (ParseException e) {
				e.printStackTrace();
			}
			retIntersection.setGeohash(c.getString(c.getColumnIndex("geohash")));
			retIntersection.setLatitude(c.getDouble(c.getColumnIndex("latitude")));
			retIntersection.setLongitude(c.getDouble(c.getColumnIndex("longitude")));
			retIntersection.setRadius(c.getDouble(c.getColumnIndex("radius")));
			retIntersection.setAddress(c.getString(c.getColumnIndex("address")));
		}
		return retIntersection;
	}

	// Fetch the whole table into a list of Intersection objects
	@SuppressLint("SimpleDateFormat")
	public List<Intersection> getAllIntersections() {
		List<Intersection> retIntersections = new ArrayList<Intersection>();
		SQLiteDatabase db = this.getReadableDatabase();
		
		String sqlQuery = "SELECT * FROM intersection";
		//Log.d(LOG, sqlQuery);
		
		Cursor c = db.rawQuery(sqlQuery, null);
		if (c.moveToFirst()) {
			do {
				Intersection retIntersection = new Intersection();
				retIntersection.setId(c.getLong(c.getColumnIndex("id")));
				retIntersection.setIdArea(c.getLong(c.getColumnIndex("id_area")));
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				try {
					String strTemp = c.getString(c.getColumnIndex("timestamp"));
					Date date = sdf.parse(strTemp);
					retIntersection.setTime(date);
				} catch (ParseException e) {
					e.printStackTrace();
				}
				retIntersection.setGeohash(c.getString(c.getColumnIndex("geohash")));
				retIntersection.setLatitude(c.getDouble(c.getColumnIndex("latitude")));
				retIntersection.setLongitude(c.getDouble(c.getColumnIndex("longitude")));
				retIntersection.setRadius(c.getDouble(c.getColumnIndex("radius")));
				retIntersection.setAddress(c.getString(c.getColumnIndex("address")));
				retIntersections.add(retIntersection);
			} while (c.moveToNext());
		}
		return retIntersections;
	}
	
	// Fetch all intersections belonging to an area
	@SuppressLint("SimpleDateFormat")
	public List<Intersection> getIntersectionsOfArea(Area area) {
		List<Intersection> retIntersections = new ArrayList<Intersection>();
		SQLiteDatabase db = this.getReadableDatabase();
		
		String sqlQuery = "SELECT i.* "
						+ "FROM area AS a, intersection AS i "
						+ "WHERE a.name = '" + area.getName() + "' "
						+ "AND a.id = i.id_area";
		//Log.d(LOG, sqlQuery);
		
		Cursor c = db.rawQuery(sqlQuery, null);
		if (c.moveToFirst()) {
			do {
				Intersection retIntersection = new Intersection();
				retIntersection.setId(c.getLong(c.getColumnIndex("id")));
				retIntersection.setIdArea(c.getLong(c.getColumnIndex("id_area")));
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				try {
					String strTemp = c.getString(c.getColumnIndex("timestamp"));
					Date date = sdf.parse(strTemp);
					retIntersection.setTime(date);
				} catch (ParseException e) {
					e.printStackTrace();
				}
				retIntersection.setGeohash(c.getString(c.getColumnIndex("geohash")));
				retIntersection.setLatitude(c.getDouble(c.getColumnIndex("latitude")));
				retIntersection.setLongitude(c.getDouble(c.getColumnIndex("longitude")));
				retIntersection.setRadius(c.getDouble(c.getColumnIndex("radius")));
				retIntersection.setAddress(c.getString(c.getColumnIndex("address")));
				retIntersections.add(retIntersection);
			} while (c.moveToNext());
		}
		return retIntersections;
	}

	// Fetch the intersections closest to this location into a list of Intersection objects
	public List<Intersection> getNearestIntersections(Area area, String geohash, int hashLength) {	
		List<Intersection> retIntersections = new ArrayList<Intersection>();
		String strTemp;
		
		SQLiteDatabase db = this.getReadableDatabase();
		
		List aa = GeoHashUtils.getAllAdjacentAreasList(geohash.substring(0,hashLength));
		//Log.d(LOG, aa.toString());
		
		strTemp = "(";
		for (int i = 0; i <= aa.size()-2; i++) {
			strTemp += "'" + aa.get(i).toString().substring(0,hashLength) + "', ";
		}
		strTemp += "'" + aa.get(aa.size()-1).toString().substring(0,hashLength) + "')";
		
		String sqlQuery = "SELECT i.* "
						+ "FROM area as a, intersection AS i "
						+ "WHERE a.name = '" + area.getName() + "' "
						+ "AND a.id = i.id_area "
						+ "AND SUBSTR(geohash,1," + hashLength + ") IN " + strTemp;
		
		Cursor c = db.rawQuery(sqlQuery, null);
		if (c.moveToFirst()) {
			do {
				Intersection retIntersection = new Intersection();
				retIntersection.setId(c.getLong(c.getColumnIndex("id")));
				retIntersection.setIdArea(c.getLong(c.getColumnIndex("id_area")));
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				try {
					String strTemp2 = c.getString(c.getColumnIndex("timestamp"));
					Date date = sdf.parse(strTemp2);
					retIntersection.setTime(date);
				} catch (ParseException e) {
					e.printStackTrace();
				}
				retIntersection.setGeohash(c.getString(c.getColumnIndex("geohash")));
				retIntersection.setLatitude(c.getDouble(c.getColumnIndex("latitude")));
				retIntersection.setLongitude(c.getDouble(c.getColumnIndex("longitude")));
				retIntersection.setRadius(c.getDouble(c.getColumnIndex("radius")));
				retIntersection.setAddress(c.getString(c.getColumnIndex("address")));
				retIntersections.add(retIntersection);
			} while (c.moveToNext());
		}
		return retIntersections;
	}
	
	// Fetch the intersections with an invalid street address (empty or beginning with "ADDR_")
	public List<Intersection> getIntersectionsWithInvalidAddress() {	
		List<Intersection> retIntersections = new ArrayList<Intersection>();
		
		SQLiteDatabase db = this.getReadableDatabase();
		
		String sqlQuery = "SELECT * "
						+ "FROM intersection "
						+ "WHERE address IS NULL "
						+ "OR address = '' "
						+ "OR address LIKE '%ADDR%'";
		
		Cursor c = db.rawQuery(sqlQuery, null);
		if (c.moveToFirst()) {
			do {
				Intersection retIntersection = new Intersection();
				retIntersection.setId(c.getLong(c.getColumnIndex("id")));
				retIntersection.setIdArea(c.getLong(c.getColumnIndex("id_area")));
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				try {
					String strTemp2 = c.getString(c.getColumnIndex("timestamp"));
					Date date = sdf.parse(strTemp2);
					retIntersection.setTime(date);
				} catch (ParseException e) {
					e.printStackTrace();
				}
				retIntersection.setGeohash(c.getString(c.getColumnIndex("geohash")));
				retIntersection.setLatitude(c.getDouble(c.getColumnIndex("latitude")));
				retIntersection.setLongitude(c.getDouble(c.getColumnIndex("longitude")));
				retIntersection.setRadius(c.getDouble(c.getColumnIndex("radius")));
				retIntersection.setAddress(c.getString(c.getColumnIndex("address")));
				retIntersections.add(retIntersection);
			} while (c.moveToNext());
		}
		return retIntersections;
	}
	
	// Update a row
	public int updateIntersection(Intersection intersection) {
		SQLiteDatabase db = this.getWritableDatabase();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		ContentValues values = new ContentValues();
		values.put("id_area", intersection.getIdArea());
		values.put("timestamp", dateFormat.format(intersection.getTime()));
		values.put("geohash", intersection.getGeohash());
		values.put("latitude", intersection.getLatitude());
		values.put("longitude", intersection.getLongitude());
		values.put("radius", intersection.getRadius());
		values.put("address", intersection.getAddress());
		
		// Update a row
		return db.update("intersection", values, "id = ?", new String[] { String.valueOf(intersection.getId()) });
	}
	
	// Temp method to do a batch update to the intersection geohashes in case
	// intersection coordinates are manually changed
    public void updateIntersectionGeohashes() {
    	SQLiteDatabase db = this.getWritableDatabase();
    	List<Intersection> intersections = new ArrayList<Intersection>();
    	
    	intersections = getAllIntersections();
    	for (Intersection intersection : intersections) {
    		//Log.d("SQLITE", "(old) " + intersection.getGeohash() + " : "
    		//	+ GeoHashUtils.encode(intersection.getLatitude(), intersection.getLongitude()) + " (new)");
    		intersection.setGeohash(GeoHashUtils.encode(intersection.getLatitude(), intersection.getLongitude()));
    	}
    	
        	
    }
	
	// NOTE: Deletions of intersections SHOULD cascade (needs to be tested)
	
	// Delete a row from "intersection" table based on id
	public void deleteIntersection(long id) {
		SQLiteDatabase db = this.getWritableDatabase();
		db.delete("intersection", "id = ?", new String[] { String.valueOf(id) });
	}
	
	// Delete a row from "intersection" table based on geohash
	public void deleteIntersection(String geohash) {
		SQLiteDatabase db = this.getWritableDatabase();
		//db.delete("area", "geohash = ?", new String[] { String.valueOf(geohash) });
		db.delete("intersection", "geohash = ?", new String[] { geohash });
	}
	
	// Delete the newest record from intersection table
	public void deleteNewestIntersectionFromArea(Area area) {
		SQLiteDatabase db = this.getWritableDatabase();
		
		String sqlQuery = "SELECT * FROM intersection WHERE id_area = " + area.getId() + " ORDER BY timestamp DESC LIMIT 1";
		//Log.d(LOG, sqlQuery);
		
		Cursor c = db.rawQuery(sqlQuery, null);
		if (c.moveToFirst()) {
			deleteIntersection(c.getLong(c.getColumnIndex("id")));
		}
	}
	
	///////////////////////////////////////////////////////////////////////////
	// Table "trafficlight"
	///////////////////////////////////////////////////////////////////////////

	// Create a new trafficlight record without referring to an intersection ("orphan trafficlight event")
	public long createTrafficlight(Trafficlight trafficlight) {
		SQLiteDatabase db = this.getWritableDatabase();
		//SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); 
		
		// Insert data
		ContentValues values = new ContentValues();
		//values.put("timestamp", dateFormat.format(trafficlight.getTime()));
		values.put("id_intersection", trafficlight.getIdIntersection());
		values.put("geohash", trafficlight.getGeohash());
		values.put("latitude", trafficlight.getLatitude());
		values.put("longitude", trafficlight.getLongitude());
		values.put("light", trafficlight.getLight());
		
		// Insert the new row to table
		long id = db.insert("trafficlight", null, values);
		return id;
	}
	
	// Create a new trafficlight record and put a relation to an intersection
	public long createTrafficlight(Intersection intersection, Trafficlight trafficlight) {
		SQLiteDatabase db = this.getWritableDatabase();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		// Insert data
		ContentValues values = new ContentValues();
		values.put("id_intersection", intersection.getId());  // FK
		values.put("timestamp", dateFormat.format(trafficlight.getTime()));
		values.put("geohash", trafficlight.getGeohash());
		values.put("latitude", trafficlight.getLatitude());
		values.put("longitude", trafficlight.getLongitude());
		values.put("light", trafficlight.getLight());
		
		// Insert the new row to table
		long id = db.insert("trafficlight", null, values);
		return id;
	}
	
	// Fetch the whole table into a list of Intersection objects
	@SuppressLint("SimpleDateFormat")
	public List<Trafficlight> getAllTrafficlights() {
		List<Trafficlight> retTrafficlights = new ArrayList<Trafficlight>();
		SQLiteDatabase db = this.getReadableDatabase();
		
		String sqlQuery = "SELECT * FROM trafficlight";
		//Log.d(LOG, sqlQuery);
		
		Cursor c = db.rawQuery(sqlQuery, null);
		if (c.moveToFirst()) {
			do {
				Trafficlight retTrafficlight = new Trafficlight();
				retTrafficlight.setId(c.getLong(c.getColumnIndex("id")));
				retTrafficlight.setIdIntersection(c.getLong(c.getColumnIndex("id_intersection")));
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				try {
					String strTemp = c.getString(c.getColumnIndex("timestamp"));
					Date date = sdf.parse(strTemp);
					retTrafficlight.setTime(date);
				} catch (ParseException e) {
					e.printStackTrace();
				}
				retTrafficlight.setGeohash(c.getString(c.getColumnIndex("geohash")));
				retTrafficlight.setLatitude(c.getDouble(c.getColumnIndex("latitude")));
				retTrafficlight.setLongitude(c.getDouble(c.getColumnIndex("longitude")));
				retTrafficlight.setLight(c.getString(c.getColumnIndex("light")));
				retTrafficlights.add(retTrafficlight);
			} while (c.moveToNext());
		}
		return retTrafficlights;
	}
	
	// Get all trafficlight events of an area, either RED or GREEN
	public int getLightsOfArea(Area area, String light) {
		SQLiteDatabase db = this.getReadableDatabase();
		
		String sqlQuery = "SELECT t.* FROM"
						+ " (SELECT i.* FROM area AS a, intersection AS i"
						+ "  WHERE a.name = '" + area.getName() + "' AND a.id = i.id_area)"
						+ "AS tmp, trafficlight AS t "
						+ "WHERE tmp.id = t.id_intersection "
						+ "AND t.light = '" + light + "'";
		Cursor c = db.rawQuery(sqlQuery, null);
		return c.getCount();
	}
	
	// Get trafficlight counts of an intersection
	public int getLightsOfIntersection(Intersection intersection, String light) {
		SQLiteDatabase db = this.getReadableDatabase();
		
		String sqlQuery = "SELECT * FROM trafficlight "
				        + "WHERE id_intersection = " + intersection.getId() + " "
				        + "AND light = '" + light + "'";
		Cursor c = db.rawQuery(sqlQuery, null);
		return c.getCount();
	}
	
	// Update a row
	public int updateTrafficlight(Trafficlight trafficlight) {
		SQLiteDatabase db = this.getWritableDatabase();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		ContentValues values = new ContentValues();
		values.put("id_intersection", trafficlight.getIdIntersection());
		values.put("timestamp", dateFormat.format(trafficlight.getTime()));
		values.put("geohash", trafficlight.getGeohash());
		values.put("latitude", trafficlight.getLatitude());
		values.put("longitude", trafficlight.getLongitude());
		values.put("light", trafficlight.getLight());
		
		// Update a row
		return db.update("trafficlight", values, "id = ?", new String[] { String.valueOf(trafficlight.getId()) });
	}
	
	public long createLinkToIntersection(Area area, Trafficlight trafficlight, double radius) {
		List<Intersection> intersections = new ArrayList<Intersection>();
		
		intersections = getIntersectionsOfArea(area);
		for (Intersection intersection : intersections) {
			double startLat = trafficlight.getLatitude();
			double startLon = trafficlight.getLongitude();
			double endLat   = intersection.getLatitude();
			double endLon   = intersection.getLongitude();
			
			float[] results = new float[3];

			Location.distanceBetween(startLat, startLon, endLat, endLon, results);
			float distance = results[0];
			
			if (distance <= radius) {
				String str = "trafficlight: " + trafficlight.getId()
					+ " links to intersection: " + intersection.getId()
					+ " distance: " + distance;
				Log.d("SQLITE", str);
				trafficlight.setIdIntersection(intersection.getId());
			}
		}
		updateTrafficlight(trafficlight);
		
		return trafficlight.getIdIntersection();
	}
	
	// Delete a row from "intersection" table based on id
	public void deleteTrafficlight(long id) {
		SQLiteDatabase db = this.getWritableDatabase();
		db.delete("trafficlight", "id = ?", new String[] { String.valueOf(id) });
	}
	
	// Delete the newest RED or GREEN light in an area
	public void deleteNewestTrafficlightFromArea(Area area, String light) {
		SQLiteDatabase db = this.getWritableDatabase();
		
		String sqlQuery = "SELECT t.* FROM"
				+ " (SELECT i.* FROM area AS a, intersection AS i"
				+ "  WHERE a.name = '" + area.getName() + "' AND a.id = i.id_area)"
				+ "AS tmp, trafficlight AS t "
				+ "WHERE tmp.id = t.id_intersection "
				+ "AND t.light = '" + light + "' "
				+ "ORDER BY t.timestamp DESC LIMIT 1";
		Cursor c = db.rawQuery(sqlQuery, null);
		if (c.moveToFirst()) {
			deleteTrafficlight(c.getLong(c.getColumnIndex("id")));
		}
	}	
}
