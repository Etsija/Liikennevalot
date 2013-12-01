package com.github.etsija.liikennevalot;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

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
import android.content.Context;
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
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;

public class MainActivity extends FragmentActivity {

	String lastList;
	Area currArea = new Area();
	Location currLocation = null;
	static Geocoder geocoder;
	LocationManager mLocationManager;
	LocationListener mLocationListener;
	DatabaseHelper db;
	private static final double radius = 30.0;
	List<Intersection> nearestIntersections = new ArrayList<Intersection>();
	Vehicle vehicle = new Vehicle();
	MediaPlayer mp;
	int soundGreen, soundRed;
	
	// UI elements
	Spinner listat;
	Button btnIntersection;
	EditText txtStatus;
	Button btnRed;
	Button btnGreen;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// Location services
		mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		mLocationListener = new MyLocationListener();
	    mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
	    geocoder = new Geocoder(this, Locale.ENGLISH);  // For offline address search
	    
		// Fetch all preferences into prefKeys HashMap
		final SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
	    lastList = settings.getString("lastList", null);
	    Log.d("LIIKENNEVALOT", "lastList = " + lastList);
	    
	    // Drop-down for the different lists
		listat = (Spinner) findViewById(R.id.listat);
		listat.setSelection(getIndex(listat, lastList));
		
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
		setButtonText(btnIntersection, Integer.toString(db.getIntersectionsOfArea(currArea).size()));
		if (currLocation == null)
			btnIntersection.setEnabled(false);
		
		// TEXTFIELD for showing intersection info
		txtStatus = (EditText) findViewById(R.id.txtStatus);
		txtStatus.getBackground().setColorFilter(Color.DKGRAY, Mode.MULTIPLY);
		txtStatus.setTextColor(Color.WHITE);
		txtStatus.setTextSize(14);
		
		// RED BUTTON
		btnRed = (Button) findViewById(R.id.buttonRed); 
		btnRed.getBackground().setColorFilter(Color.RED, Mode.MULTIPLY);
		btnRed.setTextColor(Color.WHITE);
		btnRed.setTextSize(20);
		setButtonText(btnRed, Integer.toString(db.getLightsOfArea(currArea, "RED")));
		if (currLocation == null)
			btnRed.setEnabled(false);

		// GREEN BUTTON
		btnGreen = (Button) findViewById(R.id.buttonGreen); 
		btnGreen.getBackground().setColorFilter(Color.GREEN, Mode.MULTIPLY);
		btnGreen.setTextColor(Color.WHITE);
		btnGreen.setTextSize(20);
		setButtonText(btnGreen, Integer.toString(db.getLightsOfArea(currArea, "GREEN")));
		if (currLocation == null)
			btnGreen.setEnabled(false);
		
		// Controlling audio volume
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
		
		///////////////////////////////////////////////////////////////////////
		// Listeners
		///////////////////////////////////////////////////////////////////////
		
