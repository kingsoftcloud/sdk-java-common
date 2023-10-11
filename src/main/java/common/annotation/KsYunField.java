package common.annotation;

import java.lang.annotation.*;

/**
 * @Classname KsyunField
 * @Description TODO
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.FIELD)
public @interface KsYunField {

    /**
     * 参数名称
     *
     * @return
     */
    String name() default "";

    /**
     * 1-filter;2-array
     *
     * @return
     */
    int type() default 1;

}
