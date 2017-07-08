package com.location.app.locationlibrary;

import android.graphics.Bitmap;
import android.util.Log;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by Ting on 17/7/4.
 */

public class MemoryCache {
    private static final String TAG = "MemoryCache";

    /**
     * 放入缓存时是个同步操作
     * LinkedHashMap构造方法的最后一个参数true代表这个map里的元素将按照最近使用次数由少到多排列，即LRU。
     * 这样的好处是如果要将缓存中的元素替换，则先遍历出最近最少使用的元素来替换以提高效率
     */
    private Map<String, String> cache
            = Collections.synchronizedMap(new LinkedHashMap<String, String>(10, 0.75f, true));

    // 缓存中图片所占用的字节，初始0，将通过此变量严格控制缓存所占用的堆内存
    private long size = 0;
    // 缓存只能占用的最大堆内存
    private long limit = 10000;

    public MemoryCache() {
        setLimit(Runtime.getRuntime().maxMemory() / 8);
    }


    public void setLimit(long new_limit) {
        limit = new_limit;
        Log.i(TAG, "MemoryCache will use up to " + limit / 1024. / 1024. + "MB");
    }


    public String get(String id) {
        try {
            if (!cache.containsKey(id))
                return null;

            return cache.get(id);

        } catch (NullPointerException ex) {
            return null;
        }

    }


    public void put(String id, String message) {
        try {
            if (cache.containsKey(id))
                size -= getSizeInBytes(cache.get(id));

            cache.put(id, message);
            size += getSizeInBytes(message);
            checkSize();
        } catch (Throwable th) {
            th.printStackTrace();
        }

    }


    /**
     * 严格控制堆内存，如果超过将首先替换最近最少使用的那个图片缓存
     */

    private void checkSize() {
        Log.i(TAG, "cache size=" + size + " length=" + cache.size());
        if (size > limit) {
            // 先遍历最近最少使用的元素
            Iterator<Map.Entry<String, String>> iter = cache.entrySet().iterator();

            while (iter.hasNext()) {
                Map.Entry<String, String> entry = iter.next();
                size -= getSizeInBytes(entry.getValue());
                iter.remove();
                if (size <= limit)
                    break;
            }
            Log.i(TAG, "Clean cache. New size " + cache.size());
        }
    }


    public void clear() {
        cache.clear();
    }


    /**
     * 图片占用的内存
     *
     * @return
     */

    long getSizeInBytes(String message) {
        if (message == null)
            return 0;

        return message.getBytes().length;
    }
}
