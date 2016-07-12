package org.commcare.hub.mirror;

import android.content.ContentValues;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;

import java.util.Date;

/**
 * Created by ctsims on 12/14/2015.
 */
public class SyncableUser {

    public static final String TABLE_NAME = "SyncableUser";

    long rowId = -1;
    public String username;
    public String domain;

    public UserStatus status;

    private String keyFile;
    private String password;
    private String syncFile;

    public UserStatus getStatus() {
        return status;
    }

    public Date lastUpdate;

    public long getId() {
        return rowId;
    }

    public String getUsername() {
        return username;
    }

    public String getDomain() {
        return domain;
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public void updateStatus(UserStatus status) {
        this.status = status;
        lastUpdate = new Date();
    }

    public void setKeyFile(String keyFile) {
        this.keyFile = keyFile;
    }

    public String getPasswordHash() {
        return password;
    }

    public void setPasswordHash(String passwordHash) {
        this.password = passwordHash;
    }

    public String getKeyRecord() {
        return keyFile;
    }

    public void setSyncFile(String syncFile) {
        this.syncFile = syncFile;
    }

    public String getSyncRecord() {
        return syncFile;
    }

    public static enum UserStatus {
        Requested,
        AuthError,
        KeysFetched,
        ModelFetched
    }

    private SyncableUser() {

    }

    public SyncableUser(String username, String domain) {
        this.username = username;
        this.domain = domain;
        this.status = UserStatus.Requested;
        this.lastUpdate = new Date();
    }

    public void setRetry() {
        this.status = UserStatus.Requested;
    }


    public static SyncableUser fromDb(Cursor c) {
        SyncableUser u = new SyncableUser();

        u.rowId = c.getInt(c.getColumnIndexOrThrow("id"));
        u.username = c.getString(c.getColumnIndexOrThrow("username"));
        u.domain = c.getString(c.getColumnIndexOrThrow("domain"));
        u.status = UserStatus.valueOf(c.getString(c.getColumnIndexOrThrow("status")));
        u.lastUpdate = new Date(c.getLong(c.getColumnIndexOrThrow("last_updated")));

        u.password = c.getString(c.getColumnIndexOrThrow("password"));
        u.keyFile =  c.getString(c.getColumnIndexOrThrow("key_record_location"));
        u.syncFile = c.getString(c.getColumnIndexOrThrow("sync_file_location"));

        return u;
    }


    public void writeToDb(SQLiteDatabase database) {

        ContentValues cv = new ContentValues();

        cv.put("username", username);
        cv.put("domain", domain);
        cv.put("status", status.name());
        cv.put("last_updated", lastUpdate.getTime());
        cv.put("password", password);
        cv.put("key_record_location", keyFile);
        cv.put("sync_file_location", syncFile);


        if(rowId == -1) {
            rowId = database.insertOrThrow(TABLE_NAME, null, cv);
        } else {
            database.update(TABLE_NAME, cv, "id=?", new String[]{String.valueOf(rowId)});
        }
    }
    public void removeFromDb(SQLiteDatabase database) {
    }


}
