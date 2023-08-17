package common;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import common.annotation.KsYunField;
import common.constant.ErrorCode;
import common.exception.ClientException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import common.annotation.KsYunFieldPropertySerializer;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @Classname BaseClient
 * @Description 基类
 */
@Slf4j
public class BaseClient {

    private static final List<String> basicFieldTypeNameList;

    static {
        basicFieldTypeNameList = new ArrayList<>();
        basicFieldTypeNameList.add("int");
        basicFieldTypeNameList.add("java.lang.Integer");
        basicFieldTypeNameList.add("java.lang.String");
        basicFieldTypeNameList.add("java.lang.Boolean");
        basicFieldTypeNameList.add("boolean");
        basicFieldTypeNameList.add("java.lang.Long");
        basicFieldTypeNameList.add("long");
        basicFieldTypeNameList.add("java.lang.Double");
        basicFieldTypeNameList.add("double");
        basicFieldTypeNameList.add("java.lang.Float");
        basicFieldTypeNameList.add("float");
        basicFieldTypeNameList.add("java.math.BigDecimal");
        basicFieldTypeNameList.add("char");
    }

    /**
     * 公共参数
     */
    public JSONObject getCommonParams(Credential credential, JSONObject requestParams) {
        if (credential == null) {
            throw new ClientException(ErrorCode.INNER_ERROR_CODE, "credential error");
        }
        if (requestParams == null) {
            throw new ClientException(ErrorCode.INNER_ERROR_CODE, "requestParams error");
        }
        credential.check();
        requestParams.put("Accesskey", credential.getSecretKey());
        requestParams.put("Timestamp", credential.getTimestamp());
        requestParams.put("SignatureVersion", credential.getSignatureVersion());
        requestParams.put("SignatureMethod", credential.getSignatureMethod());
        requestParams.put("Region", credential.getRegion());
        return requestParams;
    }

    /**
     * 设置请求体请求参数
     *
     * @param requestObj
     * @param requestParams
     * @throws Exception
     */
    public void setRequestField(Object requestObj, JSONObject requestParams) throws Exception {
        fillJSONObject(requestObj, requestParams, "");
    }



    private void fillJSONObject(Object requestObj, JSONObject requestParams, String preKeyName) throws Exception {
        Class<? extends Object> requestClass = requestObj.getClass();
        Field[] fields = requestClass.getDeclaredFields();
        if (fields != null && fields.length > 0) {
            for (Field field : fields) {
                try {
                    field.setAccessible(true);
                    if (field == null) {
                        continue;
                    }

                    if (field.get(requestObj) == null) {
                        continue;
                    }

                    Object fieldValue = field.get(requestObj);
                    if (fieldValue==null){
                        continue;
                    }

                    KsYunField firstKsYunField = field.getAnnotation(KsYunField.class);
                    String fieldTypeName = field.getType().getName();
                    log.info("fieldTypeName:{}", fieldTypeName);

                    //基本类型
                    if (basicFieldTypeNameList.contains(fieldTypeName)) {
                        if (fieldValue == null) {
                            continue;
                        }
                        if (firstKsYunField != null) {
                            String requestKey = firstKsYunField.name();
                            if (StringUtils.isEmpty(preKeyName)) {
                                requestParams.put(requestKey, fieldValue.toString());
                            } else {
                                requestParams.put(preKeyName + requestKey, fieldValue);
                            }
                        } else {
                            if (StringUtils.isNotEmpty(preKeyName)) {
                                requestParams.put(preKeyName, fieldValue);
                            }
                        }
                    } else if (fieldTypeName.equals("java.util.List")) {
                        List<Object> dtoList = (List) field.get(requestObj);
                        if (dtoList != null && dtoList.size() > 0) {
                            for (int i = 0; i < dtoList.size(); i++) {
                                int index = i + 1;
                                Object obj = dtoList.get(i);
                                if (obj == null) {
                                    continue;
                                }

                                String listObjClassName = obj.getClass().getName();
                                if (basicFieldTypeNameList.contains(listObjClassName)) {
                                    String dtoPreKeyName ="";
                                    if (firstKsYunField.type()==1){
                                        //filter
                                        dtoPreKeyName =
                                                new StringBuilder(preKeyName).append(firstKsYunField.name()).append(".").append(index).toString();
                                    }else {
                                        //list
                                        dtoPreKeyName =
                                                new StringBuilder(preKeyName).append(firstKsYunField.name()).append(
                                                        "[").append(i).append("]").toString();
                                    }
                                    requestParams.put(dtoPreKeyName, obj);
                                } else {
                                    String dtoPreKeyName ="";
                                    if (firstKsYunField.type()==1){
                                        //filter
                                        dtoPreKeyName =new StringBuilder(preKeyName).append(firstKsYunField.name()).append(".").append(index).append(".").toString();
                                    }else {
                                        //list
                                        dtoPreKeyName =
                                                new StringBuilder(preKeyName).append(firstKsYunField.name()).append(
                                                        "[").append(index).append("]").append(".").toString();
                                    }
                                    fillJSONObject(obj, requestParams, dtoPreKeyName);
                                }
                            }
                        }
                    } else {
                        //对象类型
                        Object obj = field.get(requestObj);
                        String dtoPreKeyName =
                                new StringBuilder(preKeyName).append(firstKsYunField.name()).append(".").toString();
                        fillJSONObject(obj, requestParams, dtoPreKeyName);
                    }
                } finally {
                    field.setAccessible(false);
                }

            }
        }
    }


