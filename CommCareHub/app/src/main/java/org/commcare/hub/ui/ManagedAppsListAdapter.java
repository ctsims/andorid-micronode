package org.commcare.hub.ui;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.TextView;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;

import org.commcare.hub.R;
import org.commcare.hub.apps.AppAssetModel;

import java.util.ArrayList;

/**
 * Created by ctsims on 12/14/2015.
 */
public class ManagedAppsListAdapter implements ListAdapter
{

    ArrayList<AppAssetModel> apps = new ArrayList<>();
    Context context;

    SQLiteDatabase database;

    public ManagedAppsListAdapter(Context context, SQLiteDatabase database) {
        this.database = database;
        this.context = context;
        update();
    }

    public void update() {
        apps.clear();
        Cursor c = database.query(AppAssetModel.TABLE_NAME, null, null, null, null, null, null);
        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            AppAssetModel u = AppAssetModel.fromDb(c);

            apps.add(u);
        }
        c.close();
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public boolean isEnabled(int position) {
        return true;
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {

    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {

    }

    @Override
    public int getCount() {
        return apps.size();
    }

    @Override
    public AppAssetModel getItem(int position) {
        return apps.get(position);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getRowId();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        AppAssetModel app = getItem(position);
        if(convertView == null) {
            convertView = View.inflate(context, R.layout.list_user_record, null);
        }

        String name = String.valueOf(app.getAppManifestId());
        if(name == null) {
            name = "[uninit]";
        }

        ((TextView)convertView.findViewById(R.id.list_user_username)).setText(name);
        ((TextView)convertView.findViewById(R.id.list_user_status)).setText(app.getStatus().toString());
        ((TextView)convertView.findViewById(R.id.list_user_domain)).setText("");

//        CharSequence dateFormat = DateUtils.formatSameDayTime(user.getLastUpdate().getTime(),
//                new Date().getTime(), DateFormat.DEFAULT, DateFormat.DEFAULT);
        ((TextView)convertView.findViewById(R.id.list_user_last_updated)).setText("");

        return convertView;
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }
}
