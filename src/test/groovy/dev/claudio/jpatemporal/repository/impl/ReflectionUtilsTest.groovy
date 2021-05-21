package dev.claudio.jpatemporal.repository.impl

import static java.lang.annotation.ElementType.FIELD
import static java.lang.annotation.ElementType.METHOD

import dev.claudio.jpatemporal.annotation.FromDate
import dev.claudio.jpatemporal.annotation.TemporalId
import dev.claudio.jpatemporal.annotation.ToDate
import dev.claudio.jpatemporal.annotation.UniqueKey
import spock.lang.Specification

import javax.persistence.Column
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target
import java.lang.reflect.Field

class ReflectionUtilsTest extends Specification {

    def "fetchAnnotatedColumnName()"() {
        expect:
            ReflectionUtils.fetchAnnotatedColumnName(TargetClass1, TemporalId).isEmpty()
            ReflectionUtils.fetchAnnotatedColumnName(TargetClass1, UniqueKey).isEmpty()
        and:
            ReflectionUtils.fetchAnnotatedColumnName(TargetClass1, FromDate).get() == 'fieldC1'
            ReflectionUtils.fetchAnnotatedColumnName(TargetClass2, FromDate).get() == 'fieldC2'
            ReflectionUtils.fetchAnnotatedColumnName(TargetClass1, ToDate).get() == 'fieldD2'
            ReflectionUtils.fetchAnnotatedColumnName(TargetClass2, ToDate).get() == 'fieldD2'
        and:
            ReflectionUtils.fetchAnnotatedColumnName(TargetClass1, ExtraAnnotation).get() == 'custom_column_name_field_b1'
            ReflectionUtils.fetchAnnotatedColumnName(TargetClass2, ExtraAnnotation).get() == 'an_annotated_getter'
    }

    def "fetchAnnotatedMethods()"() {
        expect:
            ReflectionUtils.fetchAnnotatedMethods(TargetClass1, TemporalId) == []
            ReflectionUtils.fetchAnnotatedMethods(TargetClass2, TemporalId) == []
        and:
            ReflectionUtils.fetchAnnotatedMethods(TargetClass1, UniqueKey) == []
            ReflectionUtils.fetchAnnotatedMethods(TargetClass2, UniqueKey) == [TargetClass2.getDeclaredMethod('annotatedMethodTC2')]
    }

    def "fetchAnnotatedFields()"() {
        expect:
            ReflectionUtils.fetchAnnotatedFields(TargetClass1, TemporalId) == []
            ReflectionUtils.fetchAnnotatedFields(TargetClass2, TemporalId) == []
        and:
            ReflectionUtils.fetchAnnotatedFields(TargetClass1, UniqueKey) == []
            ReflectionUtils.fetchAnnotatedFields(TargetClass2, UniqueKey) == [TargetClass2.getDeclaredField('annotatedField')]
    }

    def "getField()"() {
        when:
            Field fieldA1 = ReflectionUtils.fetchField("fieldA1", TargetClass1).get()
            Field fieldB1 = ReflectionUtils.fetchField("fieldB1", TargetClass1).get()
            Field fieldC1 = ReflectionUtils.fetchField("fieldC1", TargetClass1).get()
            Field fieldA2 = ReflectionUtils.fetchField("fieldA2", TargetClass1).get()
            Field fieldB2 = ReflectionUtils.fetchField("fieldB2", TargetClass1).get()
            Field fieldC2 = ReflectionUtils.fetchField("fieldC2", TargetClass1).get()
            Field fieldClash = ReflectionUtils.fetchField("fieldClash", TargetClass1).get()
            Field fieldClash2 = ReflectionUtils.fetchField("fieldClash", TargetClass2).get()
        then:
            fieldA1 == TargetClass1.getDeclaredField("fieldA1")
            fieldB1 == TargetClass1.getDeclaredField("fieldB1")
            fieldC1 == TargetClass1.getDeclaredField("fieldC1")
            fieldA2 == TargetClass2.getDeclaredField("fieldA2")
            fieldB2 == TargetClass2.getDeclaredField("fieldB2")
            fieldC2 == TargetClass2.getDeclaredField("fieldC2")
            fieldClash == TargetClass1.getDeclaredField("fieldClash")
            fieldClash2 == TargetClass2.getDeclaredField("fieldClash")
    }

    def "getField() - empty"() {
        expect:
            ReflectionUtils.fetchField("foobar", TargetClass1).isEmpty()
    }

    class TargetClass1 extends TargetClass2 {
        String fieldA1

        @Column(name = 'custom_column_name_field_b1')
        @ExtraAnnotation
        int fieldB1

        @FromDate
        Object fieldC1
        Object fieldClash


        @Column(name = 'custom_column_name')
        def annotatedMethodTC1() {}

        def nonAnnotatedMethodTC1() {}
    }

    class TargetClass2 {

        String fieldA2

        int fieldB2

        @FromDate
        Object fieldC2
        Object fieldClash

        @ToDate
        Object fieldD2

        @UniqueKey
        String annotatedField

        @UniqueKey
        def annotatedMethodTC2() {}

        @ExtraAnnotation
        def getAn_Annotated_Getter() {}
    }
}

@Target([METHOD, FIELD])
@Retention(RetentionPolicy.RUNTIME)
@interface ExtraAnnotation {

}
