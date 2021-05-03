package dev.claudio.jpatemporal.repository.impl;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

class EntityAccessSupport<T> {
    private final Map<String, Function<T, Object>> getters = new HashMap<>();
    private final Map<String, BiConsumer<T, Object>> setters = new HashMap<>();

    EntityAccessSupport(final Class<T> domainClass, final Set<String> fieldsToAccess) {
        try {
            Arrays.stream(Introspector.getBeanInfo(domainClass, Object.class).getPropertyDescriptors())
                    .filter(it -> fieldsToAccess.contains(it.getName()))
                    .forEach(it -> {
                        getters.put(it.getName(), createGetterFunction(it));
                        setters.put(it.getName(), createSetterFunction(it));
                    });
        } catch (IntrospectionException e) {
            throw new RuntimeException("Could not create PropertyDescriptors for " + domainClass, e);
        }
        if (getters.size() != fieldsToAccess.size() || setters.size() != fieldsToAccess.size()) {
            throw new RuntimeException("Could not correctly identify property getters/setters for " + domainClass);
        }
    }

    public Object getAttribute(final String attribute, final T entity) {
        return getters.get(attribute).apply(entity);
    }

    public void setAttribute(final String attribute, final T entity, final Object value) {
        setters.get(attribute).accept(entity, value);
    }

    private ThrowingFunction<T, Object> createGetterFunction(final PropertyDescriptor it) {
        return it.getReadMethod() != null
                ? (entity) -> it.getReadMethod().invoke(entity)
                : (entity) -> ReflectionUtils.getField(it.getName(), entity.getClass()).get(entity);
    }

    private ThrowingConsumer<T, Object> createSetterFunction(final PropertyDescriptor it) {
        return it.getWriteMethod() != null
                ? (entity, value) -> it.getWriteMethod().invoke(entity, value)
                : (entity, value) -> ReflectionUtils.getField(it.getName(), entity.getClass()).set(entity, value);
    }

    @FunctionalInterface
    private interface ThrowingFunction<T, U> extends Function<T, U> {

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
    private interface ThrowingConsumer<T, U> extends BiConsumer<T, U> {

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
