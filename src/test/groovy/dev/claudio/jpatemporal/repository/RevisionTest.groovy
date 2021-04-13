package dev.claudio.jpatemporal.repository

import dev.claudio.jpatemporal.BaseTestSpecification
import dev.claudio.jpatemporal.domain.Employee
import dev.claudio.jpatemporal.repository.impl.RevisionMetadataImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.history.Revision
import org.springframework.data.history.Revisions

class RevisionTest extends BaseTestSpecification {

    def "findLastChangeRevision - missing"() {
        expect:
            repository.findLastChangeRevision(0).isEmpty()
            repository.findLastChangeRevision(100).isEmpty()
    }

    def "findLastChangeRevision"() {
        when:
            def barneyLastRevision = repository.findLastChangeRevision(4).get()
        then:
            barneyLastRevision.getEntity() == new Employee(temporal_id: 7, employee_id: 4, name: 'Barney Gumble',   job: 'Human Guinea Pig', from_date: year(1997), to_date: year(1999))
            barneyLastRevision.getRevisionNumber().get() == 1
            barneyLastRevision.getRevisionInstant().get() == year(1997)
        when:
            def homerLastRevision = repository.findLastChangeRevision(1).get()
        then:
            homerLastRevision.getEntity() == homerLatestJob()
            homerLastRevision.getRevisionNumber().get() == 4
            homerLastRevision.getRevisionInstant().get() == year(1998)
    }

    def "findRevisions - missing"() {
        expect:
            repository.findRevisions(0).content.size() == 0
            repository.findRevisions(100).content.size() == 0
    }

    def "findRevisions"() {
        when:
            Revisions<Integer, Employee> barneyRevisions = repository.findRevisions(4)
        then:
            barneyRevisions.content.size() == 1
            barneyRevisions.content[0] == barneyRevisions.latestRevision
            barneyRevisions.content[0].entity == new Employee(temporal_id: 7, employee_id: 4, name: 'Barney Gumble',   job: 'Human Guinea Pig', from_date: year(1997), to_date: year(1999))
            barneyRevisions.content[0].revisionNumber.get() == 1
            barneyRevisions.content[0].revisionInstant.get() == year(1997)
        when:
            Revisions<Integer, Employee> homerRevisions = repository.findRevisions(1)
        then:
            homerRevisions.content.size() == 4
            homerRevisions.content[3] == homerRevisions.latestRevision
            homerRevisions.content[3].entity == homerLatestJob()
            homerRevisions.content[3].revisionNumber.get() == 4
            homerRevisions.content[3].revisionInstant.get() == year(1998)
        and:
            homerRevisions.content[0].entity == new Employee(temporal_id: 1, employee_id: 1, name: 'Homer Simpson',   job: 'Nuclear Technician', from_date: year(1995), to_date: year(1996))
            homerRevisions.content[0].revisionNumber.get() == 1
            homerRevisions.content[0].revisionInstant.get() == year(1995)
        and:
            homerRevisions.content[1].entity == new Employee(temporal_id: 5, employee_id: 1, name: 'Homer Simpson',   job: 'Nuclear Safety Inspector', from_date: year(1996), to_date: year(1997))
            homerRevisions.content[1].revisionNumber.get() == 2
            homerRevisions.content[1].revisionInstant.get() == year(1996)
        and:
            homerRevisions.content[2].entity == new Employee(temporal_id: 8, employee_id: 1, name: 'Homer Simpson',   job: 'Snow Plow Driver', from_date: year(1997), to_date: year(1998))
            homerRevisions.content[2].revisionNumber.get() == 3
            homerRevisions.content[2].revisionInstant.get() == year(1997)
    }

    def "findRevision - missing"() {
        expect:
            repository.findRevision(0, 1).isEmpty()
            repository.findRevision(100, 100).isEmpty()
            repository.findRevision(1, 0).isEmpty()
            repository.findRevision(1, 5).isEmpty()
            repository.findRevision(100, 100).isEmpty()
            repository.findRevision(4, 0).isEmpty()
            repository.findRevision(4, 2).isEmpty()
    }

    def "metadata entity = revision entity"() {
        when:
            Revision<Integer, Employee> barneyRevision1 = repository.findRevision(4, 1).get()
        then:
            barneyRevision1.entity == barneyRevision1.getMetadata().getDelegate()
            barneyRevision1.entity == ((RevisionMetadataImpl) barneyRevision1.getMetadata()).getEntity()
    }

    def "findRevision"() {
        when:
            Revision<Integer, Employee> barneyRevision1 = repository.findRevision(4, 1).get()
        then:
            barneyRevision1.getMetadata().getDelegate() == barneyRevision1.entity
            ((RevisionMetadataImpl) barneyRevision1.getMetadata()).getEntity() == barneyRevision1.entity
            barneyRevision1.entity == new Employee(temporal_id: 7, employee_id: 4, name: 'Barney Gumble',   job: 'Human Guinea Pig', from_date: year(1997), to_date: year(1999))
            barneyRevision1.revisionNumber.get() == 1
            barneyRevision1.revisionInstant.get() == year(1997)
        when:
            def homerRevision4 = repository.findRevision(1, 4).get()
        then:
            homerRevision4.entity == homerLatestJob()
            homerRevision4.revisionNumber.get() == 4
            homerRevision4.revisionInstant.get() == year(1998)
        when:
            def homerRevision1 = repository.findRevision(1, 1).get()
        then:
            homerRevision1.entity == new Employee(temporal_id: 1, employee_id: 1, name: 'Homer Simpson',   job: 'Nuclear Technician', from_date: year(1995), to_date: year(1996))
            homerRevision1.revisionNumber.get() == 1
            homerRevision1.revisionInstant.get() == year(1995)
        when:
            def homerRevision2 = repository.findRevision(1, 2).get()
        then:
            homerRevision2.entity == new Employee(temporal_id: 5, employee_id: 1, name: 'Homer Simpson',   job: 'Nuclear Safety Inspector', from_date: year(1996), to_date: year(1997))
            homerRevision2.revisionNumber.get() == 2
            homerRevision2.revisionInstant.get() == year(1996)
        when:
            def homerRevision3 = repository.findRevision(1, 3).get()
        then:
            homerRevision3.entity == new Employee(temporal_id: 8, employee_id: 1, name: 'Homer Simpson',   job: 'Snow Plow Driver', from_date: year(1997), to_date: year(1998))
            homerRevision3.revisionNumber.get() == 3
            homerRevision3.revisionInstant.get() == year(1997)
    }

    def "findRevisions pageable - UnsupportedOperationException"() {
        when:
            repository.findRevisions(0, Mock(Pageable))
        then:
            thrown(UnsupportedOperationException)
    }
}
