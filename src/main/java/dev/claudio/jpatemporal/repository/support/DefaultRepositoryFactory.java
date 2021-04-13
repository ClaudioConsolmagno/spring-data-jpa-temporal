package dev.claudio.jpatemporal.repository.support;

import dev.claudio.jpatemporal.repository.impl.TemporalRepositoryImpl;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.lang.NonNull;

import javax.persistence.EntityManager;
import java.util.Optional;

// https://dzone.com/articles/customizing-spring-data-jpa
public class DefaultRepositoryFactory extends JpaRepositoryFactory {

    /**
     * Creates a new {@link JpaRepositoryFactory}.
     *
     * @param entityManager must not be {@literal null}
     */
    public DefaultRepositoryFactory(final EntityManager entityManager) {
        super(entityManager);
    }

    @Override
    @NonNull
    protected Class<?> getRepositoryBaseClass(@NonNull RepositoryMetadata metadata) {
        return TemporalRepositoryImpl.class;
    }

    @Override
    @NonNull
    protected Optional<QueryLookupStrategy> getQueryLookupStrategy(final QueryLookupStrategy.Key key, @NonNull final QueryMethodEvaluationContextProvider evaluationContextProvider) {
        return super.getQueryLookupStrategy(QueryLookupStrategy.Key.USE_DECLARED_QUERY, evaluationContextProvider);
    }
}
