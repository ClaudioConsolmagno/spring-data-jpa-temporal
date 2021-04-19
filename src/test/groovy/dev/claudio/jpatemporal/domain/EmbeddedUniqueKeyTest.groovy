package dev.claudio.jpatemporal.domain

import dev.claudio.jpatemporal.annotation.UniqueKey
import dev.claudio.jpatemporal.repository.TemporalRepository
import groovy.transform.Canonical
import groovy.transform.EqualsAndHashCode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.jpa.repository.JpaRepository
import spock.lang.Specification

import javax.persistence.Embeddable
import javax.persistence.Embedded
import javax.persistence.Entity
import javax.persistence.Table

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class EmbeddedUniqueKeyTest extends Specification {

    @Autowired RepositoryWithEmbeddedUniqueKey repositoryWithEmbeddedUniqueKey
    @Autowired RepositoryWithEmbeddedUniqueKeyJpa repositoryWithEmbeddedUniqueKeyJpa

    def setup() {
        assert repositoryWithEmbeddedUniqueKeyJpa.count() == 0
    }

    def cleanup() {
        repositoryWithEmbeddedUniqueKeyJpa.deleteAll()
    }

    def "Can use @EntityId on an @Embedded object as key"() {
        given:
            def homer = new EmployeeWithEmbeddedUniqueKey(employeeKey: new EmployeeKey(employee_id: 1, employee_id_pt_2: 100), name: 'Homer Simpson')
            def marge = new EmployeeWithEmbeddedUniqueKey(employeeKey: new EmployeeKey(employee_id: 1, employee_id_pt_2: 200), name: 'Marge Bouvier')
        when:
            def savedHomer = repositoryWithEmbeddedUniqueKey.save(homer)
            def savedMarge = repositoryWithEmbeddedUniqueKey.save(marge)
        then:
            savedHomer.name == repositoryWithEmbeddedUniqueKey.findById(new EmployeeKey(employee_id: 1, employee_id_pt_2: 100)).get().name
            savedMarge.name == repositoryWithEmbeddedUniqueKey.findById(new EmployeeKey(employee_id: 1, employee_id_pt_2: 200)).get().name
            savedHomer.name == 'Homer Simpson'
            savedMarge.name == 'Marge Bouvier'
        when:
            def newMarge = new EmployeeWithEmbeddedUniqueKey(employeeKey: new EmployeeKey(employee_id: 1, employee_id_pt_2: 200), name: 'Marge Simpson')
            def savedNewMarge = repositoryWithEmbeddedUniqueKey.save(newMarge)
        then:
            savedNewMarge.name == repositoryWithEmbeddedUniqueKey.findById(new EmployeeKey(employee_id: 1, employee_id_pt_2: 200)).get().name
            savedNewMarge.name == 'Marge Simpson'
        and:
            assert repositoryWithEmbeddedUniqueKey.count() == 2
            assert repositoryWithEmbeddedUniqueKeyJpa.count() == 3
    }
}

@Entity
@Table(name = "employees_object_key")
@Canonical
@EqualsAndHashCode(callSuper = false)
class EmployeeWithEmbeddedUniqueKey extends Temporal {
    @UniqueKey
    @Embedded
    EmployeeKey employeeKey
    String name
}

@Embeddable
@Canonical
class EmployeeKey {
    Integer employee_id
    Integer employee_id_pt_2
}

interface RepositoryWithEmbeddedUniqueKey extends TemporalRepository<EmployeeWithEmbeddedUniqueKey, EmployeeKey> { }

interface RepositoryWithEmbeddedUniqueKeyJpa extends JpaRepository<EmployeeWithEmbeddedUniqueKey, Long> { }
