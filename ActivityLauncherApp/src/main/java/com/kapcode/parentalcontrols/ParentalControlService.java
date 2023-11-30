package com.kapcode.parentalcontrols;
//Author Of File KYLE PROSPERT

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.IBinder;
import android.icu.util.Calendar;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

import de.szalkowski.activitylauncher.R;

public class ParentalControlService extends Service {
    static final int pollRate = 2000;
    public static ArrayList<ViewGroup> activitiesViewGroupList,packagesViewGroupList;
    public static String AMAZON_FIRE_LAUNCHER = "com.amazon.firelauncher/com.amazon.firelauncher.Launcher",
            NOVA_LAUNCHER="com.teslacoilsw.launcher/com.teslacoilsw.launcher.NovaLauncher";

    public static String DEFAULT_LAUNCHER = NOVA_LAUNCHER;
    public static ComponentName launcher =ComponentName.unflattenFromString(DEFAULT_LAUNCHER);

    private static ArrayList<MyPackageInfo> blocked_packages = new ArrayList<>();
    private static HashMap<String, MyPackageInfo> blocked_packages_map = new HashMap<>();

    static HashMap<String,Boolean> taskMap_BLOCKED = new HashMap<>();
    public static List<MyPackageInfo> all_packages;//is set at end of loading in all packages AllTaskListAddapter.resolve(AllTasksListAsyncProvider.Updater updater)
    static PendingIntent pendingWatchDogIntent;
    static Intent watchDogIntent;
    public static AlarmManager alarmManager;
    public static Service s;
    static volatile AtomicBoolean serviceIsRunning = new AtomicBoolean(false);
    static volatile AtomicBoolean serviceStoppedByUser = new AtomicBoolean(false);
    static volatile Thread serviceThread;
    //NEW IMPORTS //TODO WEED OUT
    static PackageManager pm;
    private static ArrayList<String> failedPackagesList;
    LayoutInflater inflater;
    static SharedPreferences prefs;
    static List<MyPackageInfo> packages;

    @Override
    public void onStart(Intent intent, int startId) {
        System.out.println("Started");
        s = this;
        super.onStart(intent, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        System.out.println("BIND");
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        System.out.println("Start");
        serviceStoppedByUser.set(false);
        serviceIsRunning.set(true);
        System.out.println("Service Create");
        // Create the Foreground Service
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? createNotificationChannel(notificationManager) : "";
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId);
        Notification notification = notificationBuilder.setOngoing(true)
                .setPriority(Notification.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_SERVICE).setContentText("Text")
                .build();
        startForeground(ID_SERVICE, notification);
        super.onCreate();
        serviceThread = new Thread(new Runnable() {
            @Override
            public void run() {
                int loop = 10000;//test watchdog
                while (serviceIsRunning.get()) { //loop counting down to 0 is simulating android killing off service, or end of work,
                    // you can use this to test watch dog, or to simulate end of work
                    System.out.println(loop);
                    loop--;
                    try {//should block plex app.
                        String task = getForegroundTask();
                        System.out.println(task);

                        if (task != null && taskMap_BLOCKED.get(task)) {
                            //the value is a boolean, true means app is blocked.
                            //Launcher.launchActivity(s.getApplication().getApplicationContext(), launcher);
                            //false means app is okay.
                            goHome();
                            System.out.println("Going home!");
                        }

                        //chosen from ui, derived from ComponentName.unflattenFromString()
                        //blocked_packages_map.get("");
                        //go to com.amazon.firelauncher.Launcher .. amazon homescreen
                        printForegroundTask();
                        Thread.sleep(pollRate);

                    } catch (InterruptedException e) {

                    }

                }

                serviceIsRunning.set(false);
                s.stopService(intent);
            }

        });
        serviceThread.start();


        return START_STICKY;
    }

    public static void goHome(){
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        s.getApplication().getApplicationContext().startActivity(startMain);
    }
    // Constants
    private static final int ID_SERVICE = 10129;

    @Override
    public void onCreate() {
        //super.onCreate();
        // do stuff like register for BroadcastReceiver, etc.

    }

