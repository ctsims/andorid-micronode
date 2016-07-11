package org.commcare.hub.mirror;

import android.content.Context;
import android.util.Base64;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;

import org.commcare.hub.application.HubApplication;
import org.commcare.hub.database.DatabaseUnavailableException;
import org.commcare.hub.monitor.ServicesMonitor;
import org.commcare.hub.services.HubRunnable;
import org.commcare.hub.util.StreamsUtil;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

/**
 *
 * Created by ctsims on 12/14/2015.
 */
public class UserSyncThread extends HubRunnable {
    Context context;

    public UserSyncThread(Context context) {
        this.context = context;
    }

    public void runInternal() throws DatabaseUnavailableException {
        SQLiteDatabase database = HubApplication._().getDatabaseHandle();

        Cursor c = database.query("SyncableUser", null, null, null, null, null, null);
        try {
            SyncableUser actionable = null;
            for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                SyncableUser u = SyncableUser.fromDb(c);

                if (u.getStatus() != SyncableUser.UserStatus.ModelFetched && u.getStatus() != SyncableUser.UserStatus.AuthError) {
                    //Only grab the first user to act on, we don't wanna be greedy in this thread
                    actionable = u;
                    break;
                }
            }


            if (actionable == null) {
                return;
            }

            if (actionable.getStatus() == SyncableUser.UserStatus.Requested) {
                fetchKeyForUser(actionable, database);
            } else if(actionable.getStatus() == SyncableUser.UserStatus.KeysFetched) {
                fetchModelForUser(actionable, database);
            }

        } finally {
            c.close();
        }

    }

    //HttpURLConnection

    private void fetchModelForUser(SyncableUser actionable, SQLiteDatabase database) {
        String template = "https://www.commcarehq.org/a/" +actionable.getDomain() + "/phone/restore/?version=2.0";
        String syncLocation = fetchForUser(template, actionable, database);
        if(syncLocation != null) {
            actionable.setSyncFile(syncLocation);
            actionable.updateStatus(SyncableUser.UserStatus.ModelFetched);
            actionable.writeToDb(database);

            ServicesMonitor.reportMessage("Sync record fetch successful for " + actionable.getUsername());
        }

    }

    private void fetchKeyForUser(final SyncableUser actionable, SQLiteDatabase database) {
        String template = "https://www.commcarehq.org/a/" +actionable.getDomain() + "/phone/keys/";
        String keyLocation = fetchForUser(template, actionable, database);
        if(keyLocation != null) {
            actionable.setKeyFile(keyLocation);
            actionable.updateStatus(SyncableUser.UserStatus.KeysFetched);
            actionable.writeToDb(database);

            ServicesMonitor.reportMessage("Key record fetch successful for " + actionable.getUsername());
        }
    }

    private String fetchForUser(String template, final SyncableUser actionable, SQLiteDatabase database) {
        try {
            URL url = new URL(template);

            final String fullUsername =
                    actionable.getUsername() + "@" + actionable.getDomain() + ".commcarehq.org";


            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            setupGetConnection(conn);

            conn.setRequestProperty("Authorization", "basic " +
                    Base64.encodeToString((fullUsername + ":" + actionable.getPassword()).getBytes(), Base64.DEFAULT));

            int response = conn.getResponseCode();

            ServicesMonitor.reportMessage("HTTP Record Request for " + fullUsername +  " response code: " + response);

            if(response >= 500) {
                actionable.updateStatus(SyncableUser.UserStatus.AuthError);
                actionable.writeToDb(database);

                ServicesMonitor.reportMessage("Error fetching user data" + conn.getResponseMessage());
                return null;
            }
            else if(response >= 400) {
                actionable.updateStatus(SyncableUser.UserStatus.AuthError);
                actionable.writeToDb(database);
                return null;
            }

            else if(response == 200) {
                try {
                    String guid = UUID.randomUUID().toString();
                    InputStream responseStream = new BufferedInputStream(conn.getInputStream());
                    StreamsUtil.writeFromInputToOutput(responseStream, context.openFileOutput(guid, Context.MODE_PRIVATE));

                    return guid;

                } catch(IOException e) {
                    e.printStackTrace();
                    ServicesMonitor.reportMessage("Error downloading user record " + e.getMessage());
                }
            }
        }catch( IOException ioe) {

        }
        return null;
    }



    private static void setupGetConnection(HttpURLConnection con) throws IOException {
        con.setConnectTimeout(CONNECTION_TIMEOUT);
        con.setReadTimeout(CONNECTION_SO_TIMEOUT);
        con.setRequestMethod("GET");
        con.setDoInput(true);
        con.setInstanceFollowRedirects(true);
    }

    /**
     * How long to wait when opening network connection in milliseconds
     */
    public static final int CONNECTION_TIMEOUT = 2 * 60 * 1000;

    /**
     * How long to wait when receiving data (in milliseconds)
     */
    public static final int CONNECTION_SO_TIMEOUT = 1 * 60 * 1000;


}
