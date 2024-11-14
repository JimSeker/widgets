package edu.cs4730.tapremote;

import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.os.Bundle;
import android.text.format.Formatter;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class testActivity extends AppCompatActivity {
    TextView logger;
    Button mkconn, clientconn, sendbutton;
    EditText hostname, port;

    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            Object path = msg.obj;
            logger.append("\n" + path.toString());
            return true;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.testmain), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        @SuppressWarnings("deprecation")
        String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());


        logger = findViewById(R.id.logger);
        logger.append("IP is " + ip);
        hostname = findViewById(R.id.EThostname);
        hostname.setText("10.121.171.235");
        port = findViewById(R.id.ETport);
        mkconn = findViewById(R.id.makeconn);
        mkconn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logger.append("port number is " + port.getText().toString());
                Intent i = new Intent(getApplicationContext(), MyNetworkService.class);
                String portn = port.getText().toString();
                i.putExtra(myConstants.KEY_PORT, portn);
                i.putExtra(myConstants.KEY_CMD, myConstants.CMD_CONNECT);
                i.putExtra(myConstants.KEY_SERVER, true);

                Messenger messenger = new Messenger(handler);
                i.putExtra(myConstants.KEY_MESSAGER, messenger);
                startService(i);
            }
        });
        clientconn = findViewById(R.id.clientbutton);
        clientconn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String hostn = hostname.getText().toString();
                String portn = port.getText().toString();
                logger.append("port number is " + portn);
                logger.append("host is " + hostn);

                Intent i = new Intent(getApplicationContext(), MyNetworkService.class);
                i.putExtra(myConstants.KEY_CMD, myConstants.CMD_CONNECT);
                i.putExtra(myConstants.KEY_SERVER, false);
                i.putExtra(myConstants.KEY_PORT, portn);
                i.putExtra(myConstants.KEY_HOST, hostn);
                Messenger messenger = new Messenger(handler);
                i.putExtra(myConstants.KEY_MESSAGER, messenger);

                startService(i);
            }
        });
        sendbutton = findViewById(R.id.sendbutton);
        sendbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getApplicationContext(), MyNetworkService.class);
                i.putExtra(myConstants.KEY_CMD, myConstants.CMD_WRITE);
                i.putExtra(myConstants.KEY_MSG, "L");

                startService(i);
            }
        });
        findViewById(R.id.closebutton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getApplicationContext(), MyNetworkService.class);
                i.putExtra(myConstants.KEY_CMD, myConstants.CMD_CLOSE);
                startService(i);
            }
        });

    }

    @Override
    protected void onStop() {
        super.onStop();
        Intent i = new Intent(getApplicationContext(), MyNetworkService.class);
        i.putExtra(myConstants.KEY_CMD, myConstants.CMD_CLOSE);
        startService(i);
    }
}
