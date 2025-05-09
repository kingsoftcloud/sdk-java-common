package common;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class RpcRequestContentModel {
    /**
     * 服务名
     */
    private String service = "";


    /**
     * 机房
     */
    private String region = "";

    /**
     * api版本
     */
    private String version = "";

    /**
     * action
     */
    private String action = "";

    /**
     * ak
     */
    private String accessKeyId = "";

    /**
     * sk
     */
    private String secretAccessKey = "";

    /**
     * 链接时间10s
     */
    private Integer connectTimeout = 10000;

    /**
     * 读取时间10s
     */
    private Integer socketTimeout = 10000;




}
