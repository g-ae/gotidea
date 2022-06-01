package ch.edueptm.goncarie.gotidea;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.api.services.drive.DriveScopes;

import org.json.JSONException;
import org.json.JSONObject;

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
                // Use the authenticated account to sign in to the Drive service.
                DriveConnector.setInstance(this, googleAccount);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                        String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
                        requestPermissions(permissions, 100);
                    }
                }

                AlertDialog progressDialog = setProgressDialog();
                progressDialog.show();

                DriveConnector.getInstance().getSavedFileContent()
                        .addOnSuccessListener(content -> {
                            Log.i("DriveConnector", "Successfully got file content from Drive.");
                            AlertDialog ad = new AlertDialog.Builder(this).create();
                            ad.setTitle(getString(R.string.app_name));
                            ad.setMessage(getString(R.string.import_data));
                            ad.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.yes), (dialog, which) -> {
                                // TODO IMPORT DATA
                                JSONConstructor.writeToFile(content, this);
                                finish();
                            });
                            ad.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.no), (dialog, which) -> {
                                // delete data from cloud and make new file
                                JSONConstructor.writeToFile(null, this);
                                finish();
                            });
                            ad.show();
                            progressDialog.dismiss();
                        }).addOnFailureListener(e -> {
                            JSONConstructor.writeToFile(null, this);
                            Log.e("DriveConnector", "File not available");
                            progressDialog.dismiss();
                            finish();
                        });

            })
            .addOnFailureListener(exception -> Log.e("GotIdea", "Unable to sign in.", exception));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        switch (requestCode) {
            case DriveConnector.REQUEST_CODE_SIGN_IN:
                if (resultCode == Activity.RESULT_OK && resultData != null)
                    handleSignInResult(resultData);
                else
                    Log.e("GotIdea", "Sign in failed.");
                break;
        }
        super.onActivityResult(requestCode, resultCode, resultData);
    }
    public AlertDialog setProgressDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);   // user can't cancel it
        builder.setView(R.layout.layout_loading_dialog);
        return builder.create();
    }
}