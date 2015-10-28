package edu.cs4730.tapremote;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in {@link TapWidgetConfigureActivity TapWidgetConfigureActivity}
 */
public class TapWidget extends AppWidgetProvider {
    private static final String ButtonClick1 = "ButtonClickTag1";
    private static final String ButtonClick2 = "ButtonClickTag2";
    private static final String TextClick = "TextClickTag1";
    private static final String MESSAGE = "Message";
    private static final String TAG = "widget";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        final int N = appWidgetIds.length;
        for (int i = 0; i < N; i++) {
            updateAppWidget(context, appWidgetManager, appWidgetIds[i]);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        // When the user deletes the widget, delete the preference associated with it.
        final int N = appWidgetIds.length;
        for (int i = 0; i < N; i++) {
            TapWidgetConfigureActivity.deleteTitlePref(context, appWidgetIds[i]);
        }
    }


    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        String widgetText = TapWidgetConfigureActivity.loadTitlePref(context, appWidgetId);
        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.tap_widget);
        views.setTextViewText(R.id.appwidget_text, widgetText);
        //setup actions for the two buttons and textview as too.
        if (widgetText.compareTo("DC") != 0) {// if we are connected setup buttons,
            views.setOnClickPendingIntent(R.id.button, getPendingSelfIntent(context, ButtonClick1, appWidgetId));
            views.setOnClickPendingIntent(R.id.button2, getPendingSelfIntent(context, ButtonClick2, appWidgetId));
        }
        //setup text for the config activity for connections
        Intent Buttonintent = new Intent(context, TapWidgetConfigureActivity.class);
        //match the info the activity expects to see.
        Buttonintent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        PendingIntent ButtonpendingIntent = PendingIntent.getActivity(context, 0, Buttonintent, 0);
        //views.setOnClickPendingIntent(R.id.appwidget_text, getPendingSelfIntent(context, TextClick, appWidgetId));
        views.setOnClickPendingIntent(R.id.appwidget_text, ButtonpendingIntent);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    static protected PendingIntent getPendingSelfIntent(Context context, String action, int appWidgetId) {
        Intent intent = new Intent(context, TapWidget.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);  //incase we need it for preferences.
        intent.setAction(action);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        //now we check to see which of the actions and then respond.
        //most the buttons just show static info.
        //the textview will get the id and toast it's name.
        if (ButtonClick1.equals(intent.getAction())) {
            //send service a write message for short.
            Intent i = new Intent(context, MyNetworkService.class);
            i.putExtra(myConstants.KEY_CMD, myConstants.CMD_WRITE);
            i.putExtra(myConstants.KEY_MSG, "S");
            context.startService(i);

            Toast.makeText(context, "Button1", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "Clicked button1");
        } else if (ButtonClick2.equals(intent.getAction())) {
            //send service a write message for short.
            Intent i = new Intent(context, MyNetworkService.class);
            i.putExtra(myConstants.KEY_CMD, myConstants.CMD_WRITE);
            i.putExtra(myConstants.KEY_MSG, "L");
            context.startService(i);
            Toast.makeText(context, "Button2", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "Clicked button2");
        } else if (MESSAGE.equals(intent.getAction())) {
            //we have received a message.
            Bundle extras = intent.getExtras();
            if (extras != null) {
                String msg = extras.getString(myConstants.KEY_MSG, "");
                int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                        AppWidgetManager.INVALID_APPWIDGET_ID);
                switch (msg) {
                    case "S":  //short message
                        vibrate(context, 1); //short
                        break;
                    case "L": //long message
                        vibrate(context, 2); //long
                        break;
                    case myConstants.MSG_CONNECT: //we are connected
                        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                            TapWidgetConfigureActivity.saveTitlePref(context, appWidgetId, "C");
                        }
                        break;
                    case myConstants.MSG_DISCONNECT: //disconnected
                        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                            TapWidgetConfigureActivity.saveTitlePref(context, appWidgetId, "DC");
                        }

                        break;
                    default:
                        Log.v(TAG, "message is " + msg);
                }
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                TapWidget.updateAppWidget(context, appWidgetManager, appWidgetId);
            }
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
        Intent i = new Intent(context, MyNetworkService.class);
        context.stopService(i);
    }

    public void vibrate(Context context, int time) {
        Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        // from http://www.learn-android-easily.com/2012/11/how-to-vibrate-android-phone.html
        // pass the number of millseconds fro which you want to vibrate the phone here we
        // have passed 2000 so phone will vibrate for 2 seconds.

        v.vibrate(time * 500);   //time * half a second.

        // If you want to vibrate  in a pattern
        //  long pattern[]={0,800,200,1200,300,2000,400,4000};
        //this is pattern in which we want to Vibrate the Phone
        //first 0  means silent for 0 milisecond
        //800 means vibrate for 800 milisecond
        //200 means  means silent for 200 milisecond
        //1200  means vibrate for 1200 miliseconds
        // 2nd argument is for repetition pass -1 if you do not want to repeat the Vibrate
        // v.vibrate(pattern,-1);
    }

}