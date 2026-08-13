package com.kct.jcredit.core_backend.validation;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;

/**
 * Validates {@link RequiredIf}. Reads both properties reflectively and reports
 * the violation against the dependent field so error keys stay field-scoped.
 */
@Slf4j
public class RequiredIfValidator implements ConstraintValidator<RequiredIf, Object> {

    /** Property lookups are cached — this runs on every page save across ~34 pages. */
    private static final Map<Class<?>, Map<String, PropertyDescriptor>> DESCRIPTOR_CACHE =
            new ConcurrentHashMap<>();

    private String field;
    private String when;
    private boolean is;
    private String message;

    @Override
    public void initialize(RequiredIf annotation) {
        this.field = annotation.field();
        this.when = annotation.when();
        this.is = annotation.is();
        this.message = annotation.message();
    }

    @Override
    public boolean isValid(Object target, ConstraintValidatorContext context) {
        if (target == null) {
            return true;
        }

        Object toggle = readProperty(target, when);
        if (!(toggle instanceof Boolean booleanToggle) || booleanToggle != is) {
            // Condition not met — the dependent field is genuinely optional.
            return true;
        }

        if (isPresent(readProperty(target, field))) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(field)
                .addConstraintViolation();
        return false;
    }

    private static boolean isPresent(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof CharSequence text) {
            return !text.toString().trim().isEmpty();
        }
        if (value instanceof java.util.Collection<?> collection) {
            return !collection.isEmpty();
        }
        return true;
    }

    private static Object readProperty(Object target, String propertyName) {
        PropertyDescriptor descriptor =
                DESCRIPTOR_CACHE
                        .computeIfAbsent(target.getClass(), RequiredIfValidator::describe)
                        .get(propertyName);

        if (descriptor == null || descriptor.getReadMethod() == null) {
            // A typo in the annotation must not silently pass validation.
            throw new IllegalStateException(
                    "@RequiredIf references unknown property '" + propertyName
                            + "' on " + target.getClass().getName());
        }

        try {
            Method reader = descriptor.getReadMethod();
            reader.setAccessible(true);
            return reader.invoke(target);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "Unable to read property '" + propertyName
                            + "' on " + target.getClass().getName(), e);
        }
    }

    private static Map<String, PropertyDescriptor> describe(Class<?> type) {
        Map<String, PropertyDescriptor> descriptors = new ConcurrentHashMap<>();
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(type, Object.class);
            for (PropertyDescriptor descriptor : beanInfo.getPropertyDescriptors()) {
                descriptors.put(descriptor.getName(), descriptor);
            }
        } catch (IntrospectionException e) {
            throw new IllegalStateException("Unable to introspect " + type.getName(), e);
        }
        return descriptors;
    }
}
