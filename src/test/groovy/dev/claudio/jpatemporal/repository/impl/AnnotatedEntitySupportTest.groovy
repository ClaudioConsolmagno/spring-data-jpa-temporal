package dev.claudio.jpatemporal.repository.impl

import dev.claudio.jpatemporal.annotation.FromDate
import dev.claudio.jpatemporal.annotation.TemporalId
import dev.claudio.jpatemporal.annotation.ToDate
import dev.claudio.jpatemporal.annotation.UniqueKey
import dev.claudio.jpatemporal.domain.Temporal
import dev.claudio.jpatemporal.exception.JpaTemporalException
import spock.lang.Specification
import spock.lang.Unroll

import javax.persistence.ManyToMany
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.OneToOne

class AnnotatedEntitySupportTest extends Specification {

    def "AnnotatedEntitySupport constructor should throw exception for class #targetClass"() {
        when:
            new AnnotatedEntitySupport(targetClass);
        then:
            thrown(JpaTemporalException)
        where:
            targetClass << [
                ClassMissingUniqueKey.class,
                ClassMissingTemporalId.class,
                ClassMissingFromDate.class,
                ClassMissingToDate.class,
                ClassOneToOneRelation.class,
                ClassOneToManyRelation.class,
                ClassManyToOneRelation.class,
                ClassManyToManyRelation.class,
            ]
    }

    def "AnnotatedEntitySupport constructor should correctly set annotated fields for #targetClass"() {
        when:
            AnnotatedEntitySupport entitySupport = new AnnotatedEntitySupport(targetClass);
        then:
            entitySupport.uniqueKey == uniqueKey
            entitySupport.temporalId == temporalId
            entitySupport.fromDate == fromDate
            entitySupport.toDate == toDate
        where:
            targetClass           | uniqueKey | temporalId    | fromDate    | toDate
            ClassAllAnnotations   | 'a'       | 'b'           | 'c'         | 'd'
            ClassExtendsOverrides | 'a'       | 'b'           | 'c'         | 'd'
            ClassExtendsTemporal  | 'a'       | 'temporal_id' | 'from_date' | 'to_date'
    }

    class ClassMissingUniqueKey {
        @TemporalId Object b
        @FromDate Object c
        @ToDate Object d
    }

    class ClassMissingTemporalId {
        @UniqueKey Object a
        @FromDate Object c
        @ToDate Object d
    }

    class ClassMissingFromDate {
        @UniqueKey Object a
        @TemporalId Object b
        @ToDate Object d
    }

    class ClassMissingToDate {
        @UniqueKey Object a
        @TemporalId Object b
        @FromDate Object c
    }

    class ClassOneToOneRelation extends Temporal {
        @OneToOne @UniqueKey Object a
    }

    class ClassOneToManyRelation extends Temporal {
        @OneToMany @UniqueKey Object a
    }

    class ClassManyToOneRelation extends Temporal {
        @ManyToOne @UniqueKey Object a
    }

    class ClassManyToManyRelation extends Temporal {
        @ManyToMany @UniqueKey Object a
    }

    class ClassAllAnnotations {
        @UniqueKey Object a
        @TemporalId Object b
        @FromDate Object c
        @ToDate Object d
    }

    class ClassExtendsOverrides extends Temporal {
        @UniqueKey Object a
        @TemporalId Object b
        @FromDate Object c
        @ToDate Object d
    }

    class ClassExtendsTemporal extends Temporal {
        @UniqueKey Object a
    }
}
