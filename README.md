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
$ java -jar target/mvnquery.jar --help
MvnQuery version 1.0-SNAPSHOT
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
      Default: ${user.home}/.mvnquery
    --config-repo
      Set repository URL
      Default: https://repo1.maven.org/maven2
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
    --timestamp-format
      User defined format to print the lastModifiedTime ('iso',
      'yyyyMMddHHmmssSSS', etc.)
    --use-timestamp, -t
      Print also the lastModifiedTime
      Default: false
    --version, -v
      Print version
      Default: false
```

Run the default query (query `jar` artifacts, changed in the last 14 days, with empty classifier).

```bash
$ java --enable-native-access=ALL-UNNAMED -jar mvnquery.jar

Initiating indexing context for https://repo1.maven.org/maven2
        - repository index data location: /home/user/.mvnquery/nT1zvdLBhX
Updating Index ...
        This might take a while on first run, so please be patient!
        No update needed, index is up to date!
        Finished in 0 sec

Building the query
        +p:jar -l:* +m2:[1740592566516 TO 9223372036854775807]
Querying index
------
org.eclipse.pass:pass-notification-service:1.15.0:jar:
org.eclipse.pass:pass-journal-loader-nih:1.15.0:jar:
org.eclipse.pass:pass-grant-loader:1.15.0:jar:
org.eclipse.pass:pass-data-client:1.15.0:jar:
org.eclipse.pass:pass-core-usertoken:1.15.0:jar:
org.eclipse.pass:pass-core-user-service:1.15.0:jar:
org.eclipse.pass:pass-core-test-config:1.15.0:jar:
org.eclipse.pass:pass-core-policy-service:1.15.0:jar:
org.eclipse.pass:pass-core-object-service:1.15.0:jar:
org.eclipse.pass:pass-core-metadataschema-service:1.15.0:jar:
...[cut]...
------
Total response size: 48806
Artifacts listed: 48806
Query took 0 seconds
```

Run query for specified artifact without limiting the modification timestamp. Don't print the debug output.

```bash
$ java --enable-native-access=ALL-UNNAMED -jar mvnquery.jar --groupId com.hazelcast --artifactId hazelcast --lastDays 0 --quiet 
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

# Use all the packaging and all the classifiers
java --enable-native-access=ALL-UNNAMED -jar mvnquery.jar --packaging - --classifier -

# Change index directory location
java --enable-native-access=ALL-UNNAMED -jar mvnquery.jar --config-data-dir /opt/mvnquery

# Query index from another repository
java --enable-native-access=ALL-UNNAMED -jar mvnquery.jar --config-repo https://repo.jenkins-ci.org/artifactory/releases
```