package dev.claudio.jpatemporal.repository.impl;

import dev.claudio.jpatemporal.annotation.FromDate;
import dev.claudio.jpatemporal.annotation.TemporalId;
import dev.claudio.jpatemporal.annotation.ToDate;
import dev.claudio.jpatemporal.annotation.UniqueKey;
import lombok.Getter;

import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

class AnnotatedEntitySupport<T> {
    private static final Set<Class<? extends Annotation>> RELATIONAL_ANNOTATIONS =
            Set.of(OneToOne.class, OneToMany.class, ManyToOne.class, ManyToMany.class);

    private final Map<String, Function<T, Object>> readMethod = new HashMap<>();
    private final Map<String, BiConsumer<T, Object>> writeMethod = new HashMap<>();

    @Getter private final String uniqueKey;
    @Getter private final String temporalId;
    @Getter private final String fromDate;
    @Getter private final String toDate;

    public AnnotatedEntitySupport(final Class<T> domainClass) {
        validateNoRelationalAnnotations(domainClass);
        this.uniqueKey = ReflectionUtils.fetchAnnotatedField(domainClass, UniqueKey.class);
        this.temporalId = ReflectionUtils.fetchAnnotatedField(domainClass, TemporalId.class);
        this.fromDate = ReflectionUtils.fetchAnnotatedField(domainClass, FromDate.class);
        this.toDate = ReflectionUtils.fetchAnnotatedField(domainClass, ToDate.class);
        try {
            Arrays.stream(Introspector.getBeanInfo(domainClass, Object.class).getPropertyDescriptors())
                    .filter(it -> Set.of(uniqueKey, temporalId, fromDate, toDate).contains(it.getName()))
                    .forEach(it -> {
                        readMethod.put(it.getName(), createGetterFunction(it));
                        writeMethod.put(it.getName(), createSetterFunction(it));
                    });
            if (readMethod.size() != 4 || writeMethod.size() != 4) {
                throw new RuntimeException("Could not correctly identify property getters/setters for " + domainClass);
            }
        } catch (IntrospectionException e) {
            throw new RuntimeException("Could not create PropertyDescriptors for " + domainClass, e);
        }
    }

    public Object getAttribute(final String attribute, final T entity) {
        return readMethod.get(attribute).apply(entity);
    }

    public void setAttribute(final String attribute, final T entity, final Object value) {
        writeMethod.get(attribute).accept(entity, value);
    }

    private void validateNoRelationalAnnotations(final Class<?> domainClass) {
        boolean hasRelationalAnnotations = RELATIONAL_ANNOTATIONS.stream()
                .anyMatch(annotation ->
                        ReflectionUtils.fetchAnnotatedMethods(domainClass, annotation).size() +
                        ReflectionUtils.fetchAnnotatedFields(domainClass, annotation).size() > 0
                );
        if (hasRelationalAnnotations) {
            throw new RuntimeException("Relational Annotations are not supported: " + RELATIONAL_ANNOTATIONS);
        }
    }

    private ThrowingFunction<T, Object> createGetterFunction(PropertyDescriptor it) {
        return it.getReadMethod() != null ?
                (entity) -> it.getReadMethod().invoke(entity) :
                (entity) -> ReflectionUtils.getField(it.getName(), entity.getClass()).get(entity);
    }

    private ThrowingConsumer<T, Object> createSetterFunction(PropertyDescriptor it) {
        return it.getWriteMethod() != null ?
                (entity, value) -> it.getWriteMethod().invoke(entity, value) :
                (entity, value) -> ReflectionUtils.getField(it.getName(), entity.getClass()).set(entity, value);
    }

    @FunctionalInterface
    interface ThrowingFunction<T, U> extends Function<T, U> {

        @Override
        default U apply(final T elem) {
            try {
                return applyThrows(elem);
            } catch (final Exception e) {
                throw new RuntimeException(String.format("Could not apply '%s'", elem), e);
            }
        }

        U applyThrows(T elem) throws Exception;
    }

    @FunctionalInterface
    interface ThrowingConsumer<T, U> extends BiConsumer<T, U> {

        @Override
        default void accept(final T elem, final U elem2) {
            try {
                acceptThrows(elem, elem2);
            } catch (final Exception e) {
                throw new RuntimeException(String.format("Could not accept '%s' with '%s'", elem, elem2), e);
            }
        }

        void acceptThrows(T elem, U elem2) throws Exception;
    }
}
