package com.kapcode.parentalcontrols;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filterable;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import de.szalkowski.activitylauncher.R;

public class MainActivity extends AppCompatActivity {
    static MainActivity mainActivity;
    private SharedPreferences prefs;
    public static Activity activity;
    public static Handler handler;
    private String localeString;
    private Filterable filterTarget = null;
    private String filter = "";
    public static LinearLayout layoutToPutInstalledAppInfoInto;

    public static int screenHeight,screenWidth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        screenHeight = displayMetrics.heightPixels;
        screenWidth = displayMetrics.widthPixels;
        mainActivity = this;
        layoutToPutInstalledAppInfoInto = (LinearLayout) findViewById(R.id.ll_apinfo);
        handler=new Handler();
        activity=this;
        setTitle(R.string.app_name);
        this.prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        this.localeString = prefs.getString("language", "System Default");
        //Configuration config = SettingsUtils.createLocaleConfiguration(this.localeString);
        //getBaseContext().getResources().updateConfiguration(config,
               // getBaseContext().getResources().getDisplayMetrics());
        stopParentalControlButtonButton(null);
            ParentalControlService.resolve(this);

    }
    public void addDone(){
        TextView done = new TextView(this);
        done.setText("DONE");
        LinearLayout ll = (LinearLayout) findViewById(R.id.ll_apinfo);
        done.setTextSize(30);
        ll.addView(done);
    }

    public void startParentalControlButtonButton(View v){//KYLE PROSPERT
        //watchDog starts the Alarm, Watching the service
        ParentalControlService.startService(this);
        findViewById(R.id.startParentalControlButton).setEnabled(false);
        findViewById(R.id.stopParentalControlButton).setEnabled(true);
        ParentalControlService.alarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        ParentalControlService.watchDog(ParentalControlService.alarmManager,this);
        layoutToPutInstalledAppInfoInto.setVisibility(View.GONE);
    }
    public void stopParentalControlButtonButton(View v){//KYLE PROSPERT
        ParentalControlService.stopService();
        findViewById(R.id.startParentalControlButton).setEnabled(true);
        findViewById(R.id.stopParentalControlButton).setEnabled(false);
        layoutToPutInstalledAppInfoInto.setVisibility(View.VISIBLE);
    }

    @Override
    public void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        // Restore the previously serialized current dropdown position.
        super.onRestoreInstanceState(savedInstanceState);
    }




    @Override
    protected void onResume() {
        super.onResume();
        stopParentalControlButtonButton(null);//always stop when resuming
    }







    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        // Serialize the current dropdown position.
        super.onSaveInstanceState(outState);
    }


    //KYLE PROSPERT -- //TODO Site sources from stackoverflow help on dialog, and Getting USAGE_STATS access (in bookmarks bar)


    public void grantAccessButton(View v){
        String title = "";
        String message = "";
        String pos = "";
        String neg = "";
        if (isAccessUsageAccessGranted()) {
            title = "Usage Access Already Granted";
            message = "Do you want do revoke Usage Access Permission by going to setting now?\n" +
                    "NOTE: This will disable parts of the app.";
            pos = "Yes";
            neg = "No";
        }else{
            title = "Grant Usage Access?";
            message = "You will be taken to setting to grant usage access to this app, is that okay?\n" +
                    "Reason for usage access: to get current running application. (NO DATA IS COLLECTED)";
            pos = "YES";
            neg = "NO";
        }
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)

                // Specifying a listener allows you to take an action before dismissing the dialog.
                // The dialog is automatically dismissed when a dialog button is clicked.
                .setPositiveButton(pos, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                        startActivity(intent);
                    }
                })

                // A null listener allows the button to dismiss the dialog and take no further action.
                .setNegativeButton(neg, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }
    private boolean isAccessUsageAccessGranted() {//<uses-permission android:name="android.permission.PACKAGE_USAGE_STATS" tools:ignore="ProtectedPermissions"/>
        try {
            PackageManager packageManager = getPackageManager();
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(getPackageName(), 0);
            AppOpsManager appOpsManager = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
            int mode = 0;
            if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.KITKAT) {
                mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                        applicationInfo.uid, applicationInfo.packageName);
            }
            return (mode == AppOpsManager.MODE_ALLOWED);

        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    static String readFromFile(String activityPackageNameAs_fileName, Context context) {

        String ret = null;

        try {
            InputStream inputStream = context.openFileInput(activityPackageNameAs_fileName);

            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append("\n").append(receiveString);
                }

                inputStream.close();
                ret = stringBuilder.toString();
            }
        }
        catch (FileNotFoundException e) {
           // System.out.println("login activity"+ "File not found: " + e.toString());

        } catch (IOException e) {
            //System.out.println("login activity"+ "Can not read file: " + e.toString());

        }

        return ret;
    }


    static void writeToFile(String activityPackageNameAs_fileName, String data, Context context) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput(activityPackageNameAs_fileName, Context.MODE_PRIVATE));
            outputStreamWriter.write(data.toString());
            outputStreamWriter.close();
        }
        catch (IOException e) {
            //System.out.println("Exception"+ "File write failed: " + e.toString());
        }
    }



}
