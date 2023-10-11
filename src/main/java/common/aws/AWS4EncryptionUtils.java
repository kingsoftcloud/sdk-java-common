package common.aws;

import com.ksc.KscClientException;
import com.ksc.auth.SigningAlgorithm;
import com.ksc.util.json.Jackson;
import common.utils.SdkHttpUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static com.ksc.util.StringUtils.UTF8;

@Slf4j
public class AWS4EncryptionUtils {
    public static void generateSignature(AWS4EncryptionFactory aws4EncryptionFactory, String requestMethod) throws Exception {
        //请求内容hash
        String contentSha256 = calculateContentHash(requestMethod, aws4EncryptionFactory);

        //1、创建规范请求
        String canonicalRequest = createCanonicalRequest(requestMethod.toUpperCase(), contentSha256, aws4EncryptionFactory);
        log.info("canonicalRequest:{}", canonicalRequest);

        //2、创建要签名的内容
        String stringToSign = createStringToSign(canonicalRequest, aws4EncryptionFactory);


        //3、创建签名秘钥
        byte[] signingKey = newSigningKey(aws4EncryptionFactory.getSecretKey(), aws4EncryptionFactory.getTimeStamp(),
                aws4EncryptionFactory.getRegion(), aws4EncryptionFactory.getService());

        //4、计算签名
        byte[] signature = computeSignature(stringToSign, signingKey);
        String signatureStr = BinaryUtils.toHex(signature);
        StringBuilder authorizationBuild = new StringBuilder(aws4EncryptionFactory.getAlgorithm()).append(" ").append("Credential=")
                .append(aws4EncryptionFactory.getAccessKey()).append("/")
                .append(aws4EncryptionFactory.getCredentialScope()).append(", ").append("SignedHeaders=")
                .append(aws4EncryptionFactory.getHeadKey()).append(",").append("Signature=").append(signatureStr);
        String authorization = authorizationBuild.toString();

        //设置签名
        aws4EncryptionFactory.getHead().put(aws4EncryptionFactory.X_Authorization, authorization);
        //设置请求时间戳
        aws4EncryptionFactory.getHead().put(aws4EncryptionFactory.X_AMZ_DATA, aws4EncryptionFactory.getFormattedTimestamp());

    }


    private static byte[] computeSignature(String stringToSign, byte[] signingKey) {
        return sign(stringToSign.getBytes(Charset.forName("UTF-8")), signingKey, SigningAlgorithm.HmacSHA256);
    }

    /**
     * 创建要签名的内容
     *
     * @param canonicalRequest      规范请求
     * @param aws4EncryptionFactory 请求上下文
     * @return
     */
    protected static String createStringToSign(String canonicalRequest, AWS4EncryptionFactory aws4EncryptionFactory) {

        StringBuilder credentialScopeBuilder = new StringBuilder(aws4EncryptionFactory.getTimeStamp());
        String credentialScope = credentialScopeBuilder.append("/").append(aws4EncryptionFactory.getRegion()).append("/")
                .append(aws4EncryptionFactory.getService()).append("/aws4_request").toString();

        aws4EncryptionFactory.setCredentialScope(credentialScope);

        StringBuilder stringToSignBuilder = new StringBuilder(aws4EncryptionFactory.getAlgorithm());
        stringToSignBuilder.append("\n").append(aws4EncryptionFactory.getFormattedTimestamp())
                .append("\n").append(credentialScope).append("\n").append(BinaryUtils.toHex(BinaryUtils.hash(canonicalRequest)));
        String stringToSign = stringToSignBuilder.toString();
        return stringToSign;
    }


    /**
     * 对请求内容hash
     *
     * @return
     */
    private static String calculateContentHash(String requestMethod, AWS4EncryptionFactory aws4EncryptionFactory) {
        Map<String, Object> requestParamMap = aws4EncryptionFactory.getRequestParam();
        InputStream payloadStream = new ByteArrayInputStream(new byte[0]);
        if (requestMethod.toUpperCase().equals("POST")) {
            String contentType = aws4EncryptionFactory.getHead().get("Content-Type");
            if (contentType.equals("application/json")) {
                payloadStream = new ByteArrayInputStream(Jackson.toJsonString(requestParamMap).getBytes(UTF8));
            } else {
                String requestParamUri = AWS4EncryptionFactory.getRequestParamUri(requestParamMap);
                payloadStream = new ByteArrayInputStream(requestParamUri.getBytes(UTF8));
            }
        }
        payloadStream.mark(-1);
        String contentSha256 = BinaryUtils.toHex(BinaryUtils.hash(payloadStream));

        try {
            payloadStream.reset();
            return contentSha256;
        } catch (IOException var6) {
            throw new KscClientException("Unable to reset stream after calculating AWS4 signature", var6);
        }
    }

