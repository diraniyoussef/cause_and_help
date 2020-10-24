package com.youssefdirani.cause_and_help;

import android.util.Log;
import android.widget.Toast;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

class SocketConnection { //some methods are not static in this class, this is why it is not made static.
    //My 2 sockets issue**********************************
    final int MaxSocketsNumber = 2;
    Socket client[] = new Socket[MaxSocketsNumber]; //PLEASE CONSIDER USING ATOMICREFERENCE TO ENSURE VISIBILITY https://stackoverflow.com/questions/3964211/when-to-use-atomicreference-in-java
    volatile int activeSocketIndex = -1; //convention is that the value is either 0 or 1 and that it starts with -1
    OutputStream[] outputStreams = new OutputStream[MaxSocketsNumber];//PLEASE CONSIDER USING ATOMICREFERENCE TO ENSURE VISIBILITY
    //****************************************************
    CommunicateWithServer communication;
    private final int SocketTimeout = 1500;

    private volatile boolean newSocketIsCurrentlyUnderCreation = false;

    static boolean silentToast;

    private MainActivity activity;
    private Toasting toasting;

    SocketConnection( MainActivity activity ) { //constructor
        this.activity = activity;
        this.toasting = activity.toasting;
        communication = new CommunicateWithServer( this.activity , this );
    }

    //The purpose of this method is to message the server whenever we have a socket...
    private int index_at_the_beginning = -1;
    void socketConnectionSetup( byte[] message ) {
        if (activeSocketIndex == -1) {//only the first time
            //disableSwitches(); //user recognizes disabling so no need to toast.
            //if (createNewSocketAndEnableSwitches(activeSocketIndex)) {
            Log.i("Youssef sock...", "Entering activeSocketIndex with message " + message);
            if( newSocketIsCurrentlyUnderCreation ) {
                return;
            }
            index_at_the_beginning = nextClientIndex( index_at_the_beginning );
            CreateSocketAttempt createSocketAttempt = new CreateSocketAttempt( index_at_the_beginning, message , true);
            createSocketAttempt.start();
        } else {
            Log.i("Youssef sock...", "to initiate a new order without creating initially a new socket.");
            communication.setThreads(message, true);
        }
    }

    private void callback_socketConnectedAtTheBeginning( byte[] message ) {
        Log.i("Youssef sock...", "finished creating a new socket client 0.");
        communication.delayToManipulateSockets.startTiming();
        communication.setThreads(message, false);
    }

    private void callback_switchSocket() {
        //silentToastAfterNewSocket = true; //this is to make the following toast apparent to the user.
        //Generic.toasting.toast("Connection is better now.\nYou may continue or try again.", Toast.LENGTH_LONG, silentToast);
        toasting.toast("Connection is better now",
                Toast.LENGTH_LONG, silentToast);

        communication.considerResponseReceived(); //I believe not needed because (if you think deeply) manipulateSocketsAndStartTiming won't be called since it's protected by !destroySocketTimer.isAwaitingToDestroy condition

        //destroy the old socket after some time, because I want to keep listening to incoming messages on previous socket
        communication.destroySocketTimer.startTiming( nextClientIndex( activeSocketIndex ) );
        communication.delayToManipulateSockets.startTiming();

        //now divert all newcoming orders to the new socket.
        //socketConnection.activeSocketIndex = socketConnection.nextClientIndex(socketConnection.activeSocketIndex);
        Log.i("Youssef Sock...", "about manipulateSocketsAndStartTiming          Now the newcoming orders should be diverted to " +
                activeSocketIndex  +
                " and port " + getPortFromIndex( activeSocketIndex) );
    }

    int nextClientIndex(int i) {
        if (i < MaxSocketsNumber - 1){
            i++;
        } else { //i == MaxSocketsNumber - 1
            i = 0;
        }
        return(i);
    }

    private static int i; //for debugging
    class CreateSocketAttempt extends Thread {
        int index;
        byte[] message;
        boolean atTheBeginning;

        CreateSocketAttempt( int client_index , byte[] message , boolean atTheBeginning ) {
            this.index = client_index;
            this.message = message;
            this.atTheBeginning = atTheBeginning;
        }

        CreateSocketAttempt( int client_index ) {
            this.index = client_index;
            //this.message = null //no need to initialize it
            this.atTheBeginning = false;
        }

