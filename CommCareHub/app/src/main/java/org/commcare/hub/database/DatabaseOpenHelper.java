/**
 *
 */
package org.commcare.hub.database;

import android.content.Context;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteOpenHelper;

import org.commcare.hub.util.StreamsUtil;

import java.io.IOException;

/***
 * @author ctsims
 */
public class DatabaseOpenHelper extends SQLiteOpenHelper {

    /**
     * Version History
     */

    /**
     */
    private static final int USER_DB_VERSION = 1;

    private Context context;

    public DatabaseOpenHelper(Context context) {
        super(context, getDbName(), null, USER_DB_VERSION);
        this.context = context;}

    public static String getDbName() {
        return "user_database";
    }


    public static final String DB_ASSETS = "dbfixtures";
    @Override
    public void onCreate(SQLiteDatabase database) {
        try {
            database.beginTransaction();

            String[] assets = context.getAssets().list(DB_ASSETS);
            for(String asset : assets) {
                String assetString = loadAssetAsString(DB_ASSETS + "/" + asset);
                String[] statements = assetString.split(";");
                for(String statement : statements) {
                    if(!"".equals(statement.trim())) {
                        database.execSQL(statement);
                    }
                }
            }

            database.setVersion(USER_DB_VERSION);

            database.setTransactionSuccessful();
        } catch (IOException e) {
            RuntimeException re = new RuntimeException("Error creating database");
            re.initCause(e);
            throw re;
        } finally {
            database.endTransaction();
        }
    }


    private String loadAssetAsString(String asset) throws IOException {
        return StreamsUtil.loadToString(context.getAssets().open(asset));
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

}
