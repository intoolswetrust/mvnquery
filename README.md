# MvnQuery

MvnQuery fetches a Maven repository index and queries it.
By default it uses Maven Central repository index and the index data are stored to `${HOME}/.mvnquery`

The artifacts returned by the query are listed by default in format `groupId:artifactId:version:packaging:classifier`.
If you want to see the `lastModified` timestamp in the result use `-t` program parameter. Then the output format is
`groupId:artifactId:version:packaging:classifier:lastModifiedTimestamp`

The MvnQuery is based on [apache/maven-indexer](https://github.com/apache/maven-indexer/) project.

## Quickstart

Print help.

```bash
$ java -jar ~/tools/mvnquery.jar --help
MvnQuery version 0.3.0
MvnQuery retrieves Maven repository index and makes query on it.

Usage:
java --enable-native-access=ALL-UNNAMED -jar mvnquery.jar [options]

  Options:
    --artifactId, -a
      Filter by artifactId
    --classifier, -c
      Filter by classifier
      Default: -
    --config-data-dir
      Set data directory for index
      Default: /home/kwart/.mvnquery
    --config-repo
      Set repository URL
      Default: https://repo1.maven.org/maven2
    --force-update
      Force index update even if interval hasn't passed
      Default: false
    --groupId, -g
      Filter by groupId
    --help, -h
      Prints this help
    --lastDays, -d
      Filter artifacts modified in last X days
      Default: 14
    --packaging, -p
      Filter by packaging type
      Default: jar
    --quiet, -q
      Don't print progress
      Default: false
    --skip-update
      Skip index update even if interval has passed
      Default: false
    --timestamp-format
      User defined format to print the lastModifiedTime ('iso', 
      'yyyyMMddHHmmssSSS', etc.)
    --use-timestamp, -t
      Include the lastModified field in query results
      Default: false
    --version, -v
      Print version
      Default: false
```

Run the default query (query `jar` artifacts, changed in the last 14 days, with all classifiers).

```bash
$ java --enable-native-access=ALL-UNNAMED -jar mvnquery.jar

Use --quiet (-q) argument to supress the debug output. Use --help (-h) to print the help.

Initiating indexing context for https://repo1.maven.org/maven2
        - repository index data location: /home/kwart/.mvnquery/nT1zvdLBhX
Skipping index update (not needed or explicitly suppressed)
Building the query
        +p:jar +m2:[1748346028323 TO 9223372036854775807]
Querying index
------
com.kylecorry.andromeda:core:14.3.0:jar:sources
com.kylecorry.andromeda:core:14.3.0:jar:javadoc
com.kylecorry.andromeda:connection:14.3.0:jar:sources
com.kylecorry.andromeda:connection:14.3.0:jar:javadoc
com.kylecorry.andromeda:compression:14.3.0:jar:sources
com.kylecorry.andromeda:compression:14.3.0:jar:javadoc
com.kylecorry.andromeda:clipboard:14.3.0:jar:sources
com.kylecorry.andromeda:clipboard:14.3.0:jar:javadoc
com.kylecorry.andromeda:canvas:14.3.0:jar:sources
...[cut]...
------
Total response size: 123669
Artifacts listed: 123669
Query took 1 seconds
```

Run query for specified artifact without limiting the modification timestamp and querying the empty classifier only. Don't print the debug output.

```bash
$ java --enable-native-access=ALL-UNNAMED -jar mvnquery.jar --groupId com.hazelcast --artifactId hazelcast --classifier '' --lastDays 0 --quiet 
com.hazelcast:hazelcast:3.4.7:jar:
com.hazelcast:hazelcast:3.7:jar:
com.hazelcast:hazelcast:3.7-EA:jar:
com.hazelcast:hazelcast:3.6.4:jar:
com.hazelcast:hazelcast:3.6.3:jar:
...[cut]...

```

Other.

```bash
# Use wildcards
java --enable-native-access=ALL-UNNAMED -jar mvnquery.jar --artifactId '*hazelcast*' --lastDays 90

# Use all the packaging and with the "sources" classifiers
java --enable-native-access=ALL-UNNAMED -jar mvnquery.jar --packaging - --classifier sources

# Change index directory location
java --enable-native-access=ALL-UNNAMED -jar mvnquery.jar --config-data-dir /opt/mvnquery

# Query index from another repository
java --enable-native-access=ALL-UNNAMED -jar mvnquery.jar --config-repo https://repo.jenkins-ci.org/artifactory/releases
```
