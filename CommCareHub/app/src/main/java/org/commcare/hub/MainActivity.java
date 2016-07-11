package org.commcare.hub;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import net.sqlcipher.database.SQLiteDatabase;

import org.commcare.hub.application.HubApplication;
import org.commcare.hub.apps.AppAssetModel;
import org.commcare.hub.monitor.ServicesMonitor;
import org.commcare.hub.server.ServerService;
import org.commcare.hub.ui.ManagedAppsFragment;
import org.commcare.hub.ui.UsersFragment;

import java.io.IOException;

public class MainActivity extends AppCompatActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    public static final int RESULT_LOGIN = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout)findViewById(R.id.drawer_layout));
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();

        if(position == 1) {
            fragmentManager.beginTransaction()
                    .replace(R.id.container, new UsersFragment())
                    .commit();
            return;
        }

        if(position == 2) {
            fragmentManager.beginTransaction()
                    .replace(R.id.container, new ManagedAppsFragment())
                    .commit();
            return;
        }

        fragmentManager.beginTransaction()
                .replace(R.id.container, PlaceholderFragment.newInstance(position + 1))
                .commit();
    }

    public void onSectionAttached(String title) {
        mTitle = title;
    }

    public void onSectionAttached(int number) {
        switch (number) {
            case 1:
                mTitle = getString(R.string.title_section1);
                break;
            case 2:
                mTitle = getString(R.string.title_section2);
                break;
            case 3:
                mTitle = getString(R.string.title_section3);
                break;
            case 4:
                mTitle = "Connect to HQ";
                break;
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
//            SQLiteDatabase database = HubApplication._().getDatabaseHandle();
//            AppAssetModel model = AppAssetModel.createAppModelRequest("https://www.commcarehq.org/a/corpora/apps/api/download_ccz/?app_id=4c32e93d1b2840af035f68c1f6d9f890");
//            model.writeToDb(database);
//            database.close();

            Intent i = new Intent(this,LoginActivity.class);
            this.startActivityForResult(i, RESULT_LOGIN);
            return true;
        }

        //noinspection SimplifiableIfStatement
        if (id == R.id.clear_pause) {

            Intent i = new Intent(this,DomainViewActivity.class);
            this.startActivityForResult(i, RESULT_LOGIN);
            return true;
//            SQLiteDatabase database = HubApplication._().getDatabaseHandle();
//            ContentValues cv = new ContentValues();
//            cv.put("paused", Boolean.FALSE.toString());
//            database.update(AppAssetModel.TABLE_NAME, cv, null, null);
//            database.close();
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            configureView(rootView);
            return rootView;
        }

        TextView status;

        String buffer = "";

        private void configureView(final View rootView) {
            Button toggle = (Button)rootView.findViewById(R.id.server_toggle);
            if(ServerService.server.isAlive()) {
                toggle.setText("Stop Server");
            } else {
                toggle.setText("Start Server");
            }
            status = (TextView)rootView.findViewById(R.id.text_server_status);
            status.setText(ServicesMonitor.getMessages());
            toggle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (ServerService.server.isAlive()) {
                        stopServer();
                    } else {
                        startServer();
                    }
                    configureView(rootView);
                }
            });
        }

        private void startServer() {
            try {
                ServerService.server.start();

            } catch (IOException e) {
                ServicesMonitor.reportMessage("Server Startup failed: " + e.getMessage());
                e.printStackTrace();
            }
        }

        private void stopServer() {
            ServerService.server.stop();

            buffer = "";
            ServicesMonitor.reportMessage("Server stopped");
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            ((MainActivity)activity).onSectionAttached(
                    getArguments().getInt(ARG_SECTION_NUMBER));
        }
    }

}