    /**
     * 设置请求体请求参数
     *
     * @param requestObj
     * @param requestParams
     * @throws Exception
     */
    @Deprecated
    public void setRequestField(Object requestObj, JSONObject requestParams, String preKeyName) throws Exception {
        //反射获取带有注解的字段
        if (requestObj != null) {
            //反射获取属性信息
            Class<? extends Object> requestClass = requestObj.getClass();
            Field[] fields = requestClass.getDeclaredFields();
            if (fields != null && fields.length > 0) {
                for (Field field : fields) {
                    field.setAccessible(true);
                    String fieldTypeName = field.getType().getName();
                    System.out.println("fieldTypeName: " + fieldTypeName);
                    if (basicFieldTypeNameList.contains(fieldTypeName)) {
                        //基本类型
                        KsYunField ksYunField = field.getAnnotation(KsYunField.class);
                        Object fieldValue = field.get(requestObj);
                        if (ksYunField != null && fieldValue != null) {
                            String requestKey = ksYunField.name();
                            requestParams.put(requestKey, fieldValue);
                        }
                    } else {
                        //集合或者对象类型
                        if (fieldTypeName.equals("java.util.List")) {
                            //列表注解
                            KsYunField dtoListKey = field.getAnnotation(KsYunField.class);
                            List<Object> dtoList = (List) field.get(requestObj);
                            if (dtoList != null && dtoList.size() > 0) {
                                for (int i = 0; i < dtoList.size(); i++) {
                                    int index = i + 1;
                                    Object dto = dtoList.get(i);
                                    if (dto == null) {
                                        return;
                                    }
                                    Field[] dtoFields = dto.getClass().getDeclaredFields();
                                    for (Field dtoField : dtoFields) {
                                        dtoField.setAccessible(true);
                                        //对象注解
                                        KsYunField dtoFieldKey = dtoField.getAnnotation(KsYunField.class);
                                        Object fieldValue = dtoField.get(dto);
                                        if (fieldValue == null) {
                                            return;
                                        }
                                        if (dtoFieldKey != null && dtoFieldKey.name() != null && dtoFieldKey.name() != "") {
                                            requestParams.put(dtoListKey.name() + "." + index + "." + dtoFieldKey.name(),
                                                    fieldValue);
                                        } else {
                                            requestParams.put(dtoListKey.name() + "." + index,
                                                    dto);
                                        }
                                        dtoField.setAccessible(false);
                                    }
                                }
                            }
                        }
                    }
                    field.setAccessible(false);
                }
            }
        }
    }

    /**
     * 设置请求体请求参数
     * application/json
     *
     * @param requestObj
     * @param requestParams
     * @throws Exception
     */
    public void setRequestFieldForPostRaw(Object requestObj, JSONObject requestParams) throws Exception {
        fillJSONObjectForRaw(requestObj, requestParams);
    }


    /**
     * 设置请求体请求参数
     * application/json
     * @param requestObj
     * @param requestParams
     * @throws Exception
     */
    private void fillJSONObjectForRaw(Object requestObj, JSONObject requestParams) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(requestObj.getClass(), new KsYunFieldPropertySerializer());
        objectMapper.registerModule(module);
        String paramsJson = objectMapper.writeValueAsString(requestObj);

        //to JSONObject
        if (StringUtils.isNotEmpty(paramsJson)){
            JSONObject.parseObject(paramsJson).entrySet().forEach(entry->{
                requestParams.put(entry.getKey(),entry.getValue());
            });
        }