        @Override
        public void run() {
                i++; //only for debugging
                if( i == 10 ) {
                    i = 0;//I don't want i to grow indefinitely and potentially cause an overflow.
                }
                Log.i("Youssef sock...", "This is the " + i + "th time we enter in CreateSocket");
                //client = new Socket(wiFiConnection.chosenIPConfig.staticIP, port); //should be further developed.
                //client.connect(new InetSocketAddress(wiFiConnection.chosenIPConfig.staticIP, port), 1500);
            newSocketIsCurrentlyUnderCreation = true;
            try {
                client[index] = new Socket();
                Log.i("Youssef sock...", "client[" + index + "] will try to connect to server on port " + getPortFromIndex(index) );
                //client.connect(new InetSocketAddress("192.168.4.201", port),1500);
                client[index].connect( new InetSocketAddress( StaticIP , getPortFromIndex( index ) ) , SocketTimeout ); //this blocks execution for a timeout interval of time
                //When we reach here, then socket did really connect
                activeSocketIndex = index;

                //client.setSoTimeout(0); //no need to set it to infinite since all it does, if it were not infinite, is to throw an exception; it does not affect the socket.
                Log.i("Youssef sock...", "Socket " + index + " " +
                        "is connected,  on port " + getPortFromIndex(index));

                outputStreams[index] = client[index].getOutputStream();
                Log.i("Youssef sock...", "New outputStreams is made, for panel index ");

                communication.renew_InputStreamThread(index);
                //Log.i("Youssef sock...", "New bufferThread is made." + " For panel  index " + selectedServerConfig.panel_index);
                if( atTheBeginning ) {
                    callback_socketConnectedAtTheBeginning(message);
                } else {
                    callback_switchSocket();
                }

            } catch (Exception e) {//I hope this includes the IOException or the UnknownHostException because this will be thrown
                //in case the IP is wrong or the electricity on module is down.
                e.printStackTrace();
                if( client[index] != null ) {
                    try {
                        client[index].close();
                    } catch (Exception exc) {
                        //Thread.currentThread().interrupt();
                        exc.printStackTrace();
                        Log.i("Youssef sock...", "Error: closing socket.");
                    }
                }
                //it's probably better to call      destroySocket(index);       but to be checked later.
                Log.i("Youssef sock...", "Exception is thrown on port " + getPortFromIndex(index));
                //Now turn off the WiFi
/*
                if (WiFiConnection.isWiFiOn()) {
                    WiFiConnection.turnWiFiOff(); //this sometimes solves a problem..............
                }
                Generic.toasting.toast("Couldn't connect.\nPlease turn on the WiFi to refresh...", Toast.LENGTH_LONG, silentToast);
*/
                if( atTheBeginning ) {
                    toasting.toast("لم يتمكّن التطبيق من الاتصال." +
                            "\nهل الانترنت منقطع ؟", Toast.LENGTH_LONG, silentToast);
                }

                e.printStackTrace();
            }
            newSocketIsCurrentlyUnderCreation = false;
        }
    }

    //private String StaticIP = "74.122.199.173";
    private String StaticIP = "192.168.1.21";
    private int port1 = 3552; //1595 and 1596 didn't work
    private int port2 = 3553;

    int getPortFromIndex(int index) {
        if (index == 0) {
            return port1;
        } else if(index == 1) {
            return port2;
        } else {
            return -1;
        }
    }
    /*
    int getOtherPortFromIndex(int index) {
        if (index == 0) {
            return port2; //1595 and 1596 didn't work
        } else if(index == 1) {
            return port1;
        } else {
            return -1;
        }
    }
*/
    void destroySocket(int client_index) { //No need to kill the bufferThread, bufferedReader will simply update with the new socket.
        //testAndFixConnection thread also isn't available to be killed in the first place!
        // Other threads just continue their work with no real harm and finish peacfully.
        try {
            communication.null_InputStreamThread(client_index); //interesting since I want to kill the thread.

            if (outputStreams[client_index] != null){
                outputStreams[client_index].close(); //returns void
                outputStreams[client_index] = null;
            }
            if (client[client_index] != null) {
                if(client[client_index].isConnected()) //I added this line on 06062019
                    client[client_index].close(); //returns void    //this actually shows to the server since it's a proper closing of a socket.
                //client[client_index] = null;
            }
            Log.i("Youssef sock...","Now socket " + client_index + " is destroyed.");
        } catch (Exception e) {
            e.printStackTrace();
            Log.i("Youssef sock...","Error in closing socket, outputStreams, or reader................");
        }
    }

    void destroyAllSockets() {
        if( communication != null) {
            if( communication.delayToManipulateSockets != null ) {
                communication.delayToManipulateSockets.cancelTimer(); //usually there is a 2 minutes timer to manipulate the sockets, this should be cancelled.
                //it's like         communication.delayToManipulateSockets.cancelTimer();
            }
            if( communication.destroySocketTimer != null ) {
                communication.destroySocketTimer.cancelTimer(); //there might be one socket waiting to be destroyed after sometime, so I will cancel the timer and close the socket
            }
        }
        //in the current method.
        destroySocket(0);
        destroySocket(1);
        activeSocketIndex = -1;
    }

}
