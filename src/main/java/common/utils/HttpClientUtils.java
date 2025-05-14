package common.utils;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Classname HttpClientUtils
 * @Description 请求工具类
 */

@Slf4j
public class HttpClientUtils {

    private static RequestConfig requestConfig = null;

    private final static String DEFAULT_PROTOCOL = "http://";

    static {
        // 设置请求和传输超时时间
        requestConfig = RequestConfig.custom().setSocketTimeout(60000).setConnectTimeout(60000).build();
    }

    public static final String UTF8 = "UTF-8";

    /**
     * httpPost
     *
     * @param url      请求url
     * @param paramMap 请求参数
     * @return
     * @throws Exception
     */
    public static String httpPost(String url, Map<String, Object> paramMap) throws Exception {
        return httpPost(url, paramMap, null, UTF8);
    }

    /**
     * @param url      请求url
     * @param paramMap 请求参数
     * @param head     请求头
     * @return
     * @throws Exception
     */
    public static String httpPost(String url, Map<String, Object> paramMap, Map<String, String> head) throws Exception {
        return httpPost(url, paramMap, head, UTF8);
    }

    /**
     * @param url      请求url
     * @param paramMap 请求参数
     * @param head     请求头
     * @param charSet  字符集
     * @return
     * @throws Exception
     */
    private static String httpPost(String url, Map<String, Object> paramMap, Map<String, String> head,
                                   String charSet) throws Exception {
        CloseableHttpClient client = HttpClients.createDefault();
        CloseableHttpResponse response = null;
        String result = "";
        try {
            if (head == null) {
                head = new HashMap<>();
            }
            if (StringUtils.isEmpty(head.get("Content-Type"))) {
                head.put("Content-Type", "application/x-www-form-urlencoded");
            }
            if (StringUtils.isEmpty(head.get("Accept"))) {
                head.put("Accept", "application/json");
            }
            url = enhanceUrl(url);
            HttpPost httpPost = new HttpPost(url);
            httpPost.setConfig(requestConfig);
            for (Map.Entry<String, String> entry : head.entrySet()) {
                httpPost.addHeader(entry.getKey(), entry.getValue());
            }


            //设置请求参数
            String requestParams = "";
            if (paramMap != null) {
                if (head.get("Content-Type") != null && head.get("Content-Type").equalsIgnoreCase("application/x-www" +
                        "-form" +
                        "-urlencoded")) {
                    List<BasicNameValuePair> nameValuePairList = new ArrayList<>();
                    List<String> keyList = paramMap.keySet().stream().sorted().collect(Collectors.toList());
                    keyList.stream().forEach(key -> {
                        BasicNameValuePair basicNameValuePair = new BasicNameValuePair(key, String.valueOf(paramMap.get(key)));
                        nameValuePairList.add(basicNameValuePair);
                    });
                    //application/x-www-form-urlencoded
                    UrlEncodedFormEntity urlEncodedFormEntity = new UrlEncodedFormEntity(nameValuePairList, StandardCharsets.UTF_8);
                    httpPost.setEntity(urlEncodedFormEntity);
                } else if (head.get("Content-Type") != null && head.get("Content-Type").equalsIgnoreCase("application" +
                        "/json")) {

                    //application/json
                    requestParams = new JSONObject().toJSONString(paramMap);
                    StringEntity stringEntity = new StringEntity(requestParams, ContentType.APPLICATION_JSON);
                    httpPost.setEntity(stringEntity);
                }
            }

            String httpClientToCurl = convertHttpClientToCurl(httpPost);
            log.info("httpClientToCurl:{}", httpClientToCurl);

            response = client.execute(httpPost);
            HttpEntity entity = response.getEntity();
            result = EntityUtils.toString(entity, charSet);
            JSONObject jsonObject = JSONObject.parseObject(result);
            jsonObject.put("result", result);
            result = jsonObject.toJSONString();
            log.info("httpPost end,response:{}", result);
        } finally {
            response.close();
        }
        return result;
    }

    public static String convertHttpClientToCurl(HttpRequestBase httpRequestBase) throws IOException {
        try {
            StringBuilder curlCommand = new StringBuilder("curl ");
            // 添加 URL
            curlCommand.append("'").append(httpRequestBase.getURI()).append("' ");
            // 添加请求方法
            curlCommand.append("-X ").append(httpRequestBase.getMethod()).append(" ");
            // 添加请求头
            for (Header header : httpRequestBase.getAllHeaders()) {
                curlCommand.append("-H '").append(header.getName()).append(": ").append(header.getValue()).append("' ");
            }

            // post
            if (httpRequestBase instanceof HttpEntityEnclosingRequestBase) {
                HttpEntityEnclosingRequestBase httpRequest = (HttpEntityEnclosingRequestBase) httpRequestBase;
                if (httpRequest.getEntity() instanceof StringEntity) {
                    StringEntity stringEntity = (StringEntity) httpRequest.getEntity();
                    String requestBody = EntityUtils.toString(stringEntity);
                    curlCommand.append("-d '").append(requestBody).append("' ");
                }
            }
            return curlCommand.toString();
        } catch (Exception ex) {
            log.error("to curl occur error", ex);
        }
        return "";
    }


