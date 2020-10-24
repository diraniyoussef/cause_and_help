package com.youssefdirani.cause_and_help;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.InputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import androidx.appcompat.app.AlertDialog;

class CommunicateWithServer {
    //PLEASE CONSIDER USING ATOMICREFERENCE TO ENSURE VISIBILITY
    private InputStreamThread[] inputStreamThread; //it's allocated in the constructor and assigned in renew_InputStreamThread which is called in SocketConnection class
    volatile DestroySocketTimer destroySocketTimer = new DestroySocketTimer();
    volatile DelayToManipulateSockets delayToManipulateSockets = new DelayToManipulateSockets();
    private volatile boolean isManipulateSocketsLocked = false;

    private MainActivity activity;
    private SocketConnection socketConnection;
    private Toasting toasting;

    CommunicateWithServer(MainActivity act , SocketConnection socketConnection) {
        activity = act;
        toasting = act.toasting;
//        socketConnection = act.socketConnection; //this makes a crash
        this.socketConnection = socketConnection;
        inputStreamThread = new InputStreamThread[ socketConnection.MaxSocketsNumber ]; //didn't work for some reason !
    }

    private volatile boolean isAwaitingForReceivedResponse = false; // I would had used if( waitThenSwitchSocketAttemptAfterReadTimeout.hasCallbacks( runnable_switchSocketAttemptAfterReadTimeout ) )  instead, but this requires API Q and my minimum API level is API 16, so I won't be using it
    private Handler waitThenSwitchSocketAttemptAfterReadTimeout = new Handler();
    private Runnable runnable_switchSocketAttemptAfterReadTimeout = new Runnable() { //better this should be made by the deamon service
        public void run() {
            Log.i("Youssef Communi...", "testAndFixConnection    No, it's full 2.6 seconds! So if all timers are fine, creating a new socket."
                    +
                    " on port " + socketConnection.getPortFromIndex(socketConnection.activeSocketIndex));
            //the following method creates a new socket on the other port, and sets a timer to destroy the current socket.
            isAwaitingForReceivedResponse = false;
            if( !isManipulateSocketsLocked && !destroySocketTimer.isAwaitingToDestroy ) {
                toasting.toast("Enhancing connection", Toast.LENGTH_SHORT, false);
                manipulateSocketsAndStartTiming(false);
            } else {
                if (isManipulateSocketsLocked) {
                    Log.i("Youssef Communi...", "TestAndFixConnection   " +
                            "isManipulateSocketsLocked variable is true, so we won't create a new socket!" +
                            " on port " + socketConnection.getPortFromIndex(socketConnection.activeSocketIndex));
                }
                if (destroySocketTimer.isAwaitingToDestroy) {
                    Log.i("Youssef Communi...", "TestAndFixConnection                          " +
                            "We're within the 8 seconds period to destroy the old socket, so we won't create a new socket!" +
                            " on port " + socketConnection.getPortFromIndex( socketConnection.activeSocketIndex ) );
                }
            }
        }
    };

    void considerResponseReceived() {
        if( isAwaitingForReceivedResponse ) {
            waitThenSwitchSocketAttemptAfterReadTimeout.removeCallbacks( runnable_switchSocketAttemptAfterReadTimeout );
            isAwaitingForReceivedResponse = false;
        }
    }

    void setThreads( byte[] message_param, boolean boolTestAndFixConnection ) {//in case 2 seconds of no reply, you might want to
        //now we would start the InputStreamThread if it was not started already. When was it created? It was created
        //in CreateSocket class.
        Log.i("Youssef Communi...", "Now entering 'communicate' with activeSocketIndex as " +
                socketConnection.activeSocketIndex  +
                " on port " + socketConnection.getPortFromIndex( socketConnection.activeSocketIndex ) );

        if (boolTestAndFixConnection) { //this is the self-correcting mechanism...
            //if( inputStreamThread[socketConnection.activeSocketIndex] != null ) { //although this test will always give true, but it's a bad test to do,since we want to give the connection a way out if silly unexpected things happen
              //  if( !inputStreamThread[socketConnection.activeSocketIndex].killThread ) { //same as above line ! Don't uncomment it.
            if( !isAwaitingForReceivedResponse ) {
                isAwaitingForReceivedResponse = true;
                final int ReadTimeout = 2500;
                waitThenSwitchSocketAttemptAfterReadTimeout.postDelayed(runnable_switchSocketAttemptAfterReadTimeout, ReadTimeout);
            }
        }

        printMessage( message_param );
    }

