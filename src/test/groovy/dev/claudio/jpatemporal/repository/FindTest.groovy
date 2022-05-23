package dev.claudio.jpatemporal.repository


import static java.time.Instant.now

import dev.claudio.jpatemporal.BaseTestSpecification
import dev.claudio.jpatemporal.domain.Employee
import org.springframework.dao.IncorrectResultSizeDataAccessException
import org.springframework.data.domain.Example
import org.springframework.data.jpa.domain.Specification
import org.springframework.orm.jpa.JpaObjectRetrievalFailureException

import java.time.Instant

class FindTest extends BaseTestSpecification {

    def "findById"() {
        expect:
            repository.findById(1).get() == homerLatestJob()
            repository.findById(2).get() == margeLatestJob()
            repository.findById(3).get() == skinnerLatestJob()
            repository.findById(4).isEmpty()
        and: 'audit tests'
            repository.findById(1, now()).get() == homerLatestJob()
            repository.findById(1, MAX_INSTANT).get() == homerLatestJob()
        and:
            repository.findById(1, year(1994)).isEmpty()
            repository.findById(1, year(1995)).get() == new Employee(temporal_id: 1, employee_id: 1, name: 'Homer Simpson',   job: 'Nuclear Technician', from_date: year(1995), to_date: year(1996))
            repository.findById(1, Instant.parse("1995-06-01T00:00:00.000Z")).get() == new Employee(temporal_id: 1, employee_id: 1, name: 'Homer Simpson',   job: 'Nuclear Technician', from_date: year(1995), to_date: year(1996))
            repository.findById(1, Instant.parse("1995-12-31T12:59:59.999Z")).get() == new Employee(temporal_id: 1, employee_id: 1, name: 'Homer Simpson',   job: 'Nuclear Technician', from_date: year(1995), to_date: year(1996))
            repository.findById(1, year(1996)).get() == new Employee(temporal_id: 5, employee_id: 1, name: 'Homer Simpson',   job: 'Nuclear Safety Inspector', from_date: year(1996), to_date: year(1997))
    }

    def "getOne"() {
        expect:
            repository.getOne(1) == homerLatestJob()
            repository.getOne(2) == margeLatestJob()
            repository.getOne(3) == skinnerLatestJob()
    }

    def "getOne - missing entity"() {
        when:
            repository.getOne(4)
        then:
            thrown(JpaObjectRetrievalFailureException) //EntityNotFoundException is converted by jpa to EntityNotFoundException, see EntityManagerFactoryUtils
    }

    def "getById"() {
        expect:
            repository.getById(1) == homerLatestJob()
            repository.getById(2) == margeLatestJob()
            repository.getById(3) == skinnerLatestJob()
    }

    def "getById - missing entity"() {
        when:
            repository.getById(4)
        then:
            thrown(JpaObjectRetrievalFailureException) //EntityNotFoundException is converted by jpa to EntityNotFoundException, see EntityManagerFactoryUtils
    }

    def "getReferenceById"() {
        expect:
            repository.getReferenceById(1) == homerLatestJob()
            repository.getReferenceById(2) == margeLatestJob()
            repository.getReferenceById(3) == skinnerLatestJob()
    }

    def "getReferenceById - missing entity"() {
        when:
            repository.getReferenceById(4)
        then:
            thrown(JpaObjectRetrievalFailureException) //EntityNotFoundException is converted by jpa to EntityNotFoundException, see EntityManagerFactoryUtils
    }

    def "findAll"() {
        expect:
            repository.findAll().size() == 3
            repository.findAll() as Set == [homerLatestJob(), margeLatestJob(), skinnerLatestJob()] as Set
            repository.findAll(now()) as Set == [homerLatestJob(), margeLatestJob(), skinnerLatestJob()] as Set
            repository.findAll(MAX_INSTANT) as Set == [homerLatestJob(), margeLatestJob(), skinnerLatestJob()] as Set
        and: 'audit tests'
            repository.findAll(year(1990)).size() == 0
        and: 'audit tests'
            repository.findAll(year(1997)).size() == 4
            repository.findAll(year(1997)) as Set == [
                new Employee(temporal_id: 8, employee_id: 1, name: 'Homer Simpson',   job: 'Snow Plow Driver', from_date: year(1997), to_date: year(1998)),
                margeLatestJob(),
                skinnerLatestJob(),
                new Employee(temporal_id: 7, employee_id: 4, name: 'Barney Gumble',   job: 'Human Guinea Pig', from_date: year(1997), to_date: year(1999))
            ] as Set
        and: 'audit tests'
            repository.findAll(year(1997)).size() == 4
            repository.findAll(year(1997)) as Set == [
                new Employee(temporal_id: 8, employee_id: 1, name: 'Homer Simpson',   job: 'Snow Plow Driver', from_date: year(1997), to_date: year(1998)),
                margeLatestJob(),
                skinnerLatestJob(),
                new Employee(temporal_id: 7, employee_id: 4, name: 'Barney Gumble',   job: 'Human Guinea Pig', from_date: year(1997), to_date: year(1999))
            ] as Set
    }

