
package com.kapcode.parentalcontrols;
//Author Of File KYLE PROSPERT
        import android.content.BroadcastReceiver;
        import android.content.Context;
        import android.content.Intent;
        import android.widget.Toast;

public class MyAlarmReceiver extends BroadcastReceiver {//MADE NOT PUBLIC, CAUSE OF FUTURE ERROR?

    @Override
    public void onReceive(Context context, Intent intent) {
        Toast.makeText(context, "Alarm went off", Toast.LENGTH_SHORT).show();
        //user didn't stop it, and it is not running, android must have stopped it
        if(!ParentalControlService.serviceStoppedByUser.get() && !ParentalControlService.serviceIsRunning.get()){
            //Restart It Here
            System.out.println("Restart Code Here TODO");
            ParentalControlService.startService(context);
        }else{
            System.out.println("All Good");
        }
        //start watchdog again.
        ParentalControlService.watchDog(ParentalControlService.alarmManager,context);

    }


}