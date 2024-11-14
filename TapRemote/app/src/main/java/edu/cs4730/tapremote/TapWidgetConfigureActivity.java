package edu.cs4730.tapremote;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Map;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * The configuration screen for the {@link TapWidget TapWidget} AppWidget.
 */
public class TapWidgetConfigureActivity extends AppCompatActivity {

    int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    TextView mAppWidgetText, ipaddr;
    EditText hostname, port;
    private static final String PREFS_NAME = "edu.cs4730.tapremote.TapWidget";
    private static final String PREF_PREFIX_KEY = "appwidget_";
    String widgetText;

    public static String id1 = "test_channel_01";
    final String TAG = "MainActivity";
    ActivityResultLauncher<String[]> rpl;
    private final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.POST_NOTIFICATIONS};

    public TapWidgetConfigureActivity() {
        super();
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
        setResult(RESULT_CANCELED);

        // for notifications permission now required in api 33
        //this allows us to check with multiple permissions, but in this case (currently) only need 1.
        rpl = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
            new ActivityResultCallback<Map<String, Boolean>>() {
                @Override
                public void onActivityResult(Map<String, Boolean> isGranted) {
                    boolean granted = true;
                    for (Map.Entry<String, Boolean> x : isGranted.entrySet()) {
                        logthis(x.getKey() + " is " + x.getValue());
                        if (!x.getValue()) granted = false;
                    }
                    if (granted)
                        logthis("Permissions granted for api 33+");
                }
            }
        );


        setContentView(R.layout.tap_widget_configure);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.configmain), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });


        mAppWidgetText = findViewById(R.id.appwidget_text);
        findViewById(R.id.button_client).setOnClickListener(mOnClickListener);
        findViewById(R.id.button_Server).setOnClickListener(mOnClickListener);
        findViewById(R.id.button_disconnect).setOnClickListener(mOnClickListener);
        hostname = findViewById(R.id.EThostname);
        hostname.setText("10.131.209.56");  //was the default, so I didn't have retype it.
        port = findViewById(R.id.ETport);

        //What is our IP address?
        WifiManager wm = (WifiManager) getApplicationContext().getApplicationContext().getSystemService(WIFI_SERVICE);
        @SuppressWarnings("deprecation")
        String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());  //wifi doesn't have ipv6.
        ipaddr = findViewById(R.id.tv_ip);
        ipaddr.setText(ip);

        // Find the widget id from the intent.
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // If this activity was started with an intent without an app widget ID, finish with an error.
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }
        widgetText = loadTitlePref(TapWidgetConfigureActivity.this, mAppWidgetId);
        mAppWidgetText.setText(widgetText);

        createchannel();
        //for the new api 33+ notifications permissions.
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!allPermissionsGranted()) {
                rpl.launch(REQUIRED_PERMISSIONS);
            }
        }
    }

    View.OnClickListener mOnClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            final Context context = TapWidgetConfigureActivity.this;
            String portn, hostn;
            Intent i = new Intent(getApplicationContext(), MyNetworkService.class);
            boolean endservice = false;

            if (v.getId() == R.id.button_Server) {
                portn = port.getText().toString();
                i.putExtra(myConstants.KEY_PORT, portn);
                i.putExtra(myConstants.KEY_CMD, myConstants.CMD_CONNECT);
                i.putExtra(myConstants.KEY_SERVER, true);
                i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                logthis("server connect");
            } else if (v.getId() == R.id.button_client) {
                hostn = hostname.getText().toString();
                portn = port.getText().toString();
                i.putExtra(myConstants.KEY_CMD, myConstants.CMD_CONNECT);
                i.putExtra(myConstants.KEY_SERVER, false);
                i.putExtra(myConstants.KEY_PORT, portn);
                i.putExtra(myConstants.KEY_HOST, hostn);
                i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                logthis("client connect");
            } else if (v.getId() == R.id.button_disconnect) {
                i.putExtra(myConstants.KEY_CMD, myConstants.CMD_CLOSE);
                endservice = true;  // this is kludge... not a good one either, but it works.
                logthis("disconnect");
            }
            logthis("about to start the service");
            startForegroundService(i);
            //lower then Oreo, just start the service.
            // startService(i);  //send the intent.

            // When the button is clicked, store the string locally
            String widgetText = mAppWidgetText.getText().toString();
            saveTitlePref(context, mAppWidgetId, widgetText);

            // It is the responsibility of the configuration activity to update the app widget
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            TapWidget.updateAppWidget(context, appWidgetManager, mAppWidgetId);

            // Make sure we pass back the original appWidgetId
            Intent resultValue = new Intent();
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
            setResult(RESULT_OK, resultValue);

            //if disconnected
            if (endservice)
                stopService(i);
            finish();
        }
    };

    // Write the prefix to the SharedPreferences object for this widget
    static void saveTitlePref(Context context, int appWidgetId, String text) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putString(PREF_PREFIX_KEY + appWidgetId, text);
        prefs.apply();
    }

    // Read the prefix from the SharedPreferences object for this widget.
    // If there is no preference saved, get the default from a resource
    static String loadTitlePref(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        String titleValue = prefs.getString(PREF_PREFIX_KEY + appWidgetId, null);
        if (titleValue != null) {
            return titleValue;
        } else {
            return context.getString(R.string.appwidget_text);
        }
    }

    static void deleteTitlePref(Context context, int appWidgetId) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.remove(PREF_PREFIX_KEY + appWidgetId);
        prefs.apply();
    }


    /*
     * for API 26+ create notification channels
     */
    private void createchannel() {

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel mChannel = new NotificationChannel(id1,
            getString(R.string.channel_name),  //name of the channel
            NotificationManager.IMPORTANCE_LOW);   //importance level
        //important level: default is is high on the phone.  high is urgent on the phone.  low is medium, so none is low?
        // Configure the notification channel.
        mChannel.setDescription(getString(R.string.channel_description));
        mChannel.enableLights(true);
        // Sets the notification light color for notifications posted to this channel, if the device supports this feature.
        mChannel.setShowBadge(true);
        nm.createNotificationChannel(mChannel);

    }

    public void logthis(String msg) {
        Log.d(TAG, msg);
    }

    //ask for permissions when we start.
    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }


}

