package dev.claudio.jpatemporal.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;

/**
 * Specifies the exclusive end {@link java.time.Instant} of when the entity ended being valid (e.g. entity was deleted).
 * Users are not expected to ever set the value referenced by this field.
 *
 * @see FromDate
 */
@Target({METHOD, FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ToDate {
}
