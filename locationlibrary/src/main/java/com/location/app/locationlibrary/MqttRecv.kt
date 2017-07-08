package com.core.app.utils

import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.io.IOException

/**
 * Created by Ting on 17/6/19.
 */

class MqttRecv {

    /**
     * 设置当前用户私有的MQTT的接入点。例如此处示意使用XXX，实际使用请替换用户自己的接入点。接入点的获取方法是，在控制台申请MQTT实例，每个实例都会分配一个接入点域名。
     */
    val broker = ""
    /**
     * 设置阿里云的AccessKey，用于鉴权
     */
    val accessKey = "";
    /**
     * 设置阿里云的SecretKey，用于鉴权
     */
    val secretKey = "";
    /**
     * 发消息使用的一级Topic，需要先在MQ控制台里申请
     */
    val topic = "tracks";
    /**
     * MQTT的ClientID，一般由2部分组成，GroupID@@@DeviceID
     * 其中GroupID在MQ控制台里申请
     * DeviceID由应用方设置，可能是设备编号等，需要唯一，否则服务端拒绝重复的ClientID连接
     */
    val consumerClientId = "GID_track@@@DEVICE_002";
    val producerClientId = "GID_track@@@DEVICE_002";
    val persistence = MemoryPersistence()

    fun receiveMessage() {
        try {
            val sampleClient = MqttClient(broker, consumerClientId, persistence)
            val connOpts = MqttConnectOptions()
            println("Connecting to broker: " + broker)
            val sign: String = MqttMacSignature().mqttMacSignature(consumerClientId.split(("@@@").toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()[0], secretKey)
            /**
             * 计算签名，将签名作为MQTT的password。
             * 签名的计算方法，参考工具类MacSignature，第一个参数是ClientID的前半部分，即GroupID
             * 第二个参数阿里云的SecretKey
             */
            /**
             * 设置订阅方订阅的Topic集合，此处遵循MQTT的订阅规则，可以是一级Topic，二级Topic,P2P消息不需要显式订阅，
             */
            val topicFilters = arrayOf<String>(topic + "/notice/")
            val qos = intArrayOf(0)
            connOpts.userName = accessKey
            connOpts.serverURIs = arrayOf<String>(broker)
            connOpts.password = sign.toCharArray()
            connOpts.isCleanSession = false
            connOpts.keepAliveInterval = 100
            sampleClient.setCallback(object : MqttCallback {
                override fun connectionLost(throwable: Throwable) {
                    println("mqtt connection lost")
                    throwable.printStackTrace()
                    while (!sampleClient.isConnected) {
                        try {
                            sampleClient.connect(connOpts)
                            sampleClient.subscribe(topicFilters, qos)
                        } catch (e: MqttException) {
                            e.printStackTrace()
                        }
                        try {
                            Thread.sleep(1000)
                        } catch (e: InterruptedException) {
                            e.printStackTrace()
                        }
                    }
                }

                @Throws(Exception::class)
                override fun messageArrived(topic: String, mqttMessage: MqttMessage) {
                    println("messageArrived:" + topic + "------" + String(mqttMessage.getPayload()))
                }

                override fun deliveryComplete(iMqttDeliveryToken: IMqttDeliveryToken) {
                    System.out.println("deliveryComplete:" + iMqttDeliveryToken.getMessageId())
                }
            })
            sampleClient.connect(connOpts)
            sampleClient.subscribe(topicFilters, qos)
            Thread.sleep(Integer.MAX_VALUE.toLong())
        } catch (me: Exception) {
            me.printStackTrace()
        }
    }
}
