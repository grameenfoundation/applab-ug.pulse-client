/**
 * Copyright (C) 2010 Grameen Foundation
Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations under
the License.
 */
package applab.pulse.client;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import applab.pulse.client.R;

public class Locate implements LocationListener, Runnable {

	private LocationManager locationManager;
	private String latitude; 
	private String longitude;
	private String accuracy; 
	private String altitude;

	public Locate(LocationManager l) {
		locationManager = l;
	}
	
	public synchronized void update() {
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
	}

	public synchronized void cancel() {
		locationManager.removeUpdates(this);
	}

	@Override
	public synchronized void run() {
		// TODO Auto-generated method stub
	}

	public void getLastKnownLocation() {
		Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		latitude = formatGps(location.getLatitude(), "lat");
		longitude = formatGps(location.getLongitude(), "lon");
		accuracy = String.valueOf(location.getAccuracy());
		altitude = String.valueOf(location.getAltitude());
	}

	@Override
	public void onLocationChanged(Location arg0) {
		latitude = formatGps(arg0.getLatitude(), "lat");
		longitude = formatGps(arg0.getLongitude(), "lon");
		accuracy = String.valueOf(arg0.getAccuracy());
		altitude = String.valueOf(arg0.getAltitude());
	}

	public void onProviderDisabled(String arg0) {
		// TODO
	}

	public void onProviderEnabled(String arg0) {
		// TODO
	}

	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		// TODO
	}

	/**
	 * formats the location coordinates to DMS
	 * 
	 * @param coordinates
	 *            the location coordinates
	 * @param type
	 *            lat or lon (latitude or longitude)
	 * @return the DMS formated GPS coordinate
	 */
	private String formatGps(double coordinates, String type) {
		String location = Double.toString(coordinates);
		String degreeSign = "\u00B0";
		String degree = location.substring(0, location.indexOf("."))
				+ degreeSign;
		location = "0." + location.substring(location.indexOf(".") + 1);
		double temp = Double.valueOf(location) * 60;
		location = Double.toString(temp);
		String mins = location.substring(0, location.indexOf(".")) + "'";

		location = "0." + location.substring(location.indexOf(".") + 1);
		temp = Double.valueOf(location) * 60;
		location = Double.toString(temp);
		String secs = location.substring(0, location.indexOf(".")) + '"';
		if (type.equalsIgnoreCase("lon")) {
			if (degree.startsWith("-")) {
				degree = "W " + degree.replace("-", "") + mins + secs;
			} else
				degree = "E " + degree.replace("-", "") + mins + secs;
		} else {
			if (degree.startsWith("-")) {
				degree = "S " + degree.replace("-", "") + mins + secs;
			} else
				degree = "N " + degree.replace("-", "") + mins + secs;
		}
		return degree;
	}

}
