package me.trive.trivemessage;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.software.shell.fab.ActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class AllMessagesFragment extends Fragment implements AddMessageFragment.AddGeofenceFragmentListener {

  // Initialization and attributes
  private ViewHolder viewHolder;

  private ViewHolder getViewHolder() {
    return viewHolder;
  }

  private AllMessagesAdapter allGeofencesAdapter;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
  }


  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_all_messages, container, false);
    viewHolder = new ViewHolder();
    return view;
  }

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    getViewHolder().populate(view);

    viewHolder.geofenceRecyclerView.setHasFixedSize(true);

    LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
    viewHolder.geofenceRecyclerView.setLayoutManager(layoutManager);

    allGeofencesAdapter = new AllMessagesAdapter(GeofenceController.getInstance().getNamedGeofences());
    viewHolder.geofenceRecyclerView.setAdapter(allGeofencesAdapter);
    allGeofencesAdapter.setListener(new AllMessagesAdapter.AllGeofencesAdapterListener() {
      @Override
      public void onDeleteTapped(NamedGeofence namedGeofence) {
        List<NamedGeofence> namedGeofences = new ArrayList<>();
        namedGeofences.add(namedGeofence);
        GeofenceController.getInstance().removeGeofencesOffline(namedGeofences, geofenceControllerListener);
      }
    });

    viewHolder.actionButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        AddMessageFragment dialogFragment = new AddMessageFragment();
        dialogFragment.setListener(AllMessagesFragment.this);
        dialogFragment.show(getActivity().getSupportFragmentManager(), "AddMessageFragment");
      }
    });

    viewHolder.ownMessagesButton.setOnClickListener(new View.OnClickListener()  {
      @Override
      public void onClick(View v) {
        Intent newIntent = new Intent(getActivity(),OwnMessagesActivity.class);
        startActivity(newIntent);
      }
    });

    refresh();
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();
    if (id == R.id.action_delete_all) {
      AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
      builder.setMessage(R.string.AreYouSure)
              .setPositiveButton(R.string.Yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                  GeofenceController.getInstance().removeAllGeofencesOffline(geofenceControllerListener);
                }
              })
              .setNegativeButton(R.string.No, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                  // User cancelled the dialog
                }
              })
              .create()
              .show();
      return true;
    }

    if(id == R.id.refresh)  {

      final ProgressDialog progressDialog = new ProgressDialog(getActivity(),
              R.style.AppTheme_Dark_Dialog);
      progressDialog.setIndeterminate(true);
      progressDialog.setMessage("Loading messages...");
      progressDialog.show();

      GeofenceController.getInstance().loadGeofencesFromDb(geofenceControllerListener);

      new android.os.Handler().postDelayed(
              new Runnable() {
                public void run() {
                  progressDialog.dismiss();
                }
              }, 3000);
    }

    if (id == R.id.logout)  {

      final SharedPreferences loginState;
      loginState = this.getActivity().getSharedPreferences(Constants.SharedPrefs.loginState, Context.MODE_PRIVATE);
      SharedPreferences.Editor editor = loginState.edit();
      editor.putBoolean("loginState", false);
      editor.commit();



      final ProgressDialog progressDialog = new ProgressDialog(getActivity(),
            R.style.AppTheme_Dark_Dialog);
    progressDialog.setIndeterminate(true);
    progressDialog.setMessage("Logging out...");
    progressDialog.show();

    new android.os.Handler().postDelayed(
            new Runnable() {
              public void run() {

                Intent myIntent = new Intent(getActivity(), LoginActivity.class);
                startActivity(myIntent);
                getActivity().finish();
                ((AllMessagesActivity)getActivity()).stopService();
                progressDialog.dismiss();
              }
            }, 3000);

  }

    return super.onOptionsItemSelected(item);
  }

  // Listen to GeofenceController
  private GeofenceController.GeofenceControllerListener geofenceControllerListener = new GeofenceController.GeofenceControllerListener() {
    @Override
    public void onGeofencesUpdated() {
      refresh();
    }

    @Override
    public void refreshView() {
      refreshViewFragment();
    }

    @Override
    public void onError() {
      showErrorToast();
    }
  };

  // Update message view
  private void refresh() {
    allGeofencesAdapter.notifyDataSetChanged();

    if (allGeofencesAdapter.getItemCount() > 0) {
      getViewHolder().emptyState.setVisibility(View.INVISIBLE);
    } else {
      getViewHolder().emptyState.setVisibility(View.VISIBLE);
    }
    getActivity().invalidateOptionsMenu();
  }

  // refresh the view
  public void refreshViewFragment() {
    GeofenceController.getInstance().loadGeofencesFromDb(geofenceControllerListener);
  }

  // send error if an error occurs
  private void showErrorToast() {
    Toast.makeText(getActivity(), getActivity().getString(R.string.Toast_Error), Toast.LENGTH_SHORT).show();
  }

  @Override
  public void onDialogPositiveClick(android.support.v4.app.DialogFragment dialog, NamedGeofence geofence) {
    GeofenceController.getInstance().addGeofence(geofence, geofenceControllerListener);
  }

  @Override
  public void onDialogNegativeClick(android.support.v4.app.DialogFragment dialog) {
    // Do nothing
  }

  @Override
  public void onResume() {
    super.onResume();

  }

  @Override
  public void onPause() {
    super.onPause();

  }

  // create ViewHolder
  static class ViewHolder {
    ViewGroup container;
    ViewGroup emptyState;
    RecyclerView geofenceRecyclerView;
    FloatingActionButton actionButton;
    Button allMessagesButton;
    Button ownMessagesButton;

    public void populate(View v) {
      container = (ViewGroup) v.findViewById(R.id.fragment_all_geofences_container);
      emptyState = (ViewGroup) v.findViewById(R.id.fragment_all_geofences_emptyState);
      geofenceRecyclerView = (RecyclerView) v.findViewById(R.id.fragment_all_geofences_geofenceRecyclerView);
      actionButton = (FloatingActionButton) v.findViewById(R.id.fragment_all_geofences_actionButton);
      allMessagesButton = (Button) v.findViewById(R.id.All_Button);
      ownMessagesButton = (Button) v.findViewById(R.id.Own_Button);
    }
  }
}
