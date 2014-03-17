package com.watamidoing.tasks;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Locale;

import com.waid.R;
import com.watamidoing.contentproviders.Authentication;
import com.watamidoing.contentproviders.DatabaseHandler;
import com.watamidoing.utils.ConnectionResult;
import com.watamidoing.utils.HttpConnectionHelper;
import com.watamidoing.view.WhatAmIdoing;

import android.app.Activity;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View.OnClickListener;

public class WAIDLocationListener implements LocationListener {

	private Activity mContext;
	private SendLocationInformationTask locationTaskView;


	public WAIDLocationListener(WhatAmIdoing activity) {
		
		mContext = activity;
	}
	
	@Override
	public void onLocationChanged(Location location) {
		sendLocation(location);
		
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		
	}
	
	
	public void sendLocation(Location location){
		
		
		Double lat = Double.valueOf(location.getLatitude());
		Double longitude = Double.valueOf(location.getLongitude());
		String shareLocationUrl = mContext.getString(R.string.share_location_url);
		Authentication auth =  DatabaseHandler.getInstance(mContext).getDefaultAuthentication();
		
		Geocoder gcd = new Geocoder(mContext, Locale.getDefault());
		List<Address> addresses;
		try {
			addresses = gcd.getFromLocation(lat, longitude, 1);
			if (addresses.size() > 0) 
			    System.out.println(addresses.get(0).toString());
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
		
		String url = shareLocationUrl+"?token="+auth.getToken()+"&latitude="+lat+"&longitude="+longitude;
		Log.i("WAIDLocationListener.sendLocation",url);
		
		locationTaskView = new SendLocationInformationTask(url,mContext);
		locationTaskView.execute((Void) null);
	
		
	}

}
