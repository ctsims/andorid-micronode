package org.commcare.hub;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.commcare.hub.application.HubApplication;
import org.commcare.hub.sync.ServerMetadataSyncService;

public class LoginActivity extends AppCompatActivity {

    public static final String KEY_USERNAME = "username";
    public static final String KEY_PASSWORD = "password";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configure_user);

        loadDefaultValues();

        ((Button)this.findViewById(R.id.login_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setServiceParameters();

                restartService();
            }
        });
    }

    private void restartService() {
        HubApplication._().getServiceHost().restart(ServerMetadataSyncService.class);
    }

    private void setServiceParameters() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        preferences.edit().putString(KEY_USERNAME, ((EditText)findViewById(R.id.edit_username)).getText().toString()).commit();

        preferences.edit().putString(KEY_PASSWORD, ((EditText)findViewById(R.id.edit_password)).getText().toString()).commit();
    }

    private void loadDefaultValues() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        ((EditText)this.findViewById(R.id.edit_username)).setText(preferences.getString(KEY_USERNAME, BuildConfig.DEFAULT_USERNAME));
        ((EditText)this.findViewById(R.id.edit_password)).setText(preferences.getString(KEY_PASSWORD, BuildConfig.DEFAULT_PASSWORD));
    }




}
