package dev.claudio.jpatemporal.repository;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.lang.NonNull;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * A temporal repository extension of {@link org.springframework.data.jpa.repository.JpaRepository},
 * {@link org.springframework.data.jpa.repository.JpaSpecificationExecutor} and
 * {@link org.springframework.data.repository.history.RevisionRepository}.
 * <p>
 * The repository transparently keeps audit of the data on save/delete while find/count always returns the latest data.
 * <p>
 * Audit can be retrieved using the methods defined in this interface or via {@link RevisionRepository} methods.
 *
 * @param <T>  the type of the entity to handle
 * @param <ID> the type of the entity's identifier (referenced by {@link dev.claudio.jpatemporal.annotation.UniqueKey}
 * @author Claudio Consolmagno
 */
@NoRepositoryBean
public interface TemporalRepository<T, ID> extends JpaRepository<T, ID>, JpaSpecificationExecutor<T>, RevisionRepository<T, ID, Integer> {

    /**
     * The date 99999999-01-01T00:00:00.000Z which is far into the future representing "infinity" time. This is used by
     * {@link dev.claudio.jpatemporal.annotation.ToDate} to denote that an entity is current and has not been deleted.
     */
    Instant MAX_INSTANT = Instant.ofEpochSecond(3155633001244800L);

    /**
     * Retrieves an entity by its {@code ids} at the given {@code asOfInstant}.
     *
     * @param id must not be {@literal null}.
     * @param asOfInstant must not be {@literal null}.
     * @return the entity with the given id or {@literal Optional#empty()} if none found.
     * @throws IllegalArgumentException if {@literal id} or {@literal asOfInstant} are {@literal null}.
     */
    Optional<T> findById(@NonNull ID id, @NonNull Instant asOfInstant);

    /**
     * Returns all instances of the type {@code T} with the given {@code ids} at the given {@code asOfInstant}.
     * <p>
     * If some or all ids are not found, no entities are returned for these IDs.
     * <p>
     * Note that the order of elements in the result is not guaranteed.
     *
     * @param ids must not be {@literal null} nor contain any {@literal null} values.
     * @param asOfInstant can be {@literal null} in which case all of the audit related to {@code ids} are returned.
     * @return guaranteed to be not {@literal null}. The size can be equal or less than the number of given
     *         {@literal ids}.
     * @throws IllegalArgumentException in case the given {@link Iterable ids} or one of its items is {@literal null}.
     */
    List<T> findAllById(@NonNull Iterable<ID> ids, Instant asOfInstant);

    /**
     * Returns all instances of the type {@code T} at the given {@code asOfInstant}.
     *
     * @param asOfInstant must not be {@literal null}.
     * @return all entities
     */
    List<T> findAll(@NonNull Instant asOfInstant);

    /**
     * Returns all entities matching the given {@link Specification} at the given {@code asOfInstant}.
     *
     * @param spec can be {@literal null}.
     * @param asOfInstant must not be {@literal null}.
     * @return never {@literal null}.
     */
    List<T> findAll(Specification<T> spec, @NonNull Instant asOfInstant);

    /**
     * Returns the number of entities available at the given {@code asOfInstant}.
     *
     * @param asOfInstant must not be {@literal null}.
     * @return the number of entities.
     */
    long count(@NonNull Instant asOfInstant);

    /**
     * Returns the number of instances that the given {@link Specification} will return at the given {@code asOfInstant}.
     *
     * @param spec the {@link Specification} to count instances for. Can be {@literal null}.
     * @param asOfInstant must not be {@literal null}.
     * @return the number of instances.
     */
    long count(Specification<T> spec, @NonNull Instant asOfInstant);
}
