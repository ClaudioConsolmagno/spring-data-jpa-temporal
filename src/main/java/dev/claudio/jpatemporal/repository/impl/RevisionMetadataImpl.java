package dev.claudio.jpatemporal.repository.impl;

import lombok.Getter;
import org.springframework.data.history.RevisionMetadata;
import org.springframework.lang.NonNull;

import java.time.Instant;
import java.util.Optional;

public class RevisionMetadataImpl<T, N extends Number & Comparable<N>> implements RevisionMetadata<N> {
    @Getter private final T entity;
    private final N revisionNumber;
    private final Instant timestamp;

    public RevisionMetadataImpl(final T entity, final N revisionNumber, final Instant timestamp) {
        this.entity = entity;
        this.revisionNumber = revisionNumber;
        this.timestamp = timestamp;
    }

    @NonNull
    @Override
    public Optional<N> getRevisionNumber() {
        return Optional.of(revisionNumber);
    }

    @NonNull
    @Override
    public Optional<Instant> getRevisionInstant() {
        return Optional.of(timestamp);
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <S> S getDelegate() {
        return (S) entity;
    }
}
