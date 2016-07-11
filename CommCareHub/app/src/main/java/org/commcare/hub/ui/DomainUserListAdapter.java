package org.commcare.hub.ui;

import android.content.Context;
import android.support.v4.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import net.sqlcipher.database.SQLiteDatabase;

import org.commcare.hub.R;
import org.commcare.hub.apps.AppAssetModel;
import org.commcare.hub.mirror.SyncableUser;
import org.commcare.hub.util.DbUtil;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by ctsims on 12/14/2015.
 */
public class DomainUserListAdapter extends CursorAdapter
{
    ArrayList<String> apps = new ArrayList<>();
    Context context;

    SQLiteDatabase database;

    int domainId;

    HashMap<Integer, Pair<Integer, View>> viewMapper = new HashMap<>();

    public DomainUserListAdapter(Context context, SQLiteDatabase database, int domainId) {
        super(context, database.query(DbUtil.TABLE_USER_LIST, null, "domain_id = (? + 0)", new String[] {String.valueOf(domainId)}, null, null, null), 0);
        this.database = database;
        this.context = context;
        this.domainId = domainId;
        //update();
    }

    public void setDomainId(int domainId) {
        this.domainId = domainId;
        update();
    }

    public void update() {
        viewMapper.clear();
        changeCursor(database.query(DbUtil.TABLE_USER_LIST, null, "domain_id = (? + 0)", new String[]{String.valueOf(domainId)}, null, null, null));
//        apps.clear();
//        Cursor c = ;
//        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
//            String s = c.getString(c.getColumnIndex("app_descriptor")) + ": " + c.getString(c.getColumnIndex("version"));
//            apps.add(s);
//        }
//        c.close();
    }
//
//    @Override
//    public boolean areAllItemsEnabled() {
//        return true;
//    }

//    @Override
//    public boolean isEnabled(int position) {
//        return true;
//    }
//
//    @Override
//    public int getCount() {
//        return apps.size();
//    }
//
//    @Override
//    public String getItem(int position) {
//        return apps.get(position);
//    }
//
//    @Override
//    public long getItemId(int position) {
//        return position;
//    }
//
//    @Override
//    public boolean hasStableIds() {
//        return true;
//    }


    public void updateViewFor(int modelId) {
        Pair<Integer, View> viewData = viewMapper.get(modelId);
        android.database.Cursor c = this.getCursor();
        c.moveToPosition(viewData.first);

        bindView(viewData.second, context, c);
    }

    @Override
    public View newView(Context context, android.database.Cursor cursor, ViewGroup parent) {
        View v = LayoutInflater.from(context).inflate(R.layout.component_app_view,null);
        return v;
    }

    @Override
    public void bindView(View view, Context context, android.database.Cursor cursor) {
        final int modelId = cursor.getInt(cursor.getColumnIndexOrThrow("_id"));

        viewMapper.put(modelId, new Pair<>(cursor.getPosition(), view));

        ((TextView)view.findViewById(R.id.av_title)).setText(cursor.getString(cursor.getColumnIndexOrThrow("readable_name")));

        ((TextView)view.findViewById(R.id.av_build_number)).setVisibility(View.GONE);

        ImageButton downloadButton = (ImageButton)view.findViewById(R.id.av_button);
        int newImageId = R.drawable.btn_icon_download;
        downloadButton.setClickable(true);

        new_id_fetch:
        for (SyncableUser asset : DbUtil.getSyncableUserForWebUser(database, modelId)) {
            switch(asset.getStatus()) {
                case Requested:
                case KeysFetched:
                    newImageId = android.R.drawable.ic_menu_rotate;
                    downloadButton.setClickable(false);
                    break new_id_fetch;
                case ModelFetched:
                    newImageId = R.drawable.btn_icon_check;
                    downloadButton.setClickable(false);
                    break;
            }
        }


        downloadButton.setImageResource(newImageId);

        downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //DbUtil.requestManifestAsset(database, manifestId, version, url);
            }
        });
    }
//
//    @Override
//    public int getItemViewType(int position) {
//        return 0;
//    }
//
//    @Override
//    public int getViewTypeCount() {
//        return 1;
//    }
//
//    @Override
//    public boolean isEmpty() {
//        return false;
//    }
}
