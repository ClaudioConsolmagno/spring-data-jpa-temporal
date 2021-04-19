package dev.claudio.jpatemporal.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;

/**
 * Specifies the "unique" attribute of an entity that is always non-null and there can be no other entity with the same
 * key at the same point in time. On a non-temporal table this would be the primary key but for this library the
 * actual primary key is annotated by {@link TemporalId}. For this reason {@link javax.persistence.Id} cannot be used
 * with {@code UniqueKey} and is used with {@link TemporalId} instead.
 * <p>
 * It is possible to annotate with {@code UniqueKey} a {@link javax.persistence.Embeddable} class as long as the
 * same class also uses {@link javax.persistence.Embedded}.
 * <p>
 * It is possible to annotate the attribute with {@link javax.persistence.Column} in order to use a different database
 * column {@code name}. Other {@link javax.persistence.Column} attributes should not be used.
 *
 * @see TemporalId
 */
@Target({METHOD, FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface UniqueKey {
}
