package common.annotation;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collection;

public class KsYunFieldPropertySerializer extends JsonSerializer<Object> {
    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers)
            throws IOException, JsonProcessingException {
        Field[] fields = value.getClass().getDeclaredFields();
        if (fields.length <= 0) {
            return;
        }
        gen.writeStartObject();
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                String propertyName = field.getName();
                Object fieldValue = field.get(value);
                if (fieldValue==null){
                    continue;
                }
                if (!field.isAnnotationPresent(KsYunField.class)) {
                    gen.writeObjectField(propertyName, fieldValue);
                    continue;
                }

                KsYunField annotation = field.getAnnotation(KsYunField.class);
                if (StringUtils.isNotEmpty(annotation.name())) {
                    propertyName = annotation.name();
                }

                if (fieldValue instanceof Collection) {
                    gen.writeFieldName(propertyName);

                    gen.writeStartArray();
                    Collection<?> collection = (Collection<?>) fieldValue;
                    for (Object item : collection) {
                        String typeName = item.getClass().getTypeName();
                        if (typeName.startsWith("java")){
                            gen.writeObject(item);
                            continue;
                        }
                        serialize(item, gen, serializers);
                    }
                    gen.writeEndArray();

                } else {
                    gen.writeObjectField(propertyName, fieldValue);
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }


        }
        gen.writeEndObject();
    }

}
