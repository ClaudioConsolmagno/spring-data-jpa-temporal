package dev.claudio.jpatemporal;

import dev.claudio.jpatemporal.annotation.EnableJpaTemporalRepositories;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
@EnableJpaTemporalRepositories
public class SpringDataJpaTemporalApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringDataJpaTemporalApplication.class, args);
    }
}
