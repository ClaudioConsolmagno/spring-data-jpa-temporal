# Spring Data JPA Temporal Audit <div style="float:right"> [![Maven Central](https://img.shields.io/maven-central/v/dev.claudio/spring-data-jpa-temporal.svg?label=Maven%20Central&color=success)](https://search.maven.org/search?q=g:%22dev.claudio%22%20AND%20a:%22spring-data-jpa-temporal%22) </div>


Spring Data JPA Temporal Audit is an extension of [spring-data-jpa](https://github.com/spring-projects/spring-data-jpa) that makes it simple to keep an audit of your data in the same table as your main data itself.

It does that by implementing [temporal database](https://en.wikipedia.org/wiki/Temporal_database) functionality completely on the application side, so it works with any DB engines that JPA integrates with using minimal configuration.

More specifically, your table becomes a "system-version table". The following excerpt is from [mariadb](https://mariadb.com/docs/appdev/temporal-tables/#application-time-period-tables):

> System-Versioned Tables
>
> Normally, when you issue a statement that updates a row on a table, the new values replace the old values on the row so that only the most current data remains available to the application.
>
> With system-versioned tables, [the db server] tracks the points in time when rows change. When you update a row on these tables, it creates a new row to display as current without removing the old data. This tracking remains transparent to the application.
> When querying a system-versioned table, you can retrieve either the most current values for every row or the historic values available at a given point in time.
>
> You may find this feature useful in efficiently tracking the time of changes to continuously-monitored values that do not change frequently, such as changes in temperature over the course of a year. System versioning is often useful for auditing.

# Who is this for?

- You want a simple and lightweight auditing mechanism for your tables, i.e. no other tables are created, no triggers, minimal springboot configuration.
- You have a relatively simple model and can live with the limitations of this library described in the [Limitations](#Limitations) section below.

# Usage

Look at the latest release `${version}` in github and add a dependency to your build file:

Maven:
```xml
<dependency>
  <groupId>dev.claudio</groupId>
  <artifactId>spring-data-jpa-temporal</artifactId>
  <version>${version}</version>
</dependency>
```

Gradle:
```groovy
implementation 'dev.claudio:spring-data-jpa-temporal:${version}'
```

As a quick-start, take a look at the sample springboot application in the [src/test/java](src/test/java) directory. It shows the simplest usage of this extension. For extra information and alternative usage here's a step-by-step summary of what you need:

### Spring main class (e.g. [SpringDataJpaTemporalApplication.java](src/test/java/dev/claudio/jpatemporal/SpringDataJpaTemporalApplication.java))

Use `@EnableJpaTemporalRepositories` (see `SpringDataJpaTemporalApplication.java`).
This makes this extension work, and that it _only_ works on repositories that extend `TemporalRepository.java` (see below).
Alternatively, you can use `@EnableJpaRepositories(repositoryFactoryBeanClass = DefaultRepositoryFactoryBean.class)` if you need to configure something else for your regular JPA repositories.

### Entity (e.g. [Employee.java](src/test/java/dev/claudio/jpatemporal/domain/Employee.java))

From your domain class (e.g. `Employee.java`), extend `Temporal.java`. Alternatively, use annotations `@TemporalId`, `@FromDate` and `@ToDate` in fields of your class.
   `@TemporalId` must be your primary key so it needs to have `@Id` and `@GeneratedValue` on the same field.

Use `@UniqueKey` on your unique key attribute in your entity (e.g. `employee_id`).

If you are using Lombok and are extending `Temporal.java` mark your entity with `@EqualsAndHashCode(callSuper = false)`.
   If not, make sure your equals and hashcode implementations don't use any of the values marked with `@TemporalId`, `@FromDate` and `@ToDate`.

### Repository (e.g. [Repository.java](src/test/java/dev/claudio/jpatemporal/repository/Repository.java))

Create a repository interface that extends `TemporalRepository<T,ID>`. `T` is your entity and `ID` is the type of your unique key (marked with `@UniqueKey`).
   Example `extends TemporalRepository<Employee, Integer>`

### Database schema (e.g. [db.sql](src/test/resources/db.sql))

For better query performance create a unique index on your `@UniqueKey` and `@ToDate` columns. E.g. `create unique index employee_id_to_date_index on employee (employee_id, to_date);`

# Alternatives

I'm not aware of any other "temporal" JPA implementations although there are plenty of regular auditing libraries, Javers being my favourite of those:

- [Javers](https://github.com/javers/javers)
- [Envers](https://github.com/spring-projects/spring-data-envers) 
- A custom implementation with `@PrePersist`, `@PreUpdate`, etc.
- Triggers on the database (please don't...)
- A native temporal implementation to your database engine (e.g. [mariadb](https://mariadb.com/docs/appdev/temporal-tables/#application-time-period-tables)).
  This is a nice auditing solution, it's basically what this project does but at the DB level.
  This means it's not portable, you'll need to configure things manually and create manual audit finder methods.

# Limitations

The following 2 functionalities aren't currently supported with this library. An exception may be thrown at spring boot start-up if you try to use them. I'll try and work on those in the future. 

- Does not support [derived query methods](https://www.baeldung.com/spring-data-derived-queries), e.g. `findByNameAndAddress`, `countByNameAndAddress`, etc. However, you could create methods and use @Query annotation to specify a query to run.
- Does not support relations, e.g. `@OneToOne`, `@OneToMany`, etc.

# Next Steps

- [ ] Extra unit tests on:
  - [ ] Annotated fields and methods (i.e. entity doesn't extend `Temporal.java`)
  - [ ] Fields without getters and setters
  - [ ] Test @Query annotation on repository methods.
  - [ ] Tests for AnnotatedEntitySupport.java
  - [ ] Tests for EntityAccessSupport.java
- [ ] Java 9 modules?
- [ ] Add debug logging
- [ ] Work on limitations (section above)
  - Maybe listeners can help solve relations limitation (e.g. see EnversPreUpdateEventListenerImpl)
- [ ] Implement `findRevisions(ID, Pageable)`
- [ ] Mongodb support (separate library, same concept)

# License

Spring Data JPA Temporal Audit is Open Source software released under the [Apache 2.0 license](https://www.apache.org/licenses/LICENSE-2.0.html).
