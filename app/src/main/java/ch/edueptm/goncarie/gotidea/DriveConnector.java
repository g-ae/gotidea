package ch.edueptm.goncarie.gotidea;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.Serializable;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Class used to connect to Google Drive.
 */
public class DriveConnector implements Serializable {
    private static DriveConnector instance;
    private final Executor mExecutor = Executors.newSingleThreadExecutor();
    private final Drive mDriveService;
    private final Context context;

    public static String JSON_KEY_CONNECTED_DRIVE = "connectedDrive";
    public static String EXTRA_TAG = "driveConnector";
    public static final int REQUEST_CODE_SIGN_IN = 1;
    public static final int REQUEST_CODE_OPEN_DOCUMENT = 2;
    public static final int RESULT_SIGNED_IN = 3;
    public static final int RESULT_NOT_SIGNED_IN = 4;

    private GoogleSignInAccount account;

    public static DriveConnector setInstance(Drive driveService, Context context) {
        instance = new DriveConnector(driveService, context);
        return instance;
    }

    public static DriveConnector getInstance() {
        return instance;
    }

    private DriveConnector(Drive driveService, Context context) {
        mDriveService = driveService;
        this.context = context;
    }

    public static boolean isConnectedToDrive(Context context) {
        return (boolean)JSONConstructor.getAttribute(context, JSON_KEY_CONNECTED_DRIVE);
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
            // TODO create a JSONObject from the account
            acc = new JSONObject();
            acc.put("id", account.getId());
        } catch (JSONException e) {
            e.printStackTrace();
            acc = null;
        }
        return acc;
    }
    public void deleteAll() {
        // TODO delete all files from drive
        GoogleSignInClient client = GoogleSignIn.getClient(context, GoogleSignInOptions.DEFAULT_SIGN_IN);
        client.signOut();
        instance = null;
    }
    public void connect(Context context) {

        this.setAccount(GoogleSignIn.getLastSignedInAccount(context));
    }
}

