package net.gini.android.visiontest;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import net.gini.android.authorization.CredentialsStore;
import net.gini.android.authorization.UserCredentials;


public class GiniSettingsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gini_settings);

        final CredentialsStore credentialsStore = GiniService.INSTANCE.startService(this).getCredentialsStore();
        final UserCredentials userCredentials = credentialsStore.getUserCredentials();
        if (userCredentials != null) {
            ((TextView) findViewById(R.id.username)).setText(userCredentials.getUsername());
            ((TextView) findViewById(R.id.password)).setText(userCredentials.getPassword());
        }
    }

}
