package me.trive.trivemessage;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesUtil;

import java.util.Locale;

public class AllMessagesActivity extends AppCompatActivity {

  private TextToSpeech t1;
  public AllMessagesAdapter allMessagesAdapter;
  public BroadcastReceiver receiver;
  Intent serviceIntent;


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    Intent intent = getIntent();
    setContentView(R.layout.activity_all_messages);

    startService(new Intent(getBaseContext(), CheckDbService.class));
    allMessagesAdapter = new AllMessagesAdapter(this);

    t1=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
      @Override
      public void onInit(int status) {
        if(status != TextToSpeech.ERROR) {
          t1.setLanguage(Locale.UK);
        }
      }
    });

    getSupportActionBar().setDisplayUseLogoEnabled(true);
    getSupportActionBar().setDisplayShowHomeEnabled(true);
    getSupportActionBar().setIcon(R.drawable.trivemessage);
    //setTitle(R.string.app_title);
    if (savedInstanceState == null) {
      getSupportFragmentManager().beginTransaction()
              .add(R.id.container, new AllMessagesFragment())
              .commit();
    }

    receiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {

        AllMessagesFragment fragment = (AllMessagesFragment) getSupportFragmentManager().findFragmentById(R.id.container);
        fragment.refreshViewFragment();

      }
    };

    GeofenceController.getInstance().init(this);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    Intent intent = getIntent();
    getMenuInflater().inflate(R.menu.menu_all_geofences, menu);

    MenuItem item1 = menu.findItem(R.id.action_delete_all);
    MenuItem item2 = menu.findItem(R.id.logout);
    MenuItem item4 = menu.findItem(R.id.refresh);

    if (GeofenceController.getInstance().getNamedGeofences().size() == 0) {
      item1.setVisible(false);
    }

    return true;
  }

  @Override
  protected void onResume() {
    super.onResume();

    allMessagesAdapter = new AllMessagesAdapter(this);

    serviceIntent = new Intent(getApplicationContext(),
            CheckDbService.class);

    registerReceiver(receiver, new IntentFilter(
            CheckDbService.BROADCAST_ACTION));

    int googlePlayServicesCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
    Log.i(AllMessagesActivity.class.getSimpleName(), "googlePlayServicesCode = " + googlePlayServicesCode);

    if (googlePlayServicesCode == 1 || googlePlayServicesCode == 2 || googlePlayServicesCode == 3) {
      GooglePlayServicesUtil.getErrorDialog(googlePlayServicesCode, this, 0).show();
    }
  }

  @Override
  protected void onStart() {
    super.onStart();
    startService(new Intent(getBaseContext(), CheckDbService.class));
  }

  @Override
  protected void onPause() {
    super.onPause();
    startService(new Intent(getBaseContext(), CheckDbService.class));
  }

  @Override
  protected void onStop() {
    super.onStop();
  }

  public void stopService() {
    stopService(new Intent(AllMessagesActivity.this, CheckDbService.class));
    stopService(serviceIntent);
    unregisterReceiver(receiver);
  }

  // Read out the message
  public void readText (String message) {
    String toSpeak = message;
    t1.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
  }
}