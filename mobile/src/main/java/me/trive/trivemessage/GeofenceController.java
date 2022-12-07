package me.trive.trivemessage;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PointF;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;




public class GeofenceController{

  // Initialization and Attributes
  private final String TAG = GeofenceController.class.getName();

  private Context context;
  public GoogleApiClient googleApiClient;
  private Gson gson;
  private SharedPreferences prefs;
  private SharedPreferences ownprefs;
  private SharedPreferences shownprefs;
  private SharedPreferences knownprefs;
  private SharedPreferences userIdpref;
  private GeofenceControllerListener listener;
  List<NamedGeofence> shownGeofences;

  private List<NamedGeofence> namedGeofences;

  public List<NamedGeofence> getNamedGeofences() {
    return namedGeofences;
  }

  private List<NamedGeofence> ownGeofences;

  public List<NamedGeofence> getOwnGeofences() {
    return ownGeofences;
  }

  private List<NamedGeofence> namedGeofencesToRemove;

  private Geofence geofenceToAdd;
  private NamedGeofence namedGeofenceToAdd;
  private ArrayList<String> knownIds;
  private ArrayList<NamedGeofence> testGeofences;
  private PointF checkPoint = new PointF();
  private ArrayList<Double> distances;

  private static String url_receive;
  private static String url_send = "http://mk68548.lima-city.de/trivemessage/add_message.php";
  private static String url_delete;

  // JSON Node names
  private static final String TAG_SUCCESS = "success";
  private static final String TAG_MESSAGES = "messages";
  private static final String TAG_ID = "ID";
  private static final String TAG_CATEGORY = "Kategorie";
  private static final String TAG_TEXT = "Nachricht";
  private static final String TAG_LATITUDE = "Latitude";
  private static final String TAG_LONGITUDE = "Longitude";
  private static final String TAG_ADRESSE = "Adresse";
  private static final String TAG_TIMESTAMP = "Timestamp";
  private static final String TAG_USERID = "user_id";

  private static GeofenceController INSTANCE;

