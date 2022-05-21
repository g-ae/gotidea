package ch.edueptm.goncarie.gotidea;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

/**
 * JSONConstructor is a static class used to handle the interaction between the application and the JSON save file.
 */
public class JSONConstructor {
    /**
     * This method checaks if the save file exists. (name of file: R.string.saveFileName)
     * @param context activity context
     * @return true if the file exists, false if not.
     */
    public static boolean saveFileExists(Context context) {
        InputStream inputStream;
        try {
            // tries to open the save file with an InputStream
            inputStream = context.openFileInput(context.getString(R.string.saveFileName));
        } catch (FileNotFoundException e) {
            // if the file doesn't exist, return false
            return false;
        }
        try {
            // if the file exists, close the InputStream
            // if the file doesn't exist, the program will be out of this method already.
            inputStream.close();
        }
        catch(IOException e) {
            Log.e("ERROR", e.getMessage());
        }
        return true;
    }
    /**
     * This method writes the given data to the save file.
     * @param data data to write (JSONObject as a string)
     * @param context activity context
     */
    public static void writeToFile(String data, Context context) {
        // if no data was given, create some dummy data
        if (data == null) data = JSONConstructor.createData(null, context).toString();
        try {
            // write data to save file
            OutputStreamWriter osw = new OutputStreamWriter(context.openFileOutput(context.getString(R.string.saveFileName), Context.MODE_PRIVATE));
            osw.write(data);
            osw.close();
        }
        catch (IOException e) {
            Log.e("ERROR", "File write failed: " + e.getMessage());
        }
    }
    /**
     * This method reads the data from the given file.
     * @param name name of the file to read
     * @param context activity context
     * @return data as String
     */
    public static String readFromFile(String name, Context context) {
        // return value
        String ret = "";
        try {
            // create InputStream to read file
            InputStream inputStream = context.openFileInput(name);

            // if file exists, read it
            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString;
                StringBuilder stringBuilder = new StringBuilder();

                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append("\n").append(receiveString);
                }
                bufferedReader.close();
                inputStreamReader.close();
                inputStream.close();
                ret = stringBuilder.toString();
            }
        }
        catch (FileNotFoundException e) {
            Log.e("JSONConnector", "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.e("JSONConnector", "Can not read file: " + e.getMessage());
        }
        return ret;
    }
    /**
     * Builds a JSONObject ready to be inserted in the savefile.
     * example of the JSONObject:
     * {
     *     "version": "v",
     *     "created": "date",
     *     "connectedToDrive": "true/false",
     *     "tasks": [
     *         // array of tasks
     *         {
     *             "id": "id",
     *             "title": "title",
     *             "description": "description",
     *             "archived": "false",
     *             "lastchangedate": "date",
     *             "creationdate": "date"
     *         },
     *         ...
     *     ]
     * }
     * @param tasks JSONArray containing all the tasks (each JSONArray contains a JSONObject of GITask.getJSON()).
     * @param context context
     * @return JSONObject ready to be inserted in the savefile.
     */
    public static JSONObject createData(@Nullable JSONArray tasks, Context context) {
        JSONObject json = new JSONObject();
        JSONArray jsontasks;
        if (tasks == null) {
            jsontasks = new JSONArray();
        } else {
            jsontasks = tasks;
        }
        try {
            DriveConnector drive = DriveConnector.getInstance();
            boolean driveConnected = drive != null;

            json.put("version", context.getString(R.string.version));
            // if an account is available in the DriveConnector, set connectedToDrive to true and set account object
            // else it's false
            json.put(DriveConnector.JSON_KEY_CONNECTED_DRIVE, driveConnected);
            json.put("account", driveConnected ? drive.getAccountAsJSON() : null);
            json.put("tasks", jsontasks);
        } catch(JSONException e) {
            Log.e("ERROR", "JSON file creation failed: " + e.getMessage());
        }
        return json;
    }
    /**
     * Change task from index to the new specified one.
     * @param index index of the task to change
     * @param task new task
     * @param context activity context
     */
    public static void changeTask(int index, GITask task, Context context) {
        try {
        JSONArray tasks = new JSONObject(readFromFile(context.getString(R.string.saveFileName), context)).getJSONArray("tasks");
        JSONArray tasksnew = new JSONArray();
        for (int i = 0; i < tasks.length(); i++) {
            if (i == index) tasksnew.put(task.getJSON());
            else tasksnew.put(tasks.getJSONObject(i));
        }
        JSONConstructor.writeToFile(JSONConstructor.createData(tasksnew, context).toString(), context);
        } catch (JSONException e) {
            Toast.makeText(context, context.getString(R.string.task_error_cantsave), Toast.LENGTH_SHORT).show();
            Log.e("JSONConnector", "Failed to save task." + e.getMessage());
        }
    }

    /**
     * Add a task to the JSON file.
     * @param task
     * @param context
     */
    public static void saveTask(GITask task, Context context) {
        int index = -1;
        try {
            JSONArray array = new JSONObject(readFromFile(context.getString(R.string.saveFileName), context)).getJSONArray("tasks");
            for (int i = 0; i < array.length(); i++) {
                if (array.getJSONObject(i).getString("id").equals(task.getId())) index = i;
            }
            if (index == -1) {
                try {
                    JSONArray tasks = new JSONObject(JSONConstructor.readFromFile(context.getString(R.string.saveFileName), context)).getJSONArray("tasks");
                    tasks.put(GITask.getBaseTask(context).getJSON());
                    JSONConstructor.writeToFile(JSONConstructor.createData(tasks, context).toString(), context);
                } catch(JSONException e) {
                    Log.e("JSONConnector", "JSON file couldn't be read : " + e.getMessage());
                    JSONConstructor.writeToFile(null, context);
                }
            }
            else {
                JSONConstructor.changeTask(index, task, context);
            }
        } catch (JSONException e) {
            Log.e("JSONConnector", "Couldn't save task");
        }
    }

    /**
     * Deletes the task that has the specified id from the save file.
     * @param id id of the task to delete
     * @param context activity context
     */
    public static void deleteTask(String id, Context context) {
        try {
            JSONObject json = new JSONObject(JSONConstructor.readFromFile(context.getString(R.string.saveFileName), context));
            JSONArray array = json.getJSONArray("tasks");
            int indexToErase = -1;
            for(int i = 0; i < array.length(); i++) {
                if (array.getJSONObject(i).getString("id").equals(id)) {
                    indexToErase = i;
                }
            }
            if (indexToErase != -1)array.remove(indexToErase);
            else Toast.makeText(context, context.getString(R.string.task_not_found), Toast.LENGTH_SHORT).show();
            json.put("tasks", array);
            JSONConstructor.writeToFile(json.toString(), context);
        } catch(JSONException e) {
            Log.e("JSONConnector", "Couldn't delete task");
        }
    }

    /**
     * Reads the save file and checks if it is usable.
     * @param context activity context
     * @return true if the file is usable, false otherwise
     */
    public static boolean checkIsSaveFileUsable(Context context) {
        try {
            JSONObject json = new JSONObject(JSONConstructor.readFromFile(context.getString(R.string.saveFileName), context));
            json.getString("version");
            json.getString("date");
            json.getJSONArray("tasks");
            return true;
        } catch (JSONException e) {
            Log.e("JSONConnector", "Coudln't check use of file" + e.getMessage());
            return false;
        }
    }

    /**
     * Deletes all the data saved in the device.
     * @param context activity context
     */
    public static void deleteAllData(Context context) {
        DriveConnector driveConnector = DriveConnector.getInstance();
        try {
            File file = new File(context.getFilesDir(), context.getString(R.string.saveFileName));
            file.delete();
        } catch (Exception e) {
            Log.e("ERROR", e.getMessage());
        }
        // TODO: delete file from Drive
        if (driveConnector != null) driveConnector.deleteAll();
    }

    public static Object getAttribute(Context context, String attribute) {
        try {
            JSONObject json = new JSONObject(JSONConstructor.readFromFile(context.getString(R.string.saveFileName), context));
            return json.get(attribute);
        } catch (JSONException e) {
            Log.e("JSONConnector", "Couldn't get attribute");
            return null;
        }
    }
}
