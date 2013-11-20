package com.github.etsija.liikennevalot;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

import org.jgeohash.GeoHashUtils;

import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
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
import android.widget.Spinner;
import android.widget.Toast;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;

public class MainActivity extends Activity {

	String lastList;
	int countRed;
	int countGreen;
	HashMap<String, String> prefKeys = new HashMap<String, String>();
	Location currLocation;
	static Geocoder geocoder;
	LocationManager mLocationManager;
	LocationListener mLocationListener;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// Location services
		mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		mLocationListener = new MyLocationListener();
	    mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
	    geocoder = new Geocoder(this, Locale.ENGLISH);  // For address search
	    
	    // Button sounds
		final SoundPool sp = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
		final int soundGreen = sp.load(this, R.raw.green, 1);
		final int soundRed   = sp.load(this, R.raw.red, 1);
		
		// Fetch all preferences into prefKeys HashMap
		final SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
	    lastList = settings.getString("lastList", null);
	    Log.d("LIIKENNEVALOT", "lastList = " + lastList);
	    
	    // Drop-down for the different lists
		final Spinner listat = (Spinner) findViewById(R.id.listat);
		listat.setSelection(getIndex(listat, lastList));
		
		// Fill the counters based on # RED and GREEN entries in logfile
		countRed = countEntries(lastList + ".txt", "RED");
		countGreen = countEntries(lastList + ".txt", "GREEN");
		
		// Blue button to add an intersection
		final Button btnIntersection = (Button) findViewById(R.id.intersection);
		btnIntersection.getBackground().setColorFilter(Color.BLUE, Mode.MULTIPLY);
		btnIntersection.setTextColor(Color.WHITE);
		btnIntersection.setTextSize(20);
		btnIntersection.setText("Lisää risteys");
		/*
		// Click: add a new intersection for this list
		btnIntersection.setOnClickListener(new OnClickListener() {
	        public void onClick(View v) {
	        	v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
	        	String strAddr = getAddressFromLoc(currLocation);
	        	sp.play(soundRed, 0.3f, 0.3f, 0, 0, 1);
	        	//DataEntry testEntry = new DataEntry(getNow(), "RED", currLocation, strAddr);
	        	countRed++;
	        	setButtonText(btnRed, Integer.toString(countRed));
	        	Toast.makeText(getApplicationContext(), 
	        			       setClickInfoText(getNow(), locStringFromLoc(currLocation), strAddr), 
	        			       Toast.LENGTH_SHORT).show();
	        	
	    		// Create an entry and write to SD drive
	    		String entry = createEntry(getNow(), "RED", currLocation, strAddr);
	        	writeOnSD(lastList + ".txt", entry);
	        }
		});
		*/
		// Red button
		final Button btnRed = (Button) findViewById(R.id.buttonRed); 
		btnRed.getBackground().setColorFilter(Color.RED, Mode.MULTIPLY);
		btnRed.setTextColor(Color.WHITE);
		btnRed.setTextSize(20);
		setButtonText(btnRed, Integer.toString(countRed));
		
		// Click: increase RED counter by one and show new value in button
		btnRed.setOnClickListener(new OnClickListener() {
	        public void onClick(View v) {
	        	v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
	        	String strAddr = getAddressFromLoc(currLocation);
	        	sp.play(soundRed, 0.3f, 0.3f, 0, 0, 1);
	        	//DataEntry testEntry = new DataEntry(getNow(), "RED", currLocation, strAddr);
	        	countRed++;
	        	setButtonText(btnRed, Integer.toString(countRed));
	        	Toast.makeText(getApplicationContext(), 
	        			       setClickInfoText(getNow(), locStringFromLoc(currLocation), strAddr), 
	        			       Toast.LENGTH_SHORT).show();
	        	
	    		// Create an entry and write to SD drive
	    		String entry = createEntry(getNow(), "RED", currLocation, strAddr);
	        	writeOnSD(lastList + ".txt", entry);
	        }
		});
		
