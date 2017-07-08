package com.core.app.utils

import android.util.Log
import com.location.app.locationlibrary.MemoryCache
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.util.ArrayList

/**
 * Created by Ting on 17/6/19.
 */

class MqttSend(broker: String, accessKey: String, secretKey: String) {
    val broker = broker
    val accessKey = accessKey
    val secretKey = secretKey
    val persistence = MemoryPersistence()
    val memoryCache = MemoryCache()
    val messageList = ArrayList<String>()

    fun sendMessage(topic: String, producerClientId: String, detailJson: String) = try {
        val sampleClient = MqttClient(broker, producerClientId, persistence)
        val connOpts = MqttConnectOptions()
        println("Connecting to broker: " + broker)
        val sign: String = MqttMacSignature().mqttMacSignature(producerClientId.split(("@@@").toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()[0], secretKey)

        /**
         * 计算签名，将签名作为MQTT的password。
         * 签名的计算方法，参考工具类MacSignature，第一个参数是ClientID的前半部分，即GroupID
         * 第二个参数阿里云的SecretKey
         */
        connOpts.userName = accessKey
        connOpts.serverURIs = arrayOf(elements = broker)
        connOpts.password = sign.toCharArray()
        connOpts.isCleanSession = false
        connOpts.keepAliveInterval = 100

        sampleClient.setCallback(object : MqttCallback {
            override fun connectionLost(throwable: Throwable) {
                System.out.println("mqtt connection lost")
                throwable.printStackTrace()
                while (!sampleClient.isConnected) {
                    try {
                        sampleClient.connect(connOpts)
                        System.out.println("No----Connected")
                        if (!detailJson.equals(detailJson)) {
                            messageList.add(detailJson)
                        }
                        for (index in messageList.indices) {
                            var key = "msg" + index
                            memoryCache.put(key, messageList[index]);
                        }
                        //memoryCache.p
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
                System.out.println("messageArrived:" + topic + "------" + String(mqttMessage.payload))
            }

            override fun deliveryComplete(iMqttDeliveryToken: IMqttDeliveryToken) {
                System.out.println("deliveryComplete:" + iMqttDeliveryToken.messageId)
            }
        })
        sampleClient.connect(connOpts)
        try {
            var content: String = ""
            if (messageList.size > 0) {
                for (index in messageList.indices) {
                    content = memoryCache.get("msg" + index)
                }
            } else {
                content = detailJson
            }
            val message = MqttMessage(content.toByteArray())
            message.qos = 0

            println("detailJson:" + message)
            println("producerClientId:" + producerClientId + "------------" + "topic:" + topic)
            /**
             *消息发送到某个主题Topic，所有订阅这个Topic的设备都能收到这个消息。
             * 遵循MQTT的发布订阅规范，Topic也可以是多级Topic。此处设置了发送到二级topic
             */
            sampleClient.publish(topic, message)
            /**
             * 如果发送P2P消息，二级Topic必须是“p2p”,三级topic是目标的ClientID
             * 此处设置的三级topic需要是接收方的ClientID
             */
            //            val p2pTopic = topic + "/p2p/" + consumerClientId;
//            sampleClient.publish(p2pTopic, message)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    } catch (me: Exception) {
        me.printStackTrace()
    }
}