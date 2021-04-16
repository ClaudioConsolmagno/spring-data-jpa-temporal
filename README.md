# Spring Data JPA Temporal Audit

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

Take a look at the sample springboot application at [src/test/java](src/test/java) directory. It shows the simplest usage of this extension. He's a step-by-step summary of what you need:

### build.gradle / pom.xml
1. Look at the latest release version in github and add a dependency to your build file:

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

### Spring main class (e.g. [SpringDataJpaTemporalApplication.java](src/test/java/dev/claudio/jpatemporal/SpringDataJpaTemporalApplication.java))

2. Use `@EnableJpaTemporalRepositories` (see `SpringDataJpaTemporalApplication.java`).
   This makes this extension works, and that it _only_ works on repositories that extend `TemporalRepository.java` (step 6 below).
   Alternatively, you can use `@EnableJpaRepositories(repositoryFactoryBeanClass = DefaultRepositoryFactoryBean.class)` if you need to configure something else in the JPA annotation.

### Entity (e.g. [Employee.java](src/test/java/dev/claudio/jpatemporal/domain/Employee.java))

3. From your domain class (e.g. `Employee.java`), extend `Temporal.java`. Alternatively, use annotations `@TemporalId`, `@FromDate` and `@ToDate` in fields of your class.
   `@TemporalId` must be your primary key so it needs to have `@Id` and `@GeneratedValue` on the same field.
4. Use `@EntityId` on your unique key attribute in your entity (e.g. `employee_id`).
5. If you are using Lombok and are extending `Temporal.java` mark your entity with `@EqualsAndHashCode(callSuper = false)`.
   If not, make sure your equals and hashcode implementations don't use any of the values marked with `@TemporalId`, `@FromDate` and `@ToDate`.

### Repository (e.g. [Repository.java](src/test/java/dev/claudio/jpatemporal/repository/Repository.java))

6. Create a repository interface that extends `TemporalRepository<T,ID>`. `T` is your entity and `ID` is the type of your unique key (marked with `@EntityId`).
   Example `extends TemporalRepository<Employee, Integer>`

### Database schema (e.g. [db.sql](src/test/resources/db.sql))

7. For better query performance create a unique index on your `@EntityId` and `@ToDate` columns. 

# Alternatives

I'm not aware of any other "temporal" JPA implementations although there are plenty of regular auditing libraries, Javers being my favourite of those:

- [Javers](https://github.com/javers/javers)
- [Envers](https://github.com/spring-projects/spring-data-envers) 
- A custom implementation with `@PrePersist`, `@PreUpdate`, etc.
- Triggers on the database (please don't...)
- A native temporal implementation to your database engine (e.g. [mariadb](https://mariadb.com/docs/appdev/temporal-tables/#application-time-period-tables)).
  This is a nice auditing solution, it's basically what this project does but at the DB level.
  This means it's not portable, you'll need to configure things manually and create manual finder methods to look at audit.

# Limitations

The following 2 functionalities aren't currently supported with this library. An exception may be thrown at spring boot start-up if you try to use them. I'll try and work on those in the future. 

- Does not support [derived query methods](https://www.baeldung.com/spring-data-derived-queries), e.g. `findByNameAndAddress`, `countByNameAndAddress`, etc. However, you could create methods and use @Query annotation for specify the query.
- Does not support relations, e.g. `@OneToOne`, `@OneToMany`, etc.

# Next Steps

- [ ] Document methods for javadoc
- [ ] Remove dependency on `org.springframework.util.ReflectionUtils` (comment on class says it's for internal use only)
  - [ ] Maybe use AnnotationDetectionMethodCallback and AnnotationDetectionFieldCallback
- [ ] Publish to Maven
  - [ ] https://docs.gradle.org/current/userguide/building_java_projects.html#sec:building_java_libraries
  - [ ] 8 Only? Multi version (8 - 11 - 16) jar?
  - [ ] Switch build to pom.xml?
- [ ] Extra unit tests on:
  - [ ] Annotated fields and methods (i.e. entity doesn't extend `Temporal.java`)
  - [ ] Fields without getters and setters
  - [ ] Test @Query annotation on repository methods.
  - [ ] Test on AnnotatedAttributes.java
  - [x] Test DB Associations - they don't work
- [ ] Work on limitations (section above)
  - Maybe listeners can help solve relations limitation (e.g. see EnversPreUpdateEventListenerImpl)
- [ ] Implement `findRevisions(ID, Pageable)`
- [ ] Mongodb support (separate library, same concept)

# License

Spring Data JPA Temporal Audit is Open Source software released under the [Apache 2.0 license](https://www.apache.org/licenses/LICENSE-2.0.html).
