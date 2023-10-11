package common.exception;

/**
 * @Classname HttpRequestException
 * @Description http 请求异常
 */
public class ClientException extends RuntimeException{

    private String errorCode;
    private String errorMsg;

    public ClientException(String errorCode, String errorMsg) {
        this.errorCode = errorCode;
        this.errorMsg = errorMsg;
    }
}
