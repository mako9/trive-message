package me.trive.trivemessage;

import android.app.ListActivity;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListAdapter;
import android.widget.SimpleAdapter;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.gson.Gson;

import org.apache.http.NameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GeofenceController {

  // region Properties

  private final String TAG = GeofenceController.class.getName();
  JSONParser jParser = new JSONParser();

  ArrayList<HashMap<String, String>> messagesList;

  private static String url_db = "http://mk68548.lima-city.de/get_all_products.php";

  private static final String TAG_SUCCESS = "success";
  private static final String TAG_PRODUCTS = "products";
  private static final String TAG_PID = "pid";
  private static final String TAG_NAME = "name";

  JSONArray messages = null;

  private Context context;
  private GoogleApiClient googleApiClient;
  private Gson gson;
  private SharedPreferences prefs;
  private GeofenceControllerListener listener;

  private List<NamedGeofence> namedGeofences;
  public List<NamedGeofence> getNamedGeofences() {
    return namedGeofences;
  }

  private List<NamedGeofence> namedGeofencesToRemove;

  private Geofence geofenceToAdd;
  private NamedGeofence namedGeofenceToAdd;
  private boolean isInternetEnabled = false;
  private ProgressDialog pDialog;

  // endregion

  // region Shared Instance

  private static GeofenceController INSTANCE;

  public static GeofenceController getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new GeofenceController();
    }
    return INSTANCE;
  }

  // endregion

  // region Public

  public void init(Context context) {
    this.context = context.getApplicationContext();


    gson = new Gson();
    namedGeofences = new ArrayList<>();
    namedGeofencesToRemove = new ArrayList<>();
    prefs = this.context.getSharedPreferences(Constants.SharedPrefs.Geofences, Context.MODE_PRIVATE);

    loadGeofences();
  }

  public void addGeofence(NamedGeofence namedGeofence, GeofenceControllerListener listener) {
    this.namedGeofenceToAdd = namedGeofence;
    this.geofenceToAdd = namedGeofence.geofence();
    this.listener = listener;

    connectWithCallbacks(connectionAddListener);
  }

  public void removeGeofences(List<NamedGeofence> namedGeofencesToRemove, GeofenceControllerListener listener) {
    this.namedGeofencesToRemove = namedGeofencesToRemove;
    this.listener = listener;

    connectWithCallbacks(connectionRemoveListener);
  }

  public void removeAllGeofences(GeofenceControllerListener listener) {
    namedGeofencesToRemove = new ArrayList<>();
    for (NamedGeofence namedGeofence : namedGeofences) {
      namedGeofencesToRemove.add(namedGeofence);
    }
    this.listener = listener;

    connectWithCallbacks(connectionRemoveListener);
  }

  // endregion

  // region Private

  private void loadGeofences() {
    // Loop over all geofence keys in prefs and add to namedGeofences
    if (!isInternetEnabled) {
      new LoadAllProducts().execute();
      Map<String, ?> keys = prefs.getAll();
      for (Map.Entry<String, ?> entry : keys.entrySet()) {
        String jsonString = prefs.getString(entry.getKey(), null);
        NamedGeofence namedGeofence = gson.fromJson(jsonString, NamedGeofence.class);
        namedGeofences.add(namedGeofence);
      }

      // Sort namedGeofences by name
      Collections.sort(namedGeofences);

    }
  }


  private void connectWithCallbacks(GoogleApiClient.ConnectionCallbacks callbacks) {
    googleApiClient = new GoogleApiClient.Builder(context)
            .addApi(LocationServices.API)
            .addConnectionCallbacks(callbacks)
            .addOnConnectionFailedListener(connectionFailedListener)
            .build();
    googleApiClient.connect();
  }

  private GeofencingRequest getAddGeofencingRequest() {
    List<Geofence> geofencesToAdd = new ArrayList<>();
    geofencesToAdd.add(geofenceToAdd);
    GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
    builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
    builder.addGeofences(geofencesToAdd);
    return builder.build();
  }

  private void saveGeofence() {
    namedGeofences.add(namedGeofenceToAdd);
    if (listener != null) {
      listener.onGeofencesUpdated();
    }

    String json = gson.toJson(namedGeofenceToAdd);
    SharedPreferences.Editor editor = prefs.edit();
    editor.putString(namedGeofenceToAdd.id, json);
    editor.apply();
  }

  private void removeSavedGeofences() {
    SharedPreferences.Editor editor = prefs.edit();

    for (NamedGeofence namedGeofence : namedGeofencesToRemove) {
      int index = namedGeofences.indexOf(namedGeofence);
      editor.remove(namedGeofence.id);
      namedGeofences.remove(index);
      editor.apply();
    }

    if (listener != null) {
      listener.onGeofencesUpdated();
    }
  }

  private void sendError() {
    if (listener != null) {
      listener.onError();
    }
  }

  // endregion

  // region ConnectionCallbacks

  private GoogleApiClient.ConnectionCallbacks connectionAddListener = new GoogleApiClient.ConnectionCallbacks() {
    @Override
    public void onConnected(Bundle bundle) {
      Intent intent = new Intent(context, AreWeThereIntentService.class);
      PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
      PendingResult<Status> result = LocationServices.GeofencingApi.addGeofences(googleApiClient, getAddGeofencingRequest(), pendingIntent);
      result.setResultCallback(new ResultCallback<Status>() {
        @Override
        public void onResult(Status status) {
          if (status.isSuccess()) {
            saveGeofence();
          } else {
            Log.e(TAG, "Registering geofence failed: " + status.getStatusMessage() + " : " + status.getStatusCode());
            sendError();
          }
        }
      });
    }

    @Override
    public void onConnectionSuspended(int i) {
      Log.e(TAG, "Connecting to GoogleApiClient suspended.");
      sendError();
    }
  };

  private GoogleApiClient.ConnectionCallbacks connectionRemoveListener = new GoogleApiClient.ConnectionCallbacks() {
    @Override
    public void onConnected(Bundle bundle) {
      List<String> removeIds = new ArrayList<>();
      for (NamedGeofence namedGeofence : namedGeofencesToRemove) {
        removeIds.add(namedGeofence.id);
      }

      if (removeIds.size() > 0) {
        PendingResult<Status> result = LocationServices.GeofencingApi.removeGeofences(googleApiClient, removeIds);
        result.setResultCallback(new ResultCallback<Status>() {
          @Override
          public void onResult(Status status) {
            if (status.isSuccess()) {
              removeSavedGeofences();
            } else {
              Log.e(TAG, "Removing geofence failed: " + status.getStatusMessage());
              sendError();
            }
          }
        });
      }
    }

    @Override
    public void onConnectionSuspended(int i) {
      Log.e(TAG, "Connecting to GoogleApiClient suspended.");
      sendError();
    }
  };

  // endregion

  // region OnConnectionFailedListener

  private GoogleApiClient.OnConnectionFailedListener connectionFailedListener = new GoogleApiClient.OnConnectionFailedListener() {
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
      Log.e(TAG, "Connecting to GoogleApiClient failed.");
      sendError();
    }
  };

  // endregion

  // region Interfaces

  public interface GeofenceControllerListener {
    void onGeofencesUpdated();
    void onError();
  }

  // end region


