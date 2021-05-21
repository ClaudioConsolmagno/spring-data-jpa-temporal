package dev.claudio.jpatemporal.repository.impl;

import java.beans.FeatureDescriptor;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

class EntityAccessSupport<T> {
    private final Map<String, Function<T, Object>> getters = new HashMap<>();
    private final Map<String, BiConsumer<T, Object>> setters = new HashMap<>();

    EntityAccessSupport(final Class<T> domainClass, final Set<String> fieldsToAccess) {
        try {
            final Map<String, PropertyDescriptor> descriptorMap = Arrays.stream(Introspector.getBeanInfo(domainClass, Object.class).getPropertyDescriptors())
                    .collect(Collectors.toMap(FeatureDescriptor::getName, it -> it));
            for (String it : fieldsToAccess) {
                getters.put(it, createGetterFunction(it, descriptorMap.get(it), domainClass));
                setters.put(it, createSetterFunction(it, descriptorMap.get(it), domainClass));
            }
        } catch (IntrospectionException | IllegalAccessException e) {
            throw new RuntimeException("Could not correctly identify public properties or getters/setters for " + domainClass, e);
        }
    }

    public Object getAttribute(final String attribute, final T entity) {
        return getters.getOrDefault(attribute, (t) -> {
            throw new RuntimeException(attribute + " not declared for entity " + entity.getClass().getSimpleName());
        }).apply(entity);
    }

    public void setAttribute(final String attribute, final T entity, final Object value) {
        setters.getOrDefault(attribute, (t, o) -> {
            throw new RuntimeException(attribute + " not declared for entity " + entity.getClass().getSimpleName());
        }).accept(entity, value);
    }

    private ThrowingFunction<T, Object> createGetterFunction(final String name, final PropertyDescriptor it, final Class<T> domainClass) throws IllegalAccessException {
        if (it != null && it.getReadMethod() != null) {
            return (entity) -> it.getReadMethod().invoke(entity);
        }
        final Field field = ReflectionUtils.fetchField(name, domainClass)
                .filter(this::isFieldAccessible)
                .orElseThrow(() -> new IllegalAccessException(String.format("Could not determine a getter or public field on class %s for accessing field %s", domainClass.getName(), name)));
        return field::get;
    }

    private ThrowingConsumer<T, Object> createSetterFunction(final String name, final PropertyDescriptor it, final Class<T> domainClass) throws IllegalAccessException {
        if (it != null && it.getWriteMethod() != null) {
            return (entity, value) -> it.getWriteMethod().invoke(entity, value);
        }
        final Field field = ReflectionUtils.fetchField(name, domainClass)
                .filter(this::isFieldAccessible)
                .orElseThrow(() -> new IllegalAccessException(String.format("Could not determine a setter or public field on class %s for accessing field %s", domainClass.getName(), name)));
        return field::set;
    }

    private boolean isFieldAccessible(final Field field) {
        final int modifier = field.getModifiers();
        return Modifier.isPublic(modifier) && !Modifier.isStatic(modifier) && !Modifier.isFinal(modifier);
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
