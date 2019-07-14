package com.tk3_beacon_app;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.RemoteException;
import android.support.v4.app.ActivityCompat;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;

import com.lemmingapex.trilateration.NonLinearLeastSquaresSolver;
import com.lemmingapex.trilateration.TrilaterationFunction;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import static android.support.constraint.Constraints.TAG;


public class CustomView extends View implements BeaconConsumer {

    /*
    * initialize variables
    * */

    private BeaconManager beaconManager;

    private Rect rectangle,
            positionRect;

    private Paint paint,
            paintPoint,
            textPaint,
            positionPaint;

    public int amountBeacons;
    public String distanceString = "";

    double[][] positions;
    double[] distances;

    double[][] beaconCoords;

    List<MqttBeaconData> beaconData = new ArrayList<>();


    DecimalFormat df = new DecimalFormat("####0.00");

    int beaconCircleX = 0;
    int beaconCircleY = 0;
    final int beaconCircleRadius = 20;
    int amountBeaconsDrawn = 0;

    int positionTextX = 0;
    int positionTextY = 0;

    private Context mContext;
    boolean checkFirstElement = false;

    String clientId = MqttClient.generateClientId();
    final MqttAndroidClient client =
            new MqttAndroidClient(getActivity(), "tcp://192.168.66.1:1883",
                    clientId);

