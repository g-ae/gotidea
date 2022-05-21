package ch.edueptm.goncarie.gotidea;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.Clock;
import java.time.ZonedDateTime;

public class GITask {
    // IDs used for archiving tasks (they can't be archived directly through the task page activity)
    public static final int NO_REQUEST = 0;
    public static final int ARCHIVE_REQUEST = 1;
    // Max characters for certain values
    public static final int MAX_TITLE_LENGTH = 100;
    public static final int MAX_DESCRIPTION_LENGTH = 1000;
    /**
     * GITask's id
     */
    private String id = "" + ZonedDateTime.now().getYear() + ZonedDateTime.now().getMonthValue() + ZonedDateTime.now().getDayOfMonth() + ZonedDateTime.now().getHour() + ZonedDateTime.now().getMinute() + ZonedDateTime.now().getSecond() + ZonedDateTime.now().getNano() + (int) (Math.random() * 100);
    /**
     * Title of the task
     */
    private String title = "";
    /**
     * Description of the task
     */
    private String description = "";
    /**
     * Tells if the task is done or not
     * If the task is done, it will be archived (shown in the archives page)
     */
    private boolean archived = false;
    /**
     * Creation date time in UTC
     */
    private ZonedDateTime creationDate;
    /**
     * Last change date time in UTC
     * This date will change every time the user changes something of the task.
     */
    private ZonedDateTime lastChangeDate;
    private Context context;

    public GITask(String title, String description, Context context) {
        this.context = context;
        this.setTitle(title);
        this.setDescription(description);
        this.creationDate = ZonedDateTime.now(Clock.systemUTC());
    }
    public GITask(JSONObject json) {
        this.setFromJSON(json);
    }
    public String getId(){
        return this.id;
    }
    public String getTitle() {
        return title;
    }
    public boolean setTitle(String title) {
        if (title.length() <= MAX_TITLE_LENGTH) {
            if (!title.equals("")) this.title = title;
            else this.title = context.getString(R.string.newTask);
            onChanged();
            return true;
        }
        Toast.makeText(context, context.getString(R.string.valueTooLong), Toast.LENGTH_SHORT).show();
        return false;
    }
    public String getDescription() {
        return description;
    }
    public boolean setDescription(String description) {
        if (description.length() <= MAX_DESCRIPTION_LENGTH) {
            this.description = description;
            onChanged();
            return true;
        }
        Toast.makeText(context, context.getString(R.string.valueTooLong), Toast.LENGTH_SHORT).show();
        return false;
    }
    public ZonedDateTime getLastChangeDate() {
        return lastChangeDate;
    }
    private void setLastChangeDate(ZonedDateTime zdt) {
        this.lastChangeDate = zdt;
        onChanged();
    }
    public ZonedDateTime getCreationDate() {
        return creationDate;
    }
    public void setArchived(boolean archived) {
        this.archived = archived;
        onChanged();
    }
    public boolean getArchived() {
        return archived;
    }
    private void onChanged() {
        this.lastChangeDate = ZonedDateTime.now(Clock.systemUTC());
    }
    public JSONObject getJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("id", this.id);
            json.put("title", this.getTitle());
            json.put("description", this.getDescription());
            json.put("archived", this.archived);
            json.put("lastchangedate", this.getLastChangeDate());
            json.put("creationdate", this.creationDate);
        } catch(JSONException e) {
            Log.i("GITask.ERROR", "Could not set info in JSON.");
        }
        return json;
    }
    public void setFromJSON(String str) {
        try {
            setFromJSON(new JSONObject(str));
        } catch(JSONException e) {
            Log.e("ERROR", "Couldn't set JSON from str value");
        }
    }
    public void setFromJSON(JSONObject json) {
        try {
            this.id = json.getString("id");
            this.setTitle(json.getString("title"));
            this.setDescription(json.getString("description"));
            this.setArchived(json.getBoolean("archived"));
            this.setLastChangeDate(ZonedDateTime.parse(json.getString("lastchangedate")));
            this.creationDate = ZonedDateTime.parse(json.getString("creationdate"));

        } catch(JSONException e) {
            Log.e("ERROR", "Couldn't set JSON from JSONObject");
        }
    }
    public static GITask getBaseTask(Context context) {
        return new GITask("", "", context);
    }
    public static GITask getTaskFromIndex(int index, Context context) {
        try {
            JSONArray array = new JSONObject(JSONConstructor.readFromFile(context.getString(R.string.saveFileName), context)).getJSONArray("tasks");
            return array.getJSONObject(index).toString().equals("") ? null : new GITask(array.getJSONObject(index));
        } catch(JSONException e) {
            Log.e("ERROR", "Couldn't get task from index");
            return null;
        }
    }
    public static GITask findFromId(String id, Context context) {
        try {
            JSONArray array = new JSONObject(JSONConstructor.readFromFile(context.getString(R.string.saveFileName), context)).getJSONArray("tasks");
            for(int i = 0; i < array.length(); i++) {
                if (array.getJSONObject(i).getString("id").equals(id)) return new GITask(array.getJSONObject(i));
            }
        } catch(JSONException e) {
            Log.e("ERROR", "Couldn't find task from id");
        }
        return null;
    }
}