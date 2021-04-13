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

    public RevisionMetadataImpl(T entity, N revisionNumber, Instant timestamp) {
        this.entity = entity;
        this.revisionNumber = revisionNumber;
        this.timestamp = timestamp;
    }

    @Override
    @NonNull
    public Optional<N> getRevisionNumber() {
        return Optional.of(revisionNumber);
    }

    @Override
    @NonNull
    public Optional<Instant> getRevisionInstant() {
        return Optional.of(timestamp);
    }

    @Override
    @NonNull
    @SuppressWarnings("unchecked")
    public <S> S getDelegate() {
        return (S) entity;
    }
}
