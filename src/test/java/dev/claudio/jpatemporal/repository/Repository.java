package dev.claudio.jpatemporal.repository;

import dev.claudio.jpatemporal.domain.Employee;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface Repository extends TemporalRepository<Employee, Integer> {

    @Query("SELECT e FROM Employee e WHERE e.name = ?1 and e.to_date > now()")
    List<Employee> findByEmployeeNameCustomQuery(final String name);

    @Query("SELECT e FROM Employee e WHERE e.name = ?1 order by e.from_date")
    List<Employee> findByEmployeeNameFullAuditCustomQuery(final String name);
}
