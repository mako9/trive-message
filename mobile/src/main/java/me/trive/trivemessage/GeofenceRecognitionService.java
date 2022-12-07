package me.trive.trivemessage;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GeofenceRecognitionService extends IntentService {

  // Initialization and attributes
  private final String TAG = GeofenceRecognitionService.class.getName();

  private SharedPreferences prefs;
  private SharedPreferences shownprefs;
  private Gson gson;
  private Long timestamp;
  AndroidAutoService androidAutoService;


  public GeofenceRecognitionService() {
    super("GeofenceRecognitionService");
  }

  // handle the intent from GeofenceController class
  @Override
  protected void onHandleIntent(Intent intent) {
    prefs = getApplicationContext().getSharedPreferences(Constants.SharedPrefs.Geofences, Context.MODE_PRIVATE);
    shownprefs = getApplicationContext().getSharedPreferences(Constants.SharedPrefs.shownGeofences, Context.MODE_PRIVATE);
    gson = new Gson();

    GeofencingEvent event = GeofencingEvent.fromIntent(intent);
    if (event != null) {
      if (event.hasError()) {
        onError(event.getErrorCode());
      } else {
        int transition = event.getGeofenceTransition();
        if (transition == Geofence.GEOFENCE_TRANSITION_ENTER || transition == Geofence.GEOFENCE_TRANSITION_DWELL || transition == Geofence.GEOFENCE_TRANSITION_EXIT) {
          List<String> geofenceIds = new ArrayList<>();
          for (Geofence geofence : event.getTriggeringGeofences()) {
            geofenceIds.add(geofence.getRequestId());
          }
          if (transition == Geofence.GEOFENCE_TRANSITION_ENTER || transition == Geofence.GEOFENCE_TRANSITION_DWELL) {
            onEnteredGeofences(geofenceIds);
          }
        }
      }
    }
  }

  // Actions if user enters a given geofence
  private void onEnteredGeofences(List<String> geofenceIds) {
    for (String geofenceId : geofenceIds) {
      String geofenceName = "";
      String geofenceText = "";
      int geofenceIdInt = 0;
      timestamp = System.currentTimeMillis()/1000;

      // Loop over all geofence keys in prefs and retrieve NamedGeofence from SharedPreference
      Map<String, ?> keys = shownprefs.getAll();
      for (Map.Entry<String, ?> entry : keys.entrySet()) {
        String jsonString = shownprefs.getString(entry.getKey(), null);
        NamedGeofence namedGeofence = gson.fromJson(jsonString, NamedGeofence.class);
        if (namedGeofence.id.equals(geofenceId)) {
          geofenceName = namedGeofence.category;
          geofenceText = namedGeofence.text;
          geofenceIdInt = namedGeofence.idInt;

          Log.e("Error", "Geofence found");
          break;
        }
      }

      // Set the notification text and send the notification
      String contextText = String.format(this.getResources().getString(R.string.Notification_Text), geofenceName);

      NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
      Intent intent = new Intent(this, AllMessagesActivity.class);
      intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
      PendingIntent pendingNotificationIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

      Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

      Notification notification = new NotificationCompat.Builder(this)
              .setSmallIcon(R.mipmap.ic_launcher)
              .setLargeIcon((BitmapFactory.decodeResource(this.getResources(), R.mipmap.ic_launcher)))
              //.setContentTitle(this.getResources().getString(R.string.Notification_Title))
              .setContentTitle(contextText)
              .setContentText(geofenceText)
              .setContentIntent(pendingNotificationIntent)
              .setStyle(new NotificationCompat.BigTextStyle().bigText(contextText))
              .setPriority(NotificationCompat.PRIORITY_HIGH)
              .setAutoCancel(true)
              .setVibrate(new long[] {1000,1000,1000})
              .setSound(alarmSound)
              .build();

      notificationManager.notify(0, notification);

      // Start the Android Auto notification process
      try {
        this.androidAutoService = new AndroidAutoService(getApplicationContext());
        this.androidAutoService.sendNotification(contextText,geofenceText,R.mipmap.ic_launcher,R.mipmap.ic_launcher,geofenceIdInt);
        notificationManager.cancel(geofenceIdInt);
      }
      catch (Exception e) {
      //e.printStackTrace();
      Log.e("Error : Connection", "Impossible to connect to Android Auto", e);
      }
    }
  }

  // Log error message
  private void onError(int i) {
    Log.e(TAG, "Geofencing Error: " + i);
  }
}