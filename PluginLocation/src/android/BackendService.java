package com.ashwini.location.notification;

import java.util.Calendar;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.example.dasd.MainActivity;
import com.example.dasd.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
//import android.text.format.DateUtils;
import android.util.Log;
import android.widget.Toast;

public class BackendService extends IntentService implements LocationListener,
		GooglePlayServicesClient.ConnectionCallbacks,
		GooglePlayServicesClient.OnConnectionFailedListener{

	private final String TAG  = getClass().getSimpleName();
	private AlarmManager alarmManager;
	private Intent intent_;
	private PendingIntent pendingIntent;
	private LocationRequest mLocationRequest;
	private LocationClient mLocationClient;
	private boolean setting = false;
	
	public BackendService() {
		super("BackendService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.e(TAG, "onHandleIntent");
		Bundle bundle = intent.getExtras();
		for (String key : bundle.keySet()) {
		    Object value = bundle.get(key);
		    Log.d(TAG, String.format("%s %s (%s)", key,  
		        value.toString(), value.getClass().getName()));
		}
		if(intent.hasExtra("key")){
			String key = intent.getStringExtra("key");
			intent_.putExtras(intent.getExtras());
			if(intent.hasExtra("setting"))
				setting = intent.getBooleanExtra("setting", false);
			pendingIntent = PendingIntent.getService(this, 0, intent_, PendingIntent.FLAG_UPDATE_CURRENT);
			if(key.equalsIgnoreCase("start")){
				alarmManager.cancel(pendingIntent);
				if(intent.getBooleanExtra("daily", true))
					scheduleNextDay();
				else
					scheduleNextUpdate(intent.getIntExtra("minute", 1));
				
				/*if(!mLocationClient.isConnected())
					mLocationClient.connect();*/
			}else if(key.equalsIgnoreCase("stop")){
				alarmManager.cancel(pendingIntent);
			}
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Log.e(TAG, "onCreate");
		
		alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		intent_ = new Intent(this, this.getClass());
		
		mLocationRequest = LocationRequest.create();

		mLocationRequest.setInterval(LocationUtils.UPDATE_INTERVAL_IN_MILLISECONDS);
		mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
		mLocationRequest.setFastestInterval(LocationUtils.FAST_INTERVAL_CEILING_IN_MILLISECONDS);
	
		mLocationClient = new LocationClient(this, this, this);
		
		
		
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.e(TAG, "onDestroy");
	}
	
	private void scheduleNextDay()
	  {
	    
		Calendar calendar = Calendar.getInstance(Locale.getDefault());
		
		if(calendar.get(Calendar.HOUR_OF_DAY) == 11){
			if(!mLocationClient.isConnected())
				mLocationClient.connect();
		}
		
		calendar.add(Calendar.DAY_OF_YEAR, 1);
		calendar.set(Calendar.HOUR_OF_DAY, 11);
		calendar.set(Calendar.MINUTE, 00);
		   
	    Log.e(TAG, "next "+calendar.getTime().toString());
	    alarmManager.set(AlarmManager.RTC, calendar.getTimeInMillis(), pendingIntent);
	  }
	
	private void scheduleNextUpdate(int minute)
	  {
		Calendar calendar = Calendar.getInstance(Locale.getDefault());
		if(calendar.get(Calendar.MINUTE) % minute == 0){
			if(!mLocationClient.isConnected())
				mLocationClient.connect();
		}
	    
		Calendar calendarSet = Calendar.getInstance(Locale.getDefault());
		
		int currentMinute = calendar.get(Calendar.MINUTE);
		switch (minute) {
		case 15:
		case 30:
			calendarSet.set(Calendar.MINUTE, ((currentMinute/minute)*minute)+minute);
			break;
		case 45:
			int totalDaymin = calendar.get(Calendar.HOUR_OF_DAY)*60 + currentMinute;
			if(totalDaymin % 45 == 0){
				if(!mLocationClient.isConnected())
					mLocationClient.connect();
				calendarSet.set(Calendar.MINUTE, totalDaymin % 60);
				calendarSet.set(Calendar.HOUR_OF_DAY, totalDaymin / 60);
			}else{
				int tt = ((totalDaymin / minute) +1)*minute;//final minutes
				calendarSet.set(Calendar.MINUTE, tt % 60);
				calendarSet.set(Calendar.HOUR_OF_DAY, tt / 60);
				
			}
			break;
		case 60:
			calendarSet.set(Calendar.MINUTE, 00);
			calendarSet.add(Calendar.HOUR_OF_DAY, 1);
			break;

		default:
			break;
		}

		/*long currentTimeMillis = System.currentTimeMillis();
		long nextUpdateTimeMillis = currentTimeMillis + minute * DateUtils.MINUTE_IN_MILLIS;*/
		//Calendar calendar = Calendar.getInstance(Locale.getDefault());
		//Calendar Setcalendar = Calendar.getInstance(Locale.getDefault());
		
		calendarSet.set(Calendar.SECOND, 00);
	    Log.e(TAG, "next "+calendarSet.getTime().toString());
	    alarmManager.set(AlarmManager.RTC, calendarSet.getTimeInMillis(), pendingIntent);
	  }

	@Override
	public void onConnectionFailed(ConnectionResult arg0) {
		Log.e(TAG, "onConnectionFailed");
	}


	@Override
	public void onConnected(Bundle arg0) {
		Log.e(TAG, "onConnected");
		//if (mUpdatesRequested) {
			startPeriodicUpdates();
		//}
	}


	@Override
	public void onDisconnected() {
		Log.e(TAG, "onDisconnected");
	}


	@Override
	public void onLocationChanged(Location location) {
		Log.e(TAG, "onLocationChanged");
		Toast.makeText(getBaseContext(), LocationUtils.getLatLng(this, location), Toast.LENGTH_SHORT).show();
		createNotification();
		stopPeriodicUpdates();
	}
	
	
	/**
	 * In response to a request to start updates, send a request to Location
	 * Services
	 */
	private void startPeriodicUpdates() {

		mLocationClient.requestLocationUpdates(mLocationRequest, this);
	}

	/**
	 * In response to a request to stop updates, send a request to Location
	 * Services
	 */
	private void stopPeriodicUpdates() {
		Log.e(TAG, "stopPeriodicUpdates");
		mLocationClient.removeLocationUpdates(this);
		if(mLocationClient.isConnected())
			mLocationClient.disconnect();
	}
	
	private NotificationCompat.Builder notificationBuilder;
	private Notification notification;
	private NotificationManager mNotificationManager;
	
	private void createNotification(){
		Intent playerIntent = new Intent(this, MainActivity.class);
		playerIntent.setAction("from_notification_bar");
		
		playerIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0, playerIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		
		
		notificationBuilder = new NotificationCompat.Builder(this);
		notificationBuilder.setSmallIcon(R.drawable.ic_launcher);
		notificationBuilder.setContentIntent(resultPendingIntent);
		notificationBuilder.setContentTitle("Deals");
        notificationBuilder.setContentText("There are "+Deals()+" new deals in your area!");
        notificationBuilder.setAutoCancel(true);
        if(setting){
	        Uri alarmSound = RingtoneManager
	                .getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
	        notificationBuilder.setLights(Color.BLUE, 500, 500);
	        //notificationBuilder.setTicker("Notification from LocationNotification");
	        notificationBuilder.setVibrate(new long[] { 100, 250, 100, 250, 100, 250 });
	        notificationBuilder.setSound(alarmSound);
        }
        notification = notificationBuilder.build();
		notification.flags |= Notification.DEFAULT_LIGHTS;
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		notification.flags |= Notification.DEFAULT_SOUND;
		notification.flags |= Notification.DEFAULT_VIBRATE;
		mNotificationManager.notify(1002, notification);
	}

	
	private int Deals(){
		String json = "{ \"deals\" : [\"12534\", \"12234\", \"12734\" , \"12384\", \"12394\", \"12034\"]}";
		try {
			JSONObject obj = new JSONObject(json);
			JSONArray arr = obj.getJSONArray("deals");
			return arr.length();
		} catch (JSONException e) {
			e.printStackTrace();
			return 0;
		}
	}
}