    /**
     * get 请求
     *
     * @param url 请求url
     * @return
     * @throws Exception
     */
    public static String httpGet(String url) throws Exception {
        return httpGet(url, null, UTF8);
    }

    /**
     * get 请求
     *
     * @param url      请求url
     * @param paramMap 请求参数
     * @return
     * @throws Exception
     */
    public static String httpGet(String url, Map<String, Object> paramMap) throws Exception {
        return httpGet(url, paramMap, null, UTF8);
    }

    /**
     * get 请求
     *
     * @param url      请求url
     * @param paramMap 请求参数
     * @param head     请求头
     * @return
     * @throws Exception
     */
    public static String httpGet(String url, Map<String, Object> paramMap, Map<String, String> head) throws Exception {
        return httpGet(url, paramMap, head, UTF8);
    }

    /**
     * get 请求
     *
     * @param url      url 请求url
     * @param paramMap 参数
     * @param head     请求头
     * @param charSet  字符集
     * @return
     * @throws Exception
     */
    private static String httpGet(String url, Map<String, Object> paramMap, Map<String, String> head, String charSet) throws Exception {
        StringBuilder params = new StringBuilder();
        if (paramMap != null) {
            for (Map.Entry<String, Object> entry : paramMap.entrySet()) {
                params.append("&");
                params.append(entry.getKey());
                params.append("=");
                params.append(URLEncoder.encode(String.valueOf(entry.getValue())));
            }
        }
        if (params.length() > 0) {
            url = url.indexOf("?") > 0 ? url + params.toString() : url + "?" + params.toString().substring(1);
        }

        return httpGet(url, head, charSet);
    }


    /**
     * @param url     url
     * @param head    请求头
     * @param charSet 字符集
     * @return
     * @throws Exception
     */
    private static String httpGet(String url, Map<String, String> head, String charSet) throws Exception {
        CloseableHttpClient client = HttpClients.createDefault();
        url = enhanceUrl(url);
        HttpGet getRequest = new HttpGet(url);
        if (head == null) {
            head = new HashMap<>();
        }
        if (StringUtils.isEmpty(head.get("Accept"))) {
            head.put("Accept", "application/json");
        }
        for (Map.Entry<String, String> entry : head.entrySet()) {
            getRequest.addHeader(entry.getKey(), entry.getValue());
        }

        getRequest.setConfig(requestConfig);
        try {
            log.info("httpGet begin,url:{},head:{}:{}", url, head);
            HttpResponse response = client.execute(getRequest);
            log.info("httpGet end,response:{}", response);
            HttpEntity entity = response.getEntity();
            if (null != entity) {
                String result = EntityUtils.toString(entity, charSet);
                JSONObject jsonObject = JSONObject.parseObject(result);
                jsonObject.put("result", result);
                result = jsonObject.toJSONString();
                log.info("httpGet end,response:{}", result);
                return result;
            }
            return "";
        } finally {
            getRequest.releaseConnection();
        }
    }


    /**
     * delete 请求
     *
     * @param url 请求url
     * @return
     * @throws Exception
     */
    public static String httpDelete(String url) throws Exception {
        return httpDelete(url, null, UTF8);
    }

    /**
     * delete 请求
     *
     * @param url      请求url
     * @param paramMap 请求参数
     * @return
     * @throws Exception
     */
    public static String httpDelete(String url, Map<String, Object> paramMap) throws Exception {
        return httpGet(url, paramMap, null, UTF8);
    }

    /**
     * delete 请求
     *
     * @param url      请求url
     * @param paramMap 请求参数
     * @param head     请求头
     * @return
     * @throws Exception
     */
    public static String httpDelete(String url, Map<String, Object> paramMap, Map<String, String> head) throws Exception {
        return httpDelete(url, paramMap, head, UTF8);
    }

    /**
     * delete 请求
     *
     * @param url      url 请求url
     * @param paramMap 参数
     * @param head     请求头
     * @param charSet  字符集
     * @return
     * @throws Exception
     */
    private static String httpDelete(String url, Map<String, Object> paramMap, Map<String, String> head, String charSet) throws Exception {
        StringBuilder params = new StringBuilder();
        if (paramMap != null) {
            for (Map.Entry<String, Object> entry : paramMap.entrySet()) {
                params.append("&");
                params.append(entry.getKey());
                params.append("=");
                params.append(URLEncoder.encode(String.valueOf(entry.getValue())));
            }
        }
        if (params.length() > 0) {
            url = url.indexOf("?") > 0 ? url + params.toString() : url + "?" + params.toString().substring(1);
        }
        return httpDelete(url, head, charSet);
    }


