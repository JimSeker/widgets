package edu.cs4730.widgetdemo2;

import java.util.Random;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

public class Widgetd2Provider extends AppWidgetProvider {

    int randnum = 100;  //default value
    private static final String PREF_PREFIX_KEY = "appwidget_";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        // if there may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    /**
     * do all the heavy lifting to update the widget here.
     */
    void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        //first get the preference for this widget that is the top number.
        SharedPreferences preferences = context.getSharedPreferences("widgetDemo2", Context.MODE_PRIVATE);

        //Now get the preference for this widget, stored using it's id number.   The default value is 100, if not found.
        randnum = preferences.getInt(PREF_PREFIX_KEY + appWidgetId, 100);

        // Now create get the random number for it.
        int number = (new Random().nextInt(randnum));

        //finally update the widget view.
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
        //Log.w("WidgetExample", String.valueOf(number));
        // Set the text
        remoteViews.setTextViewText(R.id.update, String.valueOf(number));

        // Register an onClickListener
        Intent intent = new Intent(context, Widgetd2Provider.class);

        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);

        //if I wanted every widget to update use this section and comment out the one below.
        //ComponentName thisWidget = new ComponentName(context, Example.class);
        //int[] ids = appWidgetManager.getAppWidgetIds(thisWidget);

        //if I want only this one to update, then use this code.
        int[] ids = {appWidgetId};

        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        PendingIntent pendingIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            pendingIntent = PendingIntent.getBroadcast(context, appWidgetId, intent, PendingIntent.FLAG_MUTABLE);
        } else {
            pendingIntent = PendingIntent.getBroadcast(context, appWidgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        //where update is the view and is clickable.  That way, it the widget will update when clicked.
        //in this case get a new random number.
        remoteViews.setOnClickPendingIntent(R.id.update, pendingIntent);
        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, remoteViews);

    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        // When the user deletes the widget, delete the preference associated with it.
        //there maybe more then one, so loop.
        SharedPreferences.Editor editor = context.getSharedPreferences("widgetDemo2", Context.MODE_PRIVATE).edit();

        final int N = appWidgetIds.length;
        for (int i = 0; i < N; i++) {
            editor.remove(PREF_PREFIX_KEY + i);
        }

        editor.commit();  //finished, comment the preferences.
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
        //in this case, nothing is necessary, since it is handled for every widget.
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
        //it's all handled by the onDelete, so there is nothing here.
    }
}