    def "findAllById"() {
        expect:
            repository.findAllById([1,3,4]).size() == 2
            repository.findAllById([1,3,4]) as Set == [homerLatestJob(), skinnerLatestJob()] as Set
            repository.findAllById(1..10000) as Set == [homerLatestJob(), margeLatestJob(), skinnerLatestJob()] as Set
        and:
            repository.findAllById([1,3,4], now()).size() == 2
            repository.findAllById([1,3,4], now()) as Set == [homerLatestJob(), skinnerLatestJob()] as Set
        and:
            repository.findAllById([1,3,4], year(1997)).size() == 3
            repository.findAllById([1,3,4], year(1997)).size() == 3
            repository.findAllById([1,3,4], year(1997)) as Set == [
                new Employee(temporal_id: 8, employee_id: 1, name: 'Homer Simpson',   job: 'Snow Plow Driver', from_date: year(1997), to_date: year(1998)),
                skinnerLatestJob(),
                new Employee(temporal_id: 7, employee_id: 4, name: 'Barney Gumble',   job: 'Human Guinea Pig', from_date: year(1997), to_date: year(1999))
            ] as Set
    }

    def "findAll Example"() {
        expect:
            repository.findAll().size() == 3
            repository.findAll(Example.of(new Employee(employee_id: 1))) == [homerLatestJob()]
            repository.findAll(Example.of(new Employee(to_date: MAX_INSTANT))) as Set == [homerLatestJob(), margeLatestJob(), skinnerLatestJob()] as Set
    }

    def "findAll Spec"() {
        expect:
            // findAll methods clashes with groovy's built-in findAll method so need to cast Specification<Employee>
            repository.findAll((Specification<Employee>)((root, _, cb) -> cb.equal(root.get("employee_id"), 1))) == [homerLatestJob()]
            repository.findAll((Specification<Employee>)((root, _, cb) -> cb.equal(root.get("employee_id"), 2))) == [margeLatestJob()]
            repository.findAll((Specification<Employee>)((root, _, cb) -> cb.equal(root.get("employee_id"), 3))) == [skinnerLatestJob()]
            repository.findAll((Specification<Employee>)((root, _, cb) -> cb.equal(root.get("employee_id"), 4))) == []
            repository.findAll((Specification<Employee>)((root, _, cb) -> cb.equal(root.get("employee_id"), 5))) == []
            repository.findAll((Specification<Employee>)((root, _, cb) -> cb.equal(root.get("to_date"), MAX_INSTANT))) as Set == [homerLatestJob(), margeLatestJob(), skinnerLatestJob()] as Set
            repository.findAll((Specification<Employee>)((root, _, cb) -> cb.equal(root.get("job"), 'Astronaut'))) == [homerLatestJob()]
            repository.findAll((Specification<Employee>)((root, _, cb) -> cb.equal(root.get("job"), 'Snow Plow Driver'))) == []
        and:
            repository.findAll(((root, _, cb) -> cb.equal(root.get("employee_id"), 1)), now()) == [homerLatestJob()]
            repository.findAll(((root, _, cb) -> cb.equal(root.get("employee_id"), 1)), year(1995)) == [new Employee(temporal_id: 1, employee_id: 1, name: 'Homer Simpson',   job: 'Nuclear Technician', from_date: year(1995), to_date: year(1996))]
            repository.findAll((Specification<Employee>)((root, _, cb) -> cb.equal(root.get("job"), 'Astronaut')), year(1995)) == []
            repository.findAll((Specification<Employee>)((root, _, cb) -> cb.equal(root.get("job"), 'Snow Plow Driver')), year(1997)) == [new Employee(temporal_id: 8, employee_id: 1, name: 'Homer Simpson',   job: 'Snow Plow Driver', from_date: year(1997), to_date: year(1998))]
    }

    def "findOne Spec - 0 or 1 results"() {
        expect:
            repository.findOne((root, _, cb) -> cb.equal(root.get("employee_id"), 1)).get() == homerLatestJob()
            repository.findOne((root, _, cb) -> cb.equal(root.get("employee_id"), 2)).get() == margeLatestJob()
            repository.findOne((root, _, cb) -> cb.equal(root.get("employee_id"), 3)).get() == skinnerLatestJob()
            repository.findOne((root, _, cb) -> cb.equal(root.get("employee_id"), 4)).isEmpty()
            repository.findOne((root, _, cb) -> cb.equal(root.get("employee_id"), 5)).isEmpty()
            repository.findOne((root, _, cb) -> cb.equal(root.get("job"), 'Astronaut')).get() == homerLatestJob()
            repository.findOne((root, _, cb) -> cb.equal(root.get("job"), 'Snow Plow Driver')).isEmpty()
    }

    def "findOne Spec - more than 1 results"() {
        when:
            repository.findOne((root, _, cb) -> cb.equal(root.get("to_date"), MAX_INSTANT)).get()
        then:
            thrown(IncorrectResultSizeDataAccessException)
    }
}
