package org.ole.planet.myplanet.ui;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.view.MenuItem;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.ole.planet.myplanet.R;
import org.ole.planet.myplanet.datamanager.DatabaseService;
import org.ole.planet.myplanet.model.RealmUserModel;
import org.ole.planet.myplanet.service.UserProfileDbHandler;
import org.ole.planet.myplanet.ui.dashboard.DashboardActivity;
import org.ole.planet.myplanet.ui.sync.LoginActivity;
import org.ole.planet.myplanet.utilities.FileUtils;
import org.ole.planet.myplanet.utilities.LocaleHelper;
import org.ole.planet.myplanet.utilities.TimeUtils;
import org.ole.planet.myplanet.utilities.Utilities;

import java.io.File;

import io.realm.Realm;

import static org.ole.planet.myplanet.base.BaseResourceFragment.settings;
import static org.ole.planet.myplanet.ui.dashboard.DashboardFragment.PREFS_NAME;

public class SettingActivity extends AppCompatActivity {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingFragment())
                .commit();
        setTitle(getString(R.string.action_settings));


    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void finish() {
        super.finish();
        startActivity(new Intent(this, DashboardActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    public static class SettingFragment extends PreferenceFragment {
        UserProfileDbHandler profileDbHandler;
        RealmUserModel user;
        ProgressDialog dialog;


        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref);
            profileDbHandler = new UserProfileDbHandler(getActivity());
            user = profileDbHandler.getUserModel();
            dialog = new ProgressDialog(getActivity());
            setBetaToggleOn();
            setAutoSyncToggleOn();
            ListPreference lp = (ListPreference) findPreference("app_language");
            lp.setOnPreferenceChangeListener((preference, o) -> {
                LocaleHelper.setLocale(getActivity(), o.toString());
                getActivity().recreate();
                return true;
            });

//            Preference preference = findPreference("add_manager");
//            preference.setOnPreferenceClickListener(preference1 -> {
//                managerLogin();
//                return false;
//            });
            Realm mRealm = new DatabaseService(getActivity()).getRealmInstance();
            Preference preference = findPreference("reset_app");
            preference.setOnPreferenceClickListener(preference1 -> {
                new AlertDialog.Builder(getActivity()).setTitle("Are you sure??").setPositiveButton("YES", (dialogInterface, i) -> {
                    settings.edit().clear();
                    mRealm.executeTransactionAsync(realm -> realm.deleteAll(), () -> {
                        Utilities.toast(getActivity(), "Data cleared");
                        startActivity(new Intent(getActivity(), LoginActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
                        getActivity().finish();

                    });
                }).setNegativeButton("No", null).show();

                return false;
            });


            Preference pref_freeup = findPreference("freeup_space");
            pref_freeup.setOnPreferenceClickListener(preference1 -> {

                new AlertDialog.Builder(getActivity()).setTitle("Are you sure want to delete all the files?").setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        File f = new File(Utilities.SD_PATH);
                        deleteRecursive(f);
                        Utilities.toast(getActivity(), "Data cleared");
                    }
                }).setNegativeButton("No", null).show();
                return false;
            });

        }


        void deleteRecursive(File fileOrDirectory) {
            if (fileOrDirectory.isDirectory())
                for (File child : fileOrDirectory.listFiles())
                    deleteRecursive(child);
            fileOrDirectory.delete();

        }

        public void setBetaToggleOn() {
            SwitchPreference beta = (SwitchPreference) findPreference("beta_function");
            SwitchPreference course = (SwitchPreference) findPreference("beta_course");
            SwitchPreference achievement = (SwitchPreference) findPreference("beta_achievement");
            SwitchPreference rating = (SwitchPreference) findPreference("beta_rating");
            SwitchPreference myHealth = (SwitchPreference) findPreference("beta_myHealth");
            SwitchPreference healthWorker = (SwitchPreference) findPreference("beta_healthWorker");
            SwitchPreference newsAddImage = (SwitchPreference) findPreference("beta_addImageToMessage");

            beta.setOnPreferenceChangeListener((preference, o) -> {
                if (beta.isChecked()) {
                    course.setChecked(true);
                    achievement.setChecked(true);
                }

                return true;
            });

        }

        public void setAutoSyncToggleOn() {
            SwitchPreference autoSync = (SwitchPreference) findPreference("auto_sync_with_server");
            SwitchPreference autoForceWeeklySync = (SwitchPreference) findPreference("force_weekly_sync");
            SwitchPreference autoForceMonthlySync = (SwitchPreference) findPreference("force_monthly_sync");
            Preference lastSyncDate = (Preference) findPreference("lastSyncDate");
            autoSync.setOnPreferenceChangeListener((preference, o) -> {
                if (autoSync.isChecked()) {
                    if (autoForceWeeklySync.isChecked()) {
                        autoForceMonthlySync.setChecked(false);
                    } else if (autoForceMonthlySync.isChecked()) {
                        autoForceWeeklySync.setChecked(false);
                    } else {
                        autoForceWeeklySync.setChecked(true);
                    }
                }
                return true;
            });

            autoForceSync(autoSync, autoForceWeeklySync, autoForceMonthlySync);
            autoForceSync(autoSync, autoForceMonthlySync, autoForceWeeklySync);
            SharedPreferences settings = getActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            long lastSynced = settings.getLong("LastSync", 0);
            if (lastSynced == 0) {
                lastSyncDate.setTitle("Last Synced: Never");
            } else lastSyncDate.setTitle("Last Synced: " + Utilities.getRelativeTime(lastSynced));
        }
//
//        private void managerLogin() {
//            View v = LayoutInflater.from(getActivity()).inflate(R.layout.alert_manager_login, null);
//            EditText etUserName = v.findViewById(R.id.et_user_name);
//            EditText etPassword = v.findViewById(R.id.et_password);
//            new AlertDialog.Builder(getActivity()).setTitle("Add Manager Account")
//                    .setView(v)
//                    .setPositiveButton("Ok", (dialogInterface, i) -> {
//
//                        String username = etUserName.getText().toString();
//                        String password = etPassword.getText().toString();
//                        if (username.isEmpty()){
//                            Utilities.toast(getActivity(),"Please enter username");
//
//                        }else if(password.isEmpty()){
//                            Utilities.toast(getActivity(),"Please enter password");
//                        }else{
//                            ManagerSync.getInstance().login(username, password, this);
//                        }
//                    }).setNegativeButton("Cancel", null).show();
//        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            profileDbHandler.onDestory();
        }
//
//        @Override
//        public void onSyncStarted() {
//            dialog.show();
//        }
//
//        @Override
//        public void onSyncComplete() {
//            getActivity().runOnUiThread(() -> {
//                Utilities.toast(getActivity(),"Added manager user");
//                dialog.dismiss();
//            });
//        }

//        @Override
//        public void onSyncFailed(String msg) {
//          getActivity().runOnUiThread(() -> {
//              Utilities.toast(getActivity(),msg);
//              dialog.dismiss();
//          });
//        }
    }

    private static void autoForceSync(SwitchPreference autoSync, SwitchPreference autoForceA, SwitchPreference autoForceB) {
        autoForceA.setOnPreferenceChangeListener((preference, o) -> {
            if (autoSync.isChecked()) {
                autoForceB.setChecked(false);
            } else {
                autoForceB.setChecked(true);
            }
            return true;
        });
    }

}
