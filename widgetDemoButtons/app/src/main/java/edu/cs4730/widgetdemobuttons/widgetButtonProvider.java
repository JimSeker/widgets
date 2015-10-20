package edu.cs4730.widgetdemobuttons;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in {@link widgetButtonProviderConfigureActivity widgetButtonProviderConfigureActivity}
 */
public class widgetButtonProvider extends AppWidgetProvider {

    private static final String ButtonClick1 = "ButtonClickTag1";
    private static final String ButtonClick2 = "ButtonClickTag2";
    private static final String TextClick = "TextClickTag1";


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
            widgetButtonProviderConfigureActivity.deleteTitlePref(context, appWidgetIds[i]);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {

        CharSequence widgetText = widgetButtonProviderConfigureActivity.loadTitlePref(context, appWidgetId);
        // Construct the RemoteViews object
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_button_provider);
        remoteViews.setTextViewText(R.id.appwidget_text, widgetText);

        //setup actions for the two buttons and textview as too.
        remoteViews.setOnClickPendingIntent(R.id.button, getPendingSelfIntent(context, ButtonClick1, appWidgetId));
        remoteViews.setOnClickPendingIntent(R.id.button2, getPendingSelfIntent(context, ButtonClick2, appWidgetId));
        remoteViews.setOnClickPendingIntent(R.id.appwidget_text, getPendingSelfIntent(context, TextClick, appWidgetId));
        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
    }

    static protected PendingIntent getPendingSelfIntent(Context context, String action, int appWidgetId) {
        Intent intent = new Intent(context, widgetButtonProvider.class);
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

            Toast.makeText(context, "Button1", Toast.LENGTH_SHORT).show();
            Log.w("Widget", "Clicked button1");
        } else if (ButtonClick2.equals(intent.getAction())) {
            Toast.makeText(context, "Button2", Toast.LENGTH_SHORT).show();
            Log.w("Widget", "Clicked button2");
        } else if (TextClick.equals(intent.getAction())) {
            Bundle extras = intent.getExtras();
            String widgetText = "Textivew"; //default
            if (extras != null) {
                //Log.w("Widget", "Bundle is not NULL");
                int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                        AppWidgetManager.INVALID_APPWIDGET_ID);
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    //Log.w("Widget", "widget id is ok");
                    widgetText = widgetButtonProviderConfigureActivity.loadTitlePref(context, appWidgetId);
                }
            }
            Toast.makeText(context, widgetText, Toast.LENGTH_SHORT).show();
            Log.w("Widget", "TV: " + widgetText);
        }
    }
}

