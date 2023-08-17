package common;

import common.annotation.KsYunField;
import common.constant.ErrorCode;
import common.exception.ClientException;
import common.utils.GetUTCTimeUtil;

import java.time.LocalDate;
import java.time.temporal.TemporalUnit;

/**
 * @Classname Credential
 * @Description 证书
 */
public class Credential {


    /**
     * 用户在控制台创建的secretId
     */
    private String secretKey;

    /**
     * 用户在控制台创建的secretKey
     */
    private String signStr;

    /**
     * 签名算法，固定值：HMAC-SHA256
     */
    private String signatureMethod;

    /**
     * 签名版本号，固定值：1.0
     */
    private String signatureVersion;

    /**
     * 时间，UTC格式，例如：2019-08-13T17:18:36Z
     */
    private String timestamp;

    private String region;

    public Credential(String secretKey, String signStr,String region) {
        this.secretKey = secretKey;
        this.signStr = signStr;
        this.region=region;
        this.signatureMethod = "HMAC-SHA256";
        this.signatureVersion = "1.0";
        LocalDate now = LocalDate.now();
//        now.minus(-480, TemporalUnit.class)
        this.timestamp = GetUTCTimeUtil.getUTCTimeStr();
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getSignStr() {
        return signStr;
    }

    public void setSignStr(String signStr) {
        this.signStr = signStr;
    }

    public String getSignatureMethod() {
        return signatureMethod;
    }

    public void setSignatureMethod(String signatureMethod) {
        this.signatureMethod = signatureMethod;
    }

    public String getSignatureVersion() {
        return signatureVersion;
    }

    public void setSignatureVersion(String signatureVersion) {
        this.signatureVersion = signatureVersion;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public void check(){
        if (secretKey==null || secretKey==""){
            throw new ClientException(ErrorCode.INNER_ERROR_CODE, "secretKey is blank");
        }

        if (signStr==null || signStr==""){
            throw new ClientException(ErrorCode.INNER_ERROR_CODE, "signStr is blank");
        }
    }



    @Override
    public String toString() {
        return "Credential{" +
                "secretKey='" + secretKey + '\'' +
                ", signStr='" + signStr + '\'' +
                ", signatureMethod='" + signatureMethod + '\'' +
                ", signatureVersion='" + signatureVersion + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", region='" + region + '\'' +
                '}';
    }
}
