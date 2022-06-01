package ch.edueptm.goncarie.gotidea;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.api.services.drive.Drive;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    DriveConnector driveConnector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ((FloatingActionButton)findViewById(R.id.fab)).setImageDrawable(getDrawable(R.drawable.ic_action_content_new));
    }
    /**
     * On add task button click
     * @param v view
     */
    public void onAddButton(View v) {
        if (JSONConstructor.saveFileExists(this)) JSONConstructor.saveTask(GITask.getBaseTask(this), this);
        else JSONConstructor.writeToFile(null, this);
        updateLayout();
    }

    /**
     * Update list of tasks
     */
    public void updateLayout() {
        try {
            LinearLayout layout = (LinearLayout)findViewById(R.id.main_task_list);
            layout.removeAllViews();

            JSONArray tasks = new JSONObject(JSONConstructor.readFromFile(getString(R.string.saveFileName), this)).getJSONArray("tasks");
            int non_archived_tasks = 0;
            for (int i = 0; i < tasks.length(); i++) {
                GITask t = new GITask(tasks.getJSONObject(i));
                if (!t.isArchived()) {
                    non_archived_tasks++;
                    View child = getLayoutInflater().inflate(R.layout.task_row, this.findViewById(R.id.main_task_list), false);
                    CheckBox cb = child.findViewById(R.id.cbComplete);
                    TextView tv = child.findViewById(R.id.task_title);
                    tv.setText(t.getTitle());
                    cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        if (isChecked) archiveTask(t.getId());
                    });

                    child.setOnClickListener(v -> {
                        Intent intent = new Intent(this, TaskActivity.class);
                        intent.putExtra("taskId", t.getId());
                        startActivityForResult(intent, GITask.NO_REQUEST);
                    });
                    layout.addView(child);
                }
            }
            if (non_archived_tasks == 0) {
                View child = getLayoutInflater().inflate(R.layout.nothing_available_layout, this.findViewById(R.id.main_task_list), false);
                TextView tv = child.findViewById(R.id.tvBase);
                tv.setText(R.string.no_tasks);
                layout.addView(child);
            }
        }
        catch (Exception e) {
            Log.e("ERROR", "JSON file couldn't be read.");
            Log.e("ERROR", e.getMessage());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!JSONConstructor.saveFileExists(this)) {
            // save file doesn't exist so go to LoginActivity
            startActivity(new Intent(this, LoginActivity.class));
        } else if (driveConnector == null && DriveConnector.isConnectedToDrive(this)) {
            DriveConnector.setInstance(this, GoogleSignIn.getLastSignedInAccount(this));
        }

        // add tasks to scrollview
        updateLayout();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_address_list, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch(item.getItemId()) {
            case R.id.archive_menu_item:
                Intent archive = new Intent(this, ArchiveActivity.class);
                startActivity(archive);
                break;
            case R.id.settings_menu_item:
                Intent settings = new Intent(this, SettingsActivity.class);
                startActivity(settings);
                break;
            default:
                Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Creates a Snackbar in the main activity to allow users to undo their last action
     * @param taskId the id of the task that was just changed
     */
    public void createSnackbarArchive(String taskId) {
        Snackbar.make(findViewById(R.id.main_task_list), getString(R.string.archived), Snackbar.LENGTH_LONG)
            .setAction(getString(R.string.undo), v -> {
                // find task and set it to not archived
                GITask t = GITask.findFromId(taskId, this);
                t.setIsArchived(false);

                // saves the undo and updates the list
                JSONConstructor.saveTask(t, this);
                updateLayout();

                // creates a toast saying that the task has been unarchived if the user undoes their action
                Toast.makeText(this, getString(R.string.task_unarchive), Toast.LENGTH_SHORT).show();
            })
        .show();
        updateLayout();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case GITask.NO_REQUEST:
                if (resultCode == GITask.ARCHIVE_REQUEST)
                    if (data.hasExtra("taskId")) archiveTask(data.getStringExtra("taskId"));
                break;
        }
    }
    private void archiveTask(String id) {
        GITask t = GITask.findFromId(id, this);
        t.setIsArchived(true);
        JSONConstructor.saveTask(t, this);
        updateLayout();
        createSnackbarArchive(t.getId());
    }
}