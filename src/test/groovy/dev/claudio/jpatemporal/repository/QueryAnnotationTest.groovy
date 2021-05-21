package dev.claudio.jpatemporal.repository

import dev.claudio.jpatemporal.BaseTestSpecification

class QueryAnnotationTest extends BaseTestSpecification {

    def "Query annotation method"() {
        when:
            def employees = repository.findByEmployeeNameCustomQuery('Homer Simpson')
        then:
            employees.size() == 1
            employees[0] == homerLatestJob()
    }

    def "Query annotation method with audit"() {
        when:
            def employees = repository.findByEmployeeNameFullAuditCustomQuery('Homer Simpson')
        then:
            employees.size() == 4
            employees[0] == simpsonsEmployees()[0]
            employees[1] == simpsonsEmployees()[4]
            employees[2] == simpsonsEmployees()[7]
            employees[3] == simpsonsEmployees()[8]
            employees[3] == homerLatestJob()
    }
}
