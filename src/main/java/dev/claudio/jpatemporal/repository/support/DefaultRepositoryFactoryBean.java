package dev.claudio.jpatemporal.repository.support;

import dev.claudio.jpatemporal.repository.TemporalRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.lang.NonNull;

import javax.persistence.EntityManager;
import java.io.Serializable;

public class DefaultRepositoryFactoryBean<T extends JpaRepository<S, ID>, S, ID extends Serializable>
        extends JpaRepositoryFactoryBean<T, S, ID> {

    private final Class<? extends T> repositoryInterface;

    /**
     * Creates a new {@link JpaRepositoryFactoryBean} for the given repository interface.
     *
     * @param repositoryInterface must not be {@literal null}.
     */
    public DefaultRepositoryFactoryBean(final Class<? extends T> repositoryInterface) {
        super(repositoryInterface);
        this.repositoryInterface = repositoryInterface;
    }

    @Override
    @NonNull
    protected RepositoryFactorySupport createRepositoryFactory(@NonNull final EntityManager entityManager) {
        if (TemporalRepository.class.isAssignableFrom(repositoryInterface)) {
            return new DefaultRepositoryFactory(entityManager);
        } else {
            return super.createRepositoryFactory(entityManager);
        }
    }
}