  // get Instance of the Controller
  public static GeofenceController getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new GeofenceController();
    }
    return INSTANCE;
  }

  // get Context
  public void init(Context context) {
    this.context = context.getApplicationContext();

    // Initialize Lists and Preferences
    gson = new Gson();
    namedGeofences = new ArrayList<>();
    ownGeofences = new ArrayList<>();
    testGeofences = new ArrayList<>();
    shownGeofences = new ArrayList<>();
    distances = new ArrayList<>();
    knownIds = new ArrayList<>();
    namedGeofencesToRemove = new ArrayList<>();
    prefs = this.context.getSharedPreferences(Constants.SharedPrefs.Geofences, Context.MODE_PRIVATE);
    ownprefs = this.context.getSharedPreferences(Constants.SharedPrefs.OwnGeofences, Context.MODE_PRIVATE);
    shownprefs = this.context.getSharedPreferences(Constants.SharedPrefs.shownGeofences, Context.MODE_PRIVATE);
    knownprefs = this.context.getSharedPreferences(Constants.SharedPrefs.knownGeofences, Context.MODE_PRIVATE);
    userIdpref = this.context.getSharedPreferences(Constants.SharedPrefs.USER_ID,Context.MODE_PRIVATE);

    loadAllGeofences();
  }

  // Get the Message from AllMessagesAdapter
  public void addGeofence(NamedGeofence namedGeofence, GeofenceControllerListener listener) {
    this.namedGeofenceToAdd = namedGeofence;
    this.geofenceToAdd = namedGeofence.geofence();
    this.listener = listener;

    String json = gson.toJson(namedGeofence);
    SharedPreferences.Editor editor = knownprefs.edit();
    editor.putString(namedGeofence.id, json);
    editor.apply();

    saveGeofence();

  }

  // Delete selected Message (online)
  public void removeGeofences(List<NamedGeofence> namedGeofencesToRemove, GeofenceControllerListener listener) {
    this.namedGeofencesToRemove = namedGeofencesToRemove;
    this.listener = listener;

    connectWithCallbacks(connectionRemoveListener);
  }

  // Delete all Messages (offline)
  public void removeAllGeofencesOffline(GeofenceControllerListener listener) {
    namedGeofencesToRemove = new ArrayList<>();
    for (NamedGeofence namedGeofence : namedGeofences) {
      namedGeofencesToRemove.add(namedGeofence);
    }
    this.listener = listener;

    connectWithCallbacks(connectionRemoveListenerOffline);
  }

  // Delete selected Message (offline)
  public void removeGeofencesOffline(List<NamedGeofence> namedGeofencesToRemove, GeofenceControllerListener listener) {
    this.namedGeofencesToRemove = namedGeofencesToRemove;
    this.listener = listener;

    connectWithCallbacks(connectionRemoveListenerOffline);
  }

  // Delete all Messages (online)
  public void removeAllGeofences(GeofenceControllerListener listener) {
    namedGeofencesToRemove = new ArrayList<>();
    for (NamedGeofence namedGeofence : ownGeofences) {
      namedGeofencesToRemove.add(namedGeofence);
    }
    this.listener = listener;

    connectWithCallbacks(connectionRemoveListener);
  }

  // Start Loading from Database and set context of the listener
  public void loadGeofencesFromDb(GeofenceControllerListener listener) {

    this.listener = listener;

    try {
      ReadDataFromDB();
    } catch (Exception e) {
      Log.e("Error", "Impossible to load data", e);
    }
  }

  // Check and Set Geofence
  private void proofGeofences() {

    Map<String, ?> keys = prefs.getAll();
    for (Map.Entry<String, ?> entry : keys.entrySet()) {
      String jsonString = prefs.getString(entry.getKey(), null);
      final NamedGeofence namedGeofence = gson.fromJson(jsonString, NamedGeofence.class);

      String json = gson.toJson(namedGeofence);
      SharedPreferences.Editor editor = knownprefs.edit();
      editor.putString(namedGeofence.id, json);
      editor.apply();

          namedGeofenceToAdd = namedGeofence;
          geofenceToAdd = namedGeofence.geofence();

          SharedPreferences.Editor editor2 = prefs.edit();
          editor2.remove(namedGeofence.id);
          editor2.apply();

          connectWithCallbacks(connectionAddListener);
      break;
      }
  }
  // region Private

  // Update the shown Messages
  private void loadAllGeofences() {
    // Loop over all geofence keys in prefs and add to namedGeofences

    namedGeofences.clear();
    if (shownprefs.getAll().size() > 0) {
    Map<String, ?> keys = shownprefs.getAll();
    for (Map.Entry<String, ?> entry : keys.entrySet()) {
      String jsonString = shownprefs.getString(entry.getKey(), null);
      NamedGeofence namedGeofence = gson.fromJson(jsonString, NamedGeofence.class);

      if (!namedGeofences.contains(namedGeofence)) {
        namedGeofences.add(namedGeofence);
      }
    }
    }
    try {
      // Sort namedGeofences
      Collections.sort(namedGeofences, Collections.reverseOrder());
    } catch (Exception e) {
      Log.e("Error", "Impossible to sort items", e);
    }
  }

  // Update the sent Messages
  public void loadOwnGeofences() {

    if(ownprefs.getAll().size()>0)  {
    Map<String, ?> keys2 = ownprefs.getAll();
    for (Map.Entry<String, ?> entry2 : keys2.entrySet()) {
      String jsonString2 = ownprefs.getString(entry2.getKey(), null);
      NamedGeofence namedGeofence2 = gson.fromJson(jsonString2, NamedGeofence.class);

      if (!ownGeofences.contains(namedGeofence2)) {
        ownGeofences.add(namedGeofence2);
      }
    }
    }

    try {
      // Sort namedGeofences
      Collections.sort(ownGeofences, Collections.reverseOrder());
    } catch (Exception e) {
      Log.e("Error", "Impossible to sort items", e);
    }

  }

  // Start GoogleAPIClient
  private void connectWithCallbacks(GoogleApiClient.ConnectionCallbacks callbacks) {
    googleApiClient = new GoogleApiClient.Builder(context)
            .addApi(LocationServices.API)
            .addConnectionCallbacks(callbacks)
            .addOnConnectionFailedListener(connectionFailedListener)
            .build();
    googleApiClient.connect();
  }

  // Create an Geofencing element
  private GeofencingRequest getAddGeofencingRequest() {
    List<Geofence> geofencesToAdd = new ArrayList<>();
    geofencesToAdd.add(geofenceToAdd);
    GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
    builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
    builder.addGeofences(geofencesToAdd);
    return builder.build();
  }

  // save Message and update view of the fragment
  private void saveGeofence() {

    try {
      SendMessageToDB();
    } catch (Exception e) {
      Log.e("Error", "Impossible to save message in DB", e);
    }

    if (!knownIds.contains(namedGeofenceToAdd.id)) {
      String json = gson.toJson(namedGeofenceToAdd);
      SharedPreferences.Editor editor = ownprefs.edit();
      editor.putString(namedGeofenceToAdd.id, json);
      editor.apply();
    }

    if (listener != null) {
      listener.onGeofencesUpdated();
    }
  }

  // start the process of showing the Message
  public void showGeofences() {

    if (listener != null) {

      Log.e("showGeofences", String.valueOf(namedGeofenceToAdd));

      Map<String, ?> keys = shownprefs.getAll();
      for (Map.Entry<String, ?> entry : keys.entrySet()) {
        String jsonString = shownprefs.getString(entry.getKey(), null);
        NamedGeofence namedGeofence = gson.fromJson(jsonString, NamedGeofence.class);
        shownGeofences.add(namedGeofence);
      }

        if (shownGeofences.contains(namedGeofenceToAdd)) {
          loadAllGeofences();
          listener.onGeofencesUpdated();
          listener.refreshView();
        }
        else {
          String json = gson.toJson(namedGeofenceToAdd);
          SharedPreferences.Editor editor = shownprefs.edit();
          editor.putString(namedGeofenceToAdd.id, json);
          editor.apply();
        }

      shownGeofences.clear();

      SharedPreferences.Editor editor2 = prefs.edit();
      editor2.remove(namedGeofenceToAdd.id);
      editor2.apply();

      loadAllGeofences();
      listener.onGeofencesUpdated();
      }
    }


  // Delete a message from the Database
  private void removeSavedGeofences() {
    SharedPreferences.Editor editor = ownprefs.edit();

    for (NamedGeofence namedGeofence : namedGeofencesToRemove) {
      int index = ownGeofences.indexOf(namedGeofence);
      editor.remove(namedGeofence.id);
      ownGeofences.remove(index);
      editor.apply();

      url_delete = "http://mk68548.lima-city.de/trivemessage/delete_message.php?ID=" + namedGeofence.id;

      DeleteFromDb();
    }

    if (listener != null) {
      listener.onGeofencesUpdated();
    }
  }

  // Send an error
  private void sendError() {
    if (listener != null) {
      listener.onError();
    }
  }

  // Remove message only from the client's device
  private void removeSavedGeofencesOffline() {
    SharedPreferences.Editor editor = shownprefs.edit();

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

  // Start a listener for the geofencing event
  private GoogleApiClient.ConnectionCallbacks connectionAddListener = new GoogleApiClient.ConnectionCallbacks() {
    @Override
    public void onConnected(Bundle bundle) {
      Intent intent = new Intent(context, GeofenceRecognitionService.class);
      PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
      PendingResult<Status> result = LocationServices.GeofencingApi.addGeofences(googleApiClient, getAddGeofencingRequest(), pendingIntent);
      result.setResultCallback(new ResultCallback<Status>() {
        @Override
        public void onResult(Status status) {
          if (status.isSuccess()) {
            showGeofences();
            Log.e("Error", "connectionAddListener started");

            //namedGeofences.add(namedGeofenceToAdd);
            if (listener != null) {
              listener.onGeofencesUpdated();

              String json = gson.toJson(namedGeofenceToAdd);
              SharedPreferences.Editor editor = shownprefs.edit();
              editor.putString(namedGeofenceToAdd.id, json);
              editor.apply();
            }

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

  // Remove a listener for a geofencing event (online)
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

  // Remove a listener for a geofencing event (offline)
  private GoogleApiClient.ConnectionCallbacks connectionRemoveListenerOffline = new GoogleApiClient.ConnectionCallbacks() {
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
              removeSavedGeofencesOffline();
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

  // get a failed listener
  private GoogleApiClient.OnConnectionFailedListener connectionFailedListener = new GoogleApiClient.OnConnectionFailedListener() {
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
      Log.e(TAG, "Connecting to GoogleApiClient failed.");
      sendError();
      //GooglePlayServicesUtil.getErrorDialog(connectionResult);
    }
  };

  // Interface for fragment communication
  public interface GeofenceControllerListener {
    void onGeofencesUpdated();
    void refreshView();
    void onError();
  }

  // Load relevant message from the Database
  private void ReadDataFromDB() {

    GPSTracker gpsTracker = new GPSTracker(context);
    PointF center = new PointF((float)gpsTracker.latitude, (float)gpsTracker.longitude);

    final double mult = 1; // mult = 1.1; is more reliable
    int radius = 5000;
    PointF p1 = calculateDerivedPosition(center, mult * radius, 0);
    PointF p2 = calculateDerivedPosition(center, mult * radius, 90);
    PointF p3 = calculateDerivedPosition(center, mult * radius, 180);
    PointF p4 = calculateDerivedPosition(center, mult * radius, 270);

    url_receive = "http://mk68548.lima-city.de/trivemessage/get_message.php?ref1=" + p3.x + "&ref2=" + p1.x + "&ref3=" + p2.y + "&ref4=" + p4.y;

    JsonObjectRequest jreq = new JsonObjectRequest(Request.Method.GET, url_receive,
            new Response.Listener<JSONObject>() {

              @Override
              public void onResponse(JSONObject response) {
                try {
                  int success = response.getInt(TAG_SUCCESS);

                  if (success == 1) {
                    JSONArray ja = response.getJSONArray(TAG_MESSAGES);

                    for (int i = 0; i < ja.length(); i++) {

                      JSONObject jobj = ja.getJSONObject(i);
                      NamedGeofence item = new NamedGeofence();

                      item.id = jobj.getString(TAG_ID);
                      item.category = jobj.getString(TAG_CATEGORY);
                      item.text = jobj.getString(TAG_TEXT);
                      item.latitude = Double.parseDouble(jobj.getString(TAG_LATITUDE));
                      item.longitude = Double.parseDouble(jobj.getString(TAG_LONGITUDE));
                      item.address = jobj.getString(TAG_ADRESSE);
                      item.timestamp = jobj.getString(TAG_TIMESTAMP);

                      Log.e("Items", item.id);

                      Map<String, ?> keys = knownprefs.getAll();
                      for (Map.Entry<String, ?> entry : keys.entrySet()) {
                        String jsonString = knownprefs.getString(entry.getKey(), null);
                        NamedGeofence namedGeofence = gson.fromJson(jsonString, NamedGeofence.class);
                        knownIds.add(namedGeofence.id);
                      }

                      if (!knownIds.contains(item.id)) {

                        GPSTracker gpsTracker = new GPSTracker(context);

                        PointF center = new PointF((float)gpsTracker.latitude, (float)gpsTracker.longitude);
                        checkPoint = new PointF((float)item.latitude,(float)item.longitude);
                        int radius = 5000;

                        if (pointIsInCircle(checkPoint, center, radius) == true) {

                          distances.add(getDistanceBetweenTwoPoints(checkPoint,center));
                          testGeofences.add(item);
                        }
                        else  {
                          Log.e("No near message", item.id);
                        }
                      }
                      else  {
                        Log.e("Error", "ID " +item.id + " already connected");
                        SharedPreferences.Editor editor = prefs.edit();
                          editor.remove(item.id);
                          editor.apply();
                      }
                    }

                    if (distances.size() > 0) {
                      int minIndex = distances.indexOf(Collections.min(distances));
                      String json = gson.toJson(testGeofences.get(minIndex));
                      SharedPreferences.Editor editor = prefs.edit();
                      editor.putString(testGeofences.get(minIndex).id, json);
                      editor.apply();
                      distances.clear();
                      testGeofences.clear();
                    }
                    proofGeofences();
                  }
                  else {
                    Log.e("DB", "No messages to load");
                  }

                } catch (JSONException e) {
                  e.printStackTrace();
                }

              }
            }, new Response.ErrorListener() {

      @Override
      public void onErrorResponse(VolleyError error) {

        Toast.makeText(context,
                "failed to load messages", Toast.LENGTH_SHORT).show();
        VolleyLog.e("Error: " + error.getMessage());
      }
    });

    // Adding request to request queue
    MyApplication.getInstance().addToReqQueue(jreq);

  }

  // Store message in the Database
  public void SendMessageToDB() {
    StringRequest postRequest = new StringRequest(Request.Method.POST, url_send,
            new Response.Listener<String>() {
              @Override
              public void onResponse(String response) {
                Toast.makeText(context,
                        "Message Send Successfully",
                        Toast.LENGTH_SHORT).show();

              }
            }, new Response.ErrorListener() {
      @Override
      public void onErrorResponse(VolleyError error) {

        VolleyLog.e("Error:" + error.getMessage());
        Toast.makeText(context,
                "failed to insert", Toast.LENGTH_SHORT).show();
      }
    }) {
      @Override
      protected Map<String, String> getParams() {
        Map<String, String> params = new HashMap<String, String>();
        params.put(TAG_ID, namedGeofenceToAdd.id);
        params.put(TAG_CATEGORY, namedGeofenceToAdd.category);
        params.put(TAG_TEXT, namedGeofenceToAdd.text);
        params.put(TAG_LATITUDE, Double.toString(namedGeofenceToAdd.latitude));
        params.put(TAG_LONGITUDE, Double.toString(namedGeofenceToAdd.longitude));
        params.put(TAG_ADRESSE, namedGeofenceToAdd.address);
        params.put(TAG_TIMESTAMP, namedGeofenceToAdd.timestamp);
        params.put(TAG_USERID, userIdpref.getString(Constants.SharedPrefs.USER_ID, "default"));

        Log.e("userid", userIdpref.getString(Constants.SharedPrefs.USER_ID, "default"));
        PreferenceManager.getDefaultSharedPreferences(context).getString(Constants.SharedPrefs.USER_ID, "defaultStringIfNothingFound");

        return params;
      }
    };

    // Adding request to request queue
    MyApplication.getInstance().addToReqQueue(postRequest);
  }

  // Delete a message from the Database
  private void DeleteFromDb() {
    JsonObjectRequest delete_request = new JsonObjectRequest(url_delete,
            null, new Response.Listener<JSONObject>() {

      @Override
      public void onResponse(JSONObject response) {
        try {
          int success = response.getInt("success");

          if (success == 1) {
            Toast.makeText(context,
                    "Deleted Successfully",
                    Toast.LENGTH_SHORT).show();
          } else {
            Toast.makeText(context,
                    "failed to delete", Toast.LENGTH_SHORT)
                    .show();
          }
        } catch (JSONException e) {
          e.printStackTrace();
        }

      }
    }, new Response.ErrorListener() {

      @Override
      public void onErrorResponse(VolleyError error) {

      }
    });
    // Adding request to request queue
    MyApplication.getInstance().addToReqQueue(delete_request);
  }

  // Calculate the Points (p1, p2, p3, p4) to determine the relevant area
  public static PointF calculateDerivedPosition(PointF point, double range, double bearing) {

    double EarthRadius = 6371000; // m

    double latA = Math.toRadians(point.x);
    double lonA = Math.toRadians(point.y);
    double angularDistance = range / EarthRadius;
    double trueCourse = Math.toRadians(bearing);

    double lat = Math.asin(
            Math.sin(latA) * Math.cos(angularDistance) +
                    Math.cos(latA) * Math.sin(angularDistance)
                            * Math.cos(trueCourse));

    double dlon = Math.atan2(
            Math.sin(trueCourse) * Math.sin(angularDistance)
                    * Math.cos(latA),
            Math.cos(angularDistance) - Math.sin(latA) * Math.sin(lat));

    double lon = ((lonA + dlon + Math.PI) % (Math.PI * 2)) - Math.PI;

    lat = Math.toDegrees(lat);
    lon = Math.toDegrees(lon);

    PointF newPoint = new PointF((float) lat, (float) lon);

    return newPoint;
  }

  // Check whether a point is inside the relevant area
  public static boolean pointIsInCircle(PointF pointForCheck, PointF center,
                                       double radius) {
    if (getDistanceBetweenTwoPoints(pointForCheck, center) <= radius) {
      Log.e("Point is near", String.valueOf(pointForCheck));
      return true;
    }
    else {
      return false;
    }
  }

  // Calculate the distance between two points on earth
  public static double getDistanceBetweenTwoPoints(PointF p1, PointF p2) {
    double R = 6371000; // m
    double dLat = Math.toRadians(p2.x - p1.x);
    double dLon = Math.toRadians(p2.y - p1.y);
    double lat1 = Math.toRadians(p1.x);
    double lat2 = Math.toRadians(p2.x);

    double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.sin(dLon / 2)
            * Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2);
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    double d = R * c;

    return d;
  }
}