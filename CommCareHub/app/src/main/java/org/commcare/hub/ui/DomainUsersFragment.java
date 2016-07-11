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

import org.commcare.hub.MainActivity;
import org.commcare.hub.R;
import org.commcare.hub.application.HubApplication;
import org.commcare.hub.database.DatabaseUnavailableException;
import org.commcare.hub.mirror.SyncableUser;

/**
 * Created by ctsims on 12/14/2015.
 */
public class DomainUsersFragment extends Fragment implements AddUserDialogFragment.UserDialogFragmentListener, AdapterView.OnItemClickListener {
    UserListAdapter adapter;
    Activity activity;

    public DomainUsersFragment() {
        // Indicate that this fragment would like to influence the set of actions in the action bar.
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_users_to_sync, container, false);
        ListView list = (ListView)rootView.findViewById(R.id.user_list);

        try {
            adapter = new UserListAdapter(this.getContext(), HubApplication._().getDatabaseHandle());
            list.setAdapter(adapter);
            list.setOnItemClickListener(this);
        } catch (DatabaseUnavailableException e) {
            e.printStackTrace();
            //TODO: Draw a different view here
        }
        return rootView;
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_new_user) {
            AddUserDialogFragment fragment = new AddUserDialogFragment();
            fragment.attachDetails(null, this);
            fragment.show(((FragmentActivity)activity).getSupportFragmentManager(), "user_dialog");
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onFinishEditDialog(SyncableUser user) {
        SQLiteDatabase database = HubApplication._().getDatabaseHandle();
        user.writeToDb(database);
        adapter.update();
        database.close();
    }

    @Override
    public void removeUser(SyncableUser user) {
        SQLiteDatabase database = HubApplication._().getDatabaseHandle();
        user.removeFromDb(database);
        adapter.update();
        database.close();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        AddUserDialogFragment fragment = new AddUserDialogFragment();
        fragment.attachDetails(adapter.getItem(position), this);
        fragment.show(((FragmentActivity)activity).getSupportFragmentManager(), "user_dialog");
    }
}
