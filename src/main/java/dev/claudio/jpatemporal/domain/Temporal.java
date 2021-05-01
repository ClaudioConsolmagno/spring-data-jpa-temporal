package dev.claudio.jpatemporal.domain;

import dev.claudio.jpatemporal.annotation.FromDate;
import dev.claudio.jpatemporal.annotation.TemporalId;
import dev.claudio.jpatemporal.annotation.ToDate;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import java.time.Instant;

/**
 * Abstract base class for temporal entities. This is a convenience base class and is entirely optional to be used.
 * <p>
 * When used, the extending class should NOT use this class' {@code hashCode()} and {@code equals()} methods and for
 * this reason they aren't implemented here.
 *
 * @author Claudio Consolmagno
 */
@Getter
@Setter
@ToString
@MappedSuperclass
@SuppressWarnings("checkstyle:MemberName")
public abstract class Temporal {

    protected Temporal() {
        // This constructor is intentionally empty. Nothing special is needed here.
    }

    @Id
    @TemporalId
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long temporal_id;

    @FromDate
    private Instant from_date;

    @ToDate
    private Instant to_date;
}
