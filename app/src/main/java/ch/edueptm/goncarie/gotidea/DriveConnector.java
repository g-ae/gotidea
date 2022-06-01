package ch.edueptm.goncarie.gotidea;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Class used to connect to Google Drive.
 */
public class DriveConnector implements Serializable {
    // Static String values
    public static String JSON_KEY_CONNECTED_DRIVE = "connectedDrive";
    public static String DRIVE_FILE_NAME = "GotIdea.json";
    public static String DRIVE_FILE_MIME_TYPE = "application/json";

    // Request Codes
    public static final int REQUEST_CODE_SIGN_IN = 1;

    // Private variables
    private static DriveConnector instance;
    private final Executor mExecutor = Executors.newSingleThreadExecutor();
    private final Drive mDriveService;
    private final Context context;
    private final GoogleAccountCredential credential;
    private GoogleSignInAccount account;
    private String saveFileId;
    private String saveFileContent;

    public static void setInstance(Context context, GoogleSignInAccount account) {
        GoogleAccountCredential credential =
                GoogleAccountCredential.usingOAuth2(
                        context, Collections.singleton(DriveScopes.DRIVE_FILE));

        credential.setSelectedAccount(account.getAccount());

        Drive googleDriveService = new Drive.Builder(
                AndroidHttp.newCompatibleTransport(),
                new GsonFactory(),
                credential
        ).setApplicationName(context.getString(R.string.app_name))
                .build();
        setInstance(googleDriveService, credential, context);
        DriveConnector.getInstance().setAccount(account);
    }
    public static DriveConnector setInstance(Drive driveService, GoogleAccountCredential credential, Context context) {
        instance = new DriveConnector(driveService, credential, context);
        return instance;
    }
    public static DriveConnector getInstance() {
        return instance;
    }
    private DriveConnector(Drive driveService, GoogleAccountCredential credential, Context context) {
        mDriveService = driveService;
        this.credential = credential;
        this.context = context;
    }
    public GoogleAccountCredential getCredential() { return credential; }
    public static boolean isConnectedToDrive(Context context) {
        try {
            return (boolean) JSONConstructor.getAttribute(context, JSON_KEY_CONNECTED_DRIVE);
        } catch (NullPointerException e) {
            return false;
        }
    }
    public void setAccount(GoogleSignInAccount account) {
        this.account = account;
    }
    public GoogleSignInAccount getAccount() {
        return account;
    }
    public JSONObject getAccountAsJSON() {
        if (account == null) return null;
        JSONObject acc;
        try {
            acc = new JSONObject();
            acc.put("id", account.getId());
            if (saveFileId != "") acc.put("saveFileId", saveFileId);
        } catch (JSONException e) {
            e.printStackTrace();
            acc = null;
        }
        return acc;
    }
    public void deleteAll() {
        // Deletes file from Google Drive and returns true if deleted false if not.
        Tasks.call(mExecutor, () -> {
            try {
                List<File> fl = mDriveService.files().list()
                        .setQ("name = '" + DRIVE_FILE_NAME + "'")
                        .execute().getFiles();

                if (fl.size() == 0) return false;
                if (fl.size() == 1) {
                    mDriveService.files().delete(fl.get(0).getId()).execute();
                } else {
                    for (File f : fl) {
                        mDriveService.files().delete(f.getId()).execute();
                    }
                }
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }).addOnSuccessListener(v -> {
            if (v.equals(true)) Log.d("DriveConnector", "Deleted all files from Google Drive.");
            else Log.d("DriveConnector", "Failed to delete all files from Google Drive.");
        }).addOnFailureListener(e -> Log.d("DriveConnector", "Failed to delete all files from Google Drive."));

        // sign out from current google account
        GoogleSignIn.getClient(context, GoogleSignInOptions.DEFAULT_SIGN_IN).signOut();
        instance = null;
    }
    public void saveFile() {
        if (saveFileId == null) {
            Tasks.call(mExecutor, () -> {
                String id = mDriveService.files().list()
                        .setQ("name = '" + DRIVE_FILE_NAME + "'")
                        .execute().getFiles().get(0).getId();
                if (id != null) {
                    saveFileId = id;
                }
                return null;
            })
            .addOnSuccessListener(v -> updateFile())
            .addOnFailureListener(e -> createFile());
        } else {
            updateFile();
        }
    }
    private void createFile() {
        Tasks.call(mExecutor, () -> {
            try {
                java.io.File file = new java.io.File(context.getFilesDir(), context.getString(R.string.saveFileName));
                File content = new File()
                        .setName(DRIVE_FILE_NAME)
                        .setMimeType(DRIVE_FILE_MIME_TYPE);

                return mDriveService.files().create(content, new FileContent(DRIVE_FILE_MIME_TYPE, file))
                        .execute();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }).addOnSuccessListener(file -> {
            Log.d("DriveConnector", "File created successfully");
            saveFileId = file.getId();
        }).addOnFailureListener(e -> {
            Log.e("DriveConnector", "File creation failed");
            e.printStackTrace();
        });
    }
    private void updateFile() {
        Tasks.call(mExecutor, () -> {
            try {
                java.io.File file = new java.io.File(context.getFilesDir(), context.getString(R.string.saveFileName));
                List<File> list = mDriveService.files().list().setQ("name = '" + DRIVE_FILE_NAME + "'").execute().getFiles();
                if (list.size() == 0) {
                    createFile();
                    return null;
                } else {
                    saveFileId = list.get(0).getId();
                    return mDriveService.files().update(saveFileId, null, new FileContent(DRIVE_FILE_MIME_TYPE, file))
                            .execute();
                }
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }).addOnSuccessListener(file -> {
            Log.d("DriveConnector", "File updated successfully");
            if (file != null) saveFileId = file.getId();
        }).addOnFailureListener(e -> {
            Log.e("DriveConnector", "File update failed");
            e.printStackTrace();
        });
    }
    public Task<String> getSavedFileContent() {
        Log.i("DriveConnector", "Downloading file from Google Drive");
        return Tasks.call(mExecutor, () -> {
            saveFileId = mDriveService.files().list().setQ("name = '" + DRIVE_FILE_NAME + "'").execute().getFiles().get(0).getId();
            InputStream is = mDriveService.files().get(saveFileId).setAlt("media").executeMediaAsInputStream();

            String ret = "";
            if (is != null) {
                ret = JSONConstructor.readInputStream(is);
            }
            saveFileContent = ret;
            return saveFileContent;
        });
    }
}

