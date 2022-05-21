package ch.edueptm.goncarie.gotidea;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ArchiveActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_archive);

        try {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        } catch(NullPointerException e) {
            Log.e("ERROR", e.getMessage());
        }
    }
    public void updateLayout() {
        try {
            LinearLayout layout = (LinearLayout) findViewById(R.id.archive_task_list);
            layout.removeAllViews();

            JSONObject data = new JSONObject(JSONConstructor.readFromFile(getString(R.string.saveFileName), this));
            JSONArray tasks = data.getJSONArray("tasks");

            int archivedTasks = 0;
            for (int i = 0; i < tasks.length(); i++) {
                GITask t = new GITask(tasks.getJSONObject(i));
                if (t.getArchived()) {
                    archivedTasks++;
                    View child = getLayoutInflater().inflate(R.layout.task_row, layout, false);
                    CheckBox cb = child.findViewById(R.id.cbComplete);
                    TextView tv = child.findViewById(R.id.task_title);
                    tv.setText(t.getTitle());
                    cb.setChecked(true);
                    cb.setButtonTintList(getColorStateList(R.color.neon_red)); // need to create a listcolorstate to put in here

                    cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        if (!isChecked) {
                            restoreTask(t.getId());
                        }
                    });

                    child.setOnClickListener(v -> {
                        Intent intent = new Intent(this, TaskActivity.class);
                        intent.putExtra("taskId", t.getId());
                        intent.putExtra(TaskActivity.INTENT_EXTRA_ISARCHIVED, true);
                        startActivityForResult(intent, GITask.NO_REQUEST);
                    });
                    layout.addView(child);
                }
            }
            if (archivedTasks == 0) {
                View child = getLayoutInflater().inflate(R.layout.nothing_available_layout, this.findViewById(R.id.main_task_list), false);
                TextView tv = child.findViewById(R.id.tvBase);
                tv.setText(getString(R.string.no_archived_tasks));
                layout.addView(child);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateLayout();
    }
    private void restoreTask(String id) {
        GITask t = GITask.findFromId(id, this);
        t.setArchived(false);
        JSONConstructor.saveTask(t, this);
        updateLayout();
        Snackbar.make(findViewById(R.id.archive_task_list), getString(R.string.task_restored) , Snackbar.LENGTH_SHORT).show();
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home){
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
}