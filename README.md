# Broaddy

[![Run tests with maven](https://github.com/alessandrofrenna/broaddy/actions/workflows/maven-test.yml/badge.svg)](https://github.com/alessandrofrenna/broaddy/actions/workflows/maven-test.yml) [![Snyk Security](https://github.com/alessandrofrenna/broaddy/actions/workflows/snyk-security.yml/badge.svg)](https://github.com/alessandrofrenna/broaddy/actions/workflows/snyk-security.yml) [![](https://jitpack.io/v/alessandrofrenna/broaddy.svg)](https://jitpack.io/#alessandrofrenna/broaddy)

Broaddy is a lightweight library that allows the creation of small broadcast networks of objects.</br>
It is a simple implementation of the [Observer design pattern](https://en.wikipedia.org/wiki/Observer_pattern).

## What is a BroadcastNetwork?
A `BroadcastNetwork` is a centralized network of Java POJOs that uses the [Observer design pattern](https://en.wikipedia.org/wiki/Observer_pattern) to send messages to its members.
A member of the `BroadcastNetwork` is called a `NetworkPeer`.</br>
A `NetworkPeer` can subscribe to multiple `BroadcastNetwork`s.

## Installation
This library is hosted on [https://jitpack.io](https://jitpack.io/#alessandrofrenna/broaddy).</br>
The javadoc for this library is available here: [javadoc](https://javadoc.jitpack.io/com/github/alessandrofrenna/broaddy/1.0.2-SNAPSHOT/javadoc/com/github/alessandrofrenna/broaddy/package-summary.html)

### Maven

Enable the repository in your `pom.xml`:
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

Add this to your dependencies:
```xml
<dependency>
    <groupId>com.github.alessandrofrenna</groupId>
    <artifactId>broaddy</artifactId>
    <version>1.0.2-SNAPSHOT</version>
</dependency>
```

### Gradle

Add it in your root `settings.gradle` at the end of repositories:
```
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

Add this to your dependencies in `build.gradle`:
```
dependencies {
        implementation 'com.github.alessandrofrenna:broaddy:1.0.2-SNAPSHOT'
}
```


# Licensing
This project is licensed under the [Apache License v2.0](https://www.apache.org/licenses/LICENSE-2.0).