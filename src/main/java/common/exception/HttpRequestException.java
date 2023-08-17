package common.exception;

/**
 * @Classname HttpRequestException
 * @Description http 请求异常
 */
public class HttpRequestException extends RuntimeException{

    private String errorCode;
    private String errorMsg;

    public HttpRequestException(String errorCode, String errorMsg) {
        super(errorMsg);
        this.errorCode = errorCode;
        this.errorMsg = errorMsg;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMsg() {
        return errorMsg;
    }
}
