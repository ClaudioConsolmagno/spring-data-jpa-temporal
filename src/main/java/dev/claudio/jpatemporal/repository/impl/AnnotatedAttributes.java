package dev.claudio.jpatemporal.repository.impl;

import dev.claudio.jpatemporal.annotation.EntityId;
import dev.claudio.jpatemporal.annotation.FromDate;
import dev.claudio.jpatemporal.annotation.TemporalId;
import dev.claudio.jpatemporal.annotation.ToDate;
import lombok.Getter;
import org.springframework.util.ReflectionUtils;

import javax.persistence.Column;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import java.beans.FeatureDescriptor;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

class AnnotatedAttributes<T> {
    private static final Set<Class<? extends Annotation>> RELATIONAL_ANNOTATIONS = Set.of(OneToOne.class, OneToMany.class, ManyToOne.class, ManyToMany.class);

    @Getter private final String entityId;
    @Getter private final String temporalId;
    @Getter private final String fromDate;
    @Getter private final String toDate;
    private final Map<String, PropertyDescriptor> attributeToPropertyDescriptor;

    public AnnotatedAttributes(final Class<T> domainClass) {
        validateNoRelationalAnnotations(domainClass);
        this.entityId = fetchAnnotatedField(domainClass, EntityId.class);
        this.temporalId = fetchAnnotatedField(domainClass, TemporalId.class);
        this.fromDate = fetchAnnotatedField(domainClass, FromDate.class);
        this.toDate = fetchAnnotatedField(domainClass, ToDate.class);
        try {
            PropertyDescriptor[] propertyDescriptors = Introspector.getBeanInfo(domainClass, Object.class).getPropertyDescriptors();
            this.attributeToPropertyDescriptor = Arrays.stream(propertyDescriptors)
                    .filter(it -> Set.of(entityId, temporalId, fromDate, toDate).contains(it.getName()))
                    .collect(Collectors.toMap(FeatureDescriptor::getName, it -> it));
        } catch (IntrospectionException e) {
            throw new RuntimeException("Could not create PropertyDescriptors for " + domainClass, e);
        }
    }

    private void validateNoRelationalAnnotations(final Class<T> domainClass) {
        boolean hasRelationalAnnotations = RELATIONAL_ANNOTATIONS.stream()
                .anyMatch(annotation -> fetchAnnotatedMethods(domainClass, annotation).size() + fetchAnnotatedFields(domainClass, annotation).size() > 0);
        if (hasRelationalAnnotations) {
            throw new RuntimeException("Relational Annotations are not supported: " + RELATIONAL_ANNOTATIONS);
        }
    }

    public Object invokeGetter(final String attribute, final T entity) {
        PropertyDescriptor pd = this.attributeToPropertyDescriptor.get(attribute);
        try {
            if (pd != null && pd.getReadMethod() != null) {
                return pd.getReadMethod().invoke(entity);
            } else {
                Field field = Optional.ofNullable(ReflectionUtils.findField(entity.getClass(),attribute)).orElseThrow(NoSuchFieldException::new);
                return ReflectionUtils.getField(field, entity);
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchFieldException e) {
            throw new RuntimeException("Could not invoke getter for " + attribute + " on " + entity + " or field isn't public", e);
        }
    }

    public void invokeSetter(final String attribute, final T entity, final Object value) {
        PropertyDescriptor pd = this.attributeToPropertyDescriptor.get(attribute);
        try {
            if (pd != null && pd.getWriteMethod() != null) {
                pd.getWriteMethod().invoke(entity, value);
            } else {
                Field field = Optional.ofNullable(ReflectionUtils.findField(entity.getClass(),attribute)).orElseThrow(NoSuchFieldException::new);
                ReflectionUtils.setField(field, entity, value);
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchFieldException e) {
            throw new RuntimeException("Could not invoke getter for " + attribute + " on " + entity + " or field isn't public", e);
        }
    }

    protected String fetchAnnotatedField(final Class<? super T> domainClass, final Class<? extends Annotation> annotation) {
        final List<Field> annotatedFields = fetchAnnotatedFields(domainClass, annotation);
        final List<Method> annotatedMethods = fetchAnnotatedMethods(domainClass, annotation);
        if ((annotatedFields.size() + annotatedMethods.size()) != 1) {
            if (domainClass.getSuperclass() != null) {
                return fetchAnnotatedField(domainClass.getSuperclass(), annotation);
            }
            throw new RuntimeException("Should have a single annotation '" + annotation.getSimpleName() + "' on " + domainClass + " or its child");
        }
        String entityIdName;
        Optional<Column> columnAnnotation;
        if (annotatedFields.isEmpty()) {
            entityIdName = annotatedMethods.get(0).getName();
            entityIdName = entityIdName.startsWith("get") ? entityIdName.replaceFirst("get", "").toLowerCase(Locale.ROOT) : entityIdName;
            columnAnnotation = Optional.ofNullable(annotatedMethods.get(0).getAnnotation(Column.class));
        } else {
            entityIdName = annotatedFields.get(0).getName();
            columnAnnotation = Optional.ofNullable(annotatedFields.get(0).getAnnotation(Column.class));
        }
        return columnAnnotation
                .map(Column::name)
                .filter(it -> !it.isBlank())
                .orElse(entityIdName);
    }

    private List<Method> fetchAnnotatedMethods(final Class<? super T> domainClass, final Class<? extends Annotation> annotation) {
        return Arrays.stream(domainClass.getDeclaredMethods())
                .filter(it -> it.getAnnotation(annotation) != null)
                .collect(Collectors.toList());
    }

    private List<Field> fetchAnnotatedFields(final Class<? super T> domainClass, final Class<? extends Annotation> annotation) {
        return Arrays.stream(domainClass.getDeclaredFields())
                .filter(it -> it.getAnnotation(annotation) != null)
                .collect(Collectors.toList());
    }
}
