package dev.claudio.jpatemporal.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;

/**
 * Specifies the "primary key" attribute of an entity that is always non-null and there can be no other entity with the
 * same key at any point in time. Requirements:
 * <ul>
 *    <li>The same attribute should also be annotated with {@link javax.persistence.Id}.</li>
 *    <li>The same attribute should also be annotated with {@link javax.persistence.GeneratedValue}. It is suggested
 *    {@link javax.persistence.GenerationType#IDENTITY} to be used for simplicity and that works with a column that has
 *    type {@code "INT AUTO_INCREMENT PRIMARY KEY"}.</li>
 *</ul>
 *
 * It is possible to annotate the attribute with {@link javax.persistence.Column} in order to use a different database
 * column {@code name}. Other {@link javax.persistence.Column} attributes should not be used.
 *
 * @see UniqueKey
 */
@Target({METHOD, FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface TemporalId {
}
