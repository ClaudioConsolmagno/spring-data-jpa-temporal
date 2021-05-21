package dev.claudio.jpatemporal.repository.impl


import spock.lang.Specification

class EntityAccessSupportTest extends Specification {

    def "AnnotatedEntitySupport can use public fields when no getters and setters"() {
        given:
            def entityAccessSupport = new EntityAccessSupport<ClassPublicFields>(ClassPublicFields.class, ['a'] as Set);
            def targetEntity = new ClassPublicFields()
            assert targetEntity.a == 'a1'
        expect:
            entityAccessSupport.getAttribute('a', targetEntity) == 'a1'
            entityAccessSupport.setAttribute('a', targetEntity, 'a2')
            entityAccessSupport.getAttribute('a', targetEntity) == 'a2'
            targetEntity.a == 'a2'
    }

    def "AnnotatedEntitySupport prefers getters and setters over public fields: #targetEntity"() {
        given:
            def entityAccessSupport = new EntityAccessSupport(targetEntity.class, ['a'] as Set);
            assert targetEntity.@a == 'a1'
            assert !targetEntity.getAused
            assert !targetEntity.setAused
        expect:
            entityAccessSupport.getAttribute('a', targetEntity) == 'a1'
            targetEntity.getAused
            !targetEntity.setAused
        when:
            targetEntity.getAused = false
        then:
            entityAccessSupport.setAttribute('a', targetEntity, 'a2')
            !targetEntity.getAused
            targetEntity.setAused
        and:
            entityAccessSupport.getAttribute('a', targetEntity) == 'a2'
            targetEntity.@a == 'a2'
        where:
            targetEntity << [
                new ClassBean(),
                new ClassBeanPublicFields()
            ]
    }

    def "AnnotatedEntitySupport fails when using non declared attribute"() {
        given:
            def entityAccessSupport = new EntityAccessSupport<ClassPublicFields>(ClassPublicFields.class, ['a'] as Set);
            def targetEntity = new ClassPublicFields()
            assert targetEntity.b == 'b1'
        when:
            entityAccessSupport.getAttribute('b', targetEntity)
        then:
            thrown(RuntimeException)
        when:
            entityAccessSupport.setAttribute('b', targetEntity, 'b2')
        then:
            thrown(RuntimeException)
    }

    class ClassPublicFields {
        public Object a = 'a1'
        public Object b = 'b1'
    }

    class ClassBean {
        private Object a = 'a1'
        private Object b = 'b1'

        boolean getAused = false
        boolean setAused = false
        boolean getBused = false
        boolean setBused = false

        Object getA() {
            getAused = true
            return a
        }

        void setA(final Object a) {
            setAused = true
            this.a = a
        }

        Object getB() {
            getBused = true
            return b
        }

        void setB(final Object b) {
            setBused = true
            this.b = b
        }
    }

    class ClassBeanPublicFields {
        public Object a = 'a1'
        public Object b = 'b1'

        boolean getAused = false
        boolean setAused = false
        boolean getBused = false
        boolean setBused = false

        Object getA() {
            getAused = true
            return a
        }

        void setA(final Object a) {
            setAused = true
            this.a = a
        }

        Object getB() {
            getBused = true
            return b
        }

        void setB(final Object b) {
            setBused = true
            this.b = b
        }
    }
}
