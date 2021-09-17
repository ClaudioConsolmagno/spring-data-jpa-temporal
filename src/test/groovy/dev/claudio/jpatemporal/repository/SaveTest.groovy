package dev.claudio.jpatemporal.repository


import dev.claudio.jpatemporal.BaseTestSpecification
import dev.claudio.jpatemporal.domain.Employee
import org.springframework.dao.InvalidDataAccessApiUsageException
import org.springframework.orm.jpa.JpaSystemException

import java.time.Instant

class SaveTest extends BaseTestSpecification {

    Instant testStartTime

    def bartJob = { -> new Employee(temporal_id: null, employee_id: 5, name: 'Bart Simpson', job: 'Student')}

    def setup() {
        testStartTime = Instant.now()
    }

    def "save - Insert new entity with non-null UniqueKey"() {
        given:
            assert repository.count() == 3
            assert repository.findById(5).isEmpty()
        when:
            def saved = saveAndFlush
                ? repository.save(bartJob())
                : repository.saveAndFlush(bartJob())
        then:
            repository.count() == 4
            saved == repository.findById(5).get()
        and:
            assertCurrentEmployee(saved, 5, 'Bart Simpson', 'Student')
        where:
            saveAndFlush << [true,false]
    }

    def "save - Insert new entity with null UniqueKey"() {
        given:
            assert repository.count() == 3
            assert repository.findById(5).isEmpty()
        when:
            saveAndFlush
                ? repository.save(bartJob().tap {employee_id = null })
                : repository.saveAndFlush(bartJob().tap {employee_id = null })
        then:
            thrown(JpaSystemException)
        where:
            saveAndFlush << [true,false]
    }

    def "save - Update entity with no change"() {
        given:
            def homerOriginal = repository.findById(1).get()
            assert homerOriginal == homerLatestJob()
        when:
            def saved = saveAndFlush
                ? repository.save(homerOriginal.tap {job = "Astronaut"; from_date = null})
                : repository.saveAndFlush(homerOriginal.tap {job = "Astronaut"; from_date = null})
        then:
            saved == homerLatestJob()
            assertTemporalAttributesAreSame(homerOriginal, saved)
            repository.findById(1).get() == homerLatestJob()
        where:
            saveAndFlush << [true,false]
    }

    def "save - Update entity with change"() {
        given:
            assert repository.findById(1).get() == homerLatestJob()
        when:
            def saved = saveAndFlush
                ? repository.save(homerLatestJob().tap {job = "Bartender"; from_date = null})
                : repository.saveAndFlush(homerLatestJob().tap {job = "Bartender"; from_date = null})
        then:
            saved != homerLatestJob()
            saved == repository.findById(1).get()
            assertCurrentEmployee(saved, 1, 'Homer Simpson', 'Bartender')
        where:
            saveAndFlush << [true,false]
    }

    def "saveAll - null entities or null entity within Iterable param"() {
        when:
            repository.saveAll(null)
        then:
            thrown(InvalidDataAccessApiUsageException)
        when:
            repository.saveAll([null])
        then:
            thrown(InvalidDataAccessApiUsageException)
        when:
            repository.saveAll([homerLatestJob().tap {job = "Bartender"; from_date = null }, null])
        then: 'exception thrown and no change to other entities'
            thrown(InvalidDataAccessApiUsageException)
            repository.findById(1).get() == homerLatestJob()
    }

    def "saveAll - Save entities"() {
        given:
            assert repository.findById(5).isEmpty()
            assert repository.findById(6).isEmpty()
        when:
            def savedList = repository.saveAll([
                bartJob(),
                new Employee(temporal_id: null, employee_id: 6, name: 'Lisa Simpson', job: 'Student'),
            ])
        then:
            def saved5 = repository.findById(5).get()
            def saved6 = repository.findById(6).get()
            savedList as Set == [saved5, saved6] as Set
        and:
            assertCurrentEmployee(saved5, 5, 'Bart Simpson', 'Student')
            assertCurrentEmployee(saved6, 6, 'Lisa Simpson', 'Student')
    }

    def "saveAll - Update entities"() {
        given:
            def homerOriginal = repository.findById(1).get()
            def margeOriginal = repository.findById(2).get()
            assert homerOriginal == homerLatestJob()
            assert margeOriginal == margeLatestJob()
        when:
            def savedList = repository.saveAll([
                homerOriginal.tap {job = "Bartender"; from_date = null},
                margeOriginal.tap {job = "Painter"}
            ])
        then:
            def saved1 = repository.findById(1).get()
            def saved2 = repository.findById(2).get()
            savedList as Set == [saved1, saved2] as Set
        and:
            assertCurrentEmployee(saved1, 1, 'Homer Simpson', 'Bartender')
            assertCurrentEmployee(saved2, 2, 'Marge Simpson', 'Painter')
        and:
            homerOriginal == saved1
            margeOriginal == saved2
            assertTemporalAttributesAreSame(homerOriginal, saved1)
            assertTemporalAttributesAreSame(margeOriginal, saved2)
    }

    def "saveAll - Save and Update entities"() {
        given:
            def homerOriginal = repository.findById(1).get()
            assert homerOriginal == homerLatestJob()
            assert repository.findById(5).isEmpty()
        when:
            def savedList = repository.saveAll([
                bartJob(), // New entity
                homerOriginal.tap {job = "Bartender"; from_date = null}, // existing entity, should change
            ])
        then:
            def saved1 = repository.findById(1).get()
            def saved5 = repository.findById(5).get()
            savedList as Set == [saved1, saved5] as Set
        and:
            assertCurrentEmployee(saved1, 1, 'Homer Simpson', 'Bartender')
            assertCurrentEmployee(saved5, 5, 'Bart Simpson', 'Student')
    }

    def "saveAll - Save, Update, No change entities"() {
        given:
            def homerOriginal = repository.findById(1).get()
            def margeOriginal = repository.findById(2).get()
            assert homerOriginal == homerLatestJob()
            assert margeOriginal == margeLatestJob()
            assert repository.findById(5).isEmpty()
        when:
            def savedList = repository.saveAll([
                bartJob(), // New entity
                homerOriginal.tap {job = "Bartender"; from_date = null}, // existing entity, should change
                margeOriginal.tap {from_date = null; to_date = null; temporal_id = null} // existing entity, no change
            ])
        then:
            def saved1 = repository.findById(1).get()
            def saved2 = repository.findById(2).get()
            def saved5 = repository.findById(5).get()
            savedList as Set == [saved1, saved2, saved5] as Set
        and:
            assertCurrentEmployee(saved1, 1, 'Homer Simpson', 'Bartender')
            assertCurrentEmployee(saved5, 5, 'Bart Simpson', 'Student')
        and:
            margeOriginal == saved2
            assertTemporalAttributesAreSame(margeOriginal, saved2)
    }

    boolean assertCurrentEmployee(employee, employee_id, name, job) {
        assert employee.employee_id == employee_id
        assert employee.name == name
        assert employee.job == job
        assert employee.from_date.isAfter(testStartTime.plusMillis(-1))
        assert employee.from_date.isBefore(Instant.now().plusMillis(1))
        assert employee.to_date == MAX_INSTANT
        return true
    }

    boolean assertTemporalAttributesAreSame(employee, employee2) {
        assert employee.from_date == employee2.from_date
        assert employee.temporal_id == employee2.temporal_id
        assert employee.to_date == employee2.to_date
        return true
    }
}