    private void printMessage (byte[] message_param) {
        if( socketConnection.outputStreams[ socketConnection.activeSocketIndex ] != null ) { //it's for protection but I think it shouldn't be null and enter here at the same time
            if( socketConnection.client[ socketConnection.activeSocketIndex ] != null ) { //probably not needed but anyway
                if( socketConnection.client[ socketConnection.activeSocketIndex ].isConnected() ) {//probably not needed but anyway
                    final byte[] message = message_param;
                    new Thread() { //Threading network operations is recommended in general.
                        //It's good that this particular thread is anonymous so, by assumption, it works better now.
                        public void run() {
                            try {//try-catch is always good. It can prevent a crash.
                                //perhaps waiting before writing is good?
                                Thread.sleep(100); //not bad at all I guess
                                /*
                                for( int i = 0; i < message.length; i++ ) {
                                    Log.i("Youssef Communi...", "message[" + i + "] is " + message[i] + " or " + (char) message[i] );
                                }
                                */
                                //Base64.getEncoder().encode( message, encodedMessage );
                                /*
                                byte[] encodedMessage;// = new byte[ message.length ]; //no need to allocate it
                                encodedMessage = Base64.encode(message, Base64.NO_WRAP); /*the length is more than message.length
                                * Base64-encode the given data and return a newly allocated byte[] with the result.
                                * https://developer.android.com/reference/android/util/Base64
                                 */
                                /*
                                for( int i = 0; i < message.length; i++ ) {
                                    Log.i("Youssef Communi...", "encodedMessage[" + i + "] is " + encodedMessage[i] + " or " + (char) encodedMessage[i] );
                                }
                                */
                                socketConnection.outputStreams[socketConnection.activeSocketIndex].write(message); //message is something like "mob1: awake?"
                                socketConnection.outputStreams[socketConnection.activeSocketIndex].flush();
                                Log.i("Youssef Communi...", "sent message on port " + socketConnection.getPortFromIndex(socketConnection.activeSocketIndex));
                            } catch (Exception e) {//that could happen if the socket is null for some time... but I would have prevented
                                // sending a message then
                                e.printStackTrace();
                                Log.i("Youssef Communi...", "Error in sending message to server or sleeping" +
                                        " on port " + socketConnection.getPortFromIndex(socketConnection.activeSocketIndex));
                            }
                        }
                    }.start();
                }
            }
        } else {
            Log.i("Youssef Communi...", "outputStreams is set to null!!!!!!!!!!!!!!" +
                    " on port " + socketConnection.getPortFromIndex( socketConnection.activeSocketIndex ));
        }
    }

    void null_InputStreamThread(int index){
        if (inputStreamThread[index]!= null) {
            inputStreamThread[index].end_InputStreamThread();
            inputStreamThread[index] = null;
            Log.i("Youssef Communi...", "inputStreamThread " + index + " instance is now nulled." +
                    " on port (usually) different than " + socketConnection.getPortFromIndex( socketConnection.activeSocketIndex ));
        }
    }

    void renew_InputStreamThread(int index) {
        //InputStreamThread[index] = new InputStreamThread(SocketConnection.client[index], index); //this is for debugging only
        inputStreamThread[index] = new InputStreamThread( socketConnection.client[index] );
        inputStreamThread[index].start();
        Log.i("Youssef Communi...", "InputStreamThread (on server index " + index + " " +
                "of port " + socketConnection.getPortFromIndex( index ) +
                ") instance is now created and started.");
    }

    class InputStreamThread extends Thread { //this class is like a ReaderControllerThread
        boolean killThread = false;
        //InputStreamReader inputStreamReader;
        InputStream inputStream;
        Socket client;
        ActionThread actionThread;
        //private int index;//this is for debugging only
        //InputStreamThread(Socket client_param, int index_param){ //this is for debugging only
        InputStreamThread( Socket client_param ) {
            //index = index_param; //this is for debugging only
            client = client_param;
            try {
                //inputStreamReader = new InputStreamReader( client.getInputStream() );
                inputStream = client.getInputStream();
                Log.i("Youssef Communi...", "A bufferedReader instance is effectively created now" +
                        ". We may still be on port " + socketConnection.getPortFromIndex( socketConnection.activeSocketIndex ) +
                        ". Yet a socket is created on the OTHER port by now.");
            } catch (Exception e) {
                e.printStackTrace();
                Log.i("Youssef Communi...", "Error: creating bufferedReader." +
                        " on port " + socketConnection.getPortFromIndex( socketConnection.activeSocketIndex ));
            }
        }

        void end_InputStreamThread() {
            killThread = true;//VERY NECESSARY actually. Because making InputStreamThread[index] point to another instance is not sufficient to kill the thread, I guess.
            null_bufferedReader();
            considerResponseReceived();//the idea is to release the runnable callback if any
        }

        private void null_bufferedReader() {
            if (inputStream != null) {
                try{
                    inputStream.close(); //returns void
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.i("Youssef Communi...","Error in closing bufferedReader."  +
                            " on port " + socketConnection.getPortFromIndex( socketConnection.activeSocketIndex ));
                }
                inputStream = null;
            }
        }

