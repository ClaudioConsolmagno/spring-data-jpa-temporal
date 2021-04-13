package dev.claudio.jpatemporal.repository

import static dev.claudio.jpatemporal.repository.TemporalRepository.MAX_INSTANT

import dev.claudio.jpatemporal.BaseTestSpecification
import dev.claudio.jpatemporal.domain.Employee
import org.springframework.orm.jpa.JpaSystemException

import java.time.Instant

class SaveTest extends BaseTestSpecification {

    Instant testStartTime

    def bartJob = { -> new Employee(temporal_id: null, employee_id: 5, name: 'Bart Simpson', job: 'Student')}

    def setup() {
        testStartTime = Instant.now()
    }

    def "save - Insert new entity with non-null EntityId"() {
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

    def "save - Insert new entity with null EntityId"() {
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
            assert repository.findById(1).get() == homerLatestJob()
        when:
            def saved = saveAndFlush
                ? repository.save(homerLatestJob().tap {job = "Astronaut"})
                : repository.saveAndFlush(homerLatestJob().tap {job = "Astronaut"})
        then:
            saved == homerLatestJob()
            repository.findById(1).get() == homerLatestJob()
        where:
            saveAndFlush << [true,false]
    }

    def "save - Update entity with change"() {
        given:
            assert repository.findById(1).get() == homerLatestJob()
        when:
            def saved = saveAndFlush
                ? repository.save(homerLatestJob().tap {job = "Bartender"})
                : repository.saveAndFlush(homerLatestJob().tap {job = "Bartender"})
        then:
            saved != homerLatestJob()
            saved == repository.findById(1).get()
            assertCurrentEmployee(saved, 1, 'Homer Simpson', 'Bartender')
        where:
            saveAndFlush << [true,false]
    }

    def "saveAll - Save and Update entities"() {
        given:
            assert repository.findById(1).get() == homerLatestJob()
            assert repository.findById(5).isEmpty()
        when:
            def savedList = repository.saveAll([bartJob(), homerLatestJob().tap {job = "Bartender"}])
        then:
            def saved1 = repository.findById(1).get()
            def saved5 = repository.findById(5).get()
            savedList as Set == [saved1, saved5] as Set
        and:
            assertCurrentEmployee(saved1, 1, 'Homer Simpson', 'Bartender')
            assertCurrentEmployee(saved5, 5, 'Bart Simpson', 'Student')
    }

    boolean assertCurrentEmployee(employee, employee_id, name, job) {
        assert employee.employee_id == employee_id
        assert employee.name == name
        assert employee.job == job
        assert employee.from_date.isAfter(testStartTime)
        assert employee.from_date.isBefore(Instant.now())
        assert employee.to_date == MAX_INSTANT
        return true
    }
}
