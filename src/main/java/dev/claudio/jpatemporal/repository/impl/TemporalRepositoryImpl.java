package dev.claudio.jpatemporal.repository.impl;

import dev.claudio.jpatemporal.exception.JpaTemporalException;
import dev.claudio.jpatemporal.repository.TemporalRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.val;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.history.Revision;
import org.springframework.data.history.Revisions;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.lang.NonNull;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Default implementation of the {@link TemporalRepository} interface.
 * <p>
 * This implementation extends {@link SimpleJpaRepository} overriding the required methods so that all save/delete
 * queries don't remove data from the database and instead use "from" and "to date" attributes to keep track of what's
 * been delete and what's current. Find/exist queries are also overridden in order for them to use those same attributes
 * and return only the current data.
 *
 * @author Claudio Consolmagno
 */
@NoRepositoryBean
public class TemporalRepositoryImpl<T, ID> extends SimpleJpaRepository<T, ID> implements TemporalRepository<T, ID> {

    public static final ChronoUnit TIMESTAMP_PRECISION_DEFAULT = ChronoUnit.MICROS;
    public static final Instant MAX_INSTANT_DEFAULT = truncate(Instant.parse("9999-01-01T00:00:00.000000000Z"));

    private final JpaEntityInformation<T, ID> entityInformation;
    private final EntityManager em;
    private final AnnotatedEntitySupport annotatedEntitySupport;
    private final EntityAccessSupport<T> entityAccessSupport;

    @SuppressFBWarnings({"EI_EXPOSE_REP2"})
    public TemporalRepositoryImpl(final JpaEntityInformation<T, ID> entityInformation, final EntityManager em) {
        super(entityInformation, em);
        this.entityInformation = entityInformation;
        this.em = em;
        this.annotatedEntitySupport = new AnnotatedEntitySupport(entityInformation.getJavaType());
        this.entityAccessSupport = new EntityAccessSupport<>(entityInformation.getJavaType(), this.annotatedEntitySupport.getAllAttributes());
    }

    /******************************************************************************************************************
     *
     * ********************************** TemporalRepository
     *
     ******************************************************************************************************************/

    @Override
    public Optional<T> findById(@NonNull final ID id, @NonNull final Instant asOfInstant) {
        return findAllById(Collections.singletonList(id), asOfInstant).stream().findFirst();
    }

    @Override
    public List<T> findAllById(@NonNull final Iterable<ID> ids, final Instant asOfInstant) {
        return super.getQuery(inIdSpec(ids).and(toAndFromSpecification(asOfInstant)), this.getDomainClass(), Sort.unsorted()).getResultList();
    }

    @Override
    public List<T> findAll(@NonNull final Instant asOfInstant) {
        return this.findAll(null, asOfInstant);
    }

    @Override
    public List<T> findAll(final Specification<T> spec, @NonNull final Instant asOfInstant) {
        return super.getQuery(toAndFromSpecification(asOfInstant).and(spec), this.getDomainClass(), Sort.unsorted()).getResultList();
    }

    @Override
    public long count(@NonNull final Instant asOfInstant) {
        return this.count(null, asOfInstant);
    }

    @Override
    public long count(final Specification<T> spec, @NonNull final Instant asOfInstant) {
        return super.getCountQuery(toAndFromSpecification(asOfInstant).and(spec), this.getDomainClass()).getResultList()
                .stream()
                .reduce(0L, Long::sum);
    }

    /******************************************************************************************************************
     *
     * ********************************** JpaRepository
     *
     ******************************************************************************************************************/

    @NonNull
    @Override
    public Optional<T> findById(@NonNull final ID id) {
        return super.findOne((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(annotatedEntitySupport.getUniqueKey()), id));
    }

    @Override
    public boolean existsById(@NonNull final ID id) {
        return this.findById(id).isPresent();
    }

