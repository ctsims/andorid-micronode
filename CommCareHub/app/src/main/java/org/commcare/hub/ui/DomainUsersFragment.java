package org.commcare.hub.ui;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import net.sqlcipher.database.SQLiteDatabase;

import org.commcare.hub.DomainViewActivity;
import org.commcare.hub.MainActivity;
import org.commcare.hub.R;
import org.commcare.hub.application.HubApplication;
import org.commcare.hub.apps.AppAssetBroadcast;
import org.commcare.hub.apps.MobileUserModelBroadcast;
import org.commcare.hub.database.DatabaseUnavailableException;
import org.commcare.hub.events.HubBroadcastListener;
import org.commcare.hub.events.HubEventBroadcast;
import org.commcare.hub.mirror.SyncableUser;

/**
 * Created by ctsims on 12/14/2015.
 */
public class DomainUsersFragment extends Fragment implements HubBroadcastListener {
    DomainUserListAdapter adapter;
    Activity activity;

    int domainId = -1;
    ListView list;

    public DomainUsersFragment() {
        // Indicate that this fragment would like to influence the set of actions in the action bar.
        setHasOptionsMenu(true);
    }

    @Override
    public void setArguments(Bundle bundle) {
        domainId = bundle.getInt(DomainViewActivity.EXTRA_DOMAIN_ID);
        if(adapter != null) {
            adapter.setDomainId(domainId);
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_apps_to_sync, container, false);
        list = (ListView)rootView.findViewById(R.id.app_list);

        try {
            adapter = new DomainUserListAdapter(this.getContext(), HubApplication._().getDatabaseHandle(), domainId);
            list.setAdapter(adapter);
        } catch (DatabaseUnavailableException e) {
            e.printStackTrace();
            //TODO: Draw a different view here
        }
        return rootView;
    }

    @Override
    public void receiveBroadcast(HubEventBroadcast b) {
        if(b instanceof MobileUserModelBroadcast) {
            MobileUserModelBroadcast userBroadcast = (MobileUserModelBroadcast)b;
            if(userBroadcast.getDomainId() == this.domainId && userBroadcast.getEventType() == HubEventBroadcast.EventType.Addition) {
                adapter.notifyDataSetChanged();
            }
        }
    }

    private void updateIfItemExists(int manifestId) {
        for (int i = list.getFirstVisiblePosition(); i < list.getLastVisiblePosition(); ++i) {
            if((int)list.getItemIdAtPosition(i) == manifestId) {
                adapter.updateViewFor(manifestId);
            }
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = activity;

        getActivity().supportInvalidateOptionsMenu(); // calls onPrepareOptionsMenu()
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // If the drawer is open, show the global app actions in the action bar. See also
        // showGlobalContextActionBar, which controls the top-left area of the action bar.
        inflater.inflate(R.menu.users, menu);
        showGlobalContextActionBar();
        super.onCreateOptionsMenu(menu, inflater);
    }

    private void showGlobalContextActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
    }

    private ActionBar getActionBar() {
        return ((AppCompatActivity)getActivity()).getSupportActionBar();
    }
}
