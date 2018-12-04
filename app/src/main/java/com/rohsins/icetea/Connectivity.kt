package com.rohsins.icetea

import android.util.Log
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import kotlin.reflect.KCallable

class Connectivity {
    val brokerAddress = "tcp://hardware.wscada.net:1883";
    val clientId = "rohsinsKotlin";
    var persistence : MemoryPersistence ?= null;
    var connectOption : MqttConnectOptions ?= null;
    var mqttClient : MqttClient?= null;

    constructor() {
        try {
            Log.d("VTAG", "starting");
            persistence = MemoryPersistence();
            connectOption = MqttConnectOptions();

            connectOption!!.userName = "rtshardware";
            connectOption!!.password = "rtshardware".toCharArray();
//        connectOption.isCleanSession = false;
//        connectOption.isAutomaticReconnect = true;

            mqttClient = MqttClient(brokerAddress, clientId, persistence);
            mqttClient!!.setCallback(MqttCallbackRoutine());
            mqttClient!!.connect(connectOption);
        } catch (e : Exception) {
            e.printStackTrace();
        }
    }
}

class MqttCallbackRoutine : MqttCallbackExtended {
    override fun connectComplete(reconnect: Boolean, serverURI: String?) {
        Log.d("VTAG", "connected");
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun messageArrived(topic: String?, message: MqttMessage?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun connectionLost(cause: Throwable?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun deliveryComplete(token: IMqttDeliveryToken?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}
