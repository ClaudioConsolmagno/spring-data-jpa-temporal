package dev.claudio.jpatemporal.domain

import dev.claudio.jpatemporal.annotation.FromDate
import dev.claudio.jpatemporal.annotation.TemporalId
import dev.claudio.jpatemporal.annotation.ToDate
import dev.claudio.jpatemporal.annotation.UniqueKey
import dev.claudio.jpatemporal.repository.TemporalRepository
import groovy.transform.Canonical
import groovy.transform.EqualsAndHashCode
import org.hibernate.annotations.GenericGenerator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.jpa.repository.JpaRepository
import spock.lang.Specification

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.Table
import java.time.Instant

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class UuidUniqueKeyTest extends Specification {

    @Autowired RepositoryForEmployeeWithUuidUniqueKey repositoryForEmployeeWithUuidUniqueKey
    @Autowired RepositoryForEmployeeWithUuidUniqueKeyJpa repositoryForEmployeeWithUuidUniqueKeyJpa

    def setup() {
        assert repositoryForEmployeeWithUuidUniqueKeyJpa.count() == 0
    }

    def cleanup() {
        repositoryForEmployeeWithUuidUniqueKeyJpa.deleteAll()
    }

    UUID employee1 = UUID.fromString("00000000-0000-0000-0000-000000000001")
    UUID employee2 = UUID.fromString("00000000-0000-0000-0000-000000000002")

    def "Can use @GenericGenerator with @TemporalId and can change names of from/to date instants"() {
        given:
            def homer = new EmployeeWithUuidUniqueKey(employeeKey: employee1, name: 'Homer Simpson')
            def marge = new EmployeeWithUuidUniqueKey(employeeKey: employee2, name: 'Marge Bouvier')
        when:
            def savedHomer = repositoryForEmployeeWithUuidUniqueKey.save(homer)
            def savedMarge = repositoryForEmployeeWithUuidUniqueKey.save(marge)
        then:
            savedHomer.name == repositoryForEmployeeWithUuidUniqueKey.findById(employee1).get().name
            savedMarge.name == repositoryForEmployeeWithUuidUniqueKey.findById(employee2).get().name
            savedHomer.name == 'Homer Simpson'
            savedMarge.name == 'Marge Bouvier'
        when:
            def newMarge = new EmployeeWithUuidUniqueKey(employeeKey: employee2, name: 'Marge Simpson')
            def savedNewMarge = repositoryForEmployeeWithUuidUniqueKey.save(newMarge)
        then:
            savedNewMarge.name == repositoryForEmployeeWithUuidUniqueKey.findById(employee2).get().name
            savedNewMarge.name == 'Marge Simpson'
        and:
            assert repositoryForEmployeeWithUuidUniqueKey.count() == 2
            assert repositoryForEmployeeWithUuidUniqueKeyJpa.count() == 3
    }
}

@Entity
@Table(name = "employees_uuid_key")
@Canonical
@EqualsAndHashCode(excludes = ["myTemporalId","myFromDate","myToDate"])
class EmployeeWithUuidUniqueKey {
    @UniqueKey
    @Column(columnDefinition = "uuid")
    UUID employeeKey

    @Id
    @TemporalId
    @Column(columnDefinition = "uuid")
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "org.hibernate.id.UUIDGenerator")
    UUID myTemporalId

    @FromDate
    Instant myFromDate

    @ToDate
    Instant myToDate

    String name
}

interface RepositoryForEmployeeWithUuidUniqueKey extends TemporalRepository<EmployeeWithUuidUniqueKey, UUID> { }

interface RepositoryForEmployeeWithUuidUniqueKeyJpa extends JpaRepository<EmployeeWithUuidUniqueKey, UUID> { }