    /**
     * 创建请求规范
     *
     * @param requestMethod         请求方式
     * @param contentSha256         请求内容散列值
     * @param aws4EncryptionFactory 请求上下文
     * @return
     */
    private static String createCanonicalRequest(String requestMethod, String contentSha256, AWS4EncryptionFactory aws4EncryptionFactory) {
        StringBuilder canonicalRequestBuilder = new StringBuilder("");
        //method
        canonicalRequestBuilder = canonicalRequestBuilder.append(requestMethod).append("\n");
        //path
        canonicalRequestBuilder = canonicalRequestBuilder.append(getCanonicalizedResourcePath(aws4EncryptionFactory.getPath(), true)).append("\n");

        //请求参数
        if (requestMethod.toUpperCase().equals("GET") && aws4EncryptionFactory.getRequestParam() != null) {
            String requestParamUri = AWS4EncryptionFactory.getRequestParamUri(aws4EncryptionFactory.getRequestParam());
            canonicalRequestBuilder = canonicalRequestBuilder.append(requestParamUri).append("\n");
        } else {
            canonicalRequestBuilder.append("\n");
        }

        //请求头
        if (aws4EncryptionFactory.getHead() != null) {
            //设置时间戳
            aws4EncryptionFactory.setHeadMap(AWS4EncryptionFactory.X_AMZ_DATA, aws4EncryptionFactory.getFormattedTimestamp());

            StringBuilder headBuilder = new StringBuilder("");
            List<String> keyList = aws4EncryptionFactory.getHead().keySet().stream().sorted().collect(Collectors.toList());
            keyList.stream().forEach(key -> {
                headBuilder.append(key).append(":").append(aws4EncryptionFactory.getHead().get(key)).append("\n");
            });

            canonicalRequestBuilder = canonicalRequestBuilder.append(headBuilder.toString()).append("\n");

            String headKey = StringUtils.join(keyList, ";");
            canonicalRequestBuilder = canonicalRequestBuilder.append(headKey).append("\n");
            aws4EncryptionFactory.setHeadKey(headKey);
        }

        //内容hash
        canonicalRequestBuilder = canonicalRequestBuilder.append(contentSha256);

        return canonicalRequestBuilder.toString();
    }

    private static String getCanonicalizedResourcePath(String resourcePath, boolean urlEncode) {
        if (resourcePath != null && !resourcePath.isEmpty()) {
            String value = urlEncode ? SdkHttpUtils.urlEncode(resourcePath, true) : resourcePath;
            return value.startsWith("/") ? value : "/".concat(value);
        } else {
            return "/";
        }
    }


    public static String getFormattedTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date());
    }

    public static String getDateStamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date());
    }

    private static byte[] newSigningKey(String key, String dateStamp, String regionName, String serviceName) {
        byte[] kSecret = ("AWS4" + key).getBytes(Charset.forName("UTF-8"));
        byte[] kDate = sign(dateStamp, kSecret, SigningAlgorithm.HmacSHA256);
        byte[] kRegion = sign(regionName, kDate, SigningAlgorithm.HmacSHA256);
        byte[] kService = sign(serviceName, kRegion, SigningAlgorithm.HmacSHA256);
        return sign("aws4_request", kService, SigningAlgorithm.HmacSHA256);
    }


    private static byte[] sign(String stringData, byte[] key, SigningAlgorithm algorithm) throws KscClientException {
        try {
            byte[] data = stringData.getBytes(com.ksc.util.StringUtils.UTF8);
            return sign(data, key, algorithm);
        } catch (Exception var5) {
            throw new KscClientException("Unable to calculate a request signature: " + var5.getMessage(), var5);
        }
    }

    private static byte[] sign(byte[] data, byte[] key, SigningAlgorithm algorithm) throws KscClientException {
        try {
            Mac mac = Mac.getInstance(algorithm.toString());
            mac.init(new SecretKeySpec(key, algorithm.toString()));
            return mac.doFinal(data);
        } catch (Exception var5) {
            throw new KscClientException("Unable to calculate a request signature: " + var5.getMessage(), var5);
        }
    }


}