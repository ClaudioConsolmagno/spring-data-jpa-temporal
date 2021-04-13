package dev.claudio.jpatemporal.repository.impl;

import dev.claudio.jpatemporal.repository.TemporalRepository;
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

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@NoRepositoryBean
public class TemporalRepositoryImpl<T, ID> extends SimpleJpaRepository<T, ID> implements TemporalRepository<T, ID> {

    private final AnnotatedAttributes<T> annotatedAttributes;
    private final JpaEntityInformation<T, ID> entityInformation;
    private final EntityManager em;

    public TemporalRepositoryImpl(final JpaEntityInformation<T, ID> entityInformation, final EntityManager em) {
        super(entityInformation, em);
        this.entityInformation = entityInformation;
        this.em = em;
        this.annotatedAttributes = new AnnotatedAttributes<>(this.getDomainClass());
    }

    /******************************************************************************************************************
     *
     * ********************************** TemporalRepository
     *
     ******************************************************************************************************************/

    @Override
    public Optional<T> findById(@NonNull final ID id, final Instant asOfInstant) {
        return findAllById(List.of(id), asOfInstant).stream().findFirst();
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
        return super.findOne((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(annotatedAttributes.getEntityId()), id));
    }

    @Override
    public boolean existsById(@NonNull final ID id) {
        return this.findById(id).isPresent();
    }

    @NonNull
    @Override
    public List<T> findAllById(@NonNull final Iterable<ID> ids) {
        return findAllById(ids, MAX_INSTANT);
    }

    @NonNull
    @Override
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
        var currentTime = Instant.now();
        deleteById(id, currentTime);
        annotatedAttributes.invokeSetter(annotatedAttributes.getFromDate(), entity, currentTime);
        annotatedAttributes.invokeSetter(annotatedAttributes.getToDate(), entity, MAX_INSTANT);
        annotatedAttributes.invokeSetter(annotatedAttributes.getTemporalId(), entity, null);
        return super.save(entity);
    }

    @NonNull
    @Override
    public void deleteById(@NonNull  ID id) {
        if(this.deleteById(id, Instant.now()) <= 0) {
            throw new EmptyResultDataAccessException(String.format("No %s entity with id %s exists!", entityInformation.getJavaType(), id), 1);
        }
    }

    @Override
    public void delete(@NonNull final T entity) {
        final ID id = this.getIdFromEntity(entity);
        this.deleteById(id, Instant.now());
    }

    @Override
    public void deleteAllInBatch() {
        this.deleteByIds(null, Instant.now());
    }

    @Override
    public void deleteInBatch(@NonNull final Iterable<T> entities) {
        Set<ID> idsToDelete = StreamSupport.stream(entities.spliterator(), false)
                .map(this::getIdFromEntity)
                .collect(Collectors.toSet());
        if (idsToDelete.isEmpty()) return;
        this.deleteByIds(idsToDelete, Instant.now());
    }

    @Override
    @NonNull
    protected <S extends T> TypedQuery<S> getQuery(Specification<S> spec, @NonNull final Class<S> domainClass, @NonNull final Sort sort) {
        final Specification<S> toDateSpec = (root, query, criteriaBuilder) -> toAndFromPredicate(MAX_INSTANT, root, criteriaBuilder);
        return super.getQuery(toDateSpec.and(spec), domainClass, sort);
    }

    @Override
    @NonNull
    protected <S extends T> TypedQuery<Long> getCountQuery(final Specification<S> spec, @NonNull final Class<S> domainClass) {
        final Specification<S> toDateSpec = (root, query, criteriaBuilder) -> toAndFromPredicate(MAX_INSTANT, root, criteriaBuilder);
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
        var revisions = findRevisionsList(id);
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
        var revisions = findRevisionsList(id);
        return Optional.ofNullable((revisionNumber > 0 && revisions.size() >= revisionNumber) ? revisions.get(revisionNumber-1) : null);
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
        CriteriaBuilder.In<Object> inClause = criteriaBuilder.in(root.get(annotatedAttributes.getEntityId()));
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
        if (asOfInstant.equals(MAX_INSTANT)) {
            return criteriaBuilder.equal(root.get(annotatedAttributes.getToDate()), MAX_INSTANT);
        }
        return criteriaBuilder.and(
                criteriaBuilder.lessThanOrEqualTo(root.get(annotatedAttributes.getFromDate()), asOfInstant),
                criteriaBuilder.greaterThan(root.get(annotatedAttributes.getToDate()), asOfInstant)
        );
    }

    /******************************************************************************************************************
    *
    * ********************************** Other non-overridden methods
    *
    *******************************************************************************************************************/

    @SuppressWarnings("unchecked")
    protected ID getIdFromEntity(final T entity) {
        return (ID) annotatedAttributes.invokeGetter(annotatedAttributes.getEntityId(), entity);
    }

    protected int deleteById(final ID id, final Instant currentTime) {
        if (id == null) {
            throw new JpaSystemException(new RuntimeException("ids for this class must be manually assigned before calling save/delete: " + this.getDomainClass().getName()));
        }
        return deleteByIds(Set.of(id), currentTime);
    }

    protected int deleteByIds(final Set<ID> ids, final Instant currentTime) {
        var criteriaBuilder = em.getCriteriaBuilder();
        var criteriaUpdate = criteriaBuilder.createCriteriaUpdate(this.getDomainClass());
        var root = criteriaUpdate.from(this.getDomainClass());

        var predicates = new ArrayList<Predicate>();
        predicates.add(toAndFromPredicate(MAX_INSTANT, root, criteriaBuilder));
        if (ids != null) predicates.add(inIdPredicate(ids, root, criteriaBuilder));

        criteriaUpdate.set(root.get(annotatedAttributes.getToDate()), currentTime)
                .where(predicates.toArray(new Predicate[0]));
        return em.createQuery(criteriaUpdate).executeUpdate();
    }

    protected List<Revision<Integer, T>> findRevisionsList(@NonNull final ID id) {
        List<T> allByIdAsOf = findAllById(List.of(id), null);
        List<Revision<Integer, T>> metadataList = new ArrayList<>();
        for (int i = 0; i < allByIdAsOf.size(); i++) {
            T entity = allByIdAsOf.get(i);
            Instant timestamp = (Instant) annotatedAttributes.invokeGetter(annotatedAttributes.getFromDate(), entity);
            RevisionMetadataImpl<T, Integer> metadata = new RevisionMetadataImpl<>(entity, i + 1, timestamp);
            metadataList.add(Revision.of(metadata, entity));
        }
        return metadataList;
    }
}
