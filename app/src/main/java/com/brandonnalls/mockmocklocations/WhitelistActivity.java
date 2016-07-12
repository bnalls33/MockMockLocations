package com.brandonnalls.mockmocklocations;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class WhitelistActivity extends PreferenceActivity {

    // These 2 are mutually exclusive
    @BindView(R.id.list_and_empty_container) View mListAndEmptyContainer;
    @BindView(R.id.whitelist_all_view_container) View mWhitelistAllViewContainer;

    @BindView(R.id.whitelist_all_view) View mWhitelistAllView;
    @BindView(R.id.add_app) Button addAppButton;
    @BindView(R.id.donate) Button donateButton;
    @BindView(R.id.all_apps) CheckBox allAppsCheckbox;

    ArrayAdapter<String> mAdapter;
    SharedPreferences mSharedPrefs;

    final List<String> appList = new ArrayList<String>();
    boolean whitelistAllApps;

    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_whitelist);
        ButterKnife.bind(this);

        getPreferenceManager().setSharedPreferencesMode(MODE_WORLD_READABLE);
        mSharedPrefs = getSharedPreferences(Common.SHARED_PREFERENCES_FILE, MODE_WORLD_READABLE);

        loadFromPrefs();
        resetUi();

        final ToolbarListener listener = new ToolbarListener();
        addAppButton.setOnClickListener(listener);
        donateButton.setOnClickListener(listener);
        allAppsCheckbox.setOnCheckedChangeListener(listener);

        mAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, appList);
        setListAdapter(mAdapter);
    }

    private void loadFromPrefs() {
        whitelistAllApps = mSharedPrefs.getBoolean(Common.PREF_KEY_WHITELIST_ALL, true);
        appList.clear();
        appList.addAll(mSharedPrefs.getStringSet(Common.PREF_KEY_WHITELIST_APP_LIST, new HashSet<String>()));
        Collections.sort(appList);
    }

    private void saveToPrefs() {
        mSharedPrefs.edit()
                .putBoolean(Common.PREF_KEY_WHITELIST_ALL, whitelistAllApps)
                .putStringSet(Common.PREF_KEY_WHITELIST_APP_LIST, new HashSet<String>(appList))
                .apply();
        Toast.makeText(this, R.string.restart_required, Toast.LENGTH_SHORT).show();
    }

    private void resetUi() {
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
        mListAndEmptyContainer.setVisibility(whitelistAllApps ? View.GONE : View.VISIBLE);
        mWhitelistAllViewContainer.setVisibility(whitelistAllApps ? View.VISIBLE : View.GONE);
        addAppButton.setEnabled(!whitelistAllApps);
        allAppsCheckbox.setChecked(whitelistAllApps);
    }

    @Override
    protected void onListItemClick(ListView l, View v, final int position, long id) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.remove_app_title)
                .setMessage(R.string.remove_app_message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        appList.remove(position);
                        resetUi();
                        saveToPrefs();
                    }
                }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Do nothing.
            }
        }).show();
    }

    public void showAddAppDialog(final HashMap<String, String> nameMap, final String[] sortedNames) {
        progressDialog.dismiss();
        new AlertDialog.Builder(WhitelistActivity.this).setTitle(R.string.dialog_whitelist_title_add_app)
                .setItems(sortedNames, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        appList.add(nameMap.get(sortedNames[which]));
                        Collections.sort(appList);
                        resetUi();
                        saveToPrefs();
                        addAppButton.setEnabled(true);
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        addAppButton.setEnabled(true);
                    }
                })
                .show();
    }

    private class ToolbarListener implements View.OnClickListener, CheckBox.OnCheckedChangeListener{
        @Override
        public void onClick(View v) {
            switch(v.getId()) {
                case R.id.add_app:
                    addAppButton.setEnabled(false);

                    if (progressDialog == null) {
                        progressDialog = new ProgressDialog(WhitelistActivity.this);
                        progressDialog.setMessage(getString(R.string.loading_app_list));
                    }
                    progressDialog.show();
                    new PackageLookupThread(WhitelistActivity.this).start();

                    break;
                case R.id.donate:
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=42SF6QN2QATWW" +
                                    "&lc=US&item_name=MockMockLocation%20Donation&no_note=0&cn=Special%20Note%3a" +
                                    "&no_shipping=1&currency_code=USD&bn=PP%2dDonationsBF%3abtn_donateCC_LG%2egif%3aNonHosted"));
                    startActivity(browserIntent);
                    break;
            }
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            whitelistAllApps = isChecked;
            resetUi();
            saveToPrefs();
        }
    }



    private static class PackageLookupThread extends Thread {
        final WeakReference<WhitelistActivity> contextHolder;
        public PackageLookupThread(final WhitelistActivity context) {
            this.contextHolder = new WeakReference<WhitelistActivity>(context);
        }

        @Override
        public void run() {
            PackageManager pm = contextHolder.get().getPackageManager();
            final List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
            final String[] names = new String[packages.size()];
            final HashMap<String, String> nameMap = new HashMap<>();
            int i = 0;
            for (ApplicationInfo info : packages) {
                names[i] = info.loadLabel(pm) + "\n(" + info.packageName + ")";
                nameMap.put(names[i], info.packageName);
                i++;
            }
            Arrays.sort(names);

            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    contextHolder.get().showAddAppDialog(nameMap, names);
                }
            });
        }
    }
}
