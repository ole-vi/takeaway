package org.ole.planet.myplanet.utilities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.provider.Settings;
import android.view.View;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.snackbar.Snackbar;

import org.ole.planet.myplanet.MainApplication;
import org.ole.planet.myplanet.R;
import org.ole.planet.myplanet.datamanager.MyDownloadService;
import org.ole.planet.myplanet.datamanager.Service;
import org.ole.planet.myplanet.model.MyPlanet;

import java.util.ArrayList;

public class DialogUtils {

    public static ProgressDialog getProgressDialog(final Context context) {
        final ProgressDialog prgDialog = new ProgressDialog(context);
        prgDialog.setTitle(context.getString(R.string.downloading_file));
        prgDialog.setMax(100);
        prgDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        prgDialog.setCancelable(false);
        prgDialog.setButton(DialogInterface.BUTTON_POSITIVE, context.getString(R.string.dismiss), (dialogInterface, i) -> prgDialog.dismiss());
        prgDialog.setButton(DialogInterface.BUTTON_NEGATIVE, context.getString(R.string.stop_download), (dialogInterface, i) -> context.stopService(new Intent(context, MyDownloadService.class)));
        return prgDialog;
    }

    public static void showError(ProgressDialog prgDialog, String message) {
        prgDialog.setTitle(message);
        if (prgDialog.getButton(ProgressDialog.BUTTON_NEGATIVE) != null)
            prgDialog.getButton(ProgressDialog.BUTTON_NEGATIVE).setEnabled(false);
    }

    public static void showWifiSettingDialog(final Context context) {
        if (!NetworkUtils.isWifiBluetoothEnabled()) return;
        showDialog(context);
    }

    private static void showDialog(final Context context) {
        if (MainApplication.syncFailedCount > 3) {
            AlertDialog.Builder pd = new AlertDialog.Builder(context);
            String message = NetworkUtils.isBluetoothEnabled() ? "Bluetooth " : "";
            message += NetworkUtils.isWifiEnabled() ? "Wifi " : "";
            message += R.string.is_on_please_turn_of_to_save_battery;
            pd.setMessage(message);
            pd.setPositiveButton(R.string.go_to_settings, (dialogInterface, i) -> {
                MainApplication.syncFailedCount = 0;
                Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                context.startActivity(intent);
            }).setNegativeButton(context.getString(R.string.cancel), null);
            pd.setCancelable(false);
            AlertDialog d = pd.create();
//            d.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
            d.show();
        }
    }

    public static void showSnack(View v, String s) {
        if (v != null) Snackbar.make(v, s, Snackbar.LENGTH_LONG).show();
    }

    public static void showAlert(Context context, String title, String message) {
        new AlertDialog.Builder(context).setTitle(title).setMessage(message).setPositiveButton(R.string.dismiss, null).show();
    }

    public static AlertDialog getAlertDialog(Context context, String message, String pos, DialogInterface.OnClickListener listener) {
        return new AlertDialog.Builder(context).setMessage(message).setIcon(R.drawable.courses).setPositiveButton(pos, listener).setNegativeButton("Cancel", null).show();
    }

    public static void showCloseAlert(Context context, String title, String message) {
        new AlertDialog.Builder(context).setTitle(title).setMessage(message).setPositiveButton(R.string.close, null).show();
    }

    public static AlertDialog getAlertDialog(Context context, String title, View v) {
        return new AlertDialog.Builder(context).setTitle(title).setIcon(R.drawable.ic_edit).setView(v).setPositiveButton(R.string.submit, null).setNegativeButton(R.string.cancel, null).show();
    }

    public static AlertDialog.Builder getUpdateDialog(Context context, MyPlanet info, ProgressDialog progressDialog) {
        return new AlertDialog.Builder(context).setTitle(R.string.new_version_of_my_planet_available).setMessage(R.string.download_first_to_continue).setNeutralButton(R.string.upgrade_local, (dialogInterface, i) -> {
            startDownloadUpdate(context, Utilities.getApkUpdateUrl(info.getLocalapkpath()), progressDialog);
        }).setPositiveButton(R.string.upgrade, (dialogInterface, i) -> {
            startDownloadUpdate(context, info.getApkpath(), progressDialog);
        });
    }

    public static void startDownloadUpdate(Context context, String path, ProgressDialog progressDialog) {
        new Service(MainApplication.context).checkCheckSum(new Service.ChecksumCallback() {
            @Override
            public void onMatch() {
                Utilities.toast(MainApplication.context, String.valueOf(R.string.apk_already_exists));
                FileUtils.installApk(context, path);
            }

            @Override
            public void onFail() {
                ArrayList url = new ArrayList();
                url.add(path);
                if (progressDialog != null) {
                    progressDialog.setMessage(String.valueOf(R.string.downloading_file));
                    progressDialog.setCancelable(false);
                    progressDialog.show();
                }
                Utilities.openDownloadService(context, url);
            }
        }, path);
    }
}
