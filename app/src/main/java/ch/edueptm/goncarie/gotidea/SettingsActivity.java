package ch.edueptm.goncarie.gotidea;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.SignInButton;
import com.google.api.services.drive.Drive;

import org.w3c.dom.Text;

public class SettingsActivity extends AppCompatActivity {
    DriveConnector driveConnector;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        if (DriveConnector.isConnectedToDrive(this) && driveConnector == null)
            driveConnector = DriveConnector.getInstance();

        // Add back button
        try {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        } catch(NullPointerException e) {
            Log.e("ERROR", e.getMessage());
            finish();
        }

        SignInButton login = findViewById(R.id.settings_sign_in_button);
        TextView loggedin = findViewById(R.id.settings_user_connected);

        // Login with Google button
        if (driveConnector == null) {
            login.setVisibility(View.VISIBLE);
            login.setOnClickListener(v -> {
                Intent log = new Intent(this, LoginActivity.class);
            });
        } else {
            loggedin.setVisibility(View.VISIBLE);
            loggedin.setText(driveConnector.getAccount().getId() + " (" + driveConnector.getAccount().getEmail() + ")");
        }
        // Max number of characters (title + description)
        // NOT EDITABLE BY USER
        ((TextView)findViewById(R.id.max_title_length)).setText(String.format("%s: %s %s.", getString(R.string.max_title_length), GITask.MAX_TITLE_LENGTH, getString(R.string.characters)));
        ((TextView)findViewById(R.id.max_description_length)).setText(String.format("%s: %s %s.", getString(R.string.max_description_length), GITask.MAX_DESCRIPTION_LENGTH, getString(R.string.characters)));
    }
    public void onDeleteClick(View v) {
        // show a dialog to confirm the deletion
        AlertDialog ad = new AlertDialog.Builder(this).create();
        ad.setOnShowListener(adInterface -> {
            ad.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.light_gray));
            ad.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.neon_red));
        });
        ad.setTitle(getString(R.string.delete_all_data));
        ad.setMessage(getString(R.string.deleteAllDataMessage));
        ad.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.delete), (dialog, which) -> {
            Toast.makeText(this, getString(R.string.deletedAll), Toast.LENGTH_SHORT).show();
            JSONConstructor.deleteAllData(this);
            finish();
        });
        ad.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.cancel), (DialogInterface.OnClickListener) null);
        ad.show();
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (DriveConnector.isConnectedToDrive(this)) {
            findViewById(R.id.settings_sign_in_button).setEnabled(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) finish();
        return super.onOptionsItemSelected(item);
    }
}