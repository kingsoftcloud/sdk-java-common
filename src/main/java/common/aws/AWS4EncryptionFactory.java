package common.aws;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@Slf4j
@Deprecated
public class AWS4EncryptionFactory {

    public static final String X_AMZ_DATA = "x-amz-date";

    public static final String X_Authorization = "Authorization";

    public static final String HOST = "host";
    /**
     * 加密算法
     */
    private String algorithm = "AWS4-HMAC-SHA256";


    //20230804T114216Z
    private String formattedTimestamp;

    //20230804
    private String timeStamp;

    /**
     * 用户在控制台创建的secretId
     */
    private String accessKey;

    /**
     * 用户在控制台创建的secretKey
     */
    private String secretKey;

    /**
     * 服务名称
     */
    private String service;

    /**
     * 机房
     * 如：cn-shanghai-3
     */
    private String region;

    /**
     * 服务版本
     */
    private String version;

    /**
     * 动作
     * 如：ListUsers
     */
    private String action;

    /**
     * 资源路径
     * 如：/getUserInfo
     */
    private String path;

    /**
     * 请求头
     */
    private Map<String, String> head;

    /**
     * 请求参数
     */
    private Map<String, Object> requestParam;

    /**
     * 请求体
     */
    private String body;

    private String credentialScope;

    private String headKey;


    public AWS4EncryptionFactory(String accessKey, String secretKey, String service, String region) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.service = service;
        this.region = region;
        this.timeStamp = AWS4EncryptionUtils.getDateStamp();
        this.formattedTimestamp = AWS4EncryptionUtils.getFormattedTimestamp();
        if (head == null) {
            head = new HashMap<>();
        }
        if (this.requestParam == null) {
            this.requestParam = new HashMap<>();
        }
    }

    public Map<String, Object> setParamMap(String key, Object value) {
        this.requestParam.put(key, value);
        return this.requestParam;
    }

    public Map<String, String> setHeadMap(String key, String value) {
        this.head.put(key, value);
        return this.head;
    }


    public void generateSignature(String requestMethod) {
        try {
            AWS4EncryptionUtils.generateSignature(this, requestMethod);
        } catch (Exception exception) {
            log.info("generateSignature occur error:", exception);
        }

    }


    /**
     * get请求uri
     * 如a=1&b=2&c=3
     * @param requestParamMap
     * @return
     */
    public static String getRequestParamUri(Map<String, Object> requestParamMap) {
        StringBuilder params = new StringBuilder("");
        List<String> keyList = requestParamMap.keySet().stream().sorted().collect(Collectors.toList());
        keyList.stream().forEach(key -> {
            params.append("&");
            params.append(key);
            params.append("=");
            params.append(URLEncoder.encode(String.valueOf(requestParamMap.get(key))));
        });
        return params.toString().substring(1);
    }


}
