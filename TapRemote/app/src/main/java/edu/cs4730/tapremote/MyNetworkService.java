package edu.cs4730.tapremote;

import android.app.Notification;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.IBinder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Messenger;
import android.os.Process;
import android.os.Message;
import androidx.core.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

public class MyNetworkService extends Service {
    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    private static final String MESSAGE = "Message";
    //localish variables for connections
    Messenger messenger = null;
    int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    boolean connected = false;
    String TAG = "Service";
    ServerSocket serverSocket = null;
    Socket client = null;
    BufferedReader in;
    PrintWriter out;
    Thread mythread;
    String cmd = "";

    public MyNetworkService() {
    }

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            //promote to foreground and create persistent notification.
            //in Oreo we only have a few seconds to do this or the service is killed.
            Notification notification = getNotification("MyService is running");
            startForeground(msg.arg1, notification);  //not sure what the ID needs to be.

            Log.v("handler", "In handle message");
            int port = 0;
            String host = "";
            String message = "";
            boolean amserver = false;
            String TAG = "handler";

            Bundle extras = msg.getData();

            if (extras != null) {
                Log.v("handler", "extras is not null");
                //First check the command.   Connect, write, read?, close, ?
                cmd = extras.getString(myConstants.KEY_CMD, "");
                Log.v(TAG, "command " + cmd);
                switch (cmd) {
                    case myConstants.CMD_CONNECT:
                        Log.v(TAG, "Running connect code.");

                        //get the intent information
                        amserver = extras.getBoolean(myConstants.KEY_SERVER, false);
                        host = extras.getString(myConstants.KEY_HOST, "None");  //on server, this will be none
                        Log.v(TAG, "Hostname is " + host);
                        port = Integer.parseInt(extras.getString(myConstants.KEY_PORT, "0"));
                        Log.v(TAG, "Port is " + port);

                        //got the info, now ready to setup the networking
                        if (amserver) {
                            //I am the server
                            Log.wtf(TAG, "IamServer");
                            connected = serverConn(port);
                        } else {
                            //I am the client
                            Log.wtf(TAG, "IamClient");
                            connected = clientConn(host, port);
                        }
                        //get how to send info back to call, by handler or widget id.
                        messenger = (Messenger) extras.get(myConstants.KEY_MESSAGER);
                        appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                                AppWidgetManager.INVALID_APPWIDGET_ID);

                        if (connected) {
                            Log.v(TAG, "Connected, Reading!");
                            sendlocalmsg(myConstants.MSG_CONNECT);
                            //start a read loop on the seperate thread.
                            mythread = new Thread(new readthread());
                            mythread.start();
                        } else {
                            sendlocalmsg(myConstants.MSG_DISCONNECT);
                        }
                        break;
                    case myConstants.CMD_WRITE:
                        message = extras.getString(myConstants.KEY_MSG, "");
                        Log.v(TAG, "write is " + message);
                        if (message.compareTo("") != 0) {
                            sendmsg(message);
                            Log.v(TAG, "wrote message " + message);
                        }
                        break;

                    case "close":

                        Log.v(TAG, "close at top. ");
                        Log.v(TAG, "about to conn = false. ");
                        connected = false;
                        try {

                            Log.v(TAG, "connect is now false ");
                            if (out != null) {
                                out.close();
                            }
                            Log.v(TAG, "close out ");
                            if (in != null) {
                                in.close();
                            }
                            Log.v(TAG, "close in ");


                            if (client != null) {
                                client.close();

                            }
                            Log.v(TAG, "close client ");
                            if (serverSocket != null) {
                                serverSocket.close();

                            }
                            Log.v(TAG, "close serversocket ");
                        } catch (Exception e) {
                            Log.v(TAG, "close error");
                            e.printStackTrace();
                            //don't care, just close everything.
                        } finally {
                            Log.v(TAG, "close null all start ");
                            in = null;
                            out = null;
                            client = null;
                            serverSocket = null;
                            Log.v(TAG, "close null all end ");
                        }
                        //send back that we are now disconnected.
                        sendlocalmsg(myConstants.MSG_DISCONNECT);
                        break;
                    default:
                        //error
                }


            } else {
                Log.v(TAG, "damn extras are null?");
            }
            Log.v(TAG, "Connected is " + connected);
            // Stop the service using the startId, so that we don't stop
            // the service in the middle of handling another job
            //  if ( //!connected  ||   //read died or other failure
            //      (cmd.compareTo("close") == 0 )     )  //close command issued.
            //     stopSelf(msg.arg1);  //stop the service.
        }
    }

    @Override
    public void onCreate() {

        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.  We also make it
        // background priority so CPU-intensive work will not disrupt our UI.
        HandlerThread thread = new HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.v("OnStartCommand", "Starting message");
        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;//needed for stop.
        msg.setData(intent.getExtras());
        mServiceHandler.sendMessage(msg);

        // If we get killed, don't restart
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

    @Override
    public void onDestroy() {
        Log.wtf(TAG,"Service destroyed.");
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
    }


    //network service stuff.
    public boolean serverConn(int port) {
        String TAG = "ServerConn";
        boolean success = false;
        Log.v(TAG, "about to setup accept.");
        try {
            //setup new socket connection
            serverSocket = new ServerSocket(port);
            //socket created, now wait for a coonection via accept.
            client = serverSocket.accept();
        } catch (IOException e) {
            e.printStackTrace();
            serverSocket = null;
            client = null;
            Log.v(TAG, "Server connect error.");

            return success;
        }
        Log.v(TAG, "past accept.");
//setup send/receive streams.
        try {
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(client.getOutputStream())), true);
        } catch (IOException e) {
            try {
                client.close();
                serverSocket.close();
            } catch (IOException e1) {
                //seriously, required another try/catch to deal with failure... dumb.
                client = null;
                serverSocket = null;
            }
            Log.v(TAG, "in out error.");
            return success;
        }
        Log.v(TAG, "server connected.");
        //ok, everything is setup and working!
        success = true;

        return success;
    }

    class readthread implements Runnable {

        @Override
        public void run() {
            readmsg();
        }
    }

    public boolean clientConn(String iphost, int port) {
        boolean success = false;
        InetAddress serverAddr = null;
        String TAG = "clientConn";
        try {
            serverAddr = InetAddress.getByName(iphost);
        } catch (UnknownHostException e) {
            //e.printStackTrace();
            Log.v(TAG, "client connect error Inet.");
            return success;
        }

        try {
            client = new Socket(serverAddr, port);
        } catch (IOException e) {
            //e.printStackTrace();
            Log.v(TAG, "client connect error socket. " + iphost + " " + port);
            Log.v(TAG, "Error is" + e);
            return success;
        }
        Log.v(TAG, "connected.");

        //made connection, setup the read (in) and write (out)
        try {
            out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(client.getOutputStream())), true);
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));
        } catch (IOException e) {
            //e.printStackTrace();
            try {
                client.close();
            } catch (Exception e1) {
                Log.v(TAG, " " + e1);
            }
            return success;
        }
        Log.v(TAG, "client connected.");
        //yes, it is connected.
        success = true;

        return success;
    }

    //write method
    void sendmsg(String msg) {
        if (connected) {
            Log.v("sendmsg", "connected  and sending " + msg);
            out.println(msg);
        }
    }

    //send to the activity or widget the message.
    void sendlocalmsg(String msg) {
        String TAG = "sendlocalmsg";
        if (messenger != null) {
            Log.v(TAG, "sending message via handler");
            //send message back via the handler
            Message mymsg = Message.obtain();
            mymsg.obj = msg;
            try {
                messenger.send(mymsg);
            } catch (android.os.RemoteException e1) {
                Log.w(TAG, "Exception sending message", e1);
            }
        } else if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            Log.v(TAG, "sending message to widget");
            //send it to the widget
            Intent intent = new Intent(getApplicationContext(), TapWidget.class);
            intent.setAction(MESSAGE);
            intent.putExtra("msg", msg);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);  //in case we need it for preferences.
            sendBroadcast(intent);
        } else {
            //well, this is problem.  just log it I guess
            Log.v(TAG, "Read and no send: " + msg);
        }
    }

    void readmsg() {
        String msg;
        String TAG = "readmsg";
        while (connected) {
            try {
                Log.v(TAG, "starting read ");
                msg = in.readLine();
                Log.v(TAG, "read message " + msg);
                if (msg == null) {
                    connected = false;
                    Log.v(TAG, "read exiting ");
                    sendlocalmsg(myConstants.MSG_DISCONNECT);
                    return;
                }
                sendlocalmsg(msg);
            } catch (Exception e) {
                Log.v(TAG, "read failed!");
                Log.v(TAG, "exception " + e);
                connected = false;  //bail out.
            }
        }
        Log.v(TAG, "readmsg ending. ");
    }


    // build a persistent notification and return it.
    public Notification getNotification(String message) {

        return new NotificationCompat.Builder(getApplicationContext(), TapWidgetConfigureActivity.id1)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)  //persistent notification!
                .setChannelId(TapWidgetConfigureActivity.id1)
                .setContentTitle("Service")   //Title message top row.
                .setContentText(message)  //message when looking at the notification, second row
                .build();  //finally build and return a Notification.
    }
}