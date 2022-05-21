package ch.edueptm.goncarie.gotidea;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

import java.util.Collections;

public class LoginActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        findViewById(R.id.login_sign_in_button).setOnClickListener(this::onSignInWithGoogleClicked);
        findViewById(R.id.cContinueWithoutGoogle).setOnClickListener(this::onContinueWithoutGoogleClicked);
    }

    /**
     * The user has to stay in the LoginActivity page until the LoginActivity is successful.
     * Otherwise they will not be able to access the app.
     */
    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    public void onContinueWithoutGoogleClicked(View v) {
        JSONConstructor.writeToFile(null, this);
        Toast.makeText(this, getString(R.string.loginWithoutGoogle), Toast.LENGTH_SHORT).show();
        finish();
    }

    public void onSignInWithGoogleClicked(View v) {
        signInWithGoogle();
    }
    public void signInWithGoogle() {
        Log.i("LoginActivity", "signInWithGoogle");
        GoogleSignInOptions signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
                        .build();
        GoogleSignInClient client = GoogleSignIn.getClient(this, signInOptions);

        // The result of the sign-in Intent is handled in onActivityResult.
        startActivityForResult(client.getSignInIntent(), DriveConnector.REQUEST_CODE_SIGN_IN);
    }
    /**
     * Handles the result of the sign-in Intent, after the user has signed in.
     */
    private void handleSignInResult(Intent result) {
        GoogleSignIn.getSignedInAccountFromIntent(result)
            .addOnSuccessListener(googleAccount -> {
                Log.d("DriveConnector", "Signed in as " + googleAccount.getEmail());

                // Use the authenticated account to sign in to the Drive service.
                GoogleAccountCredential credential =
                    GoogleAccountCredential.usingOAuth2(
                        this, Collections.singleton(DriveScopes.DRIVE_FILE));
                credential.setSelectedAccount(googleAccount.getAccount());
                Drive googleDriveService = new Drive.Builder(
                    AndroidHttp.newCompatibleTransport(),
                    new GsonFactory(),
                    credential
                ).setApplicationName(getString(R.string.app_name))
                    .build();

                // The DriveConnector class encapsulates all REST API and SAF functionality.
                DriveConnector driveConnector = DriveConnector.setInstance(googleDriveService, this);
                driveConnector.setAccount(googleAccount);
                JSONConstructor.writeToFile(null, this);
                finish();
            })
            .addOnFailureListener(exception -> Log.e("GotIdea", "Unable to sign in.", exception));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        switch (requestCode) {
            case DriveConnector.REQUEST_CODE_SIGN_IN:
                if (resultCode == Activity.RESULT_OK && resultData != null)
                    handleSignInResult(resultData);
                break;
        }
        super.onActivityResult(requestCode, resultCode, resultData);
    }
}