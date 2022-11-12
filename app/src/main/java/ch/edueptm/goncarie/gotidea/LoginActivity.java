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

import java.time.ZonedDateTime;

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

                AlertDialog progressDialog = setProgressDialog();
                progressDialog.show();

                DriveConnector.getInstance().getNewestFileContent()
                        .addOnSuccessListener(content -> {
                            boolean isDriveNewer;
                            if (JSONConstructor.saveFileExists(this))
                                isDriveNewer = JSONConstructor.whichIsNewer(content, JSONConstructor.readFromFile(getString(R.string.saveFileName), this)) == 1;
                            else isDriveNewer = true;

                            if (isDriveNewer) {
                                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                                    String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
                                    requestPermissions(permissions, 100);
                                }
                                Log.i("DriveConnector", "Successfully got file content from Drive.");
                                AlertDialog ad = new AlertDialog.Builder(this).create();
                                ad.setTitle(getString(R.string.app_name));
                                ad.setMessage(getString(R.string.import_data));
                                ad.setOnShowListener(adInterface -> {
                                    ad.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getColor(R.color.light_gray));
                                    ad.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getColor(R.color.light_gray));
                                });
                                ad.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.yes), (dialog, which) -> {
                                    JSONConstructor.writeToFile(DriveConnector.getInstance().getSaveFileContent(), this);
                                    finish();
                                });
                                ad.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.no), (dialog, which) -> {
                                    // delete data from cloud and make new file
                                    JSONConstructor.writeToFile(null, this);
                                    finish();
                                });
                                ad.show();
                                progressDialog.dismiss();
                            } else {
                                JSONConstructor.writeToFile(JSONConstructor.readFromFile(getString(R.string.saveFileName), this), this);
                                progressDialog.dismiss();
                                finish();
                            }
                        }).addOnFailureListener(e -> {
                            e.printStackTrace();
                            JSONConstructor.writeToFile(null, this);
                            Log.e("DriveConnector", "File not available");
                            Log.e("DriveConnector", e.getMessage());
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
                if (resultCode == Activity.RESULT_OK)
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