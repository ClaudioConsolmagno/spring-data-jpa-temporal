package dev.claudio.jpatemporal.repository

import static dev.claudio.jpatemporal.repository.TemporalRepository.MAX_INSTANT

import dev.claudio.jpatemporal.BaseTestSpecification
import dev.claudio.jpatemporal.domain.Employee
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.orm.jpa.JpaSystemException

import java.time.Instant

class DeleteTest extends BaseTestSpecification {

    Instant testStartTime

    def bartJob = { -> new Employee(temporal_id: null, employee_id: 5, name: 'Bart Simpson', job: 'Student')}

    def setup() {
        testStartTime = Instant.now()
    }

    def "delete - Delete entity that doesn't exist"() {
        given:
            def bartEmployeeId = bartJob().employee_id
            assert repository.findById(bartEmployeeId).isEmpty()
            assert repository.count() == 3
        when:
            repository.delete(bartJob())
        then:
            repository.findById(bartEmployeeId).isEmpty()
            assert repository.count() == 3
    }

    def "delete - Delete entity with a null @UniqueKey"() {
        given:
            def employee = bartJob().tap {employee_id = null}
            assert repository.count() == 3
        when:
            repository.delete(employee)
        then:
            thrown(JpaSystemException)
            assert repository.count() == 3
    }

    def "delete - Delete entity that exists"() {
        given:
            assert repository.findById(1).get() == homerLatestJob()
            assert repository.count() == 3
        when:
            repository.delete(homerLatestJob())
        then:
            repository.findById(1).isEmpty()
            assert repository.count() == 2
    }

    def "deleteById - DeleteById entity that doesn't exist"() {
        given:
            def bartEmployeeId = bartJob().employee_id
            assert repository.findById(bartEmployeeId).isEmpty()
            assert repository.count() == 3
        when:
            repository.deleteById(bartEmployeeId)
        then:
            thrown(EmptyResultDataAccessException)
            assert repository.count() == 3
    }

    def "deleteById - Delete entity by ID that exists"() {
        given:
            assert repository.findById(1).get() == homerLatestJob()
            assert repository.count() == 3
        when:
            repository.deleteById(homerLatestJob().employee_id)
        then:
            repository.findById(1).isEmpty()
            assert repository.count() == 2
    }

    def "deleteAll - Delete All"() {
        given:
            assert repository.count() == 3
            assert repositoryJpa.count() == 9
            assert repositoryJpa.findAll().count {it.to_date == MAX_INSTANT } == 3
        when:
            repository.deleteAll()
        then:
            repository.count() == 0
            repositoryJpa.count() == 9
            repositoryJpa.findAll().count {it.to_date == MAX_INSTANT } == 0
    }

    def "deleteAll - Delete All in batch"() {
        given:
            assert repository.count() == 3
            assert repositoryJpa.count() == 9
            assert repositoryJpa.findAll().count {it.to_date == MAX_INSTANT } == 3
        when:
            repository.deleteAllInBatch()
        then:
            repository.count() == 0
            repositoryJpa.count() == 9
            repositoryJpa.findAll().count {it.to_date == MAX_INSTANT } == 0
    }

    def "deleteAll by entities"() {
        given:
            assert repository.findById(1).get() == homerLatestJob()
            assert repository.findById(3).get() == skinnerLatestJob()
            assert repository.count() == 3
        when:
            repository.deleteAll([homerLatestJob(), skinnerLatestJob()])
        then:
            repository.findById(1).isEmpty()
            repository.findById(3).isEmpty()
            assert repository.count() == 1
    }

    def "deleteAll by entities in batch"() {
        given:
            assert repository.findById(1).get() == homerLatestJob()
            assert repository.findById(3).get() == skinnerLatestJob()
            assert repository.count() == 3
        when:
            repository.deleteInBatch([homerLatestJob(), skinnerLatestJob()])
        then:
            repository.findById(1).isEmpty()
            repository.findById(3).isEmpty()
            assert repository.count() == 1
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
