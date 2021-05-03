package dev.claudio.jpatemporal.repository.impl;

import javax.persistence.Column;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

final class ReflectionUtils {
    private ReflectionUtils() { }

    public static String fetchAnnotatedField(final Class<?> domainClass, final Class<? extends Annotation> annotation) {
        final List<Field> annotatedFields = fetchAnnotatedFields(domainClass, annotation);
        final List<Method> annotatedMethods = fetchAnnotatedMethods(domainClass, annotation);
        final int annotationCount = annotatedFields.size() + annotatedMethods.size();
        if (annotationCount != 1) {
            if (annotationCount == 0 && domainClass.getSuperclass() != null) {
                return fetchAnnotatedField(domainClass.getSuperclass(), annotation);
            }
            throw new RuntimeException("Should have a single annotation '" + annotation.getSimpleName() + "' on " + domainClass + " or its child");
        }
        String fieldName;
        Optional<Column> columnAnnotation;
        if (annotatedFields.isEmpty()) {
            fieldName = annotatedMethods.get(0).getName();
            fieldName = fieldName.startsWith("get") ? fieldName.replaceFirst("get", "").toLowerCase(Locale.ROOT) : fieldName;
            columnAnnotation = Optional.ofNullable(annotatedMethods.get(0).getAnnotation(Column.class));
        } else {
            fieldName = annotatedFields.get(0).getName();
            columnAnnotation = Optional.ofNullable(annotatedFields.get(0).getAnnotation(Column.class));
        }
        return columnAnnotation
                .map(Column::name)
                .filter(it -> !it.isEmpty())
                .orElse(fieldName);
    }

    public static List<Method> fetchAnnotatedMethods(final Class<?> domainClass, final Class<? extends Annotation> annotation) {
        return Arrays.stream(domainClass.getDeclaredMethods())
                .filter(it -> it.getAnnotation(annotation) != null)
                .collect(Collectors.toList());
    }

    public static List<Field> fetchAnnotatedFields(final Class<?> domainClass, final Class<? extends Annotation> annotation) {
        return Arrays.stream(domainClass.getDeclaredFields())
                .filter(it -> it.getAnnotation(annotation) != null)
                .collect(Collectors.toList());
    }

    public static Field getField(final String attribute, final Class<?> entity) throws NoSuchFieldException {
        return Optional.ofNullable(
                org.springframework.data.util.ReflectionUtils.findField(entity, field -> attribute.equals(field.getName()))
        ).orElseThrow(NoSuchFieldException::new);
    }
}