    //requires API Level O ... 26 ... This is projects min sdk
    @SuppressLint("NewApi")
    private String createNotificationChannel(NotificationManager notificationManager) {
        String channelId = "my_service_channelid";
        String channelName = "My Foreground Service";
        NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
        // omitted the LED color
        channel.setImportance(NotificationManager.IMPORTANCE_HIGH);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        notificationManager.createNotificationChannel(channel);
        return channelId;
    }

    public String getForegroundTask() {
        String currentApp = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            UsageStatsManager usm = (UsageStatsManager) this.getSystemService(Context.USAGE_STATS_SERVICE);
            long time = System.currentTimeMillis();
            List<UsageStats> appList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 1000, time);
            if (appList != null && appList.size() > 0) {
                SortedMap<Long, UsageStats> mySortedMap = new TreeMap<Long, UsageStats>();
                for (UsageStats usageStats : appList) {
                    mySortedMap.put(usageStats.getLastTimeUsed(), usageStats);
                }
                if (mySortedMap != null && !mySortedMap.isEmpty()) {
                    currentApp = mySortedMap.get(mySortedMap.lastKey()).getPackageName();
                }
            }
        } else {
            ActivityManager am = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> tasks = am.getRunningAppProcesses();
            currentApp = tasks.get(0).processName;
        }
        return (currentApp);
    }

    private void printForegroundTask() {//Settings -> Security -> (Scroll down to last) Apps with usage access -> Give the permissions to our app
        System.out.println(getForegroundTask());
    }

    public void printRunningProcesses() {//returns only this app
        ActivityManager manager =
                (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> processes = manager.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo process : processes) {
            System.out.println(process.processName);
        }
    }

    public static void startService(Context context) {
        // <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
        // <service android:enabled="true" android:name="de.szalkowski.activitylauncher.ParentalControlService" />
        stopService();
        ParentalControlService.serviceIsRunning.set(true);
        Intent ServiceIntent = new Intent(context, ParentalControlService.class);
        context.startService(ServiceIntent);
    }


    @SuppressLint("NewApi")
    public static void watchDog(AlarmManager alarmManager, Context context) {
        watchDogIntent = new Intent(context, MyAlarmReceiver.class);
        pendingWatchDogIntent = PendingIntent.getBroadcast(context, 0, watchDogIntent, PendingIntent.FLAG_MUTABLE);
        Calendar time = Calendar.getInstance();
        time.setTimeInMillis(System.currentTimeMillis());
        time.add(Calendar.SECOND, 10);
        alarmManager.set(AlarmManager.RTC_WAKEUP, time.getTimeInMillis(), pendingWatchDogIntent);
    }


    public static void stopService() {// button press
        ParentalControlService.serviceStoppedByUser.set(true);
        if (pendingWatchDogIntent != null) pendingWatchDogIntent.cancel();
        ParentalControlService.serviceIsRunning.set(false);
        if (ParentalControlService.serviceThread != null) {
            ParentalControlService.serviceThread.interrupt();
            try {
                ParentalControlService.serviceThread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

        }
    }

    public static void resolve(Context context) {
        MainActivity.layoutToPutInstalledAppInfoInto.removeAllViews();
        pm = context.getPackageManager();
        PackageManagerCache cache = PackageManagerCache.getPackageManagerCache(pm);
        List<PackageInfo> all_packages = pm.getInstalledPackages(0);

        Configuration locale = SettingsUtils.createLocaleConfiguration("English");
        packages = new ArrayList<>(all_packages.size());
        ArrayList<String> failedPackagesList = new ArrayList<>();
        for (int i = 0; i < all_packages.size(); ++i) {
            PackageInfo pack = all_packages.get(i);
            MyPackageInfo mypack;

            mypack = cache.getPackageInfo(pack.packageName, locale);
            if(mypack==null){

                
               // try {  //No name on raw html, all in java script :(
                    //String html = HTML.downloadHTML("https://play.google.com/store/apps/details?id=" + pack.packageName, 3000);
                    //System.out.println(pack.packageName+"\n"+html);
                //} catch (InterruptedException e) {
                    
                //}
                

                    /*
                    one option is to grab the app name from the web off the play store.
                    */

                failedPackagesList.add(pack.packageName);
            }else{
                if (mypack.getActivitiesCount() > 0) {
                    packages.add(mypack);
                }
            }


        }
        Collections.sort(packages);
        ParentalControlService.failedPackagesList =failedPackagesList;
        ParentalControlService.all_packages = packages;
        MainActivity.handler.post(new Runnable() {
            @Override
            public void run() {
                ParentalControlService.populateLayoutWithPackageInformation(MainActivity.activity);
                MainActivity.progressBar(false);
            }
        });
    }

    public static void populateLayoutWithPackageInformation(Context context) {//use loaded packages information to populate bottom layout
        activitiesViewGroupList=new ArrayList<>();
        packagesViewGroupList=new ArrayList<>();


        for (MyPackageInfo myPackageInfo : all_packages) {
            //do not allow this app to be blocked by itself.



            MyActivityInfo[] activities = myPackageInfo.activities;
             System.out.println("" + myPackageInfo.package_name);
            taskMap_BLOCKED.put(myPackageInfo.package_name.toString(),false);

            Drawable drawable = null;
            LinearLayout layout = new LinearLayout(context);
            layout.setOrientation(LinearLayout.HORIZONTAL);
            switch (MainActivity.mainActivity.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) {
                case Configuration.UI_MODE_NIGHT_YES:
                    //process
                    //layout.setBackgroundColor(Color.DKGRAY);
                    //MainActivity.mainActivity.findViewById(R.id.ll_scoll_horiz).setBackgroundColor(Color.DKGRAY);
                    break;
                case Configuration.UI_MODE_NIGHT_NO:
                    // process
                    //layout.setBackgroundColor(Color.WHITE);
                    break;
            }

            ImageView iv = new ImageView(context);
            try {
                drawable = context.getPackageManager().getApplicationIcon(myPackageInfo.package_name);
            } catch (PackageManager.NameNotFoundException e) {

            }
            TextView packageTextView = new TextView(context);
            packageTextView.setMinWidth(MainActivity.screenWidth);
            packageTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP,16);
            packageTextView.setText(myPackageInfo.package_name);

            CheckBox checkBox = new CheckBox(context);
            layout.addView(checkBox);

            layout.addView(iv);



            TextView applicationNameTextView = new TextView(context);
            applicationNameTextView.setText(myPackageInfo.name+"   ");
            applicationNameTextView.setTypeface(null, Typeface.BOLD);
            applicationNameTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP,16);
            layout.addView(applicationNameTextView);
            layout.addView(packageTextView);
            MainActivity.layoutToPutInstalledAppInfoInto.addView(layout);
            packagesViewGroupList.add(layout);
            if (drawable != null) {
                iv.setImageDrawable(drawable);
                LinearLayout.LayoutParams ivlp = (LinearLayout.LayoutParams) iv.getLayoutParams();
                ivlp.height = 50;
                ivlp.width = 50;
                iv.setLayoutParams(ivlp);

            }
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) layout.getLayoutParams();


            String fileNameA = myPackageInfo.package_name.toString();//todo better variable names
            String contentA = MainActivity.readFromFile(fileNameA,context);


            if(myPackageInfo.package_name.equals(MainActivity.activity.getApplicationContext().getPackageName())){
                checkBox.setVisibility(View.INVISIBLE);
            }else{
                iv.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        taskMap_BLOCKED.put(myPackageInfo.package_name.toString(),!checkBox.isChecked());
                        checkBox.setChecked(!checkBox.isChecked());
                        String bs = Boolean.toString(checkBox.isChecked());
                        MainActivity.writeToFile(fileNameA,fileNameA+"\n"+bs,context);

                    }
                });
                packageTextView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        taskMap_BLOCKED.put(myPackageInfo.package_name.toString(),!checkBox.isChecked());
                        checkBox.setChecked(!checkBox.isChecked());
                        String bs = Boolean.toString(checkBox.isChecked());
                        MainActivity.writeToFile(fileNameA,fileNameA+"\n"+bs,context);
                    }
                });

                checkBox.setOnClickListener(view -> {
                    taskMap_BLOCKED.put(myPackageInfo.package_name.toString(),checkBox.isChecked());
                    String bs = Boolean.toString(checkBox.isChecked());
                    MainActivity.writeToFile(fileNameA,fileNameA+"\n"+bs,context);
                });
            }







            if(contentA!=null){
                checkBox.setChecked(Boolean.parseBoolean(contentA.split("\n")[2].replace(" ","")));
                taskMap_BLOCKED.put(myPackageInfo.package_name.toString(),checkBox.isChecked());
            }




            for (int i = 0; i < activities.length; i++) {
                if (!activities[i].is_private) {
                    TextView activityTextView = new TextView(context);
                    //activityTextView.setMinWidth(MainActivity.screenWidth);
                    activityTextView.setText(activities[i].component_name.flattenToString());
                    taskMap_BLOCKED.put(activities[i].component_name.flattenToString(),false);
                    Drawable activityDrawable = null;
                    LinearLayout activityLayout = new LinearLayout(context);
                    activityLayout.setOrientation(LinearLayout.HORIZONTAL);
                    ImageView activityImageView = new ImageView(context);
                    Button launchActivityButton = new Button(context);
                    launchActivityButton.setText("launch");
                    int finalI1 = i;
                    launchActivityButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {

                            new AlertDialog.Builder(context)
                                    .setTitle("Launch this activity?")
                                    .setMessage("This will start a new app, and take you out of this app!\n" +
                                            "Do you want to proceed?")
                                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                            Launcher.launchActivity(context, activities[finalI1].component_name);
                                        }
                                    })
                                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            dialogInterface.cancel();
                                        }
                                    })
                                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                                        @Override
                                        public void onDismiss(DialogInterface dialogInterface) {
                                            dialogInterface.cancel();
                                        }
                                    })
                                    // A null listener allows the button to dismiss the dialog and take no further action.
                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                    .show();
                        }
                    });
                    try {
                        activityDrawable = context.getPackageManager().getApplicationIcon(myPackageInfo.package_name);
                    } catch (PackageManager.NameNotFoundException e) {

                    }
                    CheckBox activityCheckBox = new CheckBox(context);
                    activityLayout.addView(activityCheckBox);
                    activityLayout.addView(activityImageView);
                    activityLayout.addView(launchActivityButton);
                    activityLayout.addView(activityTextView);

                    switch (MainActivity.mainActivity.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) {
                        case Configuration.UI_MODE_NIGHT_YES:
                            //process
                            //activityLayout.setBackgroundColor(Color.DKGRAY);
                            break;
                        case Configuration.UI_MODE_NIGHT_NO:
                            // process
                            //layout.setBackgroundColor(Color.WHITE);
                            break;
                    }

                    MainActivity.layoutToPutInstalledAppInfoInto.addView(activityLayout);
                    activitiesViewGroupList.add(activityLayout);


                    if (activityDrawable != null) {
                        activityImageView.setImageDrawable(activityDrawable);
                        LinearLayout.LayoutParams ivlp = (LinearLayout.LayoutParams) activityImageView.getLayoutParams();
                        ivlp.height = 50;
                        ivlp.width = 50;
                        activityImageView.setLayoutParams(ivlp);
                    }
                    LinearLayout.LayoutParams activity_lp = (LinearLayout.LayoutParams) layout.getLayoutParams();

                    final String fileNameB = activities[i].component_name.flattenToString().replaceAll("/","_");// todo better variable names
                    String contentB = MainActivity.readFromFile(fileNameB,context);

                    int finalI = i;
                    activityImageView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            taskMap_BLOCKED.put(activities[finalI].component_name.flattenToString(),!activityCheckBox.isChecked());
                            activityCheckBox.setChecked(!activityCheckBox.isChecked());
                            String bs = Boolean.toString(activityCheckBox.isChecked());
                            MainActivity.writeToFile(fileNameB,fileNameB+"\n"+bs,context);

                        }
                    });
                    activityTextView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            taskMap_BLOCKED.put(activities[finalI].component_name.flattenToString(),!activityCheckBox.isChecked());
                            activityCheckBox.setChecked(!activityCheckBox.isChecked());
                            String bs = Boolean.toString(activityCheckBox.isChecked());
                            MainActivity.writeToFile(fileNameB,fileNameB+"\n"+bs,context);

                        }
                    });



                    activityCheckBox.setOnClickListener(view -> {
                        taskMap_BLOCKED.put(activities[finalI].component_name.flattenToString(),activityCheckBox.isChecked());
                        String bs = Boolean.toString(activityCheckBox.isChecked());
                        MainActivity.writeToFile(fileNameB,fileNameB+"\n"+bs,context);
                    });

                    if(contentB!=null){
                        activityCheckBox.setChecked(Boolean.parseBoolean(contentB.split("\n")[2]));
                        // System.out.println(contentB.split("\n")[2]);


                    }
                    activityLayout.setVisibility(View.GONE);//hide activity layout on start.

                }


            }

        }

        for(String failedPackage:failedPackagesList){
            LinearLayout layout = new LinearLayout(context);
            layout.setOrientation(LinearLayout.HORIZONTAL);
            ImageView iv = new ImageView(context);
            Drawable drawable = null;
            try {
                drawable = context.getPackageManager().getApplicationIcon(failedPackage);
            } catch (PackageManager.NameNotFoundException e) {

            }
            TextView packageTextView = new TextView(context);
            packageTextView.setMinWidth(MainActivity.screenWidth);
            packageTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP,16);
            packageTextView.setText(failedPackage);

            CheckBox checkBox = new CheckBox(context);
            layout.addView(checkBox);
            switch (MainActivity.mainActivity.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) {
                case Configuration.UI_MODE_NIGHT_YES:
                    //process
                    //layout.setBackgroundColor(Color.DKGRAY);
                    break;
                case Configuration.UI_MODE_NIGHT_NO:
                    // process
                    //layout.setBackgroundColor(Color.WHITE);
                    break;
            }

            layout.addView(iv);
            TextView applicationNameTextView = new TextView(context);
            applicationNameTextView.setText("NAME_NOT_FOUND"+"   ");
            applicationNameTextView.setTextColor(Color.RED);
            applicationNameTextView.setTypeface(null, Typeface.BOLD);
            applicationNameTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP,16);
            layout.addView(applicationNameTextView);




            layout.addView(packageTextView);
            MainActivity.layoutToPutInstalledAppInfoInto.addView(layout);
            packagesViewGroupList.add(layout);
            if (drawable != null) {
                iv.setImageDrawable(drawable);
                LinearLayout.LayoutParams ivlp = (LinearLayout.LayoutParams) iv.getLayoutParams();
                ivlp.height = 50;
                ivlp.width = 50;
                iv.setLayoutParams(ivlp);

            }
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) layout.getLayoutParams();


            String fileNameA = failedPackage;//todo better variable names
            String contentA = MainActivity.readFromFile(fileNameA,context);


            if(failedPackage.equals(MainActivity.activity.getApplicationContext().getPackageName())){
                checkBox.setVisibility(View.INVISIBLE);
            }else{
                iv.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        taskMap_BLOCKED.put(failedPackage,!checkBox.isChecked());
                        checkBox.setChecked(!checkBox.isChecked());
                        String bs = Boolean.toString(checkBox.isChecked());
                        MainActivity.writeToFile(fileNameA,fileNameA+"\n"+bs,context);

                    }
                });
                packageTextView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        taskMap_BLOCKED.put(failedPackage,!checkBox.isChecked());
                        checkBox.setChecked(!checkBox.isChecked());
                        String bs = Boolean.toString(checkBox.isChecked());
                        MainActivity.writeToFile(fileNameA,fileNameA+"\n"+bs,context);
                    }
                });

                checkBox.setOnClickListener(view -> {
                    taskMap_BLOCKED.put(failedPackage,checkBox.isChecked());
                    String bs = Boolean.toString(checkBox.isChecked());
                    MainActivity.writeToFile(fileNameA,fileNameA+"\n"+bs,context);
                });
            }







            if(contentA!=null){
                checkBox.setChecked(Boolean.parseBoolean(contentA.split("\n")[2].replace(" ","")));
                taskMap_BLOCKED.put(failedPackage,checkBox.isChecked());
            }

        }



    }




}