    @NonNull
    @Override
    public List<T> findAllById(@NonNull final Iterable<ID> ids) {
        return findAllById(ids, MAX_INSTANT_DEFAULT);
    }

    @NonNull
    @Override
    @Deprecated
    public T getOne(@NonNull final ID id) {
        return this.findById(id).orElseThrow(EntityNotFoundException::new);
    }

    @Override
    public long count() {
        return super.count((Specification<T>) null);
    }

    @NonNull
    @Override
    @Transactional
    public <S extends T> S save(@NonNull final S entity) {
        ID id = getIdFromEntity(entity);
        Optional<T> existingEntity = findById(id);
        if (existingEntity.isPresent() && existingEntity.get().equals(entity)) {
            return entity;
        }
        val currentTime = now();
        deleteById(id, currentTime);
        entityAccessSupport.setAttribute(annotatedEntitySupport.getFromDate(), entity, currentTime);
        entityAccessSupport.setAttribute(annotatedEntitySupport.getToDate(), entity, MAX_INSTANT_DEFAULT);
        entityAccessSupport.setAttribute(annotatedEntitySupport.getTemporalId(), entity, null);
        return super.save(entity);
    }

    @NonNull
    @Override
    @Transactional
    public <S extends T> List<S> saveAll(@NonNull final Iterable<S> entities) {
        Assert.notNull(entities, "Entities must not be null!");
        val currentTime = now();
        Map<ID, S> toSaveEntities = StreamSupport.stream(entities.spliterator(), false)
                .peek(it -> Assert.notNull(it, "Entities must not be null!"))
                .collect(Collectors.toMap(this::getIdFromEntity, it -> it));
        Map<ID, T> existingEntitiesMap = findAllById(toSaveEntities.keySet(), currentTime).stream()
                .collect(Collectors.toMap(this::getIdFromEntity, it -> it));

        // Work out any entities that are already on the DB and are equals to the ones being saved. Set the temporal
        // attributes on those entities as they won't be saved but will be returned.
        Map<ID, S> entitiesThatDontNeedToBeSaved = toSaveEntities.entrySet().stream()
                .filter(it -> Optional.ofNullable(existingEntitiesMap.get(it.getKey()))
                        .filter(existingEntity -> {
                            val toBeSavedEntity = it.getValue();
                            if (existingEntity.equals(toBeSavedEntity)) {
                                Object existingTemporalId = entityAccessSupport.getAttribute(annotatedEntitySupport.getTemporalId(), existingEntity);
                                Object existingFromDate = entityAccessSupport.getAttribute(annotatedEntitySupport.getFromDate(), existingEntity);
                                entityAccessSupport.setAttribute(annotatedEntitySupport.getTemporalId(), toBeSavedEntity, existingTemporalId);
                                entityAccessSupport.setAttribute(annotatedEntitySupport.getFromDate(), toBeSavedEntity, existingFromDate);
                                entityAccessSupport.setAttribute(annotatedEntitySupport.getToDate(), toBeSavedEntity, MAX_INSTANT_DEFAULT);
                                return true;
                            }
                            return false;
                        })
                        .isPresent())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        // Delete any entities that need to be saved and exist on the DB
        Set<ID> toDeleteIds = toSaveEntities.keySet().stream()
                .filter(s -> !entitiesThatDontNeedToBeSaved.containsKey(s) && existingEntitiesMap.containsKey(s))
                .collect(Collectors.toSet());
        if (!toDeleteIds.isEmpty()) deleteByIds(toDeleteIds, currentTime);

        // Save any entities (that need to be saved) while setting temporal attributes
        Stream<S> savedEntitiesStream = toSaveEntities.entrySet().stream()
                .filter(it -> !entitiesThatDontNeedToBeSaved.containsKey(it.getKey()))
                .map(entry -> {
                    val entity = entry.getValue();
                    entityAccessSupport.setAttribute(annotatedEntitySupport.getFromDate(), entity, currentTime);
                    entityAccessSupport.setAttribute(annotatedEntitySupport.getToDate(), entity, MAX_INSTANT_DEFAULT);
                    entityAccessSupport.setAttribute(annotatedEntitySupport.getTemporalId(), entity, null);
                    return super.save(entity);
                });

        return Stream.concat(savedEntitiesStream, entitiesThatDontNeedToBeSaved.values().stream())
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(@NonNull final ID id) {
        if (this.deleteById(id, now()) <= 0) {
            throw new EmptyResultDataAccessException(String.format("No %s entity with id %s exists!", entityInformation.getJavaType(), id), 1);
        }
    }

    @Override
    public void delete(@NonNull final T entity) {
        final ID id = this.getIdFromEntity(entity);
        this.deleteById(id, now());
    }

    @Override
    public void deleteAllInBatch() {
        this.deleteByIds(null, now());
    }

    @Override
    public void deleteAllInBatch(@NonNull final Iterable<T> entities) {
        Set<ID> idsToDelete = StreamSupport.stream(entities.spliterator(), false)
                .map(this::getIdFromEntity)
                .collect(Collectors.toSet());
        if (idsToDelete.isEmpty()) return;
        this.deleteByIds(idsToDelete, now());
    }

    @Override
    public void deleteAllByIdInBatch(@NonNull final Iterable<ID> ids) {
        Set<ID> idsToDelete = StreamSupport.stream(ids.spliterator(), false)
                .collect(Collectors.toSet());
        if (idsToDelete.isEmpty()) return;
        this.deleteByIds(idsToDelete, now());
    }

    @Override
    @Deprecated
    public void deleteInBatch(@NonNull final Iterable<T> entities) {
        this.deleteAllInBatch(entities);
    }

    @Override
    @NonNull
    protected <S extends T> TypedQuery<S> getQuery(final Specification<S> spec, @NonNull final Class<S> domainClass, @NonNull final Sort sort) {
        final Specification<S> toDateSpec = (root, query, criteriaBuilder) -> toAndFromPredicate(MAX_INSTANT_DEFAULT, root, criteriaBuilder);
        return super.getQuery(toDateSpec.and(spec), domainClass, sort);
    }

    @Override
    @NonNull
    protected <S extends T> TypedQuery<Long> getCountQuery(final Specification<S> spec, @NonNull final Class<S> domainClass) {
        final Specification<S> toDateSpec = (root, query, criteriaBuilder) -> toAndFromPredicate(MAX_INSTANT_DEFAULT, root, criteriaBuilder);
        return super.getCountQuery(toDateSpec.and(spec), domainClass);
    }

    /******************************************************************************************************************
     *
     * **********************************  RevisionRepository
     *
     ******************************************************************************************************************/

    @Override
    @NonNull
    public Optional<Revision<Integer, T>> findLastChangeRevision(@NonNull final ID id) {
        val revisions = findRevisionsList(id);
        return Optional.ofNullable(revisions.size() > 0 ? revisions.get(revisions.size() - 1) : null);
    }

    @Override
    @NonNull
    public Revisions<Integer, T> findRevisions(@NonNull final ID id) {
        return Revisions.of(findRevisionsList(id));
    }

    @Override
    @NonNull
    public Page<Revision<Integer, T>> findRevisions(@NonNull final ID id, @NonNull final Pageable pageable) {
        throw new UnsupportedOperationException("Method not yet implemented");
    }

    @Override
    @NonNull
    public Optional<Revision<Integer, T>> findRevision(@NonNull final ID id, @NonNull final Integer revisionNumber) {
        val revisions = findRevisionsList(id);
        return Optional.ofNullable(revisionNumber > 0 && revisions.size() >= revisionNumber
                ? revisions.get(revisionNumber - 1)
                : null);
    }

    /******************************************************************************************************************
     *
     * ********************************** Predicates and Specifications
     *
     *******************************************************************************************************************/

    @NonNull
    protected Specification<T> inIdSpec(@NonNull final Iterable<ID> ids) {
        return (root, query, criteriaBuilder) -> inIdPredicate(ids, root, criteriaBuilder);
    }

    @NonNull
    protected Predicate inIdPredicate(@NonNull final Iterable<ID> ids, final Root<? extends T> root, final CriteriaBuilder criteriaBuilder) {
        CriteriaBuilder.In<Object> inClause = criteriaBuilder.in(root.get(annotatedEntitySupport.getUniqueKey()));
        ids.forEach(inClause::value);
        return inClause;
    }

    @NonNull
    protected Specification<T> toAndFromSpecification(final Instant asOfInstant) {
        return (root, query, criteriaBuilder) -> toAndFromPredicate(asOfInstant, root, criteriaBuilder);
    }

    @NonNull
    protected Predicate toAndFromPredicate(final Instant asOfInstant, final Root<? extends T> root, final CriteriaBuilder criteriaBuilder) {
        if (asOfInstant == null) {
            return criteriaBuilder.conjunction();
        }
        if (asOfInstant.equals(MAX_INSTANT_DEFAULT)) {
            return criteriaBuilder.equal(root.get(annotatedEntitySupport.getToDate()), MAX_INSTANT_DEFAULT);
        }
        return criteriaBuilder.and(
                criteriaBuilder.lessThanOrEqualTo(root.get(annotatedEntitySupport.getFromDate()), asOfInstant),
                criteriaBuilder.greaterThan(root.get(annotatedEntitySupport.getToDate()), asOfInstant)
        );
    }

    /******************************************************************************************************************
    *
    * ********************************** Other non-overridden methods
    *
    *******************************************************************************************************************/

    @SuppressWarnings("unchecked")
    protected ID getIdFromEntity(final T entity) {
        return (ID) entityAccessSupport.getAttribute(annotatedEntitySupport.getUniqueKey(), entity);
    }

    protected int deleteById(final ID id, final Instant currentTime) {
        if (id == null) {
            throw new JpaSystemException(new JpaTemporalException("ids for this class must be manually assigned before calling save/delete: " + this.getDomainClass().getName()));
        }
        return deleteByIds(Collections.singleton(id), currentTime);
    }

    protected int deleteByIds(final Set<ID> ids, final Instant currentTime) {
        val criteriaBuilder = em.getCriteriaBuilder();
        val criteriaUpdate = criteriaBuilder.createCriteriaUpdate(this.getDomainClass());
        val root = criteriaUpdate.from(this.getDomainClass());

        val predicates = new ArrayList<Predicate>();
        predicates.add(toAndFromPredicate(MAX_INSTANT_DEFAULT, root, criteriaBuilder));
        if (ids != null) predicates.add(inIdPredicate(ids, root, criteriaBuilder));

        criteriaUpdate.set(root.get(annotatedEntitySupport.getToDate()), currentTime)
                .where(predicates.toArray(new Predicate[0]));
        return em.createQuery(criteriaUpdate).executeUpdate();
    }

    protected List<Revision<Integer, T>> findRevisionsList(@NonNull final ID id) {
        List<T> allByIdAsOf = findAllById(Collections.singletonList(id), null);
        List<Revision<Integer, T>> metadataList = new ArrayList<>();
        for (int i = 0; i < allByIdAsOf.size(); i++) {
            T entity = allByIdAsOf.get(i);
            Instant timestamp = (Instant) entityAccessSupport.getAttribute(annotatedEntitySupport.getFromDate(), entity);
            RevisionMetadataImpl<T, Integer> metadata = new RevisionMetadataImpl<>(entity, i + 1, timestamp);
            metadataList.add(Revision.of(metadata, entity));
        }
        return metadataList;
    }

    private static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    private static Instant truncate(final Instant instant) {
        return instant.truncatedTo(TIMESTAMP_PRECISION_DEFAULT);
    }
}