		// Longclick: decrease RED counter by one
		btnRed.setOnLongClickListener(new OnLongClickListener() {
			public boolean onLongClick(View v) {
				if (countRed > 0) {
					countRed--;
					try {
						deleteLastLine(lastList + ".txt", countLines(lastList + ".txt"));
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				sp.play(soundRed, 0.3f, 0.3f, 0, 1, 1);
				setButtonText(btnRed, Integer.toString(countRed));
				return true;
			}
		});

		// Green button
		final Button btnGreen = (Button) findViewById(R.id.buttonGreen); 
		btnGreen.getBackground().setColorFilter(Color.GREEN, Mode.MULTIPLY);
		btnGreen.setTextColor(Color.WHITE);
		btnGreen.setTextSize(20);
		setButtonText(btnGreen, Integer.toString(countGreen));
		
		// Click: increase GREEN counter by one and show new value in button
		btnGreen.setOnClickListener(new OnClickListener() {
	        public void onClick(View v) {
	        	v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
	        	String strAddr = getAddressFromLoc(currLocation);
	        	sp.play(soundGreen, 0.3f, 0.3f, 0, 0, 1);
	        	countGreen++;
	        	setButtonText(btnGreen, Integer.toString(countGreen));
	        	Toast.makeText(getApplicationContext(), 
	        			       setClickInfoText(getNow(), locStringFromLoc(currLocation), strAddr),
	        			       Toast.LENGTH_SHORT).show();
	        	
	    		// Create an entry and write to SD drive
	    		String entry = createEntry(getNow(), "GREEN", currLocation, strAddr);
	        	writeOnSD(lastList + ".txt", entry);
	        }
		});
		
		// Longclick: decrease GREEN counter by one
		btnGreen.setOnLongClickListener(new OnLongClickListener() {
			public boolean onLongClick(View v) {
				if (countGreen > 0) {
					countGreen--;
					try {
						deleteLastLine(lastList + ".txt", countLines(lastList + ".txt"));
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				sp.play(soundGreen, 0.3f, 0.3f, 0, 1, 1);
				setButtonText(btnGreen, Integer.toString(countGreen));
				return true;
			}
		});
		
		// Handles the menu change (into a different list)
		listat.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view, 
		            int pos, long id) {
		        lastList = parent.getItemAtPosition(pos).toString();
		        countRed = countEntries(lastList + ".txt", "RED");
				countGreen = countEntries(lastList + ".txt", "GREEN");
		        setButtonText(btnRed, Integer.toString(countRed));
		        setButtonText(btnGreen, Integer.toString(countGreen));
		    }

		    public void onNothingSelected(AdapterView<?> parent) {
		        // Another interface callback
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
    	  //writeNewSDFile(txtList + ".txt");
      }
      
      // Stop listening to location updates when app is closed
      mLocationManager.removeUpdates(mLocationListener);
	}
	
	public void ringtone() {
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
            r.play();
        } catch (Exception e) {
        	
        }
    }
	
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
				Log.e("LIIKENNEVALOT", e.toString());
				strAddress = "ADDR_SERVICE_NOT_AVAILABLE";
			}
		}
		return strAddress;
	}
	
	// Format String to a timestamp
	private String setClickInfoText(Time time, String strLoc, String strAddr) {
		String entry = String.format("%02d", time.monthDay) + "."
			     + String.format("%02d", time.month) + "." 
			     + String.format("%04d", time.year) + " "
				 + String.format("%02d", time.hour) + ":" 
				 + String.format("%02d", time.minute) + ":"
				 + String.format("%02d", time.second) + "\n"
				 + strLoc + "\n"
				 + strAddr;
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
	
	// This is an inner class for the location listener
	public class MyLocationListener implements LocationListener {

		@Override
		public void onLocationChanged(Location loc) {
			loc.getLatitude();
			loc.getLongitude();
			currLocation = loc;
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
