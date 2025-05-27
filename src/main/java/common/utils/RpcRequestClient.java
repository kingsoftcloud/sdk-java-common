package common.utils;

import com.alibaba.fastjson.JSONObject;
import common.RpcRequestContentModel;
import lombok.extern.slf4j.Slf4j;
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

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.auth.signer.params.Aws4SignerParams;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.http.SdkHttpFullRequest.Builder;
import software.amazon.awssdk.utils.StringInputStream;
import software.amazon.awssdk.utils.StringUtils;


@Slf4j
public class RpcRequestClient {

    private final static String DEFAULT_PROTOCOL = "http://";

    private final RpcRequestContentModel rpcRequestContentModel;

    public RpcRequestClient(RpcRequestContentModel rpcRequestContentModel) {
        this.rpcRequestContentModel = rpcRequestContentModel;
    }

    public String beginRpcRequest(String url, String requestMethod, Map<String, Object> requestParams) {
        return beginRpcRequest(url, requestMethod, requestParams, new HashMap<>());
    }

    /**
     * rpc
     *
     * @param url           api地址
     * @param requestMethod 请求方法
     * @param requestParam  请求参数
     * @param head          请求头
     * @return
     */
    public String beginRpcRequest(String url, String requestMethod, Map<String, Object> requestParam, Map<String, String> head) {
        //如果没有http协议，则添加http协议
        url = enhanceUrl(url);

        // Initialize RPC headers with input head map
        final Map<String, String> rpcHead = new HashMap<>(head);

        // Initialize RPC parameters with input request parameters
        final Map<String, Object> rpcParam = new HashMap<>(requestParam);

        try {
            // 1. 创建签名请求
            SdkHttpFullRequest unsignedRequest = createUnsignedRequest(url, paseSdkHttpMethod(requestMethod), rpcParam, rpcHead);

            // 2. 使用AWS SDK签名器进行签名
            SdkHttpFullRequest signedRequest = signRequest(
                    unsignedRequest,
                    rpcRequestContentModel.getAccessKeyId(),
                    rpcRequestContentModel.getSecretAccessKey()
            );

            // 3. 转换为Apache HttpClient请求
            HttpRequestBase httpRequest = convertToHttpRequest(signedRequest, rpcParam);

            // 4. 执行请求
            return executeRequest(httpRequest);
        } catch (Exception e) {
            log.error("rpc occur error", e);
            throw new RuntimeException(e);
        }
    }

    private SdkHttpMethod paseSdkHttpMethod(String method) {
        if (StringUtils.isEmpty(method)) {
            throw new IllegalArgumentException("method is null");
        }
        switch (method.toLowerCase()) {
            case "get":
                return SdkHttpMethod.GET;
            case "post":
                return SdkHttpMethod.POST;
            case "put":
                return SdkHttpMethod.PUT;
            case "delete":
                return SdkHttpMethod.DELETE;
            case "head":
                return SdkHttpMethod.HEAD;
            case "options":
                return SdkHttpMethod.OPTIONS;
            case "patch":
                return SdkHttpMethod.PATCH;
            default:
                throw new IllegalArgumentException("method is not supported");
        }
    }


