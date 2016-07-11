package org.commcare.hub;

import android.content.Context;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.support.design.widget.TabLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import org.commcare.hub.application.HubApplication;
import org.commcare.hub.apps.AppAssetBroadcast;
import org.commcare.hub.apps.AppAssetModel;
import org.commcare.hub.events.HubActivity;
import org.commcare.hub.events.HubEventBroadcast;
import org.commcare.hub.ui.DomainAppsFragment;
import org.commcare.hub.ui.DomainUsersFragment;

public class DomainViewActivity extends HubActivity {

    public static final String EXTRA_DOMAIN_ID = "domain_id";

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    private int domainId = -1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_domain_view);

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("");

        setupSpinner();

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager)findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout)findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);
    }

    private void setupSpinner() {
        Spinner s = (Spinner)this.findViewById(R.id.domain_spinner);
        String[] columns = new String[] {
                "id AS " + BaseColumns._ID,
                "domain_guid"
        };

        s.setAdapter(new CursorAdapter(this, HubApplication._().getDatabaseHandle().query("DomainList", columns, null, null, null, null, null)) {
            @Override
            public View newView(Context context, Cursor cursor, ViewGroup parent) {
                TextView tv = (TextView)LayoutInflater.from(context).inflate(R.layout.component_spinner_text, null);
                return tv;
            }

            @Override
            public void bindView(View view, Context context, Cursor cursor) {
                ((TextView)view).setText(cursor.getString(cursor.getColumnIndex("domain_guid")));
            }
        });
        s.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateLiveDomain((int)id);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    public Fragment getCurrentPage() {
        return getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.container + ":" + mViewPager.getCurrentItem());
    }


    private void updateLiveDomain(int id) {
        this.domainId = id;
        Fragment page = getCurrentPage();
        if (page != null) {
            page.setArguments(getDomainArguments());
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_domain_view, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public Bundle getDomainArguments() {
        Bundle b = new Bundle();
        b.putInt(EXTRA_DOMAIN_ID, domainId);
        return b;
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    Fragment f = new DomainAppsFragment();
                    f.setArguments(getDomainArguments());
                    return f;
                case 1:
                    f = new DomainUsersFragment();
                    f.setArguments(getDomainArguments());
                    return f;
            }
            return null;
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Apps";
                case 1:
                    return "Users";
            }
            return null;
        }
    }
}
