/**
 * Copyright 2014 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rp.soi.dmsd.notextingwhilewalking;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Display;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import rp.soi.dmsd.notextingwhilewalking.*;
import rp.soi.dmsd.notextingwhilewalking.Constants;
import rp.soi.dmsd.notextingwhilewalking.MainActivity;

import java.util.ArrayList;

/**
 *  IntentService for handling incoming intents that are generated as a result of requesting
 *  activity updates using
 *  {@link com.google.android.gms.location.ActivityRecognitionApi#requestActivityUpdates}.
 */
public class DetectedActivitiesIntentService extends IntentService implements SensorEventListener {


    protected static final String TAG = "DetectedActivitiesIS";
    private static final int WEARABLE_NOTIFICATION_ID = 777;
    Intent mIntent;
    SensorManager mSensorManager;
    Sensor mOrientation;
    //UserPresentBroadcastReceiver userPresentBroadcastReceiver;
    float azi, pitch, roll;
    static boolean isPhoneFacingUp = false;

    /**
     * This constructor is required, and calls the super IntentService(String)
     * constructor with the name for a worker thread.
     */
    public DetectedActivitiesIntentService() {
        // Use the TAG to name the worker thread.
        super(TAG);
    }


    @Override
    public void onCreate() {

        super.onCreate();

    }

    /**
     * Handles incoming intents.
     * @param intent The Intent is provided (inside a PendingIntent) when requestActivityUpdates()
     *               is called.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mOrientation = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        mSensorManager.registerListener(this,mOrientation,SensorManager.SENSOR_DELAY_NORMAL);
        PowerManager powerManager = (PowerManager)getSystemService(Context.POWER_SERVICE);


        ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
        Intent localIntent = new Intent(rp.soi.dmsd.notextingwhilewalking.Constants.BROADCAST_ACTION);

        // Get the list of the probable activities associated with the current state of the
        // device. Each activity is associated with a confidence level, which is an int between
        // 0 and 100.
        ArrayList<DetectedActivity> detectedActivities = (ArrayList) result.getProbableActivities();

        // Log each activity.
        Log.i(TAG, "activities detected");
        for (DetectedActivity da: detectedActivities) {
            Log.i(TAG, rp.soi.dmsd.notextingwhilewalking.Constants.getActivityString(
                            getApplicationContext(),
                            da.getType()) + " " + da.getConfidence() + "%"
            );
            // trigger a notification if the walking activity has a confidence of > 50%



            //float[] xyz = mSensorManager.getOrientation();

            if (da.getType()== DetectedActivity.WALKING && da.getConfidence() > 15){

                // For API 20 and higher
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
                    Log.i(TAG, "Android version is HIGHER than 20 ");
                    if (powerManager.isInteractive()){
                        Log.i(TAG, "Screen is ON");
                        if(isPhoneFacingUp){
                            Log.i(TAG, "Phone is facing UP.");
                            createNotification(true);
                        }
                        else{
                            Log.i(TAG, "Phone is facing DOWN.");
                        }
                    }else{
                        Log.i(TAG,"Screen is OFF");
                    }
                } else{
                    Log.i(TAG, "Android version is LOWER than 20 ");
                    if(powerManager.isScreenOn()) {
                        Log.i(TAG, "Screen is ON");
                        if(isPhoneFacingUp){
                            Log.i(TAG, "Phone is facing UP.");
                            createNotification(true);
                        }else{
                            Log.i(TAG, "Phone is facing DOWN.");
                        }
                    }else{
                        Log.i(TAG,"Screen is OFF");
                    }
                }
            }


        }

        // Broadcast the list of detected activities.
        localIntent.putExtra(Constants.ACTIVITY_EXTRA, detectedActivities);
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }



    public void createNotification(boolean makeHeadsUpNotification) {

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_priority_high_black_24dp)
                .setPriority(Notification.PRIORITY_DEFAULT)
                .setCategory(Notification.CATEGORY_MESSAGE)
                .setContentTitle("No-Texting-While-Walking")
                .setContentText("It is hazardous to be texting while walking.");

        if (makeHeadsUpNotification) {

            Intent push = new Intent(getApplication(), MainActivity.class);
            push.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            //push.setClass(this, MainActivity.class);

            PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(getApplicationContext(), 0,
                    push, PendingIntent.FLAG_CANCEL_CURRENT);
            notificationBuilder
                    .setContentText("It is hazardous to be texting while walking.")
                    .setFullScreenIntent(fullScreenPendingIntent, true);
        }
        Notification notification = notificationBuilder.build();
        //---set the sound and lights---
        notification.defaults |= Notification.DEFAULT_SOUND;
        notification.defaults |= Notification.DEFAULT_LIGHTS;

        //---gets an instance of the NotificationManager service---
        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(this);

        //---build the notification and issues it
        // with notification manager---
        notificationManager.notify(WEARABLE_NOTIFICATION_ID, notification);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float azimuth_angle = event.values[0];
        float pitch_angle = event.values[1];
        float roll_angle = event.values[2];

        //Log.i(TAG, "Orientation Sensor Changed");
        //Log.i(TAG, "Azimuth = " + azimuth_angle);
        //Log.i(TAG, "Pitch = " + pitch_angle); //Facing up 0 to -90
        //Log.i(TAG, "Roll = " + roll_angle); // Facing up -45 to 45
        if((pitch_angle >= -90 && pitch_angle <=0) && (roll_angle >= -45 && roll_angle <= 45)){
            //Log.i(TAG, "Phone is facing UP.");
            isPhoneFacingUp = true;
        }else{
            //Log.i(TAG, "Phone is facing DOWN.");
            isPhoneFacingUp = false;
        }


    }

    /*public class UserPresentBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(Intent.ACTION_SCREEN_OFF)){
                Log.i(TAG,"Detected Screen OFF");
            }
            if(intent.getAction().equals(Intent.ACTION_SCREEN_ON)){
                Log.i(TAG,"Detected Screen ON");
            }
            if(intent.getAction().equals(Intent.ACTION_USER_PRESENT)){
                Log.i(TAG,"Detected User unlock screen");
            }
        }
    }*/


}
