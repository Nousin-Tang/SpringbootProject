package com.base.nousin.framework.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Http 请求类
 *
 * @author tangwc
 * @since 2019-12-2
 */
public class HttpUtil {
    /**
     * 发起GET请求，并将结果转换成对象
     *
     * @param urlParam 连接
     * @param param    参数
     * @param clazz    转换对象的class
     * @return 对象
     */
    public static <T> T get(String urlParam, Object param, Class<T> clazz) {
        try {
            URL url = new URL(urlParam);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET"); // 设置请求方式
            String result = sendRequest(connection, param);
            return JsonUtil.fromJsonString(result, clazz);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 发起GET请求，并将结果转换成对象
     *
     * @param urlParam 连接
     * @param param    参数
     * @return 结果字符串
     */
    public static String getStr(String urlParam, Object param) {
        try {
            URL url = new URL(urlParam);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET"); // 设置请求方式
            return sendRequest(connection, param);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 发起POST请求，并将结果转换成对象
     *
     * @param urlParam 连接
     * @param param    参数
     * @param clazz    转换对象的class
     * @return 对象
     */
    public static <T> T post(String urlParam, Object param, Class<T> clazz) {
        try {
            URL url = new URL(urlParam);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST"); // 设置请求方式
            String result = sendRequest(connection, param);
            return JsonUtil.fromJsonString(result, clazz);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * @param connection
     * @param param
     * @return
     */
    private static String sendRequest(HttpURLConnection connection, Object param) {
        try {
            connection.setDoInput(true); // 设置可输入
            connection.setDoOutput(true); // 设置该连接是可以输出的
            connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            String paramStr = new ObjectMapper().writeValueAsString(param);
            // 写
            PrintWriter pw = new PrintWriter(new BufferedOutputStream(connection.getOutputStream()));
            pw.write(paramStr);
            pw.flush();
            pw.close();
            // 读
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));
            String line = null;
            StringBuilder result = new StringBuilder();
            while ((line = br.readLine()) != null) { // 读取数据
                result.append(line);
            }
            connection.disconnect();
            return result.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
