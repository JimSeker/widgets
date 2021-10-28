package edu.cs4730.widgetdemo2;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;


/**
 * This is the configuration activity for the widget.
 */

public class Widgetd2Config extends AppCompatActivity implements OnClickListener {
    int randnum = 100;  //default value
    private static final String PREF_PREFIX_KEY = "appwidget_";
    EditText et;
    Button btnok, btncancel;
    int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //set canceled now, in case of failure or user exits.
        setResult(RESULT_CANCELED);

        //next get the ID number, setting the invalid number as default.
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            appWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // If they gave us an intent without the widget id, just bail.
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
        }


        setContentView(R.layout.widget_config);
        et = (EditText) findViewById(R.id.editText1);

        //we have valid ID number, but likely nothing in preferences yet, so set the default to 100.
        SharedPreferences preferences = getSharedPreferences("widgetDemo2", Context.MODE_PRIVATE);
        randnum = preferences.getInt(PREF_PREFIX_KEY + appWidgetId, 100);

        //setup the view finally.
        et.setText(String.valueOf(randnum));

        btnok = (Button) findViewById(R.id.ok);
        btnok.setOnClickListener(this);
        btncancel = (Button) findViewById(R.id.cancel);
        btncancel.setOnClickListener(this);


    }

    @Override
    public void onClick(View arg0) {

        if (arg0 == btnok) {
            //save the preference
            SharedPreferences.Editor editor = getSharedPreferences("widgetDemo2", Context.MODE_PRIVATE).edit();
            randnum = Integer.parseInt(et.getText().toString());
            editor.putInt(PREF_PREFIX_KEY + appWidgetId, randnum);
            editor.apply();

            //set the intent and go.
            Intent resultValue = new Intent();
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            setResult(RESULT_OK, resultValue);

        }
        finish();  //we are done.
    }
}
