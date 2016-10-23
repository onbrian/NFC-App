package com.enphaseenergy.nfcgatewayalpha;

import android.app.ProgressDialog;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity
{
    private final static String TAG = "NFC Gateway Beta";
    private Toolbar toolbar;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private ViewPagerAdapter viewPagerAdapter;
    private NfcAdapter mNfcAdapter;

    // drawer elements
    private ListView mDrawerList;
    private ArrayAdapter<String> mDrawerAdapter;

    // set up view pager with tabs and fragments
    private void setupViewPager(ViewPager viewPager)
    {
        String[] TAB_NAMES = {"READ", "WRITE", "INFO"};
        Fragment[] FRAGMENTS = {new OneFragment(), new TwoFragment(), new ThreeFragment()};

        this.viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());

        assert (TAB_NAMES.length == FRAGMENTS.length);

        for (int i = 0; i < TAB_NAMES.length; i++)
        {
            viewPagerAdapter.addFragment(FRAGMENTS[i], TAB_NAMES[i]);
        }

        viewPager.setAdapter(viewPagerAdapter);
        viewPager.setOffscreenPageLimit(TAB_NAMES.length);
    }

    private void addDrawerItems()
    {
        String[] osArray = { "Android", "iOS", "Windows", "OS X", "Linux" };
        mDrawerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, osArray);
        mDrawerList.setAdapter(mDrawerAdapter);
        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(MainActivity.this, "Time for an upgrade!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDrawerList = (ListView)findViewById(R.id.navList);
        addDrawerItems();

        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0)
        {
            finish();
            return;
        }

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        viewPager = (ViewPager) findViewById(R.id.viewpager);
        setupViewPager(viewPager);

        tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        tabLayout.setupWithViewPager(viewPager);

        /*
        tabLayout.getTabAt(0).setIcon(android.R.drawable.ic_delete);
        tabLayout.getTabAt(1).setIcon(android.R.drawable.ic_media_next);
        */

        // close keyboard when switching tabs
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener()
        {
            @Override
            public void onTabSelected(TabLayout.Tab tab)
            {
                View view = getCurrentFocus();
                if (view != null) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab)
            {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab)
            {
            }
        });

        // set up NFC
        this.mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        NFC_Helper.alertDeviceNFCStatus(this, this.TAG, this.mNfcAdapter);
        if (mNfcAdapter == null)
        {
            // Stop here, we definitely need NFC
            NFC_Helper.reportError(this, TAG, "This device doesn't support NFC.", null);
            finish();
            return;
        }
        //handleIntent(getIntent());
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        NFC_Helper.setupForegroundDispatch(this, TAG, mNfcAdapter);
    }

    @Override
    protected void onPause()
    {
        NFC_Helper.stopForegroundDispatch(this, mNfcAdapter);
        super.onPause();
    }

    @Override
    protected void onNewIntent(Intent intent)
    {
        handleIntent(intent);
    }

    private void handleIntent(Intent intent)
    {
        /*
        final ProgressDialog progDialog = ProgressDialog.show(this, "Progress_bar or give anything you want",
                "Give message like ....please wait....", true);
        new Thread() {
            public void run() {
                try {
                    // sleep the thread, whatever time you want.
                    sleep(2000);
                } catch (Exception e) {
                }
                progDialog.dismiss();
            }
        }.start();
        */

        if (!NFC_Helper.isValidNDEFIntent(this, TAG, intent))
        {
            return;
        }

        // extract NFC tag from intent
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

        // get current active fragment
        int current_position = viewPager.getCurrentItem();
        Fragment current_frag = viewPagerAdapter.getItem(current_position);
        //Toast.makeText(this, current_frag.getClass().getSimpleName(), Toast.LENGTH_SHORT).show();

        // execute appropriate NFC process for fragment
        if (current_position == 0)
        {
            OneFragment one_frag = (OneFragment) current_frag;
            one_frag.readNFCTag(tag);
        }
        else if (current_position == 1)
        {
            TwoFragment two_frag = (TwoFragment) current_frag;
            two_frag.writeNFCTag(tag);
        }
        else if (current_position == 2)
        {
            ThreeFragment three_frag = (ThreeFragment) current_frag;
            three_frag.displayInfo(tag);
        }
        else
        {
            NFC_Helper.reportError(this, TAG, "Unexpected view pager item!", null);
        }
    }
}