    /**
     * 创建未签名的 AWS 请求 (优化版)
     *
     * @param endpoint 请求端点URL
     * @param method   请求方法 (GET/POST/PUT/DELETE)
     * @return 构建好的请求对象
     * @throws IllegalArgumentException 如果参数无效
     * @throws URISyntaxException       如果endpoint不是有效的URI
     */
    public SdkHttpFullRequest createUnsignedRequest(
            String endpoint,
            SdkHttpMethod method,
            Map<String, Object> requestParam,
            Map<String, String> head) throws URISyntaxException {

        URI uri = new URI(endpoint);
        Builder builder = SdkHttpFullRequest.builder()
                .method(method)
                .uri(uri);

        // 根据请求方法处理参数
        if (method == SdkHttpMethod.GET
                || method == SdkHttpMethod.DELETE
                || method == SdkHttpMethod.HEAD
                || method == SdkHttpMethod.OPTIONS) {
            if (requestParam != null) {
                requestParam.entrySet().stream()
                        .filter(entry -> entry.getValue() != null)
                        .sorted(Map.Entry.comparingByKey())
                        .forEach(entry -> builder.putRawQueryParameter(entry.getKey(), entry.getValue().toString()));
            }
        } else if (method == SdkHttpMethod.POST
                || method == SdkHttpMethod.PUT
                || method == SdkHttpMethod.PATCH) {
            // 根据内容类型设置请求体
            if (requestParam != null && !requestParam.isEmpty()) {
                String contentType = head.getOrDefault("Content-Type", "application/x-www-form-urlencoded");
                if ("application/json".equalsIgnoreCase(contentType)) {
                    String jsonBody = buildJsonBody(requestParam);
                    builder.putHeader("Content-Type", "application/json")
                            .contentStreamProvider(() ->
                                    new StringInputStream(jsonBody));
                } else {
                    // 表单格式请求体 (默认)
                    String formData = buildFormData(requestParam);
                    builder.putHeader("Content-Type", "application/x-www-form-urlencoded")
                            .contentStreamProvider(() ->
                                    new StringInputStream(formData));
                }
            }
        }

        //公共请求头
        builder.putHeader("Host", uri.getHost());
        builder.putHeader("Accept", "application/json");

        //覆盖请求头
        head.forEach(builder::putHeader);

        return builder.build();
    }


    /**
     * 使用AWS SDK对请求进行签名
     */
    private SdkHttpFullRequest signRequest(
            SdkHttpFullRequest request,
            String accessKeyId,
            String secretAccessKey) {

        Aws4Signer signer = Aws4Signer.create();

        Aws4SignerParams signerParams = Aws4SignerParams.builder()
                .awsCredentials(AwsBasicCredentials.create(accessKeyId, secretAccessKey))
                .signingName(rpcRequestContentModel.getService())
                .signingRegion(Region.of(rpcRequestContentModel.getRegion()))
                .build();

        return signer.sign(request, signerParams);
    }


    /**
     * 将 AWS SDK 的请求转换为 Apache HttpClient 请求
     *
     * @param signedRequest 已签名的 AWS 请求
     * @param requestParam  请求参数
     * @return 转换后的 HttpRequestBase 对象
     */
    public HttpRequestBase convertToHttpRequest(SdkHttpFullRequest signedRequest,
                                                Map<String, Object> requestParam) {
        // 1. 根据请求方法创建对应的 HTTP 请求对象
        HttpRequestBase httpRequest = createRequestByMethod(signedRequest);

        // 2. 添加头信息
        addHeaders(httpRequest, signedRequest);

        // 3. 设置请求体和参数
        setRequestContent(httpRequest, signedRequest, requestParam);

        return httpRequest;
    }

    private HttpRequestBase createRequestByMethod(SdkHttpFullRequest signedRequest) {
        switch (signedRequest.method()) {
            case POST:
                return new HttpPost(signedRequest.getUri());
            case GET:
                return new HttpGet(signedRequest.getUri());
            case PUT:
                return new HttpPut(signedRequest.getUri());
            case DELETE:
                return new HttpDelete(signedRequest.getUri());
            case PATCH:
                return new HttpPatch(signedRequest.getUri());
            case HEAD:
                return new HttpHead(signedRequest.getUri());
            case OPTIONS:
                return new HttpOptions(signedRequest.getUri());
            default:
                throw new UnsupportedOperationException(
                        "Method not supported: " + signedRequest.method());
        }
    }

    private void addHeaders(HttpRequestBase httpRequest,
                            SdkHttpFullRequest signedRequest) {
        signedRequest.headers().forEach((name, values) -> {
            // 跳过 Content-Length 头，由 HttpClient 自动处理
            if (!"Content-Length".equalsIgnoreCase(name)) {
                values.forEach(value -> httpRequest.addHeader(name, value));
            }
        });
    }

