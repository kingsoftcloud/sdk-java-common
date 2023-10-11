package common;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;

/**
 * @Classname BaseModel
 * @Description 基类
 */
@Data
@ToString
public class BaseResponseModel {

    @JsonProperty("RequestId")
    private String requestId;

    @JsonProperty("Error")
    private ErrorDto error;

    @JsonProperty("result")
    private String result;

    @Data
    @ToString
    public static class ErrorDto {
        @JsonProperty("Type")
        private String type;

        @JsonProperty("Code")
        private String code;

        @JsonProperty("Message")
        private String message;

    }


}
