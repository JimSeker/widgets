package edu.cs4730.tapremote;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.IBinder;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Messenger;
import android.os.Process;
import android.os.Message;
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

    ServerSocket serverSocket = null;
    Socket client = null;
    BufferedReader in;
    PrintWriter out;
    Thread mythread;
    String  cmd = "";

    public MyNetworkService() {
    }

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {

            Log.v("handler", "In handle message");
            int port = 0;
            String host = "";
            String message = "";
            boolean amserver = false;

            Bundle extras = msg.getData();

            if (extras != null) {
                Log.v("handler", "extras is not null");
                //First check the command.   Connect, write, read?, close, ?
                cmd = extras.getString("cmd", "");
                Log.v("handler", "command " + cmd);
                switch (cmd) {
                    case "connect":
                        Log.v("handler", "Running connect code.");
                        //if (!amserver) { ///client, need a hostname
                            host = extras.getString("iphost", "None");
                            Log.v("Service", "Hostname is " + host);
                        //}
                        Log.v("Service", "iPort is " + extras.getString("myport"));
                        port = Integer.parseInt(extras.getString("myport", "0"));
                        Log.v("Service", "Port is " + port);
                        //got the info, now ready to setup the networking
                        amserver = extras.getBoolean("server", false);

                        if (amserver) {
                            //I am the server
                            Log.v("hander", "IamServer");
                            connected = serverConn(port);
                        } else {
                            //I am the client
                            Log.v("hander", "IamClient");
                            connected = clientConn(host, port);
                        }
                        messenger = (Messenger) extras.get("MESSENGER");
                        appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                                AppWidgetManager.INVALID_APPWIDGET_ID);

                        if (connected) {
                            Log.v("hander", "Connected, Reading!");
                            sendlocalmsg("connected");
                            //start a read loop on the seperate thread.
                            mythread = new Thread(new readthread());
                            mythread.start();

                        } else {
                            sendlocalmsg("disconnected");
                        }
                        break;
                    case "write":
                        message = extras.getString("msg", "");
                        Log.v("service", "write is " + message);
                        if (message.compareTo("") != 0) {
                            sendmsg(message);
                            Log.v("service", "wrote message " + message);
                        }
                        break;
                    case "close":
                        //if (connected) {
                        Log.v("service", "close at top. " );
                        Log.v("service", "about to conn = false. " );
                        connected = false;
                            try {

                                Log.v("service", "connect is now false " );
                                if (out != null) {
                                    out.close();
                                }
                                Log.v("service", "close out " );
                                if (in != null) {
                                    in.close();
                                }
                                Log.v("service", "close in " );


                                if (client != null) {
                                    client.close();
                                    //client = null;
                                }
                                Log.v("service", "close client " );
                                if (serverSocket != null) {
                                    serverSocket.close();
                                    //serverSocket = null;
                                }
                                Log.v("service", "close serversocket " );
                            } catch (Exception e) {
                                Log.v("service", "close error");
                                e.printStackTrace();
                                //don't care, just close everything.
                            } finally {
                                Log.v("service", "close null all start " );
                                in = null;
                                out = null;
                                client = null;
                                serverSocket = null;
                                Log.v("service", "close null all end " );
                            }

                        //}
                        sendlocalmsg("disconnected");
                        break;
                    default:
                        //error
                }


            } else {
                Log.v("handler", "shit extras are null?");
            }
            Log.v("Handler", "Connected is " + connected);
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
        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();
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
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
    }


    //network service stuff.
    public boolean serverConn(int port) {
        boolean success = false;
        Log.v("Service", "about to setup accept.");
        try {
            //setup new socket connection
            serverSocket = new ServerSocket(port);
            //socket created, now wait for a coonection via accept.
            client = serverSocket.accept();
        } catch (IOException e) {
            e.printStackTrace();
            serverSocket = null;
            client = null;
            Log.v("service", "Server connect error.");

            return success;
        }
        Log.v("Service", "past accept.");
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
            Log.v("service", "in out error.");
            return success;
        }
        Log.v("Service", "server connected.");
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
        try {
            serverAddr = InetAddress.getByName(iphost);
        } catch (UnknownHostException e) {
            //e.printStackTrace();
            Log.v("service", "client connect error Inet.");
            return success;
        }

        try {
            client = new Socket(serverAddr, port);
        } catch (IOException e) {
            //e.printStackTrace();
            Log.v("service", "client connect error socket. " + iphost + " " + port);
            Log.v("Service", "Error is"+ e);
            return success;
        }
        Log.v("Service", "connected.");

        //made connection, setup the read (in) and write (out)
        try {
            out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(client.getOutputStream())), true);
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));
        } catch (IOException e) {
            //e.printStackTrace();
            try {
                client.close();
            } catch (IOException e1) {
                //e1.printStackTrace();
            }
            return success;
        }
        Log.v("Service", "client connected.");
        //yes, it is connected.
        success = true;

        return success;
    }

    //write method
    void sendmsg(String msg) {
        if (connected) {
            Log.v("service", "connected  and sending " + msg);
            out.println(msg);
        }
    }

    //send to the activity or widget the message.
    void sendlocalmsg(String msg) {
        if (messenger != null) {
            Log.v("service", "sending message via handler");
            //send message back via the handler
            Message mymsg = Message.obtain();
            mymsg.obj = msg;
            try {
                messenger.send(mymsg);
            } catch (android.os.RemoteException e1) {
                Log.w(getClass().getName(), "Exception sending message", e1);
            }
        } else if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            Log.v("service", "sending message to widget");
            //send it to the widget
            Intent intent = new Intent(getApplicationContext(), TapWidget.class);
            intent.setAction(MESSAGE);
            intent.putExtra("msg", msg);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);  //in case we need it for preferences.
            sendBroadcast(intent);
        } else {
            //well, this is problem.  just log it I guess
            Log.v("Service", "Read and no send: " + msg);
        }
    }

    void readmsg() {
        String msg;
        while (connected) {
            try {
                Log.v("service", "starting read ");
                msg = in.readLine();
                Log.v("service", "read message " + msg);
                if (msg == null) {
                    connected = false;
                    Log.v("service", "read exiting ");
                    sendlocalmsg("disconnected");
                    return;
                }
                sendlocalmsg(msg);
            } catch (Exception e) {
                Log.v("service", "read failed!");
                Log.v("service", "exception " + e);
                connected = false;  //bail out.
            }
        }
        Log.v("service", "readmsg ending. ");
    }
}