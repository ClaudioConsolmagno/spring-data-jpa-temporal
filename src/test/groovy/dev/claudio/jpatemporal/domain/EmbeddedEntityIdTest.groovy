package dev.claudio.jpatemporal.domain

import dev.claudio.jpatemporal.annotation.EntityId
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
class EmbeddedEntityIdTest extends Specification {

    @Autowired RepositoryWithEmbeddedEntityIdKey repositoryWithEmbeddedEntityIdKey
    @Autowired RepositoryWithEmbeddedEntityIdKeyJpa repositoryWithEmbeddedEntityIdKeyJpa

    def setup() {
        assert repositoryWithEmbeddedEntityIdKeyJpa.count() == 0
    }

    def cleanup() {
        repositoryWithEmbeddedEntityIdKeyJpa.deleteAll()
    }

    def "Can use @EntityId on an @Embedded object as key"() {
        given:
            def homer = new EmployeeWithEmbeddedEntityId(employeeKey: new EmployeeKey(employee_id: 1, employee_id_pt_2: 100), name: 'Homer Simpson')
            def marge = new EmployeeWithEmbeddedEntityId(employeeKey: new EmployeeKey(employee_id: 1, employee_id_pt_2: 200), name: 'Marge Bouvier')
        when:
            def savedHomer = repositoryWithEmbeddedEntityIdKey.save(homer)
            def savedMarge = repositoryWithEmbeddedEntityIdKey.save(marge)
        then:
            savedHomer.name == repositoryWithEmbeddedEntityIdKey.findById(new EmployeeKey(employee_id: 1, employee_id_pt_2: 100)).get().name
            savedMarge.name == repositoryWithEmbeddedEntityIdKey.findById(new EmployeeKey(employee_id: 1, employee_id_pt_2: 200)).get().name
            savedHomer.name == 'Homer Simpson'
            savedMarge.name == 'Marge Bouvier'
        when:
            def newMarge = new EmployeeWithEmbeddedEntityId(employeeKey: new EmployeeKey(employee_id: 1, employee_id_pt_2: 200), name: 'Marge Simpson')
            def savedNewMarge = repositoryWithEmbeddedEntityIdKey.save(newMarge)
        then:
            savedNewMarge.name == repositoryWithEmbeddedEntityIdKey.findById(new EmployeeKey(employee_id: 1, employee_id_pt_2: 200)).get().name
            savedNewMarge.name == 'Marge Simpson'
        and:
            assert repositoryWithEmbeddedEntityIdKey.count() == 2
            assert repositoryWithEmbeddedEntityIdKeyJpa.count() == 3
    }
}

@Entity
@Table(name = "employees_object_key")
@Canonical
@EqualsAndHashCode(callSuper = true)
class EmployeeWithEmbeddedEntityId extends Temporal {
    @EntityId
    @Embedded
    EmployeeKey employeeKey;
    String name;
}

@Embeddable
@Canonical
class EmployeeKey {
    Integer employee_id;
    Integer employee_id_pt_2;
}

interface RepositoryWithEmbeddedEntityIdKey extends TemporalRepository<EmployeeWithEmbeddedEntityId, EmployeeKey> { }

interface RepositoryWithEmbeddedEntityIdKeyJpa extends JpaRepository<EmployeeWithEmbeddedEntityId, Long> { }
