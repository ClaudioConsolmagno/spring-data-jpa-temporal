package dev.claudio.jpatemporal.domain;

import dev.claudio.jpatemporal.annotation.UniqueKey;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "employee")
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = false)
public class Employee extends Temporal {
    @UniqueKey private Integer employee_id;
    private String name;
    private String job;
}
