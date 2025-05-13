package common.utils;

import com.alibaba.fastjson.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class RequestHelpUtils {

    public static Map<String, String> toParamMap(JSONObject request) {
        if (request == null) {
            return new HashMap<>();
        }
        Map<String, String> requestMap = new HashMap<>();
        for (String key : request.keySet()) {
            Object value = request.get(key);
            if (value == null) {
                continue;
            }
            requestMap.put(key, value.toString());
        }

        return requestMap;
    }

}
