package com.github.ROS_E_Stop;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.github.ROS_E_Stop.R;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;
import com.jaredrummler.android.device.DeviceName;

import org.ros.address.InetAddressFactory;
import org.ros.android.RosActivity;
import org.ros.concurrent.CancellableLoop;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;

import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;

import geometry_msgs.Twist;
import std_msgs.Bool;
import std_msgs.String;


public class MainActivity extends RosActivity implements RotationGestureDetector.OnRotationGestureListener {

    private ImageButton redButton;  //button
    private EStopNode eStopNode;    //ROS node
    private Vibrator vibrate;       //to make phone vibrate

    private ImageView connectionImage;

    private float rotationReturnPoint = 0;  //snap back point for the button to stick to if rotated beyond boundary
    private float startAngle;               //the angle of the button's rotation when rotation gesture is detected

    private RotationGestureDetector mRotationDetector;  //detects a rotate gesture on the button
    private boolean pressed = false;

    private NodeMainExecutor nodeMainExecutor;

    public static java.lang.String MASTER_URI;

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    public MainActivity() {
        // The RosActivity constructor configures the notification title and ticker messages.
        super("ROS E-Stop", "ROS E-Stop", MASTER_URI != null ? URI.create(MASTER_URI) : null);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        vibrate = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);    //init vibrator
        redButton = (ImageButton) findViewById(R.id.red_button);                    //get button view

        connectionImage = (ImageView) findViewById(R.id.connectionImage);

        mRotationDetector = new RotationGestureDetector(this, redButton);   //init rotation detector

        redButton.setScaleX(1.15f);     //make button bigger
        redButton.setScaleY(1.15f);

