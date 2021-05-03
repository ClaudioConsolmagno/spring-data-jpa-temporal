package dev.claudio.jpatemporal

import dev.claudio.jpatemporal.domain.Employee
import dev.claudio.jpatemporal.repository.Repository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.jpa.repository.JpaRepository
import spock.lang.Specification

import java.time.Instant

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class BaseTestSpecification extends Specification {

    @Autowired Repository repository
    @Autowired RepositoryJpa repositoryJpa

    static final MAX_INSTANT = year(9999)

    def setup() {
        assert repositoryJpa.count() == 0
        repositoryJpa.saveAll(simpsonsEmployees())
        assert repositoryJpa.count() == 9
    }

    def cleanup() {
        repositoryJpa.deleteAll()
    }

    def static homerLatestJob = { -> new Employee(temporal_id: 9, employee_id: 1, name: 'Homer Simpson', job: 'Astronaut', from_date: year(1998), to_date: MAX_INSTANT)}
    def static margeLatestJob = { -> new Employee(temporal_id: 6, employee_id: 2, name: 'Marge Simpson',   job: 'Estate Agent', from_date: year(1997), to_date: MAX_INSTANT)}
    def static skinnerLatestJob = { -> new Employee(temporal_id: 3, employee_id: 3, name: 'Seymour Skinner', job: 'School Principal', from_date: year(1995), to_date: MAX_INSTANT)}

    static def year(year) {  Instant.parse("$year-01-01T00:00:00.000Z") }

    static def simpsonsEmployees() {
        return [
            new Employee(temporal_id: 1, employee_id: 1, name: 'Homer Simpson',   job: 'Nuclear Technician', from_date: year(1995), to_date: year(1996)),
            new Employee(temporal_id: 2, employee_id: 2, name: 'Marge Simpson',   job: 'Pretzel Cart Saleswoman', from_date: year(1995), to_date: year(1996)),
            skinnerLatestJob(),
            new Employee(temporal_id: 4, employee_id: 2, name: 'Marge Simpson',   job: 'Bakery Owner', from_date: year(1996), to_date: year(1997)),
            new Employee(temporal_id: 5, employee_id: 1, name: 'Homer Simpson',   job: 'Nuclear Safety Inspector', from_date: year(1996), to_date: year(1997)),
            margeLatestJob(),
            new Employee(temporal_id: 7, employee_id: 4, name: 'Barney Gumble',   job: 'Human Guinea Pig', from_date: year(1997), to_date: year(1999)),
            new Employee(temporal_id: 8, employee_id: 1, name: 'Homer Simpson',   job: 'Snow Plow Driver', from_date: year(1997), to_date: year(1998)),
            homerLatestJob()
        ]
    }
}

interface RepositoryJpa extends JpaRepository<Employee, Long> { }