    /**
     * delete 请求
     *
     * @param url
     * @param head
     * @param charSet
     * @return
     * @throws Exception
     */
    private static String httpDelete(String url, Map<String, String> head, String charSet) throws Exception {
        CloseableHttpClient client = HttpClients.createDefault();
        url = enhanceUrl(url);
        HttpDelete httpDelete = new HttpDelete(url);
        if (head == null) {
            head = new HashMap<>();
        }
        if (StringUtils.isEmpty(head.get("Accept"))) {
            head.put("Accept", "application/json");
        }
        for (Map.Entry<String, String> entry : head.entrySet()) {
            httpDelete.addHeader(entry.getKey(), entry.getValue());
        }
        httpDelete.setConfig(requestConfig);
        try {
            log.info("httpDelete begin,url:{},head:{}:{}", url, head);
            HttpResponse response = client.execute(httpDelete);
            log.info("httpDelete end,response:{}", response);
            HttpEntity entity = response.getEntity();
            if (null != entity) {
                String result = EntityUtils.toString(entity, charSet);
                JSONObject jsonObject = JSONObject.parseObject(result);
                jsonObject.put("result", result);
                result = jsonObject.toJSONString();
                log.info("httpDelete end,response:{}", result);
                return result;
            }
            return "";
        } finally {
            httpDelete.releaseConnection();
        }
    }


    /**
     * httpPut
     *
     * @param url      请求url
     * @param paramMap 请求参数
     * @return
     * @throws Exception
     */
    public static String httpPut(String url, Map<String, Object> paramMap) throws Exception {
        return httpPut(url, paramMap, null, UTF8);
    }

    /**
     * httpPut
     *
     * @param url      请求url
     * @param paramMap 请求参数
     * @param head     请求头
     * @return
     * @throws Exception
     */
    public static String httpPut(String url, Map<String, Object> paramMap, Map<String, String> head) throws Exception {
        return httpPost(url, paramMap, head, UTF8);
    }


    /**
     * httpPut
     *
     * @param url
     * @param paramMap
     * @param head
     * @param charSet
     * @return
     * @throws Exception
     */
    private static String httpPut(String url, Map<String, Object> paramMap, Map<String, String> head,
                                  String charSet) throws Exception {
        CloseableHttpClient client = HttpClients.createDefault();
        CloseableHttpResponse response = null;
        String result = "";
        try {
            if (head == null) {
                head = new HashMap<>();
            }
            if (StringUtils.isEmpty(head.get("Content-Type"))) {
                head.put("Content-Type", "application/x-www-form-urlencoded");
            }
            if (StringUtils.isEmpty(head.get("Accept"))) {
                head.put("Accept", "application/json");
            }
            url = enhanceUrl(url);
            HttpPut httpPut = new HttpPut(url);
            httpPut.setConfig(requestConfig);
            for (Map.Entry<String, String> entry : head.entrySet()) {
                httpPut.addHeader(entry.getKey(), entry.getValue());
            }
            //设置请求参数
            String requestParams = "";
            if (paramMap != null) {
                if (head.get("Content-Type") != null && head.get("Content-Type").equalsIgnoreCase("application/x-www" +
                        "-form" +
                        "-urlencoded")) {
                    StringBuilder params = new StringBuilder("");
                    for (Map.Entry<String, Object> entry : paramMap.entrySet()) {
                        params.append("&");
                        params.append(entry.getKey());
                        params.append("=");
                        params.append(URLEncoder.encode(String.valueOf(entry.getValue())));
                    }
                    if (params.length() > 0) {
                        requestParams = params.toString().substring(1);
                    }
                } else if (head.get("Content-Type") != null && head.get("Content-Type").equalsIgnoreCase("application" +
                        "/json")) {
                    requestParams = new JSONObject().toJSONString(paramMap);
                }
            }
            StringEntity stringEntity = new StringEntity(requestParams);
            httpPut.setEntity(stringEntity);
            log.info("httpPut begin,url:{},params:{},head:{}", url, requestParams, head);
            response = client.execute(httpPut);
            HttpEntity entity = response.getEntity();
            result = EntityUtils.toString(entity, charSet);
            JSONObject jsonObject = JSONObject.parseObject(result);
            jsonObject.put("result", result);
            result = jsonObject.toJSONString();
            log.info("httpPut end,response:{}", result);
        } finally {
            response.close();
        }
        return result;
    }


    /**
     * 添加协议
     *
     * @param url
     */
    private static String enhanceUrl(String url) {
        if (StringUtils.isEmpty(url)) {
            return url;
        }
        if (!url.contains("http") || !url.contains("https")) {
            return DEFAULT_PROTOCOL + url;
        }
        return url;
    }


}
