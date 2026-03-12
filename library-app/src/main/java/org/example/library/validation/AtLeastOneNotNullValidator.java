package org.example.library.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.reflect.Field;

public class AtLeastOneNotNullValidator implements ConstraintValidator<AtLeastOneNotNull, Object> {

    private String[] fieldNames;

    @Override
    public void initialize(AtLeastOneNotNull constraintAnnotation) {
        this.fieldNames = constraintAnnotation.fieldNames();
    }

    @Override
    public boolean isValid(Object dto, ConstraintValidatorContext context) {
        if (dto == null) {
            return true;
        }

        for (String fieldName : fieldNames) {
            try {
                Field field = getField(dto.getClass(), fieldName);
                if (field != null) {
                    field.setAccessible(true);
                    Object value = field.get(dto);
                    if (value != null) {
                        if (value instanceof String str && !str.isBlank()) {
                            return true;
                        }
                        return true;
                    }
                }
            } catch (Exception e) {
                return false;
            }
        }

        return false;
    }

    private Field getField(Class<?> clazz, String fieldName) {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            Class<?> superClass = clazz.getSuperclass();
            if (superClass != null) {
                return getField(superClass, fieldName);
            }
            return null;
        }
    }

}
