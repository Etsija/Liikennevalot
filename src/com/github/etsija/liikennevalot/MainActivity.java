package com.github.etsija.liikennevalot;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import org.jgeohash.GeoHashUtils;
import com.github.etsija.liikennevalot.Vehicle.Status;

import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.support.v4.app.FragmentActivity;
import android.text.format.Time;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;

@SuppressLint("DefaultLocale")
public class MainActivity extends FragmentActivity {

	private static final double radius = 30.0;
	
	String lastList;
	Area currArea = new Area();
	Location currLocation = null;
	static Geocoder geocoder;
	LocationManager mLocationManager;
	LocationListener mLocationListener;
	DatabaseHelper db;
	List<Intersection> nearestIntersections = new ArrayList<Intersection>();
	Vehicle vehicle = new Vehicle();
	MediaPlayer mp;
	int soundGreen, soundRed;
	boolean isRedPressed = false;
	
	// UI elements
	Spinner        listat;
	Button         btnIntersection;
	Button         btnRed;
	Button         btnGreen;
	RelativeLayout layoutStatus;
	TextView       txtStatus;
	TextView       txtIntersections;
	TextView       txtSpeedCaption;
	TextView       txtSpeed;
	TextView       txtMoveStatus;
	TextView       txtStopCount;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// Location services
		mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		mLocationListener = new MyLocationListener();
	    mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
	    geocoder = new Geocoder(this, Locale.ENGLISH);  // For offline address search
	    