    //put,post,patch 请求设置请求体
    private void setRequestContent(HttpRequestBase httpRequest,
                                   SdkHttpFullRequest signedRequest,
                                   Map<String, Object> requestParam) {
        if (httpRequest instanceof HttpEntityEnclosingRequestBase && requestParam != null) {
            HttpEntityEnclosingRequestBase entityRequest = (HttpEntityEnclosingRequestBase) httpRequest;

            if (signedRequest == null) {
                throw new IllegalArgumentException("Signed request cannot be null");
            }

            // 根据内容类型设置请求体
            String contentType = signedRequest.firstMatchingHeader("Content-Type")
                    .map(String::toLowerCase)
                    .orElse("application/x-www-form-urlencoded");

            try {
                if (contentType.equalsIgnoreCase("application/json")) {
                    // JSON 格式请求体
                    String jsonBody = buildJsonBody(requestParam);
                    entityRequest.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON));
                } else {
                    // 表单格式请求体
                    List<BasicNameValuePair> nameValuePairList = new ArrayList<>();
                    requestParam.entrySet().stream().filter(entry -> entry.getValue() != null)
                            .sorted(Map.Entry.comparingByKey()).forEach(entry -> {
                                BasicNameValuePair basicNameValuePair = new BasicNameValuePair(entry.getKey(), validateStringValue(entry.getValue()));
                                nameValuePairList.add(basicNameValuePair);
                            });
                    UrlEncodedFormEntity urlEncodedFormEntity = new UrlEncodedFormEntity(nameValuePairList, StandardCharsets.UTF_8);
                    entityRequest.setEntity(urlEncodedFormEntity);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to set request entity", e);
            }
        }
    }


    private String validateStringValue(Object value) {
        if (value == null) {
            return "";
        }
        return value.toString();
    }


    /**
     * 构建JSON格式请求体
     */
    private String buildJsonBody(Map<String, Object> requestParam) {
        if (requestParam == null || requestParam.isEmpty()) {
            throw new IllegalArgumentException("参数不能为空");
        }
        return JSONObject.toJSONString(requestParam);
    }

    /**
     * 构建表单格式请求体
     */
    private String buildFormData(Map<String, Object> requestParam) throws IllegalArgumentException {
        if (requestParam == null || requestParam.isEmpty()) {
            throw new IllegalArgumentException("参数不能为空");
        }

        try {
            return requestParam.entrySet().stream()
                    .filter(entry -> entry.getValue() != null)
                    .sorted(Map.Entry.comparingByKey())
                    .map(entry -> {
                        try {
                            return URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8.name()) +
                                    "=" +
                                    URLEncoder.encode((validateStringValue(entry.getValue())), StandardCharsets.UTF_8.name());
                        } catch (UnsupportedEncodingException e) {
                            // UTF-8应该总是可用，但如果不可用则抛出运行时异常
                            throw new IllegalStateException("UTF-8编码不可用", e);
                        }
                    })
                    .collect(Collectors.joining("&"));
        } catch (Exception e) {
            log.error("构建表单数据失败", e);
            throw new IllegalArgumentException("构建表单数据失败: " + e.getMessage(), e);
        }
    }




    /**
     * 执行请求
     */
    private String executeRequest(HttpRequestBase httpRequest) {
        log.info("begin rpc request");
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {

            //设置超时时间
            RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(rpcRequestContentModel.getSocketTimeout()).setConnectTimeout(rpcRequestContentModel.getConnectTimeout()).build();
            httpRequest.setConfig(requestConfig);

            String curl = HttpClientUtils.convertHttpClientToCurl(httpRequest);
            log.info("begin rpc request curl:{}", curl);

            HttpResponse response = httpClient.execute(httpRequest);
            Objects.requireNonNull(response.getEntity(), "response content is null");

            String result = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            JSONObject jsonObject = JSONObject.parseObject(result);
            jsonObject.put("result", result);
            log.info("rpc request end,response:{}", jsonObject.toJSONString());

            return jsonObject.toJSONString();
        } catch (Exception e) {
            log.info("rpc request occur exception:{}", e.getMessage());
            throw new RuntimeException("rpc请求失败", e);
        }
    }

    /**
     * 添加协议
     *
     * @param url
     */
    private static String enhanceUrl(String url) {
        if (org.apache.commons.lang3.StringUtils.isEmpty(url)) {
            return url;
        }
        if (!url.contains("http") || !url.contains("https")) {
            return DEFAULT_PROTOCOL + url;
        }
        return url;
    }


}