/**
 * Background Async Task to Load all product by making HTTP Request
 * */
class LoadAllProducts extends AsyncTask<String, String, String> {

  /**
   * Before starting background thread Show Progress Dialog
   * */
  @Override
  protected void onPreExecute() {
    super.onPreExecute();
    pDialog = new ProgressDialog(GeofenceController.this);
    pDialog.setMessage("Loading products. Please wait...");
    pDialog.setIndeterminate(false);
    pDialog.setCancelable(false);
    pDialog.show();
  }

  /**
   * getting All products from url
   * */
  protected String doInBackground(String... args) {
    // Building Parameters
    List<NameValuePair> params = new ArrayList<NameValuePair>();
    // getting JSON string from URL
    JSONObject json = jParser.makeHttpRequest(url_db, "GET", params);

    // Check your log cat for JSON reponse
    Log.d("All Messages: ", json.toString());

    try {
      // Checking for SUCCESS TAG
      int success = json.getInt(TAG_SUCCESS);

      if (success == 1) {
        // products found
        // Getting Array of Products
        messages = json.getJSONArray(TAG_PRODUCTS);

        // looping through All Products
        for (int i = 0; i < messages.length(); i++) {
          JSONObject c = messages.getJSONObject(i);

          // Storing each json item in variable
          String id = c.getString(TAG_PID);
          String name = c.getString(TAG_NAME);

          // creating new HashMap
          HashMap<String, String> map = new HashMap<String, String>();

          // adding each child node to HashMap key => value
          map.put(TAG_PID, id);
          map.put(TAG_NAME, name);

          // adding HashList to ArrayList
          messagesList.add(map);
        }
      } /*else {
        // no products found
        // Launch Add New product Activity
        Intent i = new Intent(getApplicationContext(),
                NewProductActivity.class);
        // Closing all previous activities
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
      }*/
    } catch (JSONException e) {
      e.printStackTrace();
    }

    return null;
  }

  /**
   * After completing background task Dismiss the progress dialog
   * **/
  /*protected void onPostExecute(String file_url) {
    // dismiss the dialog after getting all products
    pDialog.dismiss();
    // updating UI from Background Thread
    runOnUiThread(new Runnable() {
      public void run() {
        /**
         * Updating parsed JSON data into ListView
         * */
        /*ListAdapter adapter = new SimpleAdapter(
                GeofenceController.this, messagesList,
                R.layout.list_item, new String[] { TAG_PID,
                TAG_NAME},
                new int[] { R.id.pid, R.id.name });
        // updating listview
        setListAdapter(adapter);
      }
    });

  }*/

}
}
