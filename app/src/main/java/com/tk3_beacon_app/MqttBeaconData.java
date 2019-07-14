package com.tk3_beacon_app;

/*
* class for the structure to save the data retrieved via mqtt
* */

public class MqttBeaconData {

    private String uuid;
    private int coordX;
    private int coordY;
    private long timestamp;
    private String name;

    public MqttBeaconData(){ }

    public String getUuid(){
        return this.uuid;
    }

    public int getX(){
        return this.coordX;
    }

    public int getY(){
        return this.coordY;
    }

    public void setTimestamp(long timestamp){
        this.timestamp = timestamp;
    }

    public void setName(String name){
        this.name = name;
    }

    public long getTimestamp(){
        return this.timestamp;
    }

    public String getName(){
        return this.name;
    }

    public void setUuid(String uuid){
        this.uuid = uuid;
    }

    public void setX(int x){
        this.coordX = x;
    }

    public void setY(int y){
        this.coordY = y;
    }
}