        log.info("requestParams:{}", JSONObject.toJSONString(requestParams));
    }

    public static void main(String[] args) {

        BaseClient baseClient = new BaseClient();

        TestFirstObj testFirstObj = new TestFirstObj();
        testFirstObj.setTestInt(1);
        testFirstObj.setTestInteger(2);
        String[] strings = {"3", "4"};
        testFirstObj.setTestStrList(Arrays.asList(strings));

        Integer[] intArray = {5, 6};
        testFirstObj.setTestIntList(Arrays.asList(intArray));

        Long[] longArray = {7L, 8L};
        testFirstObj.setTestLongList(Arrays.asList(longArray));

        TestSencodObj testSencodObj = new TestSencodObj();
        testSencodObj.setTestStr("sencond");
        testSencodObj.setTestLong(10L);
        testFirstObj.setTestSencodObj(testSencodObj);

        TestThirdObj testThirdObj = new TestThirdObj();
        testThirdObj.setTestStr("third");
        testThirdObj.setTestLong(11L);
        testSencodObj.setTestThirdObj(testThirdObj);

        //set list
        List<TestSencodObj> testSencodObjList = new ArrayList<>();
        testFirstObj.setTestSencodObjList(testSencodObjList);

        TestSencodObj testSencodObj1 = new TestSencodObj();
        testSencodObj1.setTestStr("zhangsan");
        testSencodObj1.setTestLong(11L);
        testSencodObj1.setTestBoolean(false);

        List<TestThirdObj> testThirdObjList1 = new ArrayList<>();
        TestThirdObj testThirdObj2 = new TestThirdObj();
        testThirdObj2.setTestStr("third");
        testThirdObj2.setTestLong(11L);
        testThirdObjList1.add(testThirdObj2);

        TestThirdObj testThirdObj3 = new TestThirdObj();
        testThirdObj3.setTestStr("third");
        testThirdObj3.setTestLong(11L);
        testThirdObjList1.add(testThirdObj3);

        testSencodObj1.setTestThirdObjList(testThirdObjList1);
        testSencodObjList.add(testSencodObj1);

        TestSencodObj testSencodObj2 = new TestSencodObj();
        testSencodObj2.setTestStr("lisi");
        testSencodObj2.setTestLong(12L);
        testSencodObj2.setTestBoolean(false);


        List<TestThirdObj> testThirdObjList2 = new ArrayList<>();
        TestThirdObj testThirdObj4 = new TestThirdObj();
        testThirdObj4.setTestStr("third");
        testThirdObj4.setTestLong(11L);
        testThirdObjList2.add(testThirdObj4);

        TestThirdObj testThirdObj5 = new TestThirdObj();
        testThirdObj5.setTestStr("third");
        testThirdObj5.setTestLong(11L);
        testThirdObjList2.add(testThirdObj5);
        testSencodObj2.setTestThirdObjList(testThirdObjList2);


        testSencodObjList.add(testSencodObj2);


        try {
            JSONObject jsonObject = new JSONObject();
            baseClient.fillJSONObject(testFirstObj, jsonObject, "");
            System.out.println("1");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    @Data
    static class TestFirstObj {

        @KsYunField(name = "testInt")
        private int testInt;

        @KsYunField(name = "testInteger")
        private Integer testInteger;

        @KsYunField(name = "testStrList")
        private List<String> testStrList;

        @KsYunField(name = "testIntList")
        private List<Integer> testIntList;

        @KsYunField(name = "testLongList")
        private List<Long> testLongList;

        @KsYunField(name = "testSencodObj")
        private TestSencodObj testSencodObj;

        @KsYunField(name = "testSencodObjList")
        private List<TestSencodObj> testSencodObjList;

    }

    @Data
    static class TestSencodObj {

        @KsYunField(name = "testIntList")
        private List<Integer> testIntList = Arrays.asList(new Integer[]{1, 2, 3, 4, 5});

        @KsYunField(name = "testStr")
        private String testStr;

        @KsYunField(name = "testBoolean")
        private Boolean testBoolean;

        @KsYunField(name = "testLong")
        private Long testLong;

        @KsYunField(name = "testDouble")
        private Double testDouble;

        @KsYunField(name = "testFloat")
        private Float testFloat;

        @KsYunField(name = "testBig")
        private BigDecimal testBig;

        @KsYunField(name = "testThirdObj")
        private TestThirdObj testThirdObj;

        @KsYunField(name = "testThirdObjList")
        private List<TestThirdObj> testThirdObjList;
    }

    @Data
    static class TestThirdObj {

        @KsYunField(name = "testStr")
        private String testStr;

        @KsYunField(name = "testBoolean")
        private Boolean testBoolean;

        @KsYunField(name = "testLong")
        private Long testLong;

        @KsYunField(name = "testDouble")
        private Double testDouble;

        @KsYunField(name = "testFloat")
        private Float testFloat;

        @KsYunField(name = "testBig")
        private BigDecimal testBig;


    }


}