        @Override
        public void run() {
            Log.i("Youssef Communi...", "InputStreamThread just started."  );
            //+ " on port " + socketConnection.getPortFromIndex( socketConnection.activeSocketIndex ));
            List<Byte> byteList = new ArrayList<>();

            byte[] received_byteArray;

            try {
                while( !killThread ) {
                    if( inputStream != null && inputStream.available() != 0 ) {
                        //I'm supposing that bufferedReader could change reference outside without any harm...
                        //if (SocketConnection.bufferedReader.available() != 0) {//could that make a problem if bufferedReader was null?
                        //Instead of the ready method you may use the read method and set the timeout of the socket
                        // using setSoTimeout. <- not sure of that, anyway never usse read() without preceding it with ready()

                        Log.i("Youssef Communi...", "InputStreamThread being started");
                        try {
                            byte readByte = (byte) inputStream.read();                            //The following "while" block, as I believe, reads the whole buffer. So I don't think I need to flush it ever more!
                            while( readByte != -1 ) {
                                //SocketConnection.client.getChannel().position(0);
                                byteList.add( readByte );

                                if( inputStream.available() != 0 ) {
                                    readByte = (byte) inputStream.read();
                                } else {
                                    break;
                                }
                            }
                            if ( !byteList.isEmpty() ) {
                                final int byteList_size = byteList.size();
                                received_byteArray = new byte[ byteList_size ];

                                char[] received_charArray = new char[ byteList_size ];

                                for (int i = 0; i < byteList_size; i++) {
                                    received_byteArray[i] = byteList.get(i);
                                    received_charArray[i] = (char) received_byteArray[i];
                                    //Log.i("Youssef Communi...", "received_byteArray[" + i + "] = " + received_byteArray[i] );
                                }
                                actionThread = new ActionThread( received_byteArray, byteList_size );
                                actionThread.start();
                                considerResponseReceived();
                                Log.i("Youssef Communi...", "Message is received " + byteList.toString() +
                                        " on port " + socketConnection.getPortFromIndex(socketConnection.activeSocketIndex) +
                                        " or message is " + received_charArray.toString());
                                //actionThread.join();
                                byteList.clear();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.i("Youssef Communi...", "Error: in reading from buffer. Maybe because the socket is dead. " +
                                    "The string was: " + byteList.toString()  +
                                    " on port " + socketConnection.getPortFromIndex( socketConnection.activeSocketIndex ));
                        }

                    }
                    try {
                        Thread.sleep(250); //give it 80 ms rest
                    } catch (Exception e) {
                        //Thread.currentThread().interrupt(); //it's not good to interrupt it.
                        Log.i("Youssef Communi...", "Error: in sleeping."  +
                                " on port " + socketConnection.getPortFromIndex( socketConnection.activeSocketIndex ));
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) { //that could happen if the socket was null for some time...
                e.printStackTrace();
                Log.i("Youssef Communi...", "Error: in receiving message from module."  +
                        " on port " + socketConnection.getPortFromIndex( socketConnection.activeSocketIndex ));
            }
        }
    }

   class ActionThread extends Thread { //it's analyze incoming message and take action.
        private byte[] incomingMessage;
        final private int length; //how lovely that it may be instantiated in the constructor

        ActionThread( byte[] s, final int length ) { //I tested it and it's sort of necessary to get the "size" as an argument instead of using s.length since the latter might change in runtime.
            this.length = length;
            incomingMessage = new byte[ length ]; //must be allocated - https://docs.oracle.com/javase/7/docs/api/java/lang/System.html#arraycopy(java.lang.Object,%20int,%20java.lang.Object,%20int,%20int)      they said If dest is null, then a NullPointerException is thrown
            System.arraycopy( s,0, incomingMessage,0, length );
        }

        void analyzeStartingFromIndex( final int startingIndex ) { /*this might be called recursively
        * and just before each recursive new call, it will either add or delete a custom marker to customMarkersList
        * using addCustomMarker and deleteCustomMarker methods (of course changes to mMap happen as well).
        */
            byte[] eightBytes = new byte[8];
            final byte trailor = 127;

            int index = startingIndex;

            while( incomingMessage[ index ] == trailor ) {
                Log.i("Youssef Communi...","A trailor is found");
                index++;
                if( incomingMessage.length - index < 1 ) {
                    return;
                }
            }

            if( incomingMessage[ index ] == 'd' ) {
                /* 'd'->8 bytes lat->8 bytes lng->1 byte 's' for salute and 'c' for cause->trailor->...repeated (starting with 'd')
                 * for other markers.....->
                 * 'd' means to delete all the mentioned coordinates. This happens when the user is connecting and
                 * while he is, the database has removed those items, so server sends a runtime delete notification to
                 * all connected users.*/
                Log.i("Youssef Communi...","analyzing a delete message");
                index++;
                if( incomingMessage.length - index < 8 ) {
                    return;
                }
                System.arraycopy( incomingMessage, index, eightBytes,0,8 );
                double lat = ByteBuffer.wrap( eightBytes ).getDouble();

                index += 8;
                if( incomingMessage.length - index < 8 ) {
                    return;
                }
                System.arraycopy( incomingMessage, index, eightBytes,0,8 );
                double lng = ByteBuffer.wrap( eightBytes ).getDouble();

                index++;
                if( incomingMessage.length - index < 1 ) {
                    return;
                }
                char s_or_c = (char) incomingMessage[ index ];
                boolean saluteNotCause;
                if( s_or_c == 's' ) {
                    saluteNotCause = true;
                } else if( s_or_c == 'c' ) {
                    saluteNotCause = false;
                } else {
                    return;
                }

                index++;
                if( incomingMessage.length - index < 1 ) {
                    return;
                }
                byte end_byte = incomingMessage[ index ];
                if( end_byte == trailor ) {
                    activity.markersAction.deleteCustomMarker( saluteNotCause, lat, lng );
                } else {
                    return;
                }

                index++;
                if( incomingMessage.length - index < 1 ) {
                    return;
                }
                analyzeStartingFromIndex( index );

            } else if( incomingMessage[ index ] == 's' ) {
             /* 's'->8 bytes lat->8 bytes lng->1 byte radius->userName->trailor->statement->trailor->
             * 8 bytes start time->4 bytes likes#->4 bytes dislikes#->trailor->...repeated (starting with 's') for other markers....
             * Having 's' (instead of 'd') means to add salutes.
             * BTW, the maximum size of 1 marker is : 1+8+8+1+20+1+150+1+8+4+4+1 = 207 bytes*/
                Log.i("Youssef Communi...","receiving a salute." );
                index++;
                analyzeStatement( index, true, eightBytes, trailor );
            } else if( incomingMessage[ index ] == 'c' ) {
                /*same as 's' case but it's 'c' for cause*/
                Log.i("Youssef Communi...","receiving a cause." );
                index++;
                analyzeStatement( index, false, eightBytes, trailor );
            }
        }

        void analyzeStatement( int index, boolean saluteNotCause, byte[] eightBytes, final byte trailor ) {
            //index will be 1 here
            Log.i("Youssef Communi...", "incoming Message length is : " + length);

            if( length - index < 8 ) {
                return;
            }
            System.arraycopy( incomingMessage, index, eightBytes,0,8 );
            double lat = ByteBuffer.wrap( eightBytes ).getDouble();
            Log.i("Youssef Communi...", "lat is : " + lat);

            index += 8;
            if( length - index < 8 ) {
                return;
            }
            System.arraycopy( incomingMessage, index, eightBytes,0,8 );
            double lng = ByteBuffer.wrap( eightBytes ).getDouble();
            Log.i("Youssef Communi...", "lng is : " + lng);

            index += 8;
            if( length - index < 1 ) {
                return;
            }
            int radius = incomingMessage[ index ] & 0xff; //https://stackoverflow.com/questions/14741152/get-unsigned-bytes-in-java
            Log.i("Youssef Communi...", "radius is : " + radius);

            index++;
            char[] userName = new char[ activity.Max_Name_Length ];
            int i;
            for( i = index ; i < length ; i++ ) {
                if( incomingMessage[ i ] != trailor ) {
                    if( i - index < userName.length ) {
                        userName[ i - index ] = (char) incomingMessage[i];
                    } else {
                        return;
                    }
                } else {
                    if( i - index < userName.length ) {
                        userName[ i - index ] = '\0';
                    }
                    break;
                }
            }
            if( i == length ) {
                return;
            }
            final int userNameMaxIndex = i - index;

            Log.i("Youssef Communi...", "userName chars is :");
            for( int j = 0 ; j < userName.length ; j++ ) {
                Log.i("Youssef Communi...", String.valueOf(userName[j]));
            }

            index = i + 1;
            char[] statement = new char[ activity.Max_Statement_Length ];
            for( i = index ; i < length ; i++ ) {
                if( incomingMessage[ i ] != trailor ) {
                    if( i - index < statement.length ) {
                        statement[ i - index ] = (char) incomingMessage[i];
                    } else {
                        return;
                    }
                } else {
                    if( i - index < statement.length ) {
                        statement[ i - index ] = '\0';
                    }
                    break;
                }
            }
            if( i == length ) {
                return;
            }
            final int statementMaxIndex = i - index;

            Log.i("Youssef Communi...", "statement chars is :");
            for( int j = 0 ; j < statement.length ; j++ ) {
                Log.i("Youssef Communi...", String.valueOf(statement[j]));
            }

            index = i + 1;
            if( length - index < 8 ) {
                return;
            }
            System.arraycopy( incomingMessage, index, eightBytes,0,8 );
            long startTime = ByteBuffer.wrap( eightBytes ).getLong();

            index += 8;
            byte[] fourBytes = new byte[4];
            if( length - index < 4 ) {
                return;
            }
            System.arraycopy( incomingMessage, index, fourBytes,0,4 );
            int likes = ByteBuffer.wrap( fourBytes ).getInt();
            Log.i("Youssef Communi...", "likes number is : " + likes);

            index += 4;
            if( length - index < 4 ) {
                return;
            }
            System.arraycopy( incomingMessage, index, fourBytes,0,4 );
            int dislikes = ByteBuffer.wrap( fourBytes ).getInt();
            Log.i("Youssef Communi...", "dislikes number is : " + dislikes);

            index += 4;
            if( length - index < 1 ) {
                return;
            }
            byte end_byte = incomingMessage[ index ];
            if( end_byte != trailor ) {
                return;
            }

            final String userName_str = String.valueOf( userName ).substring(0, userNameMaxIndex );
            final String statement_str = String.valueOf( statement ).substring(0, statementMaxIndex );

            if( saluteNotCause ) {
                Log.i("Youssef Communi...", "Adding a salute custom marker. userName : " + userName_str + ". Statement is " + statement_str);
                activity.markersAction.addCustomMarker(true, lat, lng, radius, userName_str, statement_str, startTime, likes, dislikes );
            } else {
                Log.i("Youssef Communi...", "Adding a cause custom marker. userName : " + userName_str + ". Statement is " + statement_str);
                activity.markersAction.addCustomMarker(false, lat, lng, radius, userName_str, statement_str, startTime, likes, dislikes );
            }

            index++;
            if( length - index < 1 ) {
                return;
            }
            analyzeStartingFromIndex( index );

        }

        @Override
        public void run() {
            //analyse analyze the message to update the status of switches.
            Log.i("Youssef Communi...","ActionThread just started." );
            analyzeStartingFromIndex(0);
        }
    }

    private void manipulateSocketsAndStartTiming( boolean silentToast ) {
        isManipulateSocketsLocked = true;
        //delete old socket after some time later
        socketConnection.new CreateSocketAttempt( socketConnection.nextClientIndex( socketConnection.activeSocketIndex ) ).start();
        isManipulateSocketsLocked = false;
    }

    class DelayToManipulateSockets {
        private int timer = 120000;//2 minutes means 120000. THIS DURATION SHOULD BE CAREFULLY CHOSEN AS SMALLER THAN THE SOCKET LIFETIME INDICATED IN NODEMCU.
        private Handler h = new Handler();

        Runnable manipulate = new Runnable() {
            public void run() {
                Log.i("Youssef Communi...", "DelayToManipulateSockets                      2 minutes are now due, calling manipulateSockets."
                         +
                        " on port " + socketConnection.getPortFromIndex( socketConnection.activeSocketIndex ));
                if (!isManipulateSocketsLocked && !destroySocketTimer.isAwaitingToDestroy) {
                    manipulateSocketsAndStartTiming(true);
                    Log.i("Youssef Communi...", "DelayToManipulateSockets                  finished calling manipulateSockets."
                             +
                            " on port " + socketConnection.getPortFromIndex( socketConnection.activeSocketIndex ));
                } else {
                    if( isManipulateSocketsLocked ) {
                        Log.i("Youssef Communi...", "DelayToManipulateSockets               " +
                                "isManipulateSocketsLocked variable is true, so we won't create a new socket!" +
                                "We are still on port " + socketConnection.getPortFromIndex( socketConnection.activeSocketIndex ));
                    }
                    if( destroySocketTimer.isAwaitingToDestroy ) {
                        Log.i("Youssef Communi...", "DelayToManipulateSockets                  " +
                                "We're within the 8 seconds period to destroy the old socket, so we won't create a new socket!" +
                                "We are still on port " + socketConnection.getPortFromIndex( socketConnection.activeSocketIndex ) );
                    }
                }
            }
        };

        void startTiming() {
            Log.i("Youssef Communi...", "DelayToManipulateSockets                    " +
                    "Starting the 2 minutes timer"  +
                    " on port " + socketConnection.getPortFromIndex( socketConnection.activeSocketIndex ) );
            h.removeCallbacks(manipulate);//this cancel is still important in case the program worked in an unpredictable manner (but may never be necessary in action)
            //i.e. this is a mechanism for the app to fix itself in case a critical race happened.
            // It's a good thing both sockets have only one instance of DelayToManipulateSocket
            h.postDelayed(manipulate, timer);
        }

        void cancelTimer() {
            Log.i("Youssef Communi...", "DelayToManipulateSockets                    " +
                    "Cancelling the timer.");
            h.removeCallbacks(manipulate);
        }
    }

    class DestroySocketTimer { //I made this an inner class of TestAndFixConnection because the volatile variable
        // awaitingToDestroySocket is defined in TestAndFixConnection
        volatile int clientIndex;
        volatile boolean isAwaitingToDestroy = false;
        private int timer = 8000; /*this is like a timeout determined by how fast is the NodeMCU.
            If the NodeMCU takes a delay of 750 ms and if it had like 3 clients so it should bave like 3 x 750 ms = 3000 ms
             + the traveling time, say 1000 ms va et vient.
            such that the NodeMCU handles the requests from the older to the newest.
            There is another note concerning this timer; during this timer (i.e. isAwaitingToDestroy is true) it is not
            allowed to manipulate sockets since the newest socket is very fresh! So it's not so good to wait too long. Anyway,
            there is about 2.6 seconds discounted from the timer for the wrongly decision to be made to manipulate during the timer,
             this 2.6 seconds is the testAndFixConnection read timeout.
        */
        private Handler h = new Handler();

        DestroySocketTimer() {
         /*   Log.i("Youssef Communi...", "DestroySocketTimer                        " +
                    "Creating a new DestroySocket instance."   +
                    " on port " + socketConnection.getPortFromIndex( socketConnection.activeSocketIndex ));
                    */
        }

        Runnable destroy = new Runnable() {
            public void run() {
                Log.i("Youssef Communi...", "DestroySocketTimer. 8 seconds are now due, destroying the old socket" +
                        " on port different than " + socketConnection.getPortFromIndex( socketConnection.activeSocketIndex ) );
                socketConnection.destroySocket(clientIndex);
                isAwaitingToDestroy = false;//unlocking...
            }
        };

        void startTiming(int clientIndex_param) {
            isAwaitingToDestroy = true; //locking...
            clientIndex = clientIndex_param;
//            h.removeCallbacks(destroy);
            h.postDelayed(destroy, timer);
            Log.i("Youssef Communi...", "DestroySocketTimer        " +
                    "Setting a delay so that after 8 seconds, the old socket should be destroyed"
                    + " on port other than " + socketConnection.getPortFromIndex( socketConnection.activeSocketIndex ) );
        }

        void cancelTimer() {
            Log.i("Youssef Communi...", "DestroySocketTimer " +
                    "Cancelling timer.");
            h.removeCallbacks(destroy);
        }
    }
}

class CustomMarker {
    Marker marker;
    //String userName; //set in customMarker.marker.setTitle(...) ( and gotten in customMarker.marker.getTitle() - although we might never get it since it's simply in the title. )
    String statement;
    boolean isSalutNotCase;
    double lat, lng;
    //byte radius; //can be 0, which means no circle. - I have deprecated radius in favour of circle
    Circle circle;
    //long startTime; //this is simply inserted - properly (in a good format) - in the marker's snippet
    int likes = 0, dislikes = 0;

}

class MarkersAction {
    private List<CustomMarker> customMarkersList = new ArrayList<>();
    private final MainActivity activity;
    private Handler showButtonEffect = new Handler();
    private CustomMarker lastMarkerClicked;

    private int Max_Buffer_Size; //48
    private byte message_byte[];

    MarkersAction( final MainActivity activity ) {
        this.activity = activity;


        lastMarkerClicked = new CustomMarker();

        Max_Buffer_Size = 1 + activity.Max_Name_Length + 1 + 8 + 8 + 1 + 8 + 1;
        message_byte = new byte[ Max_Buffer_Size ];

        final ImageButton imageButton_like = activity.findViewById( R.id.imagebutton_like );
        imageButton_like.setOnClickListener( new View.OnClickListener() {
            public void onClick(View v) {
                //show behavior similar to click
                //no need for runOnUiThread since this method is already called from the UI thread
                final int durationOfDisappearance = 15;
                final Animation out = new AlphaAnimation( 1.0f, 0f );
                out.setDuration( durationOfDisappearance );
                imageButton_like.setAnimation( out );
                showButtonEffect.postDelayed( new Runnable() { //better this should be made by the deamon service
                    public void run() {
                        imageButton_like.setVisibility(View.GONE);
                        imageButton_like.setVisibility(View.VISIBLE);
                    }
                }, durationOfDisappearance );

                //get the name of the user (if it's not already entered before) before you start
                String userName = activity.mPrefs.getString("userName", "");
                if( userName.equals("") ) {
                    getUserNameDialog();
                    return;
                }

                //send a message
                message_byte[ 0 ] = 'l'; //as of "like"
                byte[] final_message_byte = completeTheRemainingOfTheMessageToSend( lastMarkerClicked );
                activity.sendMessage( final_message_byte );

            }
        });

        final ImageButton imageButton_dislike = activity.findViewById( R.id.imagebutton_dislike );
        imageButton_dislike.setOnClickListener( new View.OnClickListener() {
            public void onClick(View v) {
                //show behavior similar to click
                final int durationOfDisappearance = 15;
                final Animation out = new AlphaAnimation( 1.0f, 0f );
                out.setDuration( durationOfDisappearance );
                imageButton_dislike.setAnimation( out );
                showButtonEffect.postDelayed( new Runnable() { //better this should be made by the deamon service
                    public void run() {
                        imageButton_dislike.setVisibility(View.GONE);
                        imageButton_dislike.setVisibility(View.VISIBLE);
                    }
                }, durationOfDisappearance );

                //get the name of the user (if it's not already entered before) before you start
                String userName = activity.mPrefs.getString("userName", "");
                if( userName.equals("") ) {
                    getUserNameDialog();
                    return;
                }

                //send a message
                message_byte[ 0 ] = 'd'; //as of "dislike"
                byte[] final_message_byte = completeTheRemainingOfTheMessageToSend( lastMarkerClicked );
                activity.sendMessage( final_message_byte );
            }
        });

    }

    private void getUserNameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder( activity );
        builder.setTitle("لطفاً أدخل إسمك و أعد المحاولة");

        // Set up the input
        final EditText editText_input = new EditText( activity );
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        //editText_input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(editText_input);

        // Set up the buttons
        builder.setPositiveButton("حسناً", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String m_Text = editText_input.getText().toString();
                if( !m_Text.equals("") ) {
                    activity.saveUserNameChangeToPrefs( editText_input );
                }
            }
        });
        builder.setNegativeButton("إلغاء", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private byte[] completeTheRemainingOfTheMessageToSend( CustomMarker lastMarkerClicked ) {
        /*the format :
        *l (for like) or d (for dislike)->statement_type(s for salute or c for cause)->lat->lng->trailor
        * Later it might be
         * l (for like) or d (for dislike)->userName->trailor->
         * statement_type(s for salute or c for cause)->lat->lng->radius->time->trailor
         * */
//        String userName = lastMarkerClicked.marker.getTitle();
        boolean isSalutNotCase = lastMarkerClicked.isSalutNotCase;
        double lat = lastMarkerClicked.lat, lng = lastMarkerClicked.lng;
//        byte radius = lastMarkerClicked.radius;
//        long startTime = lastMarkerClicked.startTime;


        final byte trailor = 127;
        int byte_index = 1;
        /*
        int charsToCopy = userName.length(); //we already know charsToCopy won't be 0
        for( int i = 0 ; i < charsToCopy ; i++ ) {
            message_byte[ i + byte_index ] = (byte) userName.charAt( i );
        }
        byte_index += charsToCopy;
        message_byte[ byte_index ] = trailor;
        byte_index++;
        */
        if( isSalutNotCase ) {
            message_byte[ byte_index ] = 's';
        } else {
            message_byte[ byte_index ] = 'c';
        }
        byte_index++;
        byte[] EightBytes = activity.doubleToByteArray( lat );
        for( int i = 0 ; i < 8 ; i++ ) {
            message_byte[ i + byte_index ] = EightBytes[ i ];
            EightBytes[ i ] = 0;
        }
        byte_index += 8;
        EightBytes = activity.doubleToByteArray( lng );
        for ( int i = 0 ; i < 8 ; i++ ) {
            message_byte[ i + byte_index ] = EightBytes[ i ];
            EightBytes[ i ] = 0;
        }
        byte_index += 8;
        /*
        message_byte[ byte_index ] = radius;
        byte_index++;
        EightBytes = activity.doubleToByteArray( startTime );
        for ( int i = 0 ; i < 8 ; i++ ) {
            message_byte[ i + byte_index ] = EightBytes[ i ];
            EightBytes[ i ] = 0;
        }
        byte_index += 8;
         */
        message_byte[ byte_index ] = trailor;
        /*
        byte_index++;
        if( byte_index < Max_Buffer_Size ) {
            message_byte[ byte_index ] = '\0';
        }
         */
        byte[] final_message_byte = new byte[ byte_index + 1 ];
        System.arraycopy( message_byte,0, final_message_byte,0,byte_index + 1  );
        return final_message_byte;
    }

    synchronized void deleteCustomMarker( boolean salutNotCase, double lat, double lng ) {
        Log.i("Youssef Communi...", "inside deleteCustomMarker");
        Iterator<CustomMarker> iter = customMarkersList.iterator();
        while (iter.hasNext()) { //cannot be replaced with foreach - https://www.programcreek.com/2014/05/top-10-mistakes-java-developers-make/
            CustomMarker customMarker = iter.next();

            if( customMarker != null && customMarker.lat == lat && customMarker.lng == lng &&
                    ( ( customMarker.isSalutNotCase && salutNotCase ) ||
                            ( !customMarker.isSalutNotCase && !salutNotCase ) ) ) {
                final Marker foundMarker = customMarker.marker;
                final Circle circle = customMarker.circle;
                activity.runOnUiThread(new Runnable() {
                                           public void run() {
                                               foundMarker.remove();
                                               if( circle != null ) {
                                                   circle.remove();
                                               }
                                           }
                                       });
                iter.remove();
                return;
            }
        }
    }

    synchronized void addCustomMarker( final boolean salutNotCase, final double lat, final double lng, final int radius,
                                      final String userName, String statement, long startTime, int likes, int dislikes ) {
        //checking first if there is a marker in the same place.
        Log.i("Youssef Communi...", "inside addCustomMarker where customMarkersList count is " + customMarkersList.size() );

        for( CustomMarker customMarker : customMarkersList ) {
            if( customMarker != null && customMarker.lat == lat && customMarker.lng == lng &&
                    ( ( customMarker.isSalutNotCase && salutNotCase ) ||
                            ( !customMarker.isSalutNotCase && !salutNotCase ) ) ) {
                Log.i("Youssef Communi...","A custom marker is found in the same place!");
                //we won't be deleting the Marker, just updating it.
                addFieldsToCustomMarker( customMarker, salutNotCase, lat, lng, radius, userName, statement, startTime, likes, dislikes );
                return;
            }
        }

        //let's create a new CustomMarker and add it in the array list, and then place a marker on the map
        Log.i("Youssef Communi...", "Adding a new CustomMarker to the list");
        CustomMarker customMarker = new CustomMarker();
        customMarker.isSalutNotCase = salutNotCase;
        customMarker.lat = lat;
        customMarker.lng = lng;
        Log.i("Youssef Communi...", "Adding a new CustomMarker to the map");
        addFieldsToCustomMarker( customMarker, salutNotCase, lat, lng, radius, userName, statement, startTime, likes, dislikes );
        customMarkersList.add( customMarker );
    }

    private void addFieldsToCustomMarker( final CustomMarker customMarker, final boolean salutNotCase, final double lat, final double lng,
                                  final int radius, final String userName, String statement, long startTime, int likes, int dislikes ) {
        Log.i("Youssef Communi...", "inside addFieldsToCustomMarker");
        //all following changes will be also made to the array - https://coderanch.com/t/533475/java/pass-reference-Java-loop
        customMarker.statement = statement;
        //              customMarker.radius = radius;
        customMarker.likes = likes;
        customMarker.dislikes = dislikes;
        //our date will be like     Wed 20/11 '19 at 06:26
        Date date = new Date( startTime );
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE dd/MM ''yy 'at' HH:mm");
        final String markerSnippet = simpleDateFormat.format(date);

        activity.runOnUiThread(new Runnable() {
            public void run() {
                if( customMarker.marker == null ) {
                    final LatLng latLng = new LatLng( lat, lng );
                    MarkerOptions markerOptions = new MarkerOptions();
                    markerOptions.position( latLng );
                    customMarker.marker = activity.mapSetup.mMap.addMarker( markerOptions );
                    customMarker.marker.setDraggable(false);
                    if( salutNotCase ) {
                        customMarker.marker.setTag("salute");
                        if( !activity.mapSetup.markersSize.equals("small") ) {
                            customMarker.marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.salute_normal));
                        } else {
                            customMarker.marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.salute_small));
                        }
                        Log.i("Youssef Communi...", "Salute Marker is added (or updated) in addFieldsToCustomMarker by now");
                    } else {
                        customMarker.marker.setTag("cause");
                        if( !activity.mapSetup.markersSize.equals("small") ) {
                            customMarker.marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.cause_normal));
                        } else {
                            customMarker.marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.cause_small));
                        }
                        Log.i("Youssef Communi...", "Cause Marker is added (or updated) in addFieldsToCustomMarker by now");
                    }
                } else if( customMarker.equals(lastMarkerClicked)  ) { //the case where user had beforehand pressed like or dislike. //lastMarkerClicked != null is always true
                    setLikesAndDislikes();
                }
                customMarker.marker.setSnippet( markerSnippet );
                customMarker.marker.setTitle(userName);
                if( customMarker.marker.isInfoWindowShown() ) {
                    customMarker.marker.hideInfoWindow();
                }
                if( activity.mapSetup.markersSize.equals("hidden") ) {
                    customMarker.marker.setVisible(false);
                }

                //Now let's create a circle, but first let's get rid of the previous circle is existing.
                if( customMarker.circle != null ) {
                    customMarker.circle.remove();
                    customMarker.circle = null;
                }
                //creating the new circle if existing
                if( radius != 0 ) {//radius ==0 means the new marker has no circle, i.e. it is a public statement
                    Log.i("Youssef Communi...", "making a circle for " + userName);
                    customMarker.circle = activity.mapSetup.mMap.addCircle( new CircleOptions()
                            .center(new LatLng(lat, lng))
                            .radius(radius)
                            .strokeWidth(3)
                    );
                    if( salutNotCase ) {
                        customMarker.circle.setStrokeColor(Color.BLUE);
                    } else {
                        customMarker.circle.setStrokeColor(Color.RED);
                    }
                    customMarker.circle.setVisible(false);
                    Log.i("Youssef Communi...", "circle visibility is set to false");
                } else {
                    Log.i("Youssef Communi...", "not making a circle for " + userName);
                }
            }
        });
    }

    //this is already called from the UI thread so no need to call runOnUiThread inside it
    void updateLastCustomMarkerClicked( Marker marker ) { //This happens when user clicks on a marker
        //Now fetching and updating lastMarkerClicked fields...
        boolean isCustomMarkerFound = false;
        Log.i("Youssef Communi...", "inside updateLastCustomMarkerClicked");
        if( customMarkersList.isEmpty() ) {
            Log.i("Youssef Communi...", "customMarkersList is empty");
        }
        for( CustomMarker customMarker : customMarkersList ) {
        /*
            if (customMarker.lat == marker.getPosition().latitude) {
                Log.i("Youssef Communi...", "lat is fine");
            } else {
                Log.i("Youssef Communi...", "lat is not fine");
            }
            if( customMarker.lng == marker.getPosition().longitude ) {
                Log.i("Youssef Communi...", "lng is fine");
            } else {
                Log.i("Youssef Communi...", "lng is not fine");
            }
            if(  customMarker.isSalutNotCase && marker.getTag() != null && marker.getTag().equals("salute") ) {
                Log.i("Youssef Communi...", "It's a salute and it's fine");
            } else {
                Log.i("Youssef Communi...", "Not a salute");
            }
            if( !customMarker.isSalutNotCase && marker.getTag() != null && marker.getTag().equals("cause") ) {
                Log.i("Youssef Communi...", "It's a cause and it's fine");
            } else {
                Log.i("Youssef Communi...", "Not a cause");
            }
        */
            if( customMarker != null && customMarker.lat == marker.getPosition().latitude &&
                    customMarker.lng == marker.getPosition().longitude &&
                    ( ( customMarker.isSalutNotCase && marker.getTag() != null && marker.getTag().equals("salute") ) ||
                            ( !customMarker.isSalutNotCase && marker.getTag() != null && marker.getTag().equals("cause") ) ) ) {
                //iter.remove();
                lastMarkerClicked = customMarker;
                isCustomMarkerFound = true;
                break;
            }
        }
        if( !isCustomMarkerFound ) { //I think should not happen
            Log.i("Youssef Communi...", "Marker not found in list !!!!!!!!!!!!!!!!!!!!!!!!!");
            if( customMarkersList == null ) {
                Log.i("Youssef Communi...", "customMarkersList is null");
            }

            return;
        }

                //show the circle is existing for this marker
                if (lastMarkerClicked.circle != null) {
                    /*
                    Log.i("Youssef Communi...", "circle must be shown by now, where lat is " + lastMarkerClicked.circle.getCenter().latitude + " and " +
                            "longitude is " + lastMarkerClicked.circle.getCenter().longitude + " and stroke color is " + lastMarkerClicked.circle.getStrokeColor() +
                            " and stroke width is " + lastMarkerClicked.circle.getStrokeWidth() + " and radius is " + lastMarkerClicked.circle.getRadius());
                    */
                    lastMarkerClicked.circle.setVisible(true);
/*
                    Log.i("Youssef Communi...", "lastMarkerClicked.circle visibility is set to true");
                } else {
                    Log.i("Youssef Communi...", "no circle to show");
 */
                }

                //showing the clicked marker statement
                showMarkerStatement();
    }

    private void setLikesAndDislikes() {
        ( (TextView) activity.findViewById(R.id.TextView_likes) ).setText( String.valueOf( lastMarkerClicked.likes ) );
        ( (TextView) activity.findViewById(R.id.TextView_dislikes) ).setText( String.valueOf( lastMarkerClicked.dislikes ) );
    }

    @SuppressLint("RestrictedApi") //for the sake of setVisibility on fabs
    private void showMarkerStatement() { //this is called by a method that is called when user clickes on a marker
        //no need for runOnUiThread since this method is already called from the UI thread
        ( (TextView) activity.findViewById(R.id.TextView_statement) ).setText(lastMarkerClicked.statement);
        setLikesAndDislikes();

        Log.i("Youssef Communi...", "Now showing the marker statement at the bottom");
        //now showing the statement
        final LinearLayout linearLayout_MarkerStatement = activity.findViewById(R.id.ll_MarkerStatement);
        linearLayout_MarkerStatement.setVisibility(View.VISIBLE);
        Animation showStatement = AnimationUtils.loadAnimation(activity, R.anim.bottom_to_top);
        linearLayout_MarkerStatement.startAnimation(showStatement);
        //we would want to hide the 2 fabs in the bottom as well
        final FloatingActionButton fab_changeMap = activity.findViewById(R.id.fab_changemaplayer);
        final FloatingActionButton fab_seeMyLocation = activity.findViewById(R.id.fab_currentlocation);
        fab_changeMap.setVisibility(View.GONE);
        fab_seeMyLocation.setVisibility(View.GONE);
    }

     void dontShowCircleOfLastClickedMarker() {
        if( lastMarkerClicked.marker != null && lastMarkerClicked.circle != null ) { //lastMarkerClicked != null is always true
            Log.i("Youssef Communi...", "lastMarkerClicked.circle visibility is set to false");
            lastMarkerClicked.circle.setVisible(false);
        }
     }

    //the following is to be tried
    boolean isClickedMarkerInfoWindowShown() { //to help to decide whether to consider the user lost focus on the clicked marker or not
        return lastMarkerClicked.marker != null && lastMarkerClicked.marker.isInfoWindowShown(); //lastMarkerClicked != null is always true
    }

    void hideAllMarkers() {
        for( CustomMarker customMarker : customMarkersList ) {
            if( customMarker != null && customMarker.marker != null) {
                customMarker.marker.setVisible(false);
            }
        }
    }

    void sizeAllMarkers( final String size ) {
        for( CustomMarker customMarker : customMarkersList ) {
            if( customMarker != null && customMarker.marker != null ) {
                String tag = (String) customMarker.marker.getTag();
                if( tag != null ) {
                    customMarker.marker.setVisible(true); //necessary because it is set to false in hideAllMarkers()
                    if( tag.equals("salute") ) {
                        if( size.equals("small") ) {
                            customMarker.marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.salute_small));
                        } else if( size.equals("normal") ) {
                            customMarker.marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.salute_normal));
                        }
                    } else if( tag.equals("cause") ) {
                        if( size.equals("small") ) {
                            customMarker.marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.cause_small));
                        } else if( size.equals("normal") ) {
                            customMarker.marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.cause_normal));
                        }
                    }
                }
            }
        }
    }

}

