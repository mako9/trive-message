package me.trive.trivemessage;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

public class AddMessageFragment extends DialogFragment {

  // Initialization and attributess
  private double latitude;
  private double longitude;
  private String address;

  private ViewHolder viewHolder;
  private Spinner staticSpinner;

  final int REQUEST_CODE = 11;

  private ViewHolder getViewHolder() {
    return viewHolder;
  }

  AddGeofenceFragmentListener listener;
  public void setListener(AddGeofenceFragmentListener listener) {
    this.listener = listener;
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    LayoutInflater inflater = getActivity().getLayoutInflater();
    View view = inflater.inflate(R.layout.dialog_add_messages, null);

    staticSpinner = (Spinner) view.findViewById(R.id.category);

    ArrayAdapter<CharSequence> staticAdapter = ArrayAdapter
            .createFromResource(getActivity(), R.array.category_arrays,
                    R.layout.spinner_item);

    staticAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);

    staticSpinner.setAdapter(staticAdapter);

    viewHolder = new ViewHolder();
    viewHolder.populate(view);

      AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(),R.style.MyDialogTheme);
      builder.setView(view)
              .setPositiveButton(R.string.Add, null)
              .setNeutralButton(R.string.Speak,null);

      final AlertDialog dialog = builder.create();
    dialog.requestWindowFeature(Window.FEATURE_LEFT_ICON);

      dialog.setOnShowListener(new DialogInterface.OnShowListener() {
        @Override
        public void onShow(DialogInterface dialogInterface) {
          Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

          Button neutralButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
          neutralButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_mic_2x,0,0,0);

          WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
          Display display = wm.getDefaultDisplay();
          int DESIRED_WIDTH = (int) display.getWidth() * 40/100;
          int DESIRED_HEIGHT = (int) display.getHeight() * 12/100;
          neutralButton.setTextSize(18);
          positiveButton.setTextSize(18);
          neutralButton.setHeight(DESIRED_HEIGHT);
          neutralButton.setWidth(DESIRED_WIDTH);
          positiveButton.setWidth(DESIRED_WIDTH);
          positiveButton.setHeight(DESIRED_HEIGHT);

          positiveButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

              GPSTracker gpsTracker = new GPSTracker(getActivity());

              latitude = gpsTracker.latitude;
              longitude = gpsTracker.longitude;
              address = gpsTracker.addressLine;

              if (dataIsValid()) {

                NamedGeofence geofence = new NamedGeofence();
                geofence.category = staticSpinner.getSelectedItem().toString();
                geofence.text = getViewHolder().messageEditText.getText().toString();
                geofence.latitude = latitude;
                geofence.longitude = longitude;
                //geofence.radius = 5 * 1000.0f;
                geofence.address = address;

                if (listener != null) {
                  listener.onDialogPositiveClick(AddMessageFragment.this, geofence);
                  dialog.dismiss();
                }
              } else {
                showValidationErrorToast();
              }
            }

          });
          neutralButton.setOnClickListener(new View.OnClickListener()  {
            @Override
                    public void onClick(View view) {
              Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
              intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,
                      Locale.getDefault().toString());
              intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say your Message...");
              startActivityForResult(intent, REQUEST_CODE);
            }
          });
        }
      });

    return dialog;
    }

  // check if input is valid
  private boolean dataIsValid() {
    boolean validData = true;

    String textString = getViewHolder().messageEditText.getText().toString();

    if (TextUtils.isEmpty(textString)){
      validData = false;
    }

    return validData;
  }


@Override
public void onActivityResult(int requestCode, int resultCode, Intent data) {
  super.onActivityResult(requestCode, resultCode, data);

  if(requestCode == REQUEST_CODE && resultCode == -1)  {
    ArrayList<String> speechResults = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

      String finaltext;
    if(getViewHolder().messageEditText.getText().length() >0) {
      finaltext = getViewHolder().messageEditText.getText().toString() + " " + speechResults.get(0);
    } else {
      finaltext = speechResults.get(0);
    }
    getViewHolder().messageEditText.setText(finaltext);
  }
}

  private void showValidationErrorToast() {
    Toast.makeText(getActivity(), getActivity().getString(R.string.Toast_Validation), Toast.LENGTH_SHORT).show();
  }

  public interface AddGeofenceFragmentListener {
    void onDialogPositiveClick(DialogFragment dialog, NamedGeofence geofence);
    void onDialogNegativeClick(DialogFragment dialog);
  }

  // create ViewHolder
  static class ViewHolder {
    Spinner categorySpinner;
    EditText messageEditText;

    public void populate(View v) {
      categorySpinner = (Spinner) v.findViewById(R.id.category);
      messageEditText = (EditText) v.findViewById(R.id.fragment_add_text);
    }
  }
}