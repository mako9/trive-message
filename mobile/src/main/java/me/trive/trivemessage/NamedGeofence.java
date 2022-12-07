package me.trive.trivemessage;


import android.support.annotation.NonNull;

import com.google.android.gms.location.Geofence;

import java.text.DateFormat;
import java.util.Date;

public class NamedGeofence implements Comparable {

  // Initialization and attributes
  public int idInt;
  public String id;
  public String category;
  public String text;
  public double latitude;
  public double longitude;
  public float radius = 5 * 1000.0f;
  public String address;
  public String timestamp;


  public Geofence geofence() {
    //id = UUID.randomUUID().toString();
    idInt = (int) (System.currentTimeMillis() & 0xfffffff);
    id = String.valueOf(idInt);
    timestamp = DateFormat.getDateTimeInstance().format(new Date());;
    return new Geofence.Builder()
            .setRequestId(id)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .setCircularRegion(latitude, longitude, radius)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .build();
  }

  @Override
  public int compareTo(@NonNull Object another) {
    NamedGeofence other = (NamedGeofence) another;
    return timestamp.compareTo(other.timestamp);
  }
}