    public CustomView(Context context) {
        super(context);
        int x = 100, y = 100, sideLength = 100;
        amountBeacons = 0;

        mContext = context;
        beaconCoords = new double[][]{};

        rectangle = new Rect(x, y, sideLength, sideLength);
        positionRect = new Rect(400, 540, 360, 590);

        /*
        * define the painting schemes
        * */

        paint = new Paint();
        paint.setColor(Color.GRAY);

        paintPoint = new Paint();
        paintPoint.setColor(Color.RED);

        textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(40);

        positionPaint = new Paint();
        positionPaint.setColor(Color.GREEN);

        /*
        * setup for the beacon
        * */

        beaconManager = BeaconManager.getInstanceForApplication(getActivity());
        beaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
        beaconManager.bind(this);


        /*
        * connect this client to the mqtt broker
        * */
        try {
            IMqttToken token = client.connect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // We are connected
                    Log.d(TAG, "onSuccess");
                    try {
                        /*
                        * subscribe to all required topics
                        * */
                        String topic = "/laterator/devices/+/x";
                        String topic2 = "/laterator/devices/+/y";
                        String topic3 = "/laterator/devices/+/lastActivity";
                        String topic4 = "/laterator/beacons/+/name";
                        int qos = 1;
                        IMqttToken subToken = client.subscribe(topic, qos);
                        IMqttToken subToken2 = client.subscribe(topic2, qos);
                        IMqttToken subToken3 = client.subscribe(topic3, qos);
                        IMqttToken subToken4 = client.subscribe(topic4, qos);

                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    // Something went wrong e.g. connection timeout or firewall problems
                    Log.d(TAG, "onFailure");

                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }

        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) { }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {

                String[] topics = topic.split("/");

                int i = 0;
                // check if data just needs to get updated or if a new device is found
                boolean newDevice = false;

                for (MqttBeaconData tempData: beaconData) {
                    //check for the right device
                    if(tempData.getUuid().equals(topics[3])){
                        switch(topics[4]){
                            case "x" :
                                tempData.setX(Integer.parseInt(new String(message.getPayload())));
                                beaconData.set(i, tempData);
                                break;
                            case "y" :
                                tempData.setY(Integer.parseInt(new String(message.getPayload())));
                                beaconData.set(i, tempData);
                                break;
                            case "lastActivity" :
                                tempData.setTimestamp(new Date().getTime());
                                beaconData.set(i, tempData);
                                break;
                            case "name" :
                                tempData.setName(new String(message.getPayload()));
                                beaconData.set(i, tempData);
                                break;
                            default: break;
                        }
                        newDevice = true;
                    }

                    //add the new device and check which data got send via mqtt
                    if(i == beaconData.size() -1 && !newDevice){
                        MqttBeaconData tempBeaconData = new MqttBeaconData();

                        switch(topics[4]){
                            case "x" :
                                tempBeaconData.setX(Integer.parseInt(new String(message.getPayload())));
                                beaconData.add(tempBeaconData);
                                break;
                            case "y" :
                                tempBeaconData.setY(Integer.parseInt(new String(message.getPayload())));
                                beaconData.add(tempBeaconData);
                                break;
                            case "lastActivity" :
                                tempData.setTimestamp(new Date().getTime());
                                beaconData.add(tempBeaconData);
                                break;
                            case "name" :
                                tempData.setName(new String(message.getPayload()));
                                beaconData.add(tempBeaconData);
                                break;
                            default: break;
                        }
                    }
                    i++;
                }

                /*
                if (topic.contains(beacon1)) {
                    if (topic.endsWith("x"))
                        beaconData.add(new MqttBeaconData(beacon1, Integer.parseInt(new String(message.getPayload())), "x"));
                    else
                        beaconData.add(new MqttBeaconData(beacon1, Integer.parseInt(new String(message.getPayload())), "y"));
                } else if (topic.contains(beacon2)) {
                    if (topic.endsWith("x"))
                        beaconData.add(new MqttBeaconData(beacon1, Integer.parseInt(new String(message.getPayload())), "x"));
                    else
                        beaconData.add(new MqttBeaconData(beacon1, Integer.parseInt(new String(message.getPayload())), "y"));
                } else if (topic.contains(beacon3)) {
                    if (topic.endsWith("x"))
                        beaconData.add(new MqttBeaconData(beacon1, Integer.parseInt(new String(message.getPayload())), "x"));
                    else
                        beaconData.add(new MqttBeaconData(beacon1, Integer.parseInt(new String(message.getPayload())), "y"));
                } else if (topic.contains(beacon4)) {
                    if (topic.endsWith("x"))
                        beaconData.add(new MqttBeaconData(beacon1, Integer.parseInt(new String(message.getPayload())), "x"));
                    else
                        beaconData.add(new MqttBeaconData(beacon1, Integer.parseInt(new String(message.getPayload())), "y"));
                }
                */
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) { }
        });
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.WHITE);
        canvas.drawRect(rectangle, paint);

        positions = new double[][]{{0, 1000}, {1000, 1000}, {1000, 0}, {0, 0}};
        distances = new double[]{0, 0, 0, 0};

        int amountOfBeacons = positions.length;
        amountBeaconsDrawn++;


        // draw the beacons in the corners

        for (int i = 0; i < amountOfBeacons; i++) {
            if (i == 0 || i == amountOfBeacons - 1)
                beaconCircleX = 100;
            else
                beaconCircleX = 1000;

            if (i > 1)
                beaconCircleY = 1000;
            else
                beaconCircleY = 100;

            canvas.drawCircle(beaconCircleX, beaconCircleY, beaconCircleRadius, paintPoint);
            canvas.drawText("Beacon " + (i + 1), beaconCircleX + 60, beaconCircleY + 60, textPaint);
        }


        // draw all connected devices with the distance
        for(int i = 0; i < beaconData.size(); i++){

            if((new Date().getTime() - beaconData.get(i).getTimestamp()) < 30000){
                positionRect = new Rect(beaconData.get(i).getX(), beaconData.get(i).getY(), beaconData.get(i).getX() + 50, beaconData.get(i).getY() + 50);
                canvas.drawRect(positionRect, positionPaint);
                canvas.drawText(beaconData.get(i).getName(), beaconData.get(i).getX(), beaconData.get(i).getY() + 100, textPaint);
            }
        }

        canvas.drawText(amountBeacons + " beacons are currently active", 100, 1200, textPaint);
        canvas.drawText("Distances to beacons: " + distanceString, 100, 1400, textPaint);


        this.invalidate();

        //update the number of beacons that are connected
        if (amountBeaconsDrawn == 4)
            amountBeaconsDrawn = 0;

    }

    //pass the activity context to this view
    private Activity getActivity() {
        Context context = getContext();
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    /*
    * work with the bluetooth data sent
    * */
    @Override
    public void onBeaconServiceConnect() {
        beaconManager.removeAllRangeNotifiers();
        beaconManager.addRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                distanceString = "";
                amountBeacons = 0;

                Iterator<Beacon> iter = beacons.iterator();

                Beacon temp;
                distances = new double[beacons.size()];


                for (int i = 0; i < beacons.size(); i++) {
                    temp = iter.next();
                    distanceString += String.valueOf(df.format(temp.getDistance()) + "m ");
                    distances[i] = temp.getDistance();
                }

                if(distances.length != positions.length){
                    while(distances.length < positions.length){
                        Arrays.copyOf(positions, positions.length -1);
                    }
                }

                // get the distance of the device to the beacons
                NonLinearLeastSquaresSolver solver = new NonLinearLeastSquaresSolver(new TrilaterationFunction(positions, distances), new LevenbergMarquardtOptimizer());
                LeastSquaresOptimizer.Optimum solution = solver.solve();
                double[] cords = solution.getPoint().toArray();


                //update the coordinates for the position
                positionRect = new Rect((int) cords[0], (int) cords[1], (int) cords[0] + 50, (int) cords[1] + 50);
                positionTextX = (int) cords[0];
                positionTextY = (int) cords[1] + 100;

                amountBeacons = beacons.size();

                publishMqttData((int) cords[0], (int) cords[1]);
            }
        });

        try {
            beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
        } catch (RemoteException e) {
        }
    }

    @Override
    public Context getApplicationContext() {
        return null;
    }

    @Override
    public void unbindService(ServiceConnection serviceConnection) {

    }

    @Override
    public boolean bindService(Intent intent, ServiceConnection serviceConnection, int i) {
        return false;
    }

    public void publishMqttData(int x, int y) {

        //get the permission
        TelephonyManager tManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions( getActivity(), new String[]{Manifest.permission.READ_PHONE_STATE}, 1);
        }
        String uuid = tManager.getDeviceId();


        //initializing all the variables required for the publishing ...
        String timestamp_topic = "/laterator/devices/" + uuid + "/lastActivity";
        String name_topic = "/laterator/beacons/" + uuid + "/name";
        String x_topic = "/laterator/devices/" + uuid + "/x";
        String y_topic = "/laterator/devices/" + uuid + "/y";

        String timestamp_payload = String.valueOf(new Date().getTime());
        String name_payload = "Luke";
        String x_payload = String.valueOf(x);
        String y_payload = String.valueOf(y);

        byte[] encodedPayload_timestamp = new byte[0];
        byte[] encodedPayload_name = new byte[0];
        byte[] encodedPayload_x = new byte[0];
        byte[] encodedPayload_y = new byte[0];

        try {
            encodedPayload_timestamp = timestamp_payload.getBytes("UTF-8");
            encodedPayload_name = name_payload.getBytes("UTF-8");
            encodedPayload_x = x_payload.getBytes("UTF-8");
            encodedPayload_y = y_payload.getBytes("UTF-8");

            MqttMessage message_timestamp = new MqttMessage(encodedPayload_timestamp);
            MqttMessage message_name = new MqttMessage(encodedPayload_name);
            MqttMessage message_x = new MqttMessage(encodedPayload_x);
            MqttMessage message_y = new MqttMessage(encodedPayload_y);
            message_timestamp.setRetained(true);
            message_name.setRetained(true);
            message_x.setRetained(true);
            message_y.setRetained(true);
            client.publish(timestamp_topic, message_timestamp);
            client.publish(name_topic, message_name);
            client.publish(x_topic, message_x);
            client.publish(y_topic, message_y);

            //add the first element to the list of all connected devices
            if(!checkFirstElement){
                MqttBeaconData tempBeaconData = new MqttBeaconData();
                tempBeaconData.setUuid(uuid);
                tempBeaconData.setTimestamp(new Date().getTime());
                tempBeaconData.setName(name_payload);
                tempBeaconData.setX(x);
                tempBeaconData.setY(y);
                beaconData.add(tempBeaconData);
                checkFirstElement = true;
            }

        } catch (UnsupportedEncodingException | MqttException e) {
            e.printStackTrace();
        }
    }
}