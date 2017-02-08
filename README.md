# graphql-java

[![Join the chat at https://gitter.im/graphql-java/graphql-java](https://badges.gitter.im/graphql-java/graphql-java.svg)](https://gitter.im/graphql-java/graphql-java?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

##### Friendly warning: As GraphQL itself is currently a Working Draft, expect changes.



This is an asynchronous implementation of the [GraphQL Java library](https://github.com/graphql-java/graphql-java),
which in turn is based on the [facebook specification](https://github.com/facebook/graphql)
and the JavaScript [reference implementation](https://github.com/graphql/graphql-js).

**Status**: Version `1.0.0` to be released.

The versioning follows [Semantic Versioning](http://semver.org) since `1.0.0`.

**Hint**: This README documents the latest release, but `master` contains the current development version. So please make sure
to checkout the appropriate tag when looking for the version documented here.

[![Build Status](https://travis-ci.org/graphql-java/graphql-java-async.svg?branch=master)](https://travis-ci.org/graphql-java/graphql-java-async)
[![Latest Release](https://maven-badges.herokuapp.com/maven-central/com.graphql-java/graphql-java-async/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.graphql-java/graphql-java-async/)
[![Latest Dev Build](https://api.bintray.com/packages/andimarek/graphql-java/graphql-java-async/images/download.svg)](https://bintray.com/andimarek/graphql-java/graphql-java-async/_latestVersion)


# Table of Contents

- [Overview](#overview)
- [Code of Conduct](#code-of-conduct)
- [Discussion](#discussion)
- [Hello World](#hello-world)
- [Getting started](#getting-started)
- [Manual](#manual)
    - [Data fetching](#data-fetching)
    - [Executing](#executing)
    - [Execution strategies](#execution-strategies)
    - [Logging](#logging)
- [Contributions](#contributions)
- [Build it](#build-it)
- [Development Build](#development-build)
- [Javadocs](#javadocs)
- [Details](#details)
- [Acknowledgment](#acknowledgment)
- [Related Projects](#related-projects)
- [License](#license)


### Overview

This is an asynchronous implementation of [GraphQL Java](https://github.com/graphql-java/graphql-java). This library
allows you execute a GraphQL request in a non-blocking manner.

Further, it lets the fetcher of the data to return it's result wrapped inside of a [CompletableFuture](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html),
hence giving it the ability to be non-blocking as well.

### Code of Conduct

Please note that this project is released with a [Contributor Code of Conduct](CODE_OF_CONDUCT.md).
By contributing to this project (commenting or opening PR/Issues etc) you are agreeing to follow this conduct, so please
take the time to read it.


### Discussion

If you have a question or want to discuss anything else related to this project:

- There is a mailing list (Google Group) for graphql-java: [graphql-java group](https://groups.google.com/forum/#!forum/graphql-java)
- And a chat room (Gitter.im) for graphql-java: [graphql-java chat](https://gitter.im/graphql-java/graphql-java)

### Hello World

This is the famous "hello world" in graphql-java:

```java
import graphql.async.GraphQL;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import java.util.Map;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

public class HelloWorld {

    public static void main(String[] args) {
        GraphQLObjectType queryType = newObject()
                .name("helloWorldQuery")
                .field(newFieldDefinition()
                        .type(GraphQLString)
                        .name("hello")
                        .staticValue("world"))
                .build();

        GraphQLSchema schema = GraphQLSchema.newSchema()
                .query(queryType)
                .build();

        GraphQL graphQL = GraphQL.newAsyncGraphQL(schema).build();

        graphQL.execute("{hello}", (result) -> {
          System.out.println(result.getData());
          // Prints: {hello=world}
        });
    }
}
```

### Getting started

##### How to use the latest release with Gradle

Make sure `mavenCentral` is among your repos:

```groovy
repositories {
    mavenCentral()
}

```
Dependency:

```groovy
dependencies {
  compile 'com.graphql-java:graphql-java-async:1.0.0'
}

```

##### How to use the latest release with Maven

Dependency:

```xml
<dependency>
    <groupId>com.graphql-java</groupId>
    <artifactId>graphql-java-async</artifactId>
    <version>1.0.0</version>
</dependency>

```

### Manual

#### Data fetching

The actual data that comes from `DataFetcher` objects, may be optionally wrapped inside a Java 1.8 CompletableFuture.


Example of configuring a custom `DataFetcher`, that returns a CompletableFuture:
```java

DataFetcher<Foo> fooDataFetcher = new DataFetcher<Foo>() {
    @Override
    public Foo get(DataFetchingEnvironment environment) {
        // environment.getSource() is the value of the surrounding
        // object. In this case described by objectType
        Foo value = perhapsFromDatabase(); // Perhaps getting from a DB or whatever
        return CompletableFuture.completedFuture(value);
    }
};

GraphQLObjectType objectType = newObject()
        .name("ObjectType")
        .field(newFieldDefinition()
                .name("foo")
                .type(GraphQLString)
                .dataFetcher(fooDataFetcher))
        .build();

```

#### Executing

To execute a Query/Mutation against a Schema build a new `GraphQL` Object with the appropriate arguments and then call `execute(request)`.

The result of a Query is a `ExecutionResult` Object with the result and/or a list of Errors.

Example: [GraphQL Test](src/test/groovy/graphql/GraphQLTest.groovy)

And, to execute the same asynchronously, call `execute(request, (result) -> handle(result))`.

Again, the result of the Query passed to the callback is a `ExecutionResult` Object with the result and/or a list of Errors.

Example: [GraphQL Test](src/test/groovy/graphql/execution/AsyncExecutionStrategyTest.groovy)


#### Execution strategies

In order to query the graphql.async.GraphQL asynchronously, it has to be configured to use the AsyncExecutionStrategy.

You can however provide your own execution strategies, one to use while querying data and one
to use when mutating data, as long as they extend the AsyncExecutionStrategy.

When provided fields will be executed parallel, except the first level of a mutation operation.

See [specification](http://facebook.github.io/graphql/#sec-Normal-evaluation) for details.


#### Logging

Logging is done with [SLF4J](http://www.slf4j.org/). Please have a look at the [Manual](http://www.slf4j.org/manual.html) for details.
The `grapqhl-java` root Logger name is `graphql`.


#### Contributions

Every contribution to make this project better is welcome: Thank you!

In order to make this a pleasant as possible for everybody involved, here are some tips:

- Respect the [Code of Conduct](#code-of-conduct)
- Before opening an Issue to report a bug, please try the latest development version. It can happen that the problem is already solved.
- Please use  Markdown to format your comments properly. If you are not familiar with that: [Getting started with writing and formatting on GitHub](https://help.github.com/articles/getting-started-with-writing-and-formatting-on-github/)
- For Pull Requests:
  - Here are some [general tips](https://github.com/blog/1943-how-to-write-the-perfect-pull-request)
  - Please be a as focused and clear as possible  and don't mix concerns. This includes refactorings mixed with bug-fixes/features, see [Open Source Contribution Etiquette](http://tirania.org/blog/archive/2010/Dec-31.html)
  - It would be good to add a automatic test. All tests are written in [Spock](http://spockframework.github.io/spock/docs/1.0/index.html).


#### Development Build

The latest development build is available on Bintray.

Please look at [Latest Build](https://bintray.com/andimarek/graphql-java/graphql-java-async/_latestVersion) for the
latest version value.

#### Javadocs

See the [project page](http://graphql-java.github.io/graphql-java-async/) for the javadocs associated with each release.


#### How to use the latest build with Gradle

Add the repositories:

```groovy
repositories {
    mavenCentral()
    maven { url  "http://dl.bintray.com/karthicks/graphql-java-async" }
}

```

Dependency:

```groovy
dependencies {
  compile 'com.graphql-java:graphql-java-async:INSERT_LATEST_VERSION_HERE'
}

```


#### How to use the latest build with Maven

Add the repository:

```xml
<repository>
    <snapshots>
        <enabled>false</enabled>
    </snapshots>
    <id>bintray-karthicks-graphql-java-async</id>
    <name>bintray</name>
    <url>http://dl.bintray.com/andimarek/graphql-java-async</url>
</repository>

```

Dependency:

```xml
<dependency>
    <groupId>com.graphql-java</groupId>
    <artifactId>graphql-java-async</artifactId>
    <version>INSERT_LATEST_VERSION_HERE</version>
</dependency>

```







### Build it

Just clone the repo and type

```sh
./gradlew build
```

In `build/libs` you will find the jar file.

Running the tests:

```sh
./gradlew test
```

Installing in the local Maven repository:

```sh
./gradlew install
```


### Details

The implementation is in Java 8, but the tests are in Groovy and [Spock](https://github.com/spockframework/spock).

The only runtime dependencies are GraphQL Java and Slf4J.

### Acknowledgment

This implementation is based on the [java implementation](https://github.com/graphql-java/graphql-java).

### Related projects
* [graphl-java](https://github.com/graphql-java/graphql-java): The java implemenation of the graphql specification


### License

graphql-java-async is licensed under the MIT License. See [LICENSE](LICENSE.md) for details.

Copyright (c) 2016, Karthick Sankarachary and [Contributors](https://github.com/graphql-java/graphql-java-async/graphs/contributors)

[graphql-js License](https://github.com/graphql/graphql-js/blob/master/LICENSE)

