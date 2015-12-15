package org.commcare.hub.ui;

import android.content.Context;
import android.database.DataSetObserver;
import android.text.format.DateUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.TextView;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;

import org.commcare.hub.R;
import org.commcare.hub.mirror.SyncableUser;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by ctsims on 12/14/2015.
 */
public class UserListAdapter implements ListAdapter
{

    ArrayList<SyncableUser> users = new ArrayList<>();
    Context context;

    SQLiteDatabase database;

    public UserListAdapter(Context context, SQLiteDatabase database) {
        this.database = database;
        this.context = context;
        update();
    }

    public void update() {
        users.clear();
        Cursor c = database.query("SyncableUser", null, null, null, null, null, null);
        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            SyncableUser u = SyncableUser.fromDb(c);

            users.add(u);
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
        return users.size();
    }

    @Override
    public SyncableUser getItem(int position) {
        return users.get(position);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getId();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        SyncableUser user = getItem(position);
        if(convertView == null) {
            convertView = View.inflate(context, R.layout.list_user_record, null);
        }

        ((TextView)convertView.findViewById(R.id.list_user_username)).setText(user.getUsername());
        ((TextView)convertView.findViewById(R.id.list_user_status)).setText(user.getStatus().toString());
        ((TextView)convertView.findViewById(R.id.list_user_domain)).setText(user.getDomain());

        CharSequence dateFormat = DateUtils.formatSameDayTime(user.getLastUpdate().getTime(),
                new Date().getTime(), DateFormat.DEFAULT, DateFormat.DEFAULT);
        ((TextView)convertView.findViewById(R.id.list_user_last_updated)).setText(dateFormat);

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
