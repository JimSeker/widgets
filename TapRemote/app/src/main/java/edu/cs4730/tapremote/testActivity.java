package edu.cs4730.tapremote;

import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class testActivity extends AppCompatActivity {
    TextView logger;
    Button mkconn, clientconn, sendbutton;
    EditText hostname, port;

    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            Object path = msg.obj;
            logger.append("\n" + path.toString());
            return true;
        };
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);


        WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
        String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());


        //EThostname  ETport makeconn clientconn sendbutton
        logger = (TextView) findViewById(R.id.logger);
        logger.append("IP is " + ip);
        hostname = (EditText) findViewById(R.id.EThostname);
        hostname.setText("10.121.171.235");
        port = (EditText) findViewById(R.id.ETport);
        mkconn = (Button) findViewById(R.id.makeconn);
        mkconn.setOnClickListener(new View.OnClickListener() {
            @Override
           public void onClick(View v) {
                logger.append("port number is " + port.getText().toString());
                Intent i = new Intent(getApplicationContext(), MyNetworkService.class);
                String portn = port.getText().toString();
                i.putExtra("myport", portn);
                i.putExtra("cmd", "connect");
                i.putExtra("server", true);

                Messenger messenger = new Messenger(handler);
                i.putExtra("MESSENGER", messenger);
                startService(i);
           }
        });
        clientconn = (Button) findViewById(R.id.clientbutton);
        clientconn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String host = hostname.getText().toString();
                String portn = port.getText().toString();
                logger.append("port number is " + portn);
                logger.append("host is " + host);

                Intent i = new Intent(getApplicationContext(), MyNetworkService.class);
                i.putExtra("cmd", "connect");
                i.putExtra("server", false);
                i.putExtra("myport", portn);
                i.putExtra("iphost", host);
                Messenger messenger = new Messenger(handler);
                i.putExtra("MESSENGER", messenger);

                startService(i);
            }
        });
        sendbutton = (Button) findViewById(R.id.sendbutton);
        sendbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getApplicationContext(), MyNetworkService.class);
                i.putExtra("cmd", "write");
                i.putExtra("msg", "L");

                startService(i);
            }
        });
        findViewById(R.id.closebutton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getApplicationContext(), MyNetworkService.class);
                i.putExtra("cmd", "close");
                startService(i);
            }
        });

    }

    @Override
    protected void onStop() {
        super.onStop();
        Intent i = new Intent(getApplicationContext(), MyNetworkService.class);
        i.putExtra("cmd", "close");
        startService(i);

       //stopService(i);
    }
}
