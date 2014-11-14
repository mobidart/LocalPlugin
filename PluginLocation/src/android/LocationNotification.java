package org.apache.cordova.plugin;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;

import com.ashwini.location.notification.BackendService;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

/**
 * This class echoes a string called from JavaScript.
 */
public class LocationNotification extends CordovaPlugin {

	private final String TAG = "ConnectPlugin";
	private Activity activty;
	private Intent serviceintent;
	
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    	Log.e(TAG, "execute plugin.............................."+args.getInt(0));
    	
		if(action.equalsIgnoreCase("startLocation")){
			Toast.makeText(activty, "Successfully started background!!!", Toast.LENGTH_SHORT).show();
			serviceintent.putExtra("key", "start");
			serviceintent.putExtra("minute", args.getInt(0));
			serviceintent.putExtra("daily", args.getBoolean(1));
			serviceintent.putExtra("setting", args.getBoolean(2));
			activty.startService(serviceintent);
			return true;
		}
		if(action.equalsIgnoreCase("stopLocation")){
			Toast.makeText(activty, "Successfully stoped", Toast.LENGTH_SHORT).show();
			serviceintent.putExtra("setting", args.getBoolean(0));
			serviceintent.putExtra("key", "stop");
			activty.startService(serviceintent);
			return true;
		}
		
			return false;
		
    }

    @Override
	public void initialize(CordovaInterface cordova, CordovaWebView webView) {
		super.initialize(cordova, webView);
		activty = cordova.getActivity();
		serviceintent = new Intent(activty, BackendService.class);
	}
    
}