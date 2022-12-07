package me.trive.trivemessage;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

public class OwnMessagesAdapter extends RecyclerView.Adapter<OwnMessagesAdapter.ViewHolder> {

    // Initialization and attributes
    private List<NamedGeofence> ownGeofences;

    private AllGeofencesAdapterListener listener;

    public void setListener(AllGeofencesAdapterListener listener) {
        this.listener = listener;
    }

    public OwnMessagesAdapter(List<NamedGeofence> ownGeofences) {
        this.ownGeofences = ownGeofences;
    }

    @Override
    public OwnMessagesAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ViewGroup v = (ViewGroup) LayoutInflater.from(parent.getContext()).inflate(R.layout.listitem_message, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final NamedGeofence geofence = ownGeofences.get(position);

        holder.category.setText(geofence.category);
        holder.text.setText(geofence.text);
        holder.address.setText(geofence.address);
        holder.timestamp.setText(geofence.timestamp);

        holder.deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                builder.setMessage(R.string.AreYouSure)
                        .setPositiveButton(R.string.Yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                if (listener != null) {
                                    listener.onDeleteTapped(geofence);
                                }
                            }
                        })
                        .setNegativeButton(R.string.No, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // User cancelled the dialog
                            }
                        })
                        .create()
                        .show();
            }
        });


        holder.mapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent= new Intent(v.getContext(), MapsActivity.class);
                intent.putExtra("Latitude",geofence.latitude);
                intent.putExtra("Longitude",geofence.longitude);
                intent.putExtra("Category",geofence.category);

                v.getContext().startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return ownGeofences.size();
    }


    public interface AllGeofencesAdapterListener {
        void onDeleteTapped(NamedGeofence namedGeofence);
    }


    // create ViewHolder
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView category;
        TextView text;
        TextView address;
        TextView timestamp;
        Button deleteButton;
        Button mapButton;

        public ViewHolder(ViewGroup v) {
            super(v);
            category = (TextView) v.findViewById(R.id.listitem_category);
            text = (TextView) v.findViewById(R.id.listitem_geofenceText);
            address = (TextView) v.findViewById(R.id.listitem_geofenceAddress);
            timestamp = (TextView) v.findViewById(R.id.listitem_timestamp);
            deleteButton = (Button) v.findViewById(R.id.listitem_deleteButton);
            mapButton = (Button) v.findViewById(R.id.listitem_MapButton);
        }
    }
}
