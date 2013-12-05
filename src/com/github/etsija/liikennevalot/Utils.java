package com.github.etsija.liikennevalot;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.Date;
import java.util.Scanner;

import org.jgeohash.GeoHashUtils;

import android.location.Location;
import android.os.Environment;
import android.text.format.Time;
import android.util.Log;

public class Utils {

	// This class includes various helper methods I have been using before and might need still;
	// no need to keep them all under my MainActivity
	
	// Create one entry line to the logfile
	public String createEntry(Time time, String button, Location loc, String addr) {
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
	public static boolean isExternalStorageWritable() {
	    String state = Environment.getExternalStorageState();
	    if (Environment.MEDIA_MOUNTED.equals(state)) {
	        return true;
	    }
	    return false;
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
	
    // This method reads the logfile and inserts trafficlight events from it to the "trafficlight" SQLite table
    @SuppressWarnings("deprecation")
	public void moveToDb(String logFileName, DatabaseHelper db) {
		
		if (!Utils.isExternalStorageWritable()) return;
		
		try {
			File inputFile = new File(Environment.getExternalStorageDirectory(), logFileName);
			if (!inputFile.isFile()) {
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

}