        redButton.setOnTouchListener(new View.OnTouchListener() {   //set up buttons touch listener
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                mRotationDetector.onTouchEvent(event);      //send touch events to rotate detector

                if (event.getAction() == MotionEvent.ACTION_DOWN && !pressed)    //if pressing down and button not pushed
                {

                    final int x = (int) event.getX();
                    final int y = (int) event.getY();

                    //now map the coords we got to the
                    //bitmap (because of scaling)
                    ImageView imageView = ((ImageButton) v);
                    Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
                    int pixel = bitmap.getPixel(x, y);

                    //now check alpha for transparency
                    int alpha = Color.alpha(pixel);
                    if (alpha != 0) {
                        pressed = true;       //set flag
                        vibrate.vibrate(300);   //vibrate
                        redButton.animate().scaleX(1).scaleY(1).setDuration(200).start();   //make button smaller to look pressed down, animate                 }
                    }

                }
                return false;
            }
        });
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }



    protected void init(NodeMainExecutor nodeMainExecutor)  //called when ROS master starts node
    {
        java.lang.String ip = InetAddressFactory.newNonLoopback().getHostAddress();   //get IP address
        ip = ip.replace(".", "_");        //replace '.'s with '_'s for a valid node name

        this.nodeMainExecutor = nodeMainExecutor;

        eStopNode = new EStopNode(ip);        //create ros node object

        NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress());    //set up node configuration
        nodeConfiguration.setMasterUri(getMasterUri());

        nodeMainExecutor.execute(eStopNode, nodeConfiguration);     //start the node

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d("DEBUG", "Function is running");
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.restart_app:

                AlertDialog.Builder adb = new AlertDialog.Builder(this);
                adb.setTitle("Restart App");
                adb.setIcon(android.R.drawable.ic_dialog_alert);
                adb.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent mStartActivity = new Intent(getBaseContext(), MainActivity.class);
                        int mPendingIntentId = 123456;
                        PendingIntent mPendingIntent = PendingIntent.getActivity(getBaseContext(), mPendingIntentId,    mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
                        AlarmManager mgr = (AlarmManager)getBaseContext().getSystemService(Context.ALARM_SERVICE);
                        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
                        System.exit(0);
                    } });


                adb.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    } });
                adb.show();


                return true;
            case R.id.quit_app:
                nodeMainExecutor.shutdown();
                return true;

            case R.id.change_topic:
                eStopNode.updateTopicNameTest();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    public void OnRotation(RotationGestureDetector rotationDetector)    //for rotation detector
    {
        float angle = rotationDetector.getAngle();  //get delta angle
        float finalAngle = startAngle + (angle);    //add delta angle to our start angle

        //rotation return point is the point the button will be rotated to if they pass the boundary
        //for example if they try to rotate to -10 degrees, it will stay at 0

        if (finalAngle >= 0 && finalAngle <= 45)     //if in the first half of the rotation, set return to the beginning
            rotationReturnPoint = 0;
        else if (finalAngle <= 90 && finalAngle > 45)    //if in second half of the rotation, set return to end
            rotationReturnPoint = 90;

        if (!(finalAngle >= 0 && finalAngle < 90))       //if final angle is out of bounds, go back to the return point
        {
            finalAngle = rotationReturnPoint;
        }
        redButton.setRotation(finalAngle);              //set the rotation of the button
    }

    @Override
    public void OnRotationStop(RotationGestureDetector rotationDetector)    //for rotate detector, called when rotation stops
    {
        if (redButton.getRotation() != 0)    //if button is not already at 0, animate and rotate back to 0
        {
            float finalRotation = redButton.getRotation();  //get the current button rotation
            redButton.animate().rotation(0).setDuration(300).start();   //start the animation rotating back to 0
            redButton.setRotation(0);                                   //make sure rotation is set to 0
            rotationReturnPoint = 0;                                    //reset return point
            if (pressed && (finalRotation > 80))     //if button is pressed and is rotated far enough, release button
            {
                redButton.animate().scaleX(1.15f).scaleY(1.15f).setDuration(300).start();   //make button back to normal size
                pressed = false;                          //reset flag
                vibrate.vibrate(new long[]{0, 200, 200, 200}, -1);  //vibrate two times
            }
        }
    }

    @Override
    public void OnRotationBegin(RotationGestureDetector rotationGestureDetector)    //for rotation detector, called when rotation starts
    {
        startAngle = redButton.getRotation();   //store starting angle for calculating final angle
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Main Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }


    private class EStopNode extends AbstractNodeMain {
        private Publisher<Twist> velPublisher;
        private Publisher<Bool> statusPublisher;
        private Publisher<String> modelPublisher;
        private Subscriber<String> masterChecker;


        private Timer statusUpdateTimer;
        private TimerTask statusUpdateTask;
        int statusTime = 1000;

        private java.lang.String outputVelTopicName = "/e_stop/cmd_vel";
        private java.lang.String ipAddress;

        private long lastMasterCheckTime;

        private boolean connectionError = false;


        private ConnectedNode myConnectedNode;
        private boolean publishVel;


        public EStopNode(java.lang.String ipAddress) {
            this.ipAddress = ipAddress;
        }

        @Override
        public GraphName getDefaultNodeName() {
            return GraphName.of("/E_Stop_Node_" + ipAddress);
        }

        @Override
        public void onStart(final ConnectedNode connectedNode) {

            myConnectedNode = connectedNode;
            publishVel = false;

            velPublisher = connectedNode.newPublisher(GraphName.of(outputVelTopicName), Twist._TYPE);

            masterChecker = connectedNode.newSubscriber("/e_stop/master_checker", String._TYPE);
            masterChecker.addMessageListener(new MessageListener<String>() {
                @Override
                public void onNewMessage(String message) {
                    lastMasterCheckTime = System.currentTimeMillis();
                }
            });

            statusPublisher = connectedNode.newPublisher(GraphName.of("/e_stop/status"), Bool._TYPE);

            lastMasterCheckTime = System.currentTimeMillis();
            modelPublisher = connectedNode.newPublisher(GraphName.of("/" + getDefaultNodeName().toString() + "/model"), String._TYPE);

            modelPublisher.setLatchMode(true);
            String modelMsg = modelPublisher.newMessage();
            modelMsg.setData(DeviceName.getDeviceName());
            modelPublisher.publish(modelMsg);

            statusUpdateTask = new TimerTask() {
                @Override
                public void run() {
                    updateStatus();
                }
            };

            statusUpdateTimer = new Timer();

            statusUpdateTimer.schedule(statusUpdateTask, 0, statusTime);

            final CancellableLoop loop = new CancellableLoop() {
                @Override
                protected void loop() throws InterruptedException {

                    if (pressed && publishVel) {
                        Twist twist = velPublisher.newMessage();
                        twist.getLinear().setX(0);
                        twist.getLinear().setY(0);
                        twist.getLinear().setZ(0);
                        twist.getAngular().setX(0);
                        twist.getAngular().setY(0);
                        twist.getAngular().setZ(0);
                        velPublisher.publish(twist);
                    }
                    if (System.currentTimeMillis() - lastMasterCheckTime > 3000) {
                        if (!connectionError) {
                            connectionError = true;
                            MainActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    connectionImage.setImageResource(R.drawable.red_circle);
                                    vibrate.vibrate(1000);
                                }
                            });
                        }

                    } else {
                        if (connectionError) {
                            vibrate.vibrate(new long[]{0, 300, 200, 300}, -1);  //vibrate two times
                        }
                        connectionError = false;
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                connectionImage.setImageResource(R.drawable.green_circle);
                                //vibrate.vibrate(1000);
                            }
                        });

                    }

                    Thread.sleep(10);
                }
            };
            publishVel = true;
            connectedNode.executeCancellableLoop(loop);
        }

        @Override
        public void onShutdown(Node node) {
            Log.d("DEBUG", "in node shutdown");
            //finish();
        }

        @Override
        public void onShutdownComplete(Node node) {
            Log.d("DEBUG", "in node complete shutdown");
            finish();
        }

        private void updateStatus() {
            Bool msg = statusPublisher.newMessage();
            msg.setData(true);
            statusPublisher.publish(msg);
        }

        public void updateTopicNameTest()
        {
            publishVel = false;
            velPublisher.shutdown();
            velPublisher = myConnectedNode.newPublisher(GraphName.of("/test/cmd_vel"), Twist._TYPE);
            publishVel = true;
        }

    }
}
