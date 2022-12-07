package me.trive.trivemessage;

import android.*;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;

public class LoginActivity extends AppCompatActivity {

    // Initialization and attributes
    private static final String TAG = "LoginActivity";
    private static final int REQUEST_SIGNUP = 0;
    final static int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 0;
    final static int MY_PERMISSIONS_REQUEST_ACCESS_NETWORK = 1;
    final static int MY_PERMISSIONS_REQUEST_ACCESS_INTERNET = 2;

    private Boolean login;
    private String userId;
    private String user;
    private String pass;

    private static final String login_url = "http://mk68548.lima-city.de/trivemessage/login.php";
    private static String getId_url;
    public static final String KEY_EMAIL = "username";
    public static final String KEY_PASSWORD = "password";

    // set butterknife style
    @Bind(R.id.input_email) EditText _emailText;
    @Bind(R.id.input_password) EditText _passwordText;
    @Bind(R.id.btn_login) Button _loginButton;
    @Bind(R.id.link_signup) TextView _signupLink;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED)  {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_NETWORK_STATE},
                    MY_PERMISSIONS_REQUEST_ACCESS_NETWORK);
        }

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED)  {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.INTERNET},
                    MY_PERMISSIONS_REQUEST_ACCESS_INTERNET);
        }

        final SharedPreferences loginState;
        loginState = getSharedPreferences(Constants.SharedPrefs.loginState, Context.MODE_PRIVATE);
        login = loginState.getBoolean("loginState", false);

        if (login == true) {
            Toast.makeText(getApplicationContext(), "Login successful", Toast.LENGTH_SHORT).show();
            Intent myIntent = new Intent(LoginActivity.this, AllMessagesActivity.class);
            LoginActivity.this.startActivity(myIntent);
            finish();
        } else {
            _loginButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    login();
                }
            });

            _signupLink.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    // Start the Signup activity
                    Intent intent = new Intent(getApplicationContext(), SignupActivity.class);
                    startActivityForResult(intent, REQUEST_SIGNUP);
                }
            });
        }
    }

    public void login() {
        Log.d(TAG, "Login");

        if (!validate()) {
            onLoginFailed();
            return;
        }

        _loginButton.setEnabled(false);

        final ProgressDialog progressDialog = new ProgressDialog(LoginActivity.this,
                R.style.AppTheme_Dark_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Authenticating...");
        progressDialog.show();


        //Getting values from edit texts
        user = _emailText.getText().toString().trim();
        pass = _passwordText.getText().toString().trim();

        //Creating a string request
        StringRequest stringRequest = new StringRequest(Request.Method.POST, login_url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        //If we are getting success from server
                        if(response.trim().equalsIgnoreCase("success")) {

                            getUserId();

                            //Creating a shared preference
                            SharedPreferences sharedPreferences = LoginActivity.this.getSharedPreferences(Constants.SharedPrefs.SHARED_PREF_NAME, Context.MODE_PRIVATE);

                            //Creating editor to store values to shared preferences
                            SharedPreferences.Editor editor = sharedPreferences.edit();

                            //Adding values to editor
                            editor.putBoolean(Constants.SharedPrefs.LOGGEDIN_SHARED_PREF, true);
                            editor.putString(Constants.SharedPrefs.EMAIL_SHARED_PREF, user);

                            //Saving values to editor
                            editor.commit();

                            SharedPreferences loginState = getSharedPreferences(Constants.SharedPrefs.loginState, Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor2 = loginState.edit();
                            editor2.putBoolean("loginState", true);
                            editor2.commit();

                            //Starting AllMessagesActivity
                            Intent intent = new Intent(LoginActivity.this, AllMessagesActivity.class);
                            startActivity(intent);
                        }
                        else  {
                            //If the server response is not success
                            //Displaying an error message on toast
                            Toast.makeText(LoginActivity.this, "Invalid username or password", Toast.LENGTH_LONG).show();
                            Log.e("Response", response);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                    }
                }){
            @Override
            protected Map<String, String> getParams() {
                Map<String,String> params = new HashMap<>();
                //Adding parameters to request
                params.put(KEY_EMAIL, user);
                params.put(KEY_PASSWORD, pass);

                Log.e("Params", String.valueOf(params));
                //returning parameter
                return params;
            }
        };
        MyApplication.getInstance().addToReqQueue(stringRequest);

        new android.os.Handler().postDelayed(
                new Runnable() {
                    public void run() {
                        // On complete call either onLoginSuccess or onLoginFailed
                        onLoginSuccess();
                        // onLoginFailed();
                        progressDialog.dismiss();
                    }
                }, 3000);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SIGNUP) {
            if (resultCode == RESULT_OK) {
                this.finish();
            }
        }
    }

    // get the user ID and save it persistently in SharedPreferences
    public void getUserId() {

        getId_url = "http://mk68548.lima-city.de/trivemessage/get_id.php?username=" + user;

        JsonObjectRequest jreq = new JsonObjectRequest(Request.Method.GET, getId_url,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            int success = response.getInt("success");

                            if (success == 1) {
                                JSONArray ja = response.getJSONArray("messages");

                                for (int i = 0; i < ja.length();i++) {

                                    JSONObject jobj = ja.getJSONObject(i);
                                    userId = jobj.getString("ID");
                                    Log.e("UserId", userId);

                                    final SharedPreferences user_id;
                                    user_id = getSharedPreferences(Constants.SharedPrefs.USER_ID, Context.MODE_PRIVATE);
                                    SharedPreferences.Editor editor = user_id.edit();
                                    editor.putString(Constants.SharedPrefs.USER_ID, userId);
                                    editor.apply();
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.e("Error: " + error.getMessage());
            }
        });

        // Adding request to request queue
        MyApplication.getInstance().addToReqQueue(jreq);

    }

    @Override
    public void onBackPressed() {
        // disable going back to the MainActivity
        moveTaskToBack(true);
    }

    public void onLoginSuccess() {
        _loginButton.setEnabled(true);
        finish();
    }

    public void onLoginFailed() {
        Toast.makeText(getBaseContext(), "Login failed", Toast.LENGTH_LONG).show();

        _loginButton.setEnabled(true);
    }

    // check for the right input
    public boolean validate() {
        boolean valid = true;

        String email = _emailText.getText().toString();
        String password = _passwordText.getText().toString();

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _emailText.setError("enter a valid email address");
            valid = false;
        } else {
            _emailText.setError(null);
        }

        if (password.isEmpty() || password.length() < 4 || password.length() > 10) {
            _passwordText.setError("between 4 and 10 alphanumeric characters");
            valid = false;
        } else {
            _passwordText.setError(null);
        }

        return valid;
    }

    // Check the permissions
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {

                    finish();
                }
                return;
            }

            case MY_PERMISSIONS_REQUEST_ACCESS_NETWORK: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {

                    finish();
                }
                return;
            }

            case MY_PERMISSIONS_REQUEST_ACCESS_INTERNET: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {

                    finish();
                }
                return;
            }
        }
    }
}