package dev.claudio.jpatemporal.repository;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.lang.NonNull;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface TemporalRepository<T, ID> extends JpaRepository<T, ID>, JpaRepositoryImplementation<T, ID>, RevisionRepository<T, ID, Integer> {

    Instant MAX_INSTANT = Instant.parse("9999-01-01T00:00:00.000Z");

    Optional<T> findById(@NonNull ID id, Instant asOfInstant);

    List<T> findAllById(@NonNull Iterable<ID> ids, Instant asOfInstant);

    List<T> findAll(@NonNull Instant asOfInstant);
    List<T> findAll(Specification<T> spec, @NonNull Instant asOfInstant);

    long count(@NonNull Instant asOfInstant);
    long count(Specification<T> spec, @NonNull Instant asOfInstant);
}
