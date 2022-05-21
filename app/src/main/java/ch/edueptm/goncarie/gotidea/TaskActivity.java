package ch.edueptm.goncarie.gotidea;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class TaskActivity extends AppCompatActivity {
    public static String INTENT_EXTRA_ISARCHIVED = "isarchived";

    String id;
    EditText taskTitle;
    EditText taskDescription;
    Button btnSave;
    boolean savedChanges = true;
    boolean extra_isarchived;

    String savedTitle = "";
    String savedDescription = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_page);
        // Get intent extra (isarchived), if the task is archived, the task can't be edited.
        if (getIntent().hasExtra(INTENT_EXTRA_ISARCHIVED)) extra_isarchived = getIntent().getBooleanExtra(INTENT_EXTRA_ISARCHIVED, true);

        try {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        } catch(NullPointerException e) {
            Log.e("ERROR", e.getMessage());
        }

        taskTitle = findViewById(R.id.taskTitle);
        taskDescription = findViewById(R.id.taskDescription);
        btnSave = findViewById(R.id.btnSaveChanges);

        // if the task is archived, the task can't be edited.
        taskTitle.setEnabled(!extra_isarchived);
        taskDescription.setEnabled(!extra_isarchived);
        // if the task is archived, the save button will be disabled.
        if (extra_isarchived) btnSave.setVisibility(View.GONE);
        else {
            taskTitle.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    // nothing
                }
                @Override
                public void onTextChanged(final CharSequence s, int start, int before, int count) {
                    // each time something is typed, check if the title has changed from the last saved one
                    // if it has changed, set savedChanges to false
                    checkSaved();
                }
                @Override
                public void afterTextChanged(Editable s) {
                    // nothing
                }
            });
            taskDescription.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    // nothing
                }
                @Override
                public void onTextChanged(final CharSequence s, int start, int before, int count) {
                    // each time something is typed, check if the title has changed from the last saved one
                    // if it has changed, set savedChanges to false
                    checkSaved();
                }
                @Override
                public void afterTextChanged(Editable s) {
                    // nothing
                }
            });
        }

        id = getIntent().getStringExtra("taskId");
        if (id.equals("")) {
            Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
            finish();
        }

        // get GITask
        GITask t = GITask.findFromId(id, this);
        if (t == null) {
            Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
            finish();
        }
        try {
            savedTitle = t.getTitle();
            savedDescription = t.getDescription();

            taskTitle.setText(savedTitle);
            taskDescription.setText(savedDescription);
        }
        catch (Exception e) {
            Log.e("ERROR", e.getMessage());
        }
    }
    @Override
    public void onBackPressed() {
        // checks if there are unsaved changes then leaves.
        checkSaved();
        if (!savedChanges) {
            AlertDialog ad = new AlertDialog.Builder(this).create();
            ad.setTitle(getString(R.string.taskNoSaveTitle));
            ad.setMessage(getString(R.string.taskNoSaveMessage));
            ad.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.save), (dialog, which) -> {
                // Save
                Toast.makeText(this, getString(R.string.taskSavedChanges), Toast.LENGTH_SHORT).show();
                saveChanges();
                finish();
            });
            ad.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.discard), (dialog, which) -> {
                // Discard
                Toast.makeText(this, getString(R.string.taskDiscardedChanges), Toast.LENGTH_SHORT).show();
                finish();
            });
            ad.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.cancel), (dialog, which) -> {});
            ad.show();
        } else finish();
    }
    /**
     * On click of the action buttons (back, save, delete)
     * @param item the item clicked
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) onBackPressed();
        else if (item.getItemId() == R.id.action_end) {
            // On click of the "tick" (archive) button on top of the app.
            // Checks if any changes have been made to the task.
            // If there are any, will ask the user if they want to save the changes before archiving the task.
            checkSaved();
            Intent data = new Intent();
            data.putExtra("taskId", id);
            setResult(GITask.ARCHIVE_REQUEST, data);
            if (!savedChanges) {
                AlertDialog ad = new AlertDialog.Builder(this).create();
                ad.setTitle(getString(R.string.taskNoSaveTitle));
                ad.setMessage(getString(R.string.taskNoSaveMessageArchived));
                ad.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.save), (dialog, which) -> {
                    // Save
                    Toast.makeText(this, getString(R.string.taskSavedChangesAndArchived), Toast.LENGTH_SHORT).show();
                    saveChanges();
                    if (savedChanges) finish();
                });
                ad.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.discard), (dialog, which) -> {
                    // Discard
                    Toast.makeText(this, getString(R.string.taskDiscardedChangesAndArchived), Toast.LENGTH_SHORT).show();
                    finish();
                });
                ad.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.cancel), (dialog, which) -> {});
                ad.show();
            } else finish();
        } else if (item.getItemId() == R.id.action_delete) {
            AlertDialog ad = new AlertDialog.Builder(this).create();
            ad.setTitle(getString(R.string.taskDeleteTitle));
            ad.setMessage(getString(R.string.taskDeleteMessage));
            ad.setOnShowListener(adInterface -> {
                ad.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.light_gray));
                ad.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.neon_red));
            });
            ad.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.delete), (dialog, which) -> {
                JSONConstructor.deleteTask(id, this);
                Toast.makeText(this, getString(R.string.taskDeleted), Toast.LENGTH_SHORT).show();
                finish();
            });
            ad.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.cancel), (DialogInterface.OnClickListener) null);
            ad.show();
        }
        return super.onOptionsItemSelected(item);
    }
    private void checkSaved() {
        // check if any changes are made to the task that aren't saved.
        if (savedTitle.equals(taskTitle.getText().toString())) savedChanges = savedDescription.equals(taskDescription.getText().toString());
        else savedChanges = false;

        // if there are any unsaved changes, the button will be enabled.
        // else it will be disabled.
        btnSave.setEnabled(!savedChanges);
    }

    /**
     * This function will save the changes made to the task.
     */
    private void saveChanges() {
        // Variables
        String title = taskTitle.getText().toString();
        String description = taskDescription.getText().toString();
        GITask t = GITask.findFromId(id, this);

        if (title.length() <= GITask.MAX_TITLE_LENGTH) {
            savedTitle = title;
            t.setTitle(title);
        }
        else Toast.makeText(this, R.string.taskTitleTooLong, Toast.LENGTH_SHORT).show();

        if (description.length() <= GITask.MAX_DESCRIPTION_LENGTH) {
            savedDescription = description;
            t.setDescription(description);
        }
        else Toast.makeText(this, R.string.taskDescriptionTooLong, Toast.LENGTH_SHORT).show();

        // Save in activity variables (to check if changes have been saved)
        savedChanges = true;

        // Save to JSON file
        JSONConstructor.saveTask(t, this);
    }
    public void onSaveChangesButtonClick(View v) {
        // Verifies if any value has been changed
        // if nothing was changed, no need to save.
        if (!savedChanges) saveChanges();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        // only inflates if the task is not archived
        if (!extra_isarchived) getMenuInflater().inflate(R.menu.task_address_list, menu);
        return true;
    }
}