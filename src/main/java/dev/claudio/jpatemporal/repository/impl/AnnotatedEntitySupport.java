package dev.claudio.jpatemporal.repository.impl;

import dev.claudio.jpatemporal.annotation.FromDate;
import dev.claudio.jpatemporal.annotation.TemporalId;
import dev.claudio.jpatemporal.annotation.ToDate;
import dev.claudio.jpatemporal.annotation.UniqueKey;
import lombok.Getter;
import lombok.ToString;

import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@ToString
class AnnotatedEntitySupport {
    private static final Set<Class<? extends Annotation>> RELATIONAL_ANNOTATIONS =
            Collections.unmodifiableSet(new HashSet<>(
                    Arrays.asList(OneToOne.class, OneToMany.class, ManyToOne.class, ManyToMany.class)
            ));

    @Getter private final String uniqueKey;
    @Getter private final String temporalId;
    @Getter private final String fromDate;
    @Getter private final String toDate;

    AnnotatedEntitySupport(final Class<?> domainClass) {
        validateNoRelationalAnnotations(domainClass);
        this.uniqueKey = ReflectionUtils.fetchAnnotatedField(domainClass, UniqueKey.class);
        this.temporalId = ReflectionUtils.fetchAnnotatedField(domainClass, TemporalId.class);
        this.fromDate = ReflectionUtils.fetchAnnotatedField(domainClass, FromDate.class);
        this.toDate = ReflectionUtils.fetchAnnotatedField(domainClass, ToDate.class);
    }

    public Set<String> getAllAttributes() {
        return Collections.unmodifiableSet(new HashSet<>(
                Arrays.asList(uniqueKey, temporalId, fromDate, toDate)
        ));
    }

    private void validateNoRelationalAnnotations(final Class<?> domainClass) {
        boolean hasRelationalAnnotations = RELATIONAL_ANNOTATIONS.stream()
                .anyMatch(annotation ->
                        ReflectionUtils.fetchAnnotatedMethods(domainClass, annotation).size()
                        + ReflectionUtils.fetchAnnotatedFields(domainClass, annotation).size() > 0
                );
        if (hasRelationalAnnotations) {
            throw new RuntimeException("Relational Annotations are not supported: " + RELATIONAL_ANNOTATIONS);
        }
    }
}
