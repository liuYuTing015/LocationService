package com.core.app.utils

import org.apache.commons.codec.binary.Base64
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Created by Ting on 17/6/19.
 */

class MqttMacSignature {

    @Throws(InvalidKeyException::class, NoSuchAlgorithmException::class)
    fun mqttMacSignature(text: String, secretKey: String): String {
        val charset = Charsets.UTF_8
        val algorithm: String = "HmacSHA1"
        val mac: Mac = Mac.getInstance(algorithm)
        mac.init(SecretKeySpec(secretKey.toByteArray(charset), algorithm))
        val bytes = mac.doFinal(text.toByteArray(charset))
        return String(Base64.encodeBase64(bytes), charset)
    }

    /**
     * 发送方签名方法
     *
     * @param clientId  Mqtt ClientId
     * @param secretKey 阿里云MQ secretKey
     * @return 加密后的字符串
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     */
    @Throws(NoSuchAlgorithmException::class, InvalidKeyException::class)
    fun publishSignature(clientId: String, secretKey: String): String = mqttMacSignature(clientId, secretKey)

    /**
     * 订阅方签名方法
     *
     * @param topics    要订阅的Topic集合
     * @param clientId  Mqtt ClientId
     * @param secretKey 阿里云MQ secretKey
     * @return 加密后的字符串
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     */
    @Throws(NoSuchAlgorithmException::class, InvalidKeyException::class)
    fun subSignature(topics: List<String>, clientId: String, secretKey: String): String {
        Collections.sort(topics)
        var topicText = "";
        for (topic in topics) {
            topicText += topic + "\n"
        }
        var text = topicText + clientId
        return mqttMacSignature(text, secretKey)
    }

    /**
     * 订阅方签名方法
     *
     * @param topic     要订阅的Topic
     * @param clientId  Mqtt ClientId
     * @param secretKey 阿里云MQ secretKey
     * @return 加密后的字符串
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     */
    @Throws(NoSuchAlgorithmException::class, InvalidKeyException::class)
    fun subSignature(topic: String, clientId: String, secretKey: String): String {
        val topics = ArrayList<String>()
        topics.add(topic)
        return subSignature(topics, clientId, secretKey)
    }
}
