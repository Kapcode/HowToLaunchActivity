package com.kapcode.parentalcontrols;

import static android.app.PendingIntent.getActivity;

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
import android.os.Looper;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Filterable;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

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
    public static View startButton,activitiesHidden,permissionsButton,stopButton, pin_layout;
    Button enterButton;
    HorizontalScrollView horiz_scrollView;
    public static TextView pin_instructions,pin_textView;
    public static int screenHeight,screenWidth;
    TextView maxVolumeValueTextView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        var prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        SettingsUtils.setTheme(prefs.getString("theme", "0"));
        if (!prefs.contains("hide_hide_private")) {
            prefs.edit().putBoolean("hide_hide_private", false).apply();
        }
        if (!prefs.contains("language")) {
            prefs.edit().putString("language", "System Default").apply();
        }
        setContentView(R.layout.activity_main);
        seekBarSetup();
        startButton = findViewById(R.id.startParentalControlButton);
        activitiesHidden = findViewById(R.id.activitiesToggle);
        permissionsButton = findViewById(R.id.grantPermissionButton);
        maxVolumeValueTextView = findViewById(R.id.maxVolumePercentValueTextView);
        stopButton = findViewById(R.id.stopParentalControlButton);
        enterButton = (Button) findViewById(R.id.numbPadEnter);
        pin_layout =findViewById(R.id.numpad_layout);
        pin_instructions = (TextView) findViewById(R.id.pin_instructions);
        pin_textView = (TextView) findViewById(R.id.pin_textView);
        horiz_scrollView = (android.widget.HorizontalScrollView) findViewById(R.id.horiz_scrollView);
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
        MainActivity.progressBar(true);
        //Configuration config = SettingsUtils.createLocaleConfiguration(this.localeString);
        //getBaseContext().getResources().updateConfiguration(config,
               // getBaseContext().getResources().getDisplayMetrics());

        //stopParentalControlButtonButton(null);


    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        new Thread(new Runnable() {
            @Override
            public void run() {

                ParentalControlService.resolve(mainActivity);
            }
        }).start();
    }

    public void seekBarSetup(){

        SeekBar seekBar = findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                maxVolumeValueTextView.setText(seekBar.getProgress()+"%");
                VolumeControl.setMax_percent_volume(seekBar.getProgress());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


    }


    public void volumeButtonOnClick(View view){
        Button b = (Button) view;
        int vol = Integer.parseInt(b.getText().toString().replace("%",""));

        SeekBar seekBar = findViewById(R.id.seekBar);
        seekBar.setProgress(vol);
    }
    public static void progressBar(boolean show){
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {//hide all UI that can interrupt resolve() and populate() or take user away from app until done.
                ProgressBar p = mainActivity.findViewById(R.id.volumeMaxProgressBar);
                if(show){
                    activitiesHidden.setEnabled(false);
                    pin_layout.setVisibility(View.GONE);
                    startButton.setEnabled(false);
                    stopButton.setEnabled(false);
                    permissionsButton.setEnabled(false);
                    p.setVisibility(View.VISIBLE);
                }else {
                    pin_layout.setVisibility(View.GONE);
                    activitiesHidden.setEnabled(true);

                    if(!ParentalControlService.serviceIsRunning.get()){
                        startButton.setEnabled(true);
                        stopButton.setEnabled(false);
                    }else{
                        stopButton.setEnabled(true);
                        startButton.setEnabled(false);
                    }


                    permissionsButton.setEnabled(true);
                    p.setVisibility(View.GONE);
                }
            }
        });

    }

    public void askForPin(){
        pin_layout.setVisibility(View.VISIBLE);
        horiz_scrollView.setVisibility(View.GONE);
        pin1[0]=0;
        pin_textView.setText("");
        pin_instructions.setText("Please enter your pin. If you forgot it, just restart your device!");
        enterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(Integer.parseInt(pin_textView.getText().toString())==ParentalControlService.pin){
                    ParentalControlService.stopService();
                    findViewById(R.id.startParentalControlButton).setEnabled(true);
                    findViewById(R.id.stopParentalControlButton).setEnabled(false);
                    horiz_scrollView.setVisibility(View.VISIBLE);
                    pin_layout.setVisibility(View.GONE);
                }else{
                    pin_instructions.setText("Incorrect pin! If you forgot it, just restart your device!");
                    makeMeShake(pin_instructions,50,5);
                }

            }
        });




    }


    public void createPin(){
        pin_layout.setVisibility(View.VISIBLE);
        pin1[0]=0;
        pin_instructions.setText("Type a 4 digit pin.");
        makeMeShake(pin_instructions,50,5);
        horiz_scrollView.setVisibility(View.GONE);
        pin_textView.setText("");
        enterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                        if (pin1[0]==0) {
                            pin1[0]=Integer.parseInt(pin_textView.getText().toString());
                            pin_instructions.setText("Please re-type new pin to confirm.");
                            makeMeShake(pin_instructions,50,5);
                            pin_textView.setText("");
                        }else{
                            //second time through, lets make sure they match up
                            if(pin1[0]==Integer.parseInt(pin_textView.getText().toString())){
                                ParentalControlService.pin=pin1[0];
                                System.out.println("PINS MATCH");
                                pin_instructions.setText("Type a 4 digit pin.");
                                ParentalControlService.startService(activity);
                                findViewById(R.id.startParentalControlButton).setEnabled(false);
                                findViewById(R.id.stopParentalControlButton).setEnabled(true);
                                ParentalControlService.alarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
                                ParentalControlService.watchDog(ParentalControlService.alarmManager,activity);
                                horiz_scrollView.setVisibility(View.GONE);
                                pin_textView.setText("");
                                activity.finish();
                                // no need to keep all the views in memory!
                            }else{
                                pin_instructions.setText("Pins did not Match! Type a 4 digit pin.");
                                makeMeShake(pin_instructions,50,5);
                                pin1[0]=0;
                                pin_textView.setText("");
                                System.out.println("PINS DO NOT MATCH");
                            }

                        }
            }
        });

    }
    final int[] pin1 = new int[1];
    public void numpadOnClick(View view){
        Button b = (Button) view;
            if(pin_textView.length()==4){
                makeMeShake(pin_instructions,50,5);
                Toast.makeText(activity, "4 Digits!",
                        Toast.LENGTH_LONG).show();
            }else{
                pin_textView.setText(pin_textView.getText().toString()+b.getText().toString());
            }
    }

    //Windless
    //https://stackoverflow.com/questions/9448732/shaking-wobble-view-animation-in-android
    public static View makeMeShake(View view, int duration, int offset) {
        Animation anim = new TranslateAnimation(-offset,offset,0,0);
        anim.setDuration(duration);
        anim.setRepeatMode(Animation.REVERSE);
        anim.setRepeatCount(5);
        view.startAnimation(anim);
        return view;
    }

    public void startParentalControlButtonButton(View v){//KYLE PROSPERT
        //watchDog starts the Alarm, Watching the service
        createPin();
    }
    public void stopParentalControlButtonButton(View v){//KYLE PROSPERT
        askForPin();
    }

    @Override
    public void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        // Restore the previously serialized current dropdown position.
        super.onRestoreInstanceState(savedInstanceState);
    }

    public void checkAllBox(View view){
        ToggleButton activitiesShownToggle = findViewById(R.id.activitiesToggle);
        CheckBox toggleButton = (CheckBox) view;
        for(ViewGroup vg:ParentalControlService.packagesViewGroupList){
            if(!((TextView)vg.getChildAt(2)).getText().toString().equals(MainActivity.activity.getApplicationContext().getPackageName())){
                ((CheckBox)vg.getChildAt(0)).setChecked(toggleButton.isChecked());
                vg.getChildAt(0).callOnClick();
                vg.getChildAt(0).callOnClick();
            }
        }
        if(activitiesShownToggle.isChecked()){
            for(ViewGroup vg:ParentalControlService.activitiesViewGroupList){
                ((CheckBox)vg.getChildAt(0)).setChecked(toggleButton.isChecked());
                vg.getChildAt(0).callOnClick();
                vg.getChildAt(0).callOnClick();
            }
        }
    }




    @Override
    protected void onResume() {
        super.onResume();
        //stopParentalControlButtonButton(null);//always stop when resuming
    }

    public void activitiesToggle(View view){
        ToggleButton tg = (ToggleButton) view;
        for(ViewGroup vg:ParentalControlService.activitiesViewGroupList){
            if(tg.isChecked()){
                vg.setVisibility(View.VISIBLE);
            }else{
                vg.setVisibility(View.GONE);
            }
        }
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
