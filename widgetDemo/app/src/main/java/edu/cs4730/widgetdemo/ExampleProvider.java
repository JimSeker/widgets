package edu.cs4730.widgetdemo;

import java.util.Random;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.RequiresApi;

//http://www.vogella.com/articles/AndroidWidgets/article.html
//http://code4reference.com/2012/07/android-widget-tutorial/
//http://developer.android.com/guide/topics/appwidgets/index.html
//http://developer.android.com/guide/practices/ui_guidelines/widget_design.html

/**
 * So this is actually the widget and it updater.
 * There are a couple of calls to static functions in the exampleConfActivity, so the
 * preferences can be stored/restored/deleted.
 */


public class ExampleProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        // if there may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    /**
     * This is where the actual work is done.   It is called from onUpdate for each
     * (homescreen) widget to update.   likely only one, but below we an change that functionality.
     */

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {

        // Create some random number from the shared preference number stored in the configure activity.
        int number = (new Random().nextInt(
            exampleConfActivity.loadTitlePref(context, appWidgetId)  //get the number via the shared preferences.
        ));

        // Construct the RemoteViews object
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.examplewidget);
        Log.w("WidgetExample", String.valueOf(number));

        // Set the text
        remoteViews.setTextViewText(R.id.update, String.valueOf(number));

        // Register an onClickListener
        Intent intent = new Intent(context, ExampleProvider.class);

        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);

        //if I wanted every widget to update use this section and comment out the one below.
        //ComponentName thisWidget = new ComponentName(context, Example.class);
        //int[] ids = appWidgetManager.getAppWidgetIds(thisWidget);

        //if I want only this one to update, then use this code.
        int[] ids = {appWidgetId};

        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        //intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);  //this never worked correctly.
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
        // When the user deletes the widget, delete the preference associated
        // with it.
        final int N = appWidgetIds.length;
        for (int i = 0; i < N; i++) {
            exampleConfActivity.deleteTitlePref(context,
                appWidgetIds[i]);
        }
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
