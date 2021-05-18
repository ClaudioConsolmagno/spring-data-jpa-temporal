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

    /**
     * When a {@link Column} annotation is used on the same field or method annotated with {@code annotation} then the
     * value of {@link Column#name()} is used. Otherwise:
     * <ul>
     *   <li> {@code annotation} on a field: field name is returned </li>
     *   <li> {@code annotation} on a non-getter method: the method name is returned </li>
     *   <li> {@code annotation} on a getter method: the method name in lower case without the initial {@code get} part
     *     is returned. </li>
     * </ul>
     * @return The column name that should be used by the annotated field or method in the {@code domainClass}
     */
    public static String fetchAnnotatedColumnName(final Class<?> domainClass, final Class<? extends Annotation> annotation) {
        final List<Field> annotatedFields = fetchAnnotatedFields(domainClass, annotation);
        final List<Method> annotatedMethods = fetchAnnotatedMethods(domainClass, annotation);
        final int annotationCount = annotatedFields.size() + annotatedMethods.size();
        if (annotationCount != 1) {
            if (annotationCount == 0 && domainClass.getSuperclass() != null) {
                return fetchAnnotatedColumnName(domainClass.getSuperclass(), annotation);
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

    /**
     * @return A list of {@link Method} declared within the class {@code domainClass}. Superclasses aren't searched.
     */
    public static List<Method> fetchAnnotatedMethods(final Class<?> domainClass, final Class<? extends Annotation> annotation) {
        return Arrays.stream(domainClass.getDeclaredMethods())
                .filter(it -> it.getAnnotation(annotation) != null)
                .collect(Collectors.toList());
    }

    /**
     * @return A list of {@link Field} declared within the class {@code domainClass}. Superclasses aren't searched.
     */
    public static List<Field> fetchAnnotatedFields(final Class<?> domainClass, final Class<? extends Annotation> annotation) {
        return Arrays.stream(domainClass.getDeclaredFields())
                .filter(it -> it.getAnnotation(annotation) != null)
                .collect(Collectors.toList());
    }

    /**
     * @return The first {@link Field} named {@code attribute} going up in hierarchy of class {@code entity}.
     */
    public static Optional<Field> fetchField(final String attribute, final Class<?> entity) {
        Class<?> entityCurrent = entity;
        Optional<Field> fieldOpt;
        do {
            fieldOpt = Arrays.stream(entityCurrent.getDeclaredFields())
                    .filter(field -> field.getName().equals(attribute))
                    .findFirst();
            entityCurrent = entityCurrent.getSuperclass();
        } while (!fieldOpt.isPresent() && entityCurrent != null);
        return fieldOpt;
    }
}