		// DROP-DOWN LIST: select the area
		listat.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view, 
		            int pos, long id) {
		        lastList = parent.getItemAtPosition(pos).toString();
				currArea = db.getArea(lastList);
	        	setButtonText(btnIntersection, Integer.toString(db.getIntersectionsOfArea(currArea).size()));
	        	setButtonText(btnRed, Integer.toString(db.getLightsOfArea(currArea, "RED")));
	        	setButtonText(btnGreen, Integer.toString(db.getLightsOfArea(currArea, "GREEN")));
		    }
		    public void onNothingSelected(AdapterView<?> parent) {
		    }
		});
		
		// BLUE BUTTON: handle intersections
		btnIntersection.setOnClickListener(new OnClickListener() {
	        public void onClick(View v) {
	        	String strAddr = "NO_ADDR";
	        	v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
	        	// Add a new record to intersection table
	        	Intersection intersection = new Intersection();
	        	intersection.setGeohash(GeoHashUtils.encode(currLocation.getLatitude(), currLocation.getLongitude()));
	        	intersection.setLatitude(currLocation.getLatitude());
	        	intersection.setLongitude(currLocation.getLongitude());
	        	intersection.setRadius(radius);
	        	intersection.setAddress(strAddr);
	        	db.createIntersection(currArea, intersection);
	        	playSound("intersection_click");
	        	setButtonText(btnIntersection, Integer.toString(db.getIntersectionsOfArea(currArea).size()));
	        	Toast.makeText(getApplicationContext(), 
	        			       setClickInfoText(getNow(), locStringFromLoc(currLocation)), 
	        			       Toast.LENGTH_SHORT).show();
	        }
		});
		
		btnIntersection.setOnLongClickListener(new OnLongClickListener() {
			public boolean onLongClick(View v) {
				db.deleteNewestIntersectionFromArea(currArea);
				playSound("intersection_doubleclick");
				setButtonText(btnIntersection, Integer.toString(db.getIntersectionsOfArea(currArea).size()));
				return true;
			}
		});
		
		// RED BUTTON
		btnRed.setOnClickListener(new OnClickListener() {
	        public void onClick(View v) {
	        	v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
	        	
	        	// Create a new RED trafficlight record
	        	Trafficlight trafficlight = new Trafficlight();
	        	trafficlight.setIdIntersection(vehicle.getIntersectionId());
	        	trafficlight.setGeohash(GeoHashUtils.encode(currLocation.getLatitude(), currLocation.getLongitude()));
	        	trafficlight.setLatitude(currLocation.getLatitude());
	        	trafficlight.setLongitude(currLocation.getLongitude());
	        	trafficlight.setLight("RED");
	        	db.createTrafficlight(trafficlight);
	        	
	        	playSound("red_click");
	        	setButtonText(btnRed, Integer.toString(db.getLightsOfArea(currArea, "RED")));
	        	Toast.makeText(getApplicationContext(), 
	        			       setClickInfoText(getNow(), locStringFromLoc(currLocation)), 
	        			       Toast.LENGTH_SHORT).show();
	        }
		});
		
		btnRed.setOnLongClickListener(new OnLongClickListener() {
			public boolean onLongClick(View v) {
				db.deleteNewestTrafficlightFromArea(currArea, "RED");
				playSound("red_doubleclick");
				setButtonText(btnRed, Integer.toString(db.getLightsOfArea(currArea, "RED")));
				return true;
			}
		});
		
		// GREEN BUTTON
		btnGreen.setOnClickListener(new OnClickListener() {
	        public void onClick(View v) {
	        	v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
	        	
	        	// Create a new GREEN trafficlight record
	        	Trafficlight trafficlight = new Trafficlight();
	        	trafficlight.setIdIntersection(vehicle.getIntersectionId());
	        	trafficlight.setGeohash(GeoHashUtils.encode(currLocation.getLatitude(), currLocation.getLongitude()));
	        	trafficlight.setLatitude(currLocation.getLatitude());
	        	trafficlight.setLongitude(currLocation.getLongitude());
	        	trafficlight.setLight("GREEN");
	        	db.createTrafficlight(trafficlight);
	        	
	        	playSound("green_click");
	        	setButtonText(btnGreen, Integer.toString(db.getLightsOfArea(currArea, "GREEN")));
	        	Toast.makeText(getApplicationContext(), 
	        			       setClickInfoText(getNow(), locStringFromLoc(currLocation)),
	        			       Toast.LENGTH_SHORT).show();
	        }
		});
		
		btnGreen.setOnLongClickListener(new OnLongClickListener() {
			public boolean onLongClick(View v) {
				db.deleteNewestTrafficlightFromArea(currArea, "GREEN");
				playSound("green_doubleclick");
				setButtonText(btnGreen, Integer.toString(db.getLightsOfArea(currArea, "GREEN")));
				return true;
			}
		});
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
			Log.d("LIIKENNEVALOT", txtList);
			writeKMLFile(txtList + ".txt");
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
	
	// Create one entry line to the logfile
	private String createEntry(Time time, String button, Location loc, String addr) {
		String strLoc = "NO_LOC,NO_LOC";
		String geohash = "NO_GEOHASH";
		
		if (loc != null) {
			strLoc = loc.getLatitude() + "," + loc.getLongitude();
			geohash = GeoHashUtils.encode(loc.getLatitude(), loc.getLongitude()); 
		}
		String entry = String.format("%04d", time.year) + ","
				     + String.format("%02d", time.month) + "," 
				     + String.format("%02d", time.monthDay) + ","
					 + String.format("%02d", time.hour) + "," 
					 + String.format("%02d", time.minute) + ","
					 + String.format("%02d", time.second) + ","
					 + button + ","
					 + strLoc + ","
					 + addr + ","
					 + geohash;
		return entry;
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
	
	// Write the click details on SD card
	public void writeOnSD(String sFileName, String sBody) {
		if (!isExternalStorageWritable()) return;

		File log = new File(Environment.getExternalStorageDirectory(), sFileName);
	    try {
	        BufferedWriter out = new BufferedWriter(new FileWriter(log.getAbsolutePath(), true));
	        out.append(sBody);
	        out.append("\n");
	        out.close();
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}  
	
	// Checks if external storage is available for read and write
	public boolean isExternalStorageWritable() {
	    String state = Environment.getExternalStorageState();
	    if (Environment.MEDIA_MOUNTED.equals(state)) {
	        return true;
	    }
	    return false;
	}
	
	//  How many lines in output file?
	public static int countLines(String fileName) throws IOException {
	    LineNumberReader reader = null;
	    try {
	        reader = new LineNumberReader(new FileReader(new File(Environment.getExternalStorageDirectory(), fileName)));
	        while ((reader.readLine()) != null);
	        return reader.getLineNumber();
	    } catch (Exception ex) {
	        return -1;
	    } finally { 
	        if (reader != null) 
	            reader.close();
	    }
	}
	
	// Count the RED/GREEN entries in the output file
	public static int countEntries(String fileName, String strSearch) {
		File inputFile = new File(Environment.getExternalStorageDirectory(), fileName);
		Scanner scanner = null;
		int nLines = 0;
		
		try {
			scanner = new Scanner(inputFile);
			String currentLine;
			while (scanner.hasNextLine()) {
				currentLine = scanner.nextLine();
			    if (currentLine.contains(strSearch)) {
			    	nLines++;
			    }
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return nLines;
	}
	
	// Delete last line from logfile
	public void deleteLastLine(String fileName, int nLines) {
		try {	
			File inputFile = new File(Environment.getExternalStorageDirectory(), fileName);
			if (!inputFile.isFile()) {
				Toast.makeText(getApplicationContext(), fileName + " is not a valid text file", Toast.LENGTH_SHORT).show();
				return;
			}
			// Construct the new file that will later be renamed to the original filename.
			File tempFile = new File(Environment.getExternalStorageDirectory(), "tempfile.txt");
			BufferedReader reader = new BufferedReader(new FileReader(inputFile));
			BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));

			String currentLine = null;
		
			// Write nLines-1 lines to tempfile
			for (int i = 1; i < nLines; i++) {
				currentLine = reader.readLine();
				writer.write(currentLine + "\n");
			}
			reader.close();
			writer.close();
			inputFile.delete();
			tempFile.renameTo(inputFile);
			
		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	
	public void writeNewSDFile(String logFileName) {
		String[] strTmp = logFileName.split("\\.");
		String outFileName = strTmp[0] + "_new.txt";  // inputfile.txt -> inputfile -> inputfile_new.txt
		Log.d("LIIKENNEVALOT", outFileName);
		
		if (!isExternalStorageWritable()) return;
		
		try {
			File inputFile = new File(Environment.getExternalStorageDirectory(), logFileName);
			if (!inputFile.isFile()) {
				//Toast.makeText(getApplicationContext(), logFileName + " is not a valid text file", Toast.LENGTH_SHORT).show();
				return;
			}
			File outFile = new File(Environment.getExternalStorageDirectory(), outFileName);
			BufferedReader reader = new BufferedReader(new FileReader(inputFile));
			BufferedWriter writer = new BufferedWriter(new FileWriter(outFile));
			
			String currentLine = null;
			
			while ((currentLine = reader.readLine()) != null) {
				String[] txtParse = currentLine.split(",");
				
				double lat = -9999.9;
				double lon = -9999.9;
				
				if (!(txtParse[7].contains("NO_LOC")) && !(txtParse[8].contains("NO_LOC"))) {
					lat = Double.parseDouble(txtParse[7]);
					lon = Double.parseDouble(txtParse[8]);
				}
				Log.d("LIIKENNEVALOT", lat + " " + lon);
				
				writer.write(currentLine);
				if ((txtParse[7].contains("NO_LOC")) && (txtParse[8].contains("NO_LOC"))) {
					writer.write(",NO_GEOHASH\n");
				} else {
					writer.write("," + GeoHashUtils.encode(lat, lon) + "\n");
				}
			}
			reader.close();
			writer.close();	
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// This method reads the logfile and creates a KML file for Google Earth
	public void writeKMLFile(String logFileName)  {
		
		String[] strTmp = logFileName.split("\\.");
		String kmlFileName = strTmp[0] + ".kml";  // inputfile.txt -> inputfile -> inputfile.kml
		Log.d("LIIKENNEVALOT", kmlFileName);
		
		if (!isExternalStorageWritable()) return;
		
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
        
        // Main loop: read the logfile line by line, parse, and write to KML file entry by entry
        try {
    		File inputFile = new File(Environment.getExternalStorageDirectory(), logFileName);
			if (!inputFile.isFile()) {
				//Toast.makeText(getApplicationContext(), logFileName + " is not a valid text file", Toast.LENGTH_SHORT).show();
				return;
			}
			File kmlFile = new File(Environment.getExternalStorageDirectory(), kmlFileName);
			BufferedReader reader = new BufferedReader(new FileReader(inputFile));
			BufferedWriter writer = new BufferedWriter(new FileWriter(kmlFile));
			
			// Write the static stuff first
			writer.write(kmlstart);
			writer.write(kmlstyle1);
			writer.write(kmlstyle2);

			// Then the dynamic stuff, ie. the Placemarks
			String currentLine = null;
			
			// Read one line at a time, parse, construct the needed strings -> output to KML file
			while ((currentLine = reader.readLine()) != null) {
				String[] txtParse = currentLine.split(",");
				String time     = txtParse[2] + "." + txtParse[1] + "." + txtParse[0] + " " + 
				                  txtParse[3] + ":" + txtParse[4] + ":" + txtParse[5];
				String button   = txtParse[6];
				String location = txtParse[8] + "," + txtParse[7]; // NOTE: longitude first, then latitude (GE wants this)
				String address  = txtParse[9];
				
				// Write to KML only the entries which have a location!  No use writing ones without
				if (!location.contains("NO_LOC,NO_LOC")) {
					writer.write("    <Placemark>\n");
					writer.write("      <styleUrl>#" + button + "</styleUrl>\n");
					writer.write("      <description>\n");
					writer.write("        <![CDATA[\n");
					writer.write("          <p>" + time + "</p>\n");
					writer.write("          <p><font color=\"red\">" + address + "</font></p>\n");
					writer.write("        ]]>\n");
					writer.write("      </description>\n");
					writer.write("      <Point>\n");
					writer.write("        <coordinates>" + location + "</coordinates>\n");
					writer.write("      </Point>\n");
					writer.write("    </Placemark>\n");
				}
			}
			
			// Write the end and close streams
			writer.write(kmlend);
			reader.close();
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
        if (sound == "enter_intersection")
            mp = MediaPlayer.create(this, R.raw.enter_intersection);
        else if (sound == "exit_intersection")
            mp = MediaPlayer.create(this, R.raw.exit_intersection);
        else if (sound == "intersection_click")
            mp = MediaPlayer.create(this, R.raw.intersection_click);
        else if (sound == "intersection_doubleclick")
            mp = MediaPlayer.create(this, R.raw.intersection_doubleclick);
        else if (sound == "red_click")
            mp = MediaPlayer.create(this, R.raw.red_click);
        else if (sound == "red_doubleclick")
            mp = MediaPlayer.create(this, R.raw.red_doubleclick);
        else if (sound == "green_click")
            mp = MediaPlayer.create(this, R.raw.green_click);
        else if (sound == "green_doubleclick")
            mp = MediaPlayer.create(this, R.raw.green_doubleclick);
        
        mp.setVolume(1.0f, 1.0f);
        mp.start();
    }
	
    // This method reads the logfile and inserts trafficlight events from it to the "trafficlight" SQLite table
    @SuppressWarnings("deprecation")
	public void moveToDb(String logFileName) {
		
		if (!isExternalStorageWritable()) return;
		
		try {
			File inputFile = new File(Environment.getExternalStorageDirectory(), logFileName);
			if (!inputFile.isFile()) {
				Toast.makeText(getApplicationContext(), logFileName + " is not a valid text file", Toast.LENGTH_SHORT).show();
				return;
			}
			BufferedReader reader = new BufferedReader(new FileReader(inputFile));
			
			String currentLine = null;
			
			while ((currentLine = reader.readLine()) != null) {
				String[] txtParse = currentLine.split(",");
				
				String year    = txtParse[0];
				String month   = txtParse[1];
				String day     = txtParse[2];
				String hour    = txtParse[3];
				String minute  = txtParse[4];
				String second  = txtParse[5];
				String light   = txtParse[6];
				String txtLat  = txtParse[7];
				String txtLon  = txtParse[8];
				String geohash = txtParse[10];
				
				// Set basic data
				
				Date date = new Date();
				date.setYear(Integer.parseInt(year)-1900);
				date.setMonth(Integer.parseInt(month)-1);
				date.setDate(Integer.parseInt(day));
				date.setHours(Integer.parseInt(hour));
				date.setMinutes(Integer.parseInt(minute));
				date.setSeconds(Integer.parseInt(second));
				
				double latitude = Double.parseDouble(txtLat);
				double longitude = Double.parseDouble(txtLon);
				
				Trafficlight trafficlight = new Trafficlight(date, light);
				trafficlight.setLatitude(latitude);
				trafficlight.setLongitude(longitude);
				trafficlight.setGeohash(geohash);
				
				Log.d("SQLITE", trafficlight.toString());
				db.createTrafficlight(trafficlight);
			}
			reader.close();	
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
    
	// This is an inner class for the location listener
	public class MyLocationListener implements LocationListener {

		@Override
		public void onLocationChanged(Location loc) {
			String strOutside = "";
			String strIntersection = "";
			loc.getLatitude();
			loc.getLongitude();
			
			currLocation = loc;
			if (currLocation != null) {
				// Update coordinates of the Vehicle object (representing my vehicle)
				vehicle.setLatitude(currLocation.getLatitude());
				vehicle.setLongitude(currLocation.getLongitude());

				btnIntersection.setEnabled(true);
				String geohash = GeoHashUtils.encode(currLocation.getLatitude(), currLocation.getLongitude());
				nearestIntersections = db.getNearestIntersections(currArea, geohash, 7);
				
				// Loop through all intersections near the user
				for (Intersection intersection : nearestIntersections) {

					vehicle.setDistance(intersection);
					strOutside += (int)vehicle.getDistance() + " : " + intersection.getAddress() + "\n";
					
					// State transition: OUTSIDE_INTERSECTION -> ENTER_INTERSECTION
					if ((vehicle.status == Status.OUTSIDE_INTERSECTION) && 
						(vehicle.getDistance() <= radius)) {
						playSound("enter_intersection");
						setButtonText(btnRed, Integer.toString(db.getLightsOfIntersection(intersection, "RED")));
						setButtonText(btnGreen, Integer.toString(db.getLightsOfIntersection(intersection, "GREEN")));
						vehicle.setIntersectionId(intersection.getId());
						vehicle.status = Status.ENTER_INTERSECTION;
						strIntersection = "ENTER INTERSECTION\n" 
								  	    + (int)vehicle.getDistance() + " : " + intersection.getAddress();

					// State transition: ENTER_INTERSECTION -> AT_INTERSECTION
					} else if ((vehicle.status == Status.ENTER_INTERSECTION) && 
							   (vehicle.getDistance() <= radius) &&
							   (vehicle.getIntersectionId() == intersection.getId())) {
						vehicle.status = Status.AT_INTERSECTION;
						strIntersection = "AT INTERSECTION\n" 
										+ (int)vehicle.getDistance() + " : " + intersection.getAddress();

					// State transition: AT_INTERSECTION -> AT_INTERSECTION (self-loop)
					} else if ((vehicle.status == Status.AT_INTERSECTION) &&
							   (vehicle.getDistance() <= radius) &&
							   (vehicle.getIntersectionId() == intersection.getId())) {
						vehicle.status = Status.AT_INTERSECTION;
						strIntersection = "AT INTERSECTION\n" 
										+ (int)vehicle.getDistance() + " : " + intersection.getAddress();

					// State transition: AT_INTERSECTION -> EXIT_INTERSECTION
					} else if ((vehicle.status == Status.AT_INTERSECTION) &&
							   (vehicle.getDistance() >= radius) &&
							   (vehicle.getIntersectionId() == intersection.getId())) {
						playSound("exit_intersection");
						setButtonText(btnRed, Integer.toString(db.getLightsOfArea(currArea, "RED")));
						setButtonText(btnGreen, Integer.toString(db.getLightsOfArea(currArea, "GREEN")));
						vehicle.status = Status.EXIT_INTERSECTION;
						strIntersection = "EXIT INTERSECTION\n" 
								  		+ (int)vehicle.getDistance() + " : " + intersection.getAddress();

					// State transition: EXIT_INTERSECTION -> OUTSIDE_INTERSECTION
					} else if ((vehicle.status == Status.EXIT_INTERSECTION) &&
							   (vehicle.getDistance() >= radius) &&
							   (vehicle.getIntersectionId() == intersection.getId())) {
						vehicle.setIntersectionId(-1);
						vehicle.status = Status.OUTSIDE_INTERSECTION;
					}						
				}
				
				// When not in intersection:
				// - display info about nearby intersections
				// - enable intersection add button
				// - disable the trafficlight buttons
				if (vehicle.status == Status.OUTSIDE_INTERSECTION) {
					txtStatus.setText(strOutside);
					btnIntersection.setEnabled(true);
					btnRed.setEnabled(false);
					btnGreen.setEnabled(false);
					
				// When in intersection:
				// - display info about this intersection
				// - disable intersection add button
				// - enable the trafficlight buttons
				} else { 
					txtStatus.setText(strIntersection);
					btnIntersection.setEnabled(false);
					btnRed.setEnabled(true);
					btnGreen.setEnabled(true);
				}
			} else {
				btnIntersection.setEnabled(false);
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