	    if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) { 
	        Log.d("SQLITE", "location provider enabled");
	    } else {
	    	Log.d("SQLITE", "location provider disabled");
	    }
	    
		// Fetch all preferences into prefKeys HashMap
		final SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
	    lastList = settings.getString("lastList", null);
	    
	    // Drop-down for the different lists
		listat = (Spinner) findViewById(R.id.listat);
		listat.setSelection(getIndex(listat, lastList));
		listat.setOnItemSelectedListener(areaListener);
		
	    // SQLite:
		// Loop through all items in the spinner and add a new area to SQLite table "area"
		// if such an area name doesn't exist already  
		db = new DatabaseHelper(getApplicationContext());
		for (int i = 0; i < listat.getCount(); i++) {
	    	String areaName = listat.getItemAtPosition(i).toString();
	    	if (db.getArea(areaName).getName().contains("NOT_IN_DB")) {
	    		Area newArea = new Area(areaName);
	    		db.createArea(newArea);
	    		Log.d("SQLITE", "New area added: " + db.getArea(areaName).toString());
			}
		}
		
		// Set current area based on what was last time selected
		currArea = db.getArea(lastList);	
		
		// BLUE BUTTON
		btnIntersection = (Button) findViewById(R.id.intersection);
		btnIntersection.getBackground().setColorFilter(Color.BLUE, Mode.MULTIPLY);
		btnIntersection.setTextColor(Color.WHITE);
		btnIntersection.setTextSize(20);
		btnIntersection.setOnClickListener(btnIntersectionListener);
		btnIntersection.setOnLongClickListener(btnIntersectionLongClickListener);
		if (currLocation == null)
			btnIntersection.setEnabled(false);
		
		// STATUS LAYOUT and all of its TextViews
		layoutStatus = (RelativeLayout) findViewById(R.id.layoutStatus);
		layoutStatus.setOnClickListener(null);
		layoutStatus.setOnLongClickListener(null);
		txtStatus = (TextView) findViewById(R.id.txtStatus);
		txtStatus.setText("ODOTETAAN PAIKANNUSTA...");
		txtIntersections = (TextView) findViewById(R.id.txtIntersections);
		txtSpeedCaption = (TextView) findViewById(R.id.txtSpeedCaption);
		txtSpeedCaption.setText("NOPEUS");
		txtSpeed = (TextView) findViewById(R.id.txtSpeed);
		txtSpeed.setText("*** km/h");
		txtMoveStatus = (TextView) findViewById(R.id.txtMoveStatus);
		//txtMoveStatus.setText("");
		txtMoveStatus.setText("AVG " + db.getWaitingtimeOfArea(currArea) + " s");
		txtStopCount = (TextView) findViewById(R.id.txtStopCount);
		txtStopCount.setText("");
		
		// RED BUTTON
		btnRed = (Button) findViewById(R.id.buttonRed); 
		btnRed.getBackground().setColorFilter(Color.RED, Mode.MULTIPLY);
		btnRed.setTextColor(Color.WHITE);
		btnRed.setTextSize(20);
		btnRed.setOnClickListener(btnRedListener);
		btnRed.setOnLongClickListener(btnRedLongClickListener);
		setButtonText(btnRed, Integer.toString(db.getNLightsOfArea(currArea, "RED")));
		if (currLocation == null)
			btnRed.setEnabled(false);

		// GREEN BUTTON
		btnGreen = (Button) findViewById(R.id.buttonGreen); 
		btnGreen.getBackground().setColorFilter(Color.GREEN, Mode.MULTIPLY);
		btnGreen.setTextColor(Color.WHITE);
		btnGreen.setTextSize(20);
		btnGreen.setOnClickListener(btnGreenListener);
		btnGreen.setOnLongClickListener(btnGreenLongClickListener);
		setButtonText(btnGreen, Integer.toString(db.getNLightsOfArea(currArea, "GREEN")));
		if (currLocation == null)
			btnGreen.setEnabled(false);
		
		// This application controls its own volume
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		
		// Try to solve the addresses in a background thread
		new AsyncTask<Void, String, Void>() {
			@Override
		    // This runs on a separate thread, not the main UI thread!
		    protected Void doInBackground(Void... params) {
				List<Intersection> intersections = db.getIntersectionsWithInvalidAddress();
				
				if (intersections.size() == 0) return null;
				setTitle("Invalid addresses in database: " + intersections.size());
				
				for (Intersection intersection : intersections) {
					Location loc = new Location("foo");
					loc.setLatitude(intersection.getLatitude());
					loc.setLongitude(intersection.getLongitude());
					
					// Solve the address for this intersection (if possible)
					String strAddr = getAddressFromLoc(loc);
					// ...put it in the Intersection object
					intersection.setAddress(strAddr);
					// ...and return the object to the database
					Log.d("SQLITE", intersection.toString());
					db.updateIntersection(intersection);
					publishProgress(strAddr);
				}
		        return null;
		    }
			
			// Update the progress by showing briefly the found addresses on the titlebar
		    protected void onProgressUpdate(String... progress) {
		    	setTitle(progress[0]);
		     }
			
		    @Override
		    // this runs on main thread
		    protected void onPostExecute(Void result) {
		        //setTitle("All invalid addresses handled");
		    }
		}.execute();		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		mLocationManager.removeUpdates(mLocationListener);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
	}
	
	@Override
    protected void onStop(){
		super.onStop();

		// We need an Editor object to make preference changes.
		// All objects are from android.context.Context
		SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();
      
		// Save the lastly used list into preferences
		editor.putString("lastList", lastList);
      
		// Commit the edits!
		editor.commit();
      
		// Write the KML files for all lists
		final Spinner listat = (Spinner) findViewById(R.id.listat);
		for (int i = 0; i < listat.getCount(); i++) {
			String txtList = listat.getItemAtPosition(i).toString();
			writeKMLFile(db.getArea(txtList));
		}
      
		// Stop listening to location updates when app is closed
		mLocationManager.removeUpdates(mLocationListener);
	}
	
	///////////////////////////////////////////////////////////////////////////
	// Helper methods
	///////////////////////////////////////////////////////////////////////////
	
	// Get the index of the Spinner based on the string shown
	private int getIndex(Spinner spinner, String myString) {
		int index = 0;
		for (int i = 0; i < spinner.getCount(); i++) {
			if (spinner.getItemAtPosition(i).toString().equalsIgnoreCase(myString)) {
				index = i;
			}
		}
		return index;
	} 
	
	// Set button text (counter)
	private void setButtonText(Button btn, String text) {
		btn.setText(text);
	}
	
	// Get the current time
	private Time getNow() {
		Time currTime = new Time();
		currTime.setToNow();
		// Months in Calendar class are 0-based!
		currTime.month++;
		return currTime;
	}
	
	// Format a String from the location
	public static String locStringFromLoc(final Location loc) {
		String strLoc = "NO_LOC,NO_LOC";
		if (loc != null) {
			strLoc = Location.convert(loc.getLatitude(), Location.FORMAT_DEGREES) + " "
			       + Location.convert(loc.getLongitude(), Location.FORMAT_DEGREES);
		}
		return strLoc;
	}
	
	// Find an address from a Location and return to a String (street address only)
	public static String getAddressFromLoc(final Location loc) {
		String strAddress = "NO_ADDR";
		if (loc != null) {
			try {
				List<Address> addresses = geocoder.getFromLocation(loc.getLatitude(), loc.getLongitude(), 1);
				if (addresses != null) {
					Address returnedAddress = addresses.get(0);
					strAddress = returnedAddress.getAddressLine(0).toString();
				} else {
					strAddress = "NO_ADDR_FOUND";
				}
			} catch (IOException e) {
				e.printStackTrace();
				strAddress = "ADDR_SERVICE_NOT_AVAILABLE";
			}
		}
		return strAddress;
	}

	// Format String to a timestamp
	private String setClickInfoText(Time time, String strLoc) {
		String entry = String.format("%02d", time.monthDay) + "."
			     + String.format("%02d", time.month) + "." 
			     + String.format("%04d", time.year) + " "
				 + String.format("%02d", time.hour) + ":" 
				 + String.format("%02d", time.minute) + ":"
				 + String.format("%02d", time.second) + "\n"
				 + strLoc;
		return entry;
	}
	
	// This method traverses the SQLite database and creates a KML file for Google Earth for an area
	public void writeKMLFile(Area area) {
		
		String kmlFileName = area.getName() + ".kml";
		Log.d("LIIKENNEVALOT", kmlFileName);
		
		if (!Utils.isExternalStorageWritable()) return;
		
        String kmlstart =  "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                           "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n" +
                           "  <Document>\n";
 
        String kmlstyle1 = "    <Style id=\"RED\">\n" +
        		           "      <IconStyle>\n" +
                           "        <Icon>\n" + 
                           "          <href>\n" +
                           "            http://upload.wikimedia.org/wikipedia/commons/thumb/6/66/Circle-red.svg/512px-Circle-red.svg.png\n" +
                           "          </href>\n" +
                           "        </Icon>\n" +
                           "      </IconStyle>\n" +
                           "    </Style>\n";
        
        String kmlstyle2 = "    <Style id=\"GREEN\">\n" +
		                   "      <IconStyle>\n" +
                           "        <Icon>\n" + 
                           "          <href>\n" +
                           "            http://upload.wikimedia.org/wikipedia/commons/thumb/9/92/Circle-green.svg/512px-Circle-green.svg.png\n" +
                           "          </href>\n" +
                           "        </Icon>\n" +
                           "      </IconStyle>\n" +
                           "    </Style>\n";
        
        String kmlend =    "  </Document>\n" +
        			       "</kml>";
        
		File kmlFile = new File(Environment.getExternalStorageDirectory(), kmlFileName);
		
		try {	
			BufferedWriter writer = new BufferedWriter(new FileWriter(kmlFile));
			// Write the static stuff first
			writer.write(kmlstart);
			writer.write(kmlstyle1);
			writer.write(kmlstyle2);

			List<Intersection> intersections = db.getIntersectionsOfArea(area);
			for (Intersection intersection : intersections) {
			
				int nRed   = db.getNLightsOfIntersection(intersection.getId(), "RED");
				int nGreen = db.getNLightsOfIntersection(intersection.getId(), "GREEN");
				int diff   = nRed - nGreen;
				String address  = intersection.getAddress();
				String location = intersection.getLongitude() + "," + intersection.getLatitude();
				
				// RED placemark with diff as title
				if (diff > 0) {	
					writer.write("    <Placemark>\n");
					writer.write("      <name>" + diff + "</name>\n");
					writer.write("      <styleUrl>#" + "RED" + "</styleUrl>\n");
					writer.write("      <description>\n");
					writer.write("        <![CDATA[\n");
					writer.write("          <p>Intersection ID: " + intersection.getId() + "</p>\n");
					writer.write("          <p>Geohash: " + intersection.getGeohash() + "</p>\n");
					writer.write("          <p><font color=\"red\">" + address + "</font></p>\n");
					writer.write("        ]]>\n");
					writer.write("      </description>\n");
					writer.write("      <Point>\n");
					writer.write("        <coordinates>" + location + "</coordinates>\n");
					writer.write("      </Point>\n");
					writer.write("    </Placemark>\n");
				} else if (diff < 0) {
					writer.write("    <Placemark>\n");
					writer.write("      <name>" + -diff + "</name>\n");
					writer.write("      <styleUrl>#" + "GREEN" + "</styleUrl>\n");
					writer.write("      <description>\n");
					writer.write("        <![CDATA[\n");
					writer.write("          <p>Intersection ID: " + intersection.getId() + "</p>\n");
					writer.write("          <p>Geohash: " + intersection.getGeohash() + "</p>\n");
					writer.write("          <p><font color=\"red\">" + address + "</font></p>\n");
					writer.write("        ]]>\n");
					writer.write("      </description>\n");
					writer.write("      <Point>\n");
					writer.write("        <coordinates>" + location + "</coordinates>\n");
					writer.write("      </Point>\n");
					writer.write("    </Placemark>\n");
				}
			}
			writer.write(kmlend);
			writer.close();
        } catch (IOException e) {
        	e.printStackTrace();
        }
    }
	
	// This method handles playing of the sounds in different situations
    protected void playSound(String sound) {
        if (mp != null) {
            mp.reset();
            mp.release();
        }
        if (sound.equals("enter_intersection"))
            mp = MediaPlayer.create(this, R.raw.enter_intersection);
        else if (sound.equals("enter_intersection_christmas"))
            mp = MediaPlayer.create(this, R.raw.enter_intersection_christmas);
        else if (sound.equals("exit_intersection"))
            mp = MediaPlayer.create(this, R.raw.exit_intersection);
        else if (sound.equals("exit_intersection_christmas"))
            mp = MediaPlayer.create(this, R.raw.exit_intersection_christmas);
        else if (sound.equals("intersection_click"))
            mp = MediaPlayer.create(this, R.raw.intersection_click);
        else if (sound.equals("intersection_doubleclick"))
            mp = MediaPlayer.create(this, R.raw.intersection_doubleclick);
        else if (sound.equals("red_click"))
            mp = MediaPlayer.create(this, R.raw.red_click);
        else if (sound.equals("red_doubleclick"))
            mp = MediaPlayer.create(this, R.raw.red_doubleclick);
        else if (sound.equals("green_click"))
            mp = MediaPlayer.create(this, R.raw.green_click);
        else if (sound.equals("green_doubleclick"))
            mp = MediaPlayer.create(this, R.raw.green_doubleclick);
        
        mp.setVolume(1.0f, 1.0f);
        mp.start();
    }
    
    // Suggest a GREEN trafficlight event and notify the driver
    public void addGreen(Intersection intersection) {
    	Trafficlight trafficlight = new Trafficlight();
    	trafficlight.setIdIntersection(intersection.getId());
    	trafficlight.setGeohash(GeoHashUtils.encode(intersection.getLatitude(), intersection.getLongitude()));
    	trafficlight.setLatitude(intersection.getLatitude());
    	trafficlight.setLongitude(intersection.getLongitude());
    	trafficlight.setLight("GREEN");
		db.createTrafficlight(trafficlight);
    	playSound("green_click");
    	setButtonText(btnGreen, Integer.toString(db.getNLightsOfArea(currArea, "GREEN")));
		setButtonText(btnRed, Integer.toString(db.getNLightsOfArea(currArea, "RED")));
    	
		final Timer t = new Timer();
		AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
		builder.setTitle("Valo oli VIHREÄ");
		builder.setMessage("Se lisättiin automaattisesti tietokantaan.");
		builder.setCancelable(true);
		
		final AlertDialog dlg = builder.create();
		dlg.show();
		
		t.schedule(new TimerTask() {
		    public void run() {
		        dlg.dismiss(); 
		        if (t != null) 
		        	t.cancel();
		    }
		}, 3000);
    }
    
    ///////////////////////////////////////////////////////////////////////////
    // Listeners for UI elements
    ///////////////////////////////////////////////////////////////////////////
	
	// DROP-DOWN LIST: select the area
	final OnItemSelectedListener areaListener = new OnItemSelectedListener() {
		public void onItemSelected(AdapterView<?> parent, View view, 
	            int pos, long id) {
	        lastList = parent.getItemAtPosition(pos).toString();
			currArea = db.getArea(lastList);
			setButtonText(btnIntersection, Integer.toString(db.getIntersectionsOfArea(currArea).size()));
        	setButtonText(btnRed, Integer.toString(db.getNLightsOfArea(currArea, "RED")));
        	setButtonText(btnGreen, Integer.toString(db.getNLightsOfArea(currArea, "GREEN")));
        	txtMoveStatus.setText("AVG " + db.getWaitingtimeOfArea(currArea) + " s");
	    }
		
	    public void onNothingSelected(AdapterView<?> parent) {
	    }
	};
    
	// BLUE BUTTON click: add an intersection
	final OnClickListener btnIntersectionListener = new OnClickListener() {
        public void onClick(View v) {
        	// Add a new record to intersection table - if we are not at an existing one already
        	if (vehicle.status == Status.OUTSIDE_INTERSECTION) {
	        	v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        		Intersection intersection = new Intersection();
        		intersection.setGeohash(GeoHashUtils.encode(currLocation.getLatitude(), currLocation.getLongitude()));
        		intersection.setLatitude(currLocation.getLatitude());
        		intersection.setLongitude(currLocation.getLongitude());
        		intersection.setRadius(radius);
        		intersection.setAddress("NO_ADDR");
        		db.createIntersection(currArea, intersection);
        		playSound("intersection_click");
        		setButtonText(btnIntersection, Integer.toString(db.getIntersectionsOfArea(currArea).size()));
        		Toast.makeText(getApplicationContext(), 
        					   setClickInfoText(getNow(), locStringFromLoc(currLocation)), 
        			           Toast.LENGTH_SHORT).show();
        	}
        }
	};
    
	// BLUE BUTTON longclick: delete newest intersection
	final OnLongClickListener btnIntersectionLongClickListener = new OnLongClickListener() {
		public boolean onLongClick(View v) {
			new AlertDialog.Builder(MainActivity.this)
            .setTitle("Risteyksen poisto")
            .setMessage("Haluatko poistaa uusimman risteyksen?\n" 
            		  + "Myös kaikki sen valotapahtumat poistetaan")
            .setPositiveButton("Kyllä", new DialogInterface.OnClickListener() {
            	public void onClick(DialogInterface dialog, int whichButton) {
            		// Delete this intersection
            		db.deleteNewestIntersectionFromArea(currArea);
            		setButtonText(btnIntersection, Integer.toString(db.getIntersectionsOfArea(currArea).size()));
            		setButtonText(btnRed, Integer.toString(db.getNLightsOfArea(currArea, "RED")));
					setButtonText(btnGreen, Integer.toString(db.getNLightsOfArea(currArea, "GREEN")));
					playSound("intersection_doubleclick");
            	}
            }).setNegativeButton("Ei", new DialogInterface.OnClickListener() {
            	public void onClick(DialogInterface dialog, int whichButton) {
            		// Do nothing
            	}
            }).show();
		return true;
		}
	};
	
	// STATUS SCREEN click: move intersection here
	final OnClickListener layoutStatusListener = new OnClickListener() {
        public void onClick(View v) {
        	
        	final Location loc = currLocation;
        	final Time now = getNow();
        	
        	// If the vehicle is at an intersection, make it possible to move the
        	// intersection coordinates by clicking at the new center
        	if (!(vehicle.status == Status.OUTSIDE_INTERSECTION)) {
        		new AlertDialog.Builder(MainActivity.this)
            	.setTitle("Keskipisteen siirto")
            	.setMessage("Haluatko siirtää risteyksen tähän?")
            	.setPositiveButton("Kyllä", new DialogInterface.OnClickListener() {
            		public void onClick(DialogInterface dialog, int whichButton) {
            			// Move the intersection coordinates to this position
            			Intersection intersection = db.getIntersection(vehicle.getIntersectionId());
            			double lat = loc.getLatitude();
            			double lon = loc.getLongitude();
            			intersection.setTime(now);
            			intersection.setGeohash(GeoHashUtils.encode(lat, lon));
            			intersection.setLatitude(lat);
            			intersection.setLongitude(lon);
            			db.updateIntersection(intersection);
            		}
            	}).setNegativeButton("Ei", new DialogInterface.OnClickListener() {
            		public void onClick(DialogInterface dialog, int whichButton) {
            			// Do nothing
            		}
            	}).show();
        	}
        }
	};
    
	// STATUS SCREEN longclick: delete this intersection
	final OnLongClickListener layoutStatusLongClickListener = new OnLongClickListener() {
		public boolean onLongClick(View v) {
			if (!(vehicle.status == Status.OUTSIDE_INTERSECTION)) {
				new AlertDialog.Builder(MainActivity.this)
            	.setTitle("Risteyksen poisto")
            	.setMessage("Haluatko poistaa tämän risteyksen?\n" 
            			  + "Myös kaikki sen valotapahtumat poistetaan")
            	.setPositiveButton("Kyllä", new DialogInterface.OnClickListener() {
            		public void onClick(DialogInterface dialog, int whichButton) {
            			// Delete this intersection
            			db.deleteIntersection(vehicle.getIntersectionId());
            			setButtonText(btnIntersection, Integer.toString(db.getIntersectionsOfArea(currArea).size()));
            			setButtonText(btnRed, Integer.toString(db.getNLightsOfArea(currArea, "RED")));
						setButtonText(btnGreen, Integer.toString(db.getNLightsOfArea(currArea, "GREEN")));
						//txtStatus.setOnClickListener(null);
						vehicle.status = Status.OUTSIDE_INTERSECTION;
						playSound("intersection_doubleclick");
            		}
            	}).setNegativeButton("Ei", new DialogInterface.OnClickListener() {
            		public void onClick(DialogInterface dialog, int whichButton) {
            			// Do nothing
            		}
            	}).show();
			}
			return true;
		}
	};
	
	// RED BUTTON click: add RED light event
	final OnClickListener btnRedListener = new OnClickListener() {
        public void onClick(View v) {
        	v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        	
        	// Create a new RED trafficlight record
        	Trafficlight trafficlight = new Trafficlight();
        	trafficlight.setIdIntersection(vehicle.getIntersectionId());
        	trafficlight.setGeohash(GeoHashUtils.encode(currLocation.getLatitude(), currLocation.getLongitude()));
        	trafficlight.setLatitude(currLocation.getLatitude());
        	trafficlight.setLongitude(currLocation.getLongitude());
        	trafficlight.setLight("RED");
        	vehicle.suggestGreen = false;	// If user clicks RED, no GREEN is suggested at the intersection border
        	isRedPressed = true;
        	db.createTrafficlight(trafficlight);
        	playSound("red_click");
        	if (vehicle.status == Status.OUTSIDE_INTERSECTION)
        		setButtonText(btnRed, Integer.toString(db.getNLightsOfArea(currArea, "RED")));
        	else
        		setButtonText(btnRed, Integer.toString(db.getNLightsOfIntersection(vehicle.getIntersectionId(), "RED")));
        	Toast.makeText(getApplicationContext(), 
        			       setClickInfoText(getNow(), locStringFromLoc(currLocation)), 
        			       Toast.LENGTH_SHORT).show();
        }
	};
	
	// RED BUTTON longclick: delete newest RED light event
	final OnLongClickListener btnRedLongClickListener = new OnLongClickListener() {
		public boolean onLongClick(View v) {
			db.deleteNewestTrafficlightFromArea(currArea, "RED");
			playSound("red_doubleclick");
			if (vehicle.status == Status.OUTSIDE_INTERSECTION)
        		setButtonText(btnRed, Integer.toString(db.getNLightsOfArea(currArea, "RED")));
        	else
        		setButtonText(btnRed, Integer.toString(db.getNLightsOfIntersection(vehicle.getIntersectionId(), "RED")));
			return true;
		}
	};
	
	// GREEN BUTTON click: add GREEN light event
	final OnClickListener btnGreenListener = new OnClickListener() {
        public void onClick(View v) {
        	v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        	
        	// Create a new GREEN trafficlight record
        	Trafficlight trafficlight = new Trafficlight();
        	trafficlight.setIdIntersection(vehicle.getIntersectionId());
        	trafficlight.setGeohash(GeoHashUtils.encode(currLocation.getLatitude(), currLocation.getLongitude()));
        	trafficlight.setLatitude(currLocation.getLatitude());
        	trafficlight.setLongitude(currLocation.getLongitude());
        	trafficlight.setLight("GREEN");
        	vehicle.suggestGreen = false;	// If user clicks GREEN, no light is suggested at the intersection border
        	db.createTrafficlight(trafficlight);	        	
        	playSound("green_click");
        	if (vehicle.status == Status.OUTSIDE_INTERSECTION)
        		setButtonText(btnGreen, Integer.toString(db.getNLightsOfArea(currArea, "GREEN")));
        	else
        		setButtonText(btnGreen, Integer.toString(db.getNLightsOfIntersection(vehicle.getIntersectionId(), "GREEN")));
        	Toast.makeText(getApplicationContext(), 
        			       setClickInfoText(getNow(), locStringFromLoc(currLocation)),
        			       Toast.LENGTH_SHORT).show();
        }
	};
	
	// GREEN BUTTON longclick: delete newest GREEN light event
	final OnLongClickListener btnGreenLongClickListener = new OnLongClickListener() {
		public boolean onLongClick(View v) {
			db.deleteNewestTrafficlightFromArea(currArea, "GREEN");
			playSound("green_doubleclick");
			if (vehicle.status == Status.OUTSIDE_INTERSECTION)
        		setButtonText(btnGreen, Integer.toString(db.getNLightsOfArea(currArea, "GREEN")));
        	else
        		setButtonText(btnGreen, Integer.toString(db.getNLightsOfIntersection(vehicle.getIntersectionId(), "GREEN")));
			return true;
		}
	};
	
	// Location listener (inner class)
	public class MyLocationListener implements LocationListener {

		@Override
		public void onLocationChanged(Location loc) {
			String strOutside = "";
			String strIntersection = "";
			
			currLocation = loc;
			if (currLocation == null) return;
			
			// Update coordinates of the Vehicle object (representing my vehicle)
			vehicle.setLatitude(currLocation.getLatitude());
			vehicle.setLongitude(currLocation.getLongitude());
			vehicle.setSpeed(currLocation.getSpeed());
			txtSpeed.setText(String.format("%2d", (int)vehicle.getSpeed()) + " km/h");

			btnIntersection.setEnabled(true);
			String geohash = GeoHashUtils.encode(currLocation.getLatitude(), currLocation.getLongitude());
			nearestIntersections = db.getNearestIntersections(currArea, geohash, 7);
				
			// Loop through all intersections near the user
			for (Intersection intersection : nearestIntersections) {

				vehicle.setDistance(intersection);
				String strDist  = String.format("%4d", (int)vehicle.getDistance());
				String strAddr  = intersection.getAddress();
					
				strOutside += strDist + " : " + strAddr + "\n";
					
				// State transition: OUTSIDE_INTERSECTION -> ENTER_INTERSECTION
				if ((vehicle.status == Status.OUTSIDE_INTERSECTION) && 
					(vehicle.getDistance() <= radius)) {
					playSound("enter_intersection_christmas");
					setButtonText(btnRed, Integer.toString(db.getNLightsOfIntersection(intersection.getId(), "RED")));
					setButtonText(btnGreen, Integer.toString(db.getNLightsOfIntersection(intersection.getId(), "GREEN")));
					vehicle.setIntersectionId(intersection.getId());
					vehicle.status = Status.ENTER_INTERSECTION;
					vehicle.stopCount = 0;
					vehicle.suggestGreen = true;  // Suggest GREEN light at border, IF the vehicle didn't stop
					isRedPressed = false;
					vehicle.checkStop(); // Check if it indeed stopped here
					strIntersection = strDist + " : " + strAddr;
					txtStatus.setText("SAAVUIT RISTEYKSEEN");
					//txtMoveStatus.setText(strStopped);
					txtMoveStatus.setText("AVG " + db.getWaitingtimeOfIntersection(intersection.getId(), "RED") + " s");
					txtStopCount.setText(String.format("%2d", vehicle.stopCount) + " s");
					
				// State transition: ENTER_INTERSECTION -> AT_INTERSECTION
				} else if ((vehicle.status == Status.ENTER_INTERSECTION) && 
						   (vehicle.getDistance() <= radius) &&
						   (vehicle.getIntersectionId() == intersection.getId())) {
					vehicle.status = Status.AT_INTERSECTION;
					vehicle.checkStop();
					strIntersection = strDist + " : " + strAddr;
					txtStatus.setText("RISTEYSALUEELLA");
					//txtMoveStatus.setText(strStopped);
					txtMoveStatus.setText("AVG " + db.getWaitingtimeOfIntersection(intersection.getId(), "RED") + " s");
					txtStopCount.setText(String.format("%2d", vehicle.stopCount) + " s");
						
				// State transition: AT_INTERSECTION -> AT_INTERSECTION (self-loop)
				} else if ((vehicle.status == Status.AT_INTERSECTION) &&
						   (vehicle.getDistance() <= radius) &&
						   (vehicle.getIntersectionId() == intersection.getId())) {
					vehicle.status = Status.AT_INTERSECTION;
					vehicle.checkStop();
					strIntersection = strDist + " : " + strAddr;
					txtStatus.setText("RISTEYSALUEELLA");
					//txtMoveStatus.setText(strStopped);
					txtMoveStatus.setText("AVG " + db.getWaitingtimeOfIntersection(intersection.getId(), "RED") + " s");
					txtStopCount.setText(String.format("%2d", vehicle.stopCount) + " s");
						
				// State transition: AT_INTERSECTION -> EXIT_INTERSECTION
				} else if ((vehicle.status == Status.AT_INTERSECTION) &&
						   (vehicle.getDistance() >= radius) &&
						   (vehicle.getIntersectionId() == intersection.getId())) {
					playSound("exit_intersection_christmas");
					setButtonText(btnRed, Integer.toString(db.getNLightsOfArea(currArea, "RED")));
					setButtonText(btnGreen, Integer.toString(db.getNLightsOfArea(currArea, "GREEN")));
					vehicle.status = Status.EXIT_INTERSECTION;
					vehicle.checkStop();
					strIntersection = strDist + " : " + strAddr;
					txtStatus.setText("POISTUIT RISTEYKSESTÄ");
					//txtMoveStatus.setText(strStopped);
					txtMoveStatus.setText("AVG " + db.getWaitingtimeOfIntersection(intersection.getId(), "RED") + " s");
					txtStopCount.setText(String.format("%2d", vehicle.stopCount) + " s");
						
				// State transition: EXIT_INTERSECTION -> OUTSIDE_INTERSECTION
				} else if ((vehicle.status == Status.EXIT_INTERSECTION) &&
						   (vehicle.getDistance() >= radius) &&
						   (vehicle.getIntersectionId() == intersection.getId())) {
					if (vehicle.suggestGreen)
						addGreen(intersection);
					
					// If RED was pressed at this intersection, update the event's waitingtime field in the db
					if (isRedPressed) {
						Trafficlight trafficlight = db.getNewestLightOfIntersection(intersection.getId(), "RED");
						trafficlight.setWaitingTime(vehicle.stopCount);
						db.updateTrafficlight(trafficlight);
					}
					vehicle.setIntersectionId(-1);
					vehicle.status = Status.OUTSIDE_INTERSECTION;
				}						
			}
				
			// When not in intersection:
			if (vehicle.status == Status.OUTSIDE_INTERSECTION) {
				layoutStatus.setOnClickListener(null);
				layoutStatus.setOnLongClickListener(null);
				txtStatus.setText("LÄHIMMÄT RISTEYKSET");
				txtIntersections.setText(strOutside);
				//txtMoveStatus.setText("");
				txtMoveStatus.setText("AVG " + db.getWaitingtimeOfArea(currArea) + " s");
				txtStopCount.setText("");
				btnIntersection.setEnabled(true);
				btnRed.setEnabled(false);
				btnGreen.setEnabled(false);
					
			// When in intersection:
			} else { 
				layoutStatus.setOnClickListener(layoutStatusListener);
				layoutStatus.setOnLongClickListener(layoutStatusLongClickListener);
				txtIntersections.setText(strIntersection);
				btnIntersection.setEnabled(false);
				btnRed.setEnabled(true);
				btnGreen.setEnabled(true);
			}
		}

		@Override
		public void onProviderDisabled(String provider) {
			Toast.makeText( getApplicationContext(), "Gps Disabled", Toast.LENGTH_SHORT ).show();
		}

		@Override
		public void onProviderEnabled(String provider) {
			Toast.makeText( getApplicationContext(), "Gps Enabled",	Toast.LENGTH_SHORT).show();
		}		
	
		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
		}
	}
}