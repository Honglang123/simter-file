#  Simter File Server

## Requirement

- Maven 3.6+
- Kotlin 1.3+
- Java 8+
- Spring Framework 5.2+
- Spring Boot 2.2+
- Reactor 3.3+

## Supported Rest API

See [rest-api.md](../docs/rest-api.md).

## Maven Profiles

| SN  | Name                              | Default | Supported |
|-----|-----------------------------------|:-------:|:---------:|
| 1.1 | reactive-embedded-mongodb         |         |     √     |
| 1.2 | reactive-mongodb                  |         |     √     |
| 2.1 | jpa-hibernate-embedded-h2         |         |     √     |
| 2.2 | jpa-hibernate-embedded-postgres   |         |     √     |
| 2.3 | jpa-hibernate-postgres            |         |     √     |
| 2.4 | jpa-hibernate-embedded-mysql      |         |           |
| 2.5 | jpa-hibernate-mysql               |         |           |
| 3.1 | jpa-eclipselink-embedded-h2       |         |     √     |
| 3.2 | jpa-eclipselink-embedded-postgres |         |     √     |
| 3.3 | jpa-eclipselink-postgres          |         |     √     |
| 3.4 | jpa-eclipselink-embedded-mysql    |         |           |
| 3.5 | jpa-eclipselink-mysql             |         |           |
| 4.1 | r2dbc-embedded-h2                 |    √    |     √     |
| 4.2 | r2dbc-embedded-postgres           |         |     √     |
| 4.3 | r2dbc-postgres                    |         |     √     |
| 4.4 | r2dbc-embedded-mysql              |         |           |
| 4.5 | r2dbc-mysql                       |         |           |
| 4.6 | r2dbc-mssql                       |         |           |

Use `-P {profile-name}` to override the defaults. Such as:

```bash
mvn spring-boot:run -P r2dbc-embedded-postgres
```

## Maven Properties

| Property Name | Default Value | Remark
|---------------|---------------|--------
| port          | 8085          | Web server port
| db.host       | localhost     | Database host
| db.name       | testdb        | Database name
| db.username   | tester        | Database connect username
| db.password   | password      | Database connect password
| db.init-mode  | always        | Init database by `spring.datasource.schema/data` config. `never` or `always`

Use `-D {property-name}={property-value}` to override default value. Such as:

```bash
mvn spring-boot:run -D port=8085
```

## Build Package

```bash
mvn clean package -P {profile-name} -D {property-name}={property-value}
```

## Run Package

```bash
java -jar {package-name}.jar

# or
nohup java -jar {package-name}.jar > /dev/null &
```

## Run Integration Test

Run test in the real server. See <[../simter-file-test/README.md]>.


[Embedded MongoDB]: https://github.com/flapdoodle-oss/de.flapdoodle.embed.mongo#embedded-mongodb
[MongoDB]: https://www.mongodb.com
[HyperSQL]: http://hsqldb.org
[PostgreSQL]: https://www.postgresql.org
[Embedded PostgreSQL]: https://github.com/yandex-qatools/postgresql-embedded
[MySQL]: https://www.mysql.com
[Embedded MySQL]: https://github.com/wix/wix-embedded-mysql
[../simter-file-test/README.md]: https://github.com/simter/simter-file/blob/master/simter-file-test/README.md