package edu.cs4730.widgetdemo2;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import edu.cs4730.widgetdemo2.databinding.WidgetConfigBinding;


/**
 * This is the configuration activity for the widget.
 */

public class Widgetd2Config extends AppCompatActivity implements OnClickListener {
    int randnum = 100;  //default value
    private static final String PREF_PREFIX_KEY = "appwidget_";
    WidgetConfigBinding binding;
    int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //set canceled now, in case of failure or user exits.
        setResult(RESULT_CANCELED);

        binding = WidgetConfigBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.configmain, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
        //next get the ID number, setting the invalid number as default.
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // If they gave us an intent without the widget id, just bail.
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
        }


        //we have valid ID number, but likely nothing in preferences yet, so set the default to 100.
        SharedPreferences preferences = getSharedPreferences("widgetDemo2", Context.MODE_PRIVATE);
        randnum = preferences.getInt(PREF_PREFIX_KEY + appWidgetId, 100);

        //setup the view finally.
        binding.editText1.setText(String.valueOf(randnum));

        binding.ok.setOnClickListener(this);
        binding.cancel.setOnClickListener(this);

    }

    @Override
    public void onClick(View arg0) {

        if (arg0 == binding.ok) {
            //save the preference
            SharedPreferences.Editor editor = getSharedPreferences("widgetDemo2", Context.MODE_PRIVATE).edit();
            randnum = Integer.parseInt(binding.editText1.getText().toString());
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
