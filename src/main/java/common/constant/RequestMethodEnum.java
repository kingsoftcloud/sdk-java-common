package common.constant;

/**
 * @Classname ErrorCode
 * @Description 错误代码
 */
public enum RequestMethodEnum {
        POST("post"),
        GET("get"),
        PUT("put"),
        DELETE("delete"),
        HEAD("head"),
        OPTIONS("options"),
        PATCH("patch");

        private final String method;

        RequestMethodEnum(String method) {
            this.method = method;
        }

        public String getMethod() {
            return method;
        }
    }





