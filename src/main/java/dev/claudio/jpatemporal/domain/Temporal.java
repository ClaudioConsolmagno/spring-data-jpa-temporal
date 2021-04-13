package dev.claudio.jpatemporal.domain;

import dev.claudio.jpatemporal.annotation.FromDate;
import dev.claudio.jpatemporal.annotation.TemporalId;
import dev.claudio.jpatemporal.annotation.ToDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import java.io.Serializable;
import java.time.Instant;

@Data
@MappedSuperclass
public abstract class Temporal implements Serializable {

    @Id
    @TemporalId
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Exclude
    private Long temporal_id;

    @FromDate
    private Instant from_date;

    @ToDate
    private Instant to_date;
}
