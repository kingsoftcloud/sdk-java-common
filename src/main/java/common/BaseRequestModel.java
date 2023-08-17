package common;

import lombok.Data;

/**
 * @Classname BaseRequestModel
 * @Description 公共参数
 */
@Data
public class BaseRequestModel {
    /**
     *服务名称，提供open-api的服务英文简称，访问控制服务固定值iam
     */
    private String service;

    /**
     *操作接口名，与调用的具体openAPI相关
     */
    private String action;

    /**
     *接口版本号，访问控制服务接口当前只支持一个版本，即2015-11-01
     */
    private String version;

    /**
     * 区域，默认cn-beijing-6。不同服务支持的region不同，访问控制服务使用的region为cn-beijing-6
     */
    private String region;

    /**
     *签名
     */
    private String signature;

    /**
     * 证书
     */
   private Credential credential;
}
