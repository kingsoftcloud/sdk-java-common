package common;

import lombok.Builder;
import lombok.Data;
@Data
@Builder
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
     * 连接超时,默认10s
     * 建立 TCP 连接的最大等待时间（单位：毫秒）
     */
    @Builder.Default
    private Integer connectTimeout = 10000;

    /**
     * 套接字超时/读取超时，默认10s
     * 两次数据包之间的最大间隔时间（单位：毫秒）
     */
    @Builder.Default
    private Integer socketTimeout = 10000;
}
