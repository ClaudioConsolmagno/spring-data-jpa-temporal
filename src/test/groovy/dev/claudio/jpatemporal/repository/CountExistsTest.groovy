package dev.claudio.jpatemporal.repository


import static java.time.Instant.now

import dev.claudio.jpatemporal.BaseTestSpecification
import dev.claudio.jpatemporal.domain.Employee
import org.springframework.data.domain.Example

class CountExistsTest extends BaseTestSpecification {

    def "count"() {
        expect:
            repository.count() == 3
            repository.count(now()) == 3
            repository.count(MAX_INSTANT) == 3
            repository.count(year(1997)) == 4
            repository.count(year(1990)) == 0
    }

    def "existsById"() {
        expect:
            repository.existsById(1)
            repository.existsById(2)
            repository.existsById(3)
            !repository.existsById(4)
            !repository.existsById(5)
    }

    def "count Example"() {
        expect:
            repository.count(Example.of(new Employee(employee_id: 1))) == 1
            repository.count(Example.of(new Employee(employee_id: 2))) == 1
            repository.count(Example.of(new Employee(employee_id: 3))) == 1
            repository.count(Example.of(new Employee(employee_id: 4))) == 0
            repository.count(Example.of(new Employee(employee_id: 5))) == 0
            repository.count(Example.of(new Employee(to_date: MAX_INSTANT))) == 3
            repository.count(Example.of(new Employee(job: 'Astronaut'))) == 1
            repository.count(Example.of(new Employee(job: 'Snow Plow Driver'))) == 0
    }

    def "count Spec"() {
        expect:
            repository.count((root, _, cb) -> cb.equal(root.get("employee_id"), 1)) == 1
            repository.count((root, _, cb) -> cb.equal(root.get("employee_id"), 2)) == 1
            repository.count((root, _, cb) -> cb.equal(root.get("employee_id"), 3)) == 1
            repository.count((root, _, cb) -> cb.equal(root.get("employee_id"), 4)) == 0
            repository.count((root, _, cb) -> cb.equal(root.get("employee_id"), 5)) == 0
            repository.count((root, _, cb) -> cb.equal(root.get("to_date"), MAX_INSTANT)) == 3
            repository.count((root, _, cb) -> cb.equal(root.get("job"), 'Astronaut')) == 1
            repository.count((root, _, cb) -> cb.equal(root.get("job"), 'Snow Plow Driver')) == 0
        and:
            repository.count((root, _, cb) -> cb.equal(root.get("employee_id"), 1), now()) == 1
            repository.count((root, _, cb) -> cb.equal(root.get("employee_id"), 1), MAX_INSTANT) == 1
            repository.count((root, _, cb) -> cb.equal(root.get("employee_id"), 1), year(1990)) == 0
            repository.count((root, _, cb) -> cb.equal(root.get("job"), 'Astronaut'), now()) == 1
            repository.count((root, _, cb) -> cb.equal(root.get("job"), 'Astronaut'), year(1998)) == 1
            repository.count((root, _, cb) -> cb.equal(root.get("job"), 'Astronaut'), year(1997)) == 0
            repository.count((root, _, cb) -> cb.equal(root.get("job"), 'Snow Plow Driver'), year(1997)) == 1

    }

    def "exists Example"() {
        expect:
            repository.exists(Example.of(new Employee(employee_id: 1)))
            repository.exists(Example.of(new Employee(employee_id: 2)))
            repository.exists(Example.of(new Employee(employee_id: 3)))
            !repository.exists(Example.of(new Employee(employee_id: 4)))
            !repository.exists(Example.of(new Employee(employee_id: 5)))
    }
}
