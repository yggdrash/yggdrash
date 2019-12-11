package io.yggdrash.node;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.Map;

@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Conditional(ConditionalOnRabbitMQPropertyNotEmpty.OnPropertyNotEmptyCondition.class)
public @interface ConditionalOnRabbitMQPropertyNotEmpty {

    Class value();

    class OnPropertyNotEmptyCondition implements Condition {

        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            String prefix = "rabbitmq";
            Map<String, Object> attrs = metadata.getAnnotationAttributes(ConditionalOnRabbitMQPropertyNotEmpty.class.getName());
            Class propertiesClass = (Class) attrs.get("value");
            Field[] fields = propertiesClass.getFields();
            for (Field field : fields) {
                String filedName = field.getName();
                String propName = String.format("%s.%s", prefix, filedName);
                String val = context.getEnvironment().getProperty(propName);
                if (val == null || val.trim().isEmpty()) {
                    return false;
                }
            }
            return true;
        }
    }
}
