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
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;

import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
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
    private GoogleSignInAccount account;
    private String saveFileId;
    private String saveFileContent = "";

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
        instance = new DriveConnector(googleDriveService, context);
        instance.setAccount(account);
    }
    public static DriveConnector getInstance() {
        return instance;
    }
    private DriveConnector(Drive driveService, Context context) {
        mDriveService = driveService;
        this.context = context;
    }
    public static boolean isConnectedToDrive(Context context) {
        try {
            return (boolean) JSONConstructor.getAttribute(JSON_KEY_CONNECTED_DRIVE, context);
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
            if (!Objects.equals(saveFileId, "")) acc.put("saveFileId", saveFileId);
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
                // verifies that there is not a save file already created
                if (mDriveService.files().list().setQ("name = '" + DRIVE_FILE_NAME + "'").execute().getFiles().size() == 0) {
                    java.io.File file = new java.io.File(context.getFilesDir(), context.getString(R.string.saveFileName));
                    File content = new File()
                            .setName(DRIVE_FILE_NAME)
                            .setMimeType(DRIVE_FILE_MIME_TYPE);

                    return mDriveService.files().create(content, new FileContent(DRIVE_FILE_MIME_TYPE, file))
                            .execute();
                } else {
                    throw new IOException("File already exists.");
                }
            } catch (IOException e) {
                throw e;
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
    public Task<String> getNewestFileContent() {
        return Tasks.call(mExecutor, () -> {
            File lastUpdatedFile = null;
            List<File> lf = mDriveService.files().list().setQ("name = '" + DRIVE_FILE_NAME + "'").execute().getFiles();
            for (File f : lf) {
                if (lf.get(0) == f) lastUpdatedFile = f;
                else {
                    DateTime tempsAct = f.getModifiedTime();
                    DateTime tempsNew = lastUpdatedFile.getModifiedTime();

                    if (tempsAct.getValue() - tempsNew.getValue() >= 0) {
                        lastUpdatedFile = f;
                    }
                }
            }
            Log.e("GOTIDEA", lastUpdatedFile.getId());
            getFileContent(lastUpdatedFile.getId())
                    .addOnSuccessListener(content -> {
                        this.saveFileContent = content;
                    })
                    .addOnFailureListener(Throwable::printStackTrace)
                    .addOnCanceledListener(() -> {
                        Log.e("gidbg", "ERROR get file content cancelled");
                    });
            Thread.sleep(4000);
            return saveFileContent;
        });
    }
    public Task<String> getFileContent(String id) {
        return Tasks.call(mExecutor, () -> {
            List<File> fl = mDriveService.files().list().setQ("name = '" + DRIVE_FILE_NAME + "'").execute().getFiles();
            Log.e("gidbg", String.valueOf(fl));
            File f = null;
            for (File file : fl) if (Objects.equals(file.getId(), id)) f = file;
            if (f == null) return null;
            InputStream is = mDriveService.files().get(f.getId()).setAlt("media").executeMediaAsInputStream();

            String ret = "";
            if (is != null) {
                ret = JSONConstructor.readInputStream(is);
            }
            Log.e("gidbg_ret", ret);
            return ret;
        });
    }

    public String getSaveFileContent() {
        return saveFileContent;
    }
}

