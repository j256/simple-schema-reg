Java Simple Schema Registry
===========================

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.j256.simple-schema-reg/simple-schema-reg/badge.svg?style=flat-square)](https://mvnrepository.com/artifact/com.j256.simple-schema-reg/simple-schema-reg/latest)
[![javadoc](https://javadoc.io/badge2/com.j256.simple-schema-reg/simple-schema-reg/javadoc.svg)](https://javadoc.io/doc/com.j256.simple-schema-reg/simple-schema-reg)
[![ChangeLog](https://img.shields.io/github/v/release/j256/simple-schema-reg?label=changelog&display_name=release)](https://github.com/j256/simple-schema-reg/blob/master/src/main/javadoc/doc-files/changelog.md)
[![CodeCov](https://img.shields.io/codecov/c/github/j256/simple-schema-reg.svg)](https://codecov.io/github/j256/simple-schema-reg/)
[![CircleCI](https://circleci.com/gh/j256/simple-schema-reg.svg?style=shield)](https://circleci.com/gh/j256/simple-schema-reg)
[![GitHub License](https://img.shields.io/github/license/j256/simple-schema-reg)](https://github.com/j256/simple-schema-reg/blob/master/LICENSE.txt)

The goal of this library is to be a small schema registry.  I looked around for a couple of days and
couldn't find one that satisfied my quick startup requirements.

* The source code be found on the [git repository](https://github.com/j256/simple-schema-reg)
* Maven packages are published via [Maven Central](https://mvnrepository.com/artifact/com.j256.simple-schema-reg/simple-schema-reg/latest)
* [Javadoc documentation](https://javadoc.io/doc/com.j256.simple-schema-reg/simple-schema-reg)

Enjoy.  Gray Watson

# Getting Started

The jar gets deployed in its standard form as well as a `-shaded.jar` version which is suitable for running as `java -jar`  with the Main class in `com.j256.simpleschemareg.Main`.  This will create a Jetty http/https standalone webserver.  You can also call the handler directly.

The main class handles the following command line arguments:

```
Usage: java -jar simple-schema-reg.jar [-p port | -P port] [-b bind-host] [-r dir] [-s] [-v]
       -b bind-host  name of host to bind to, if not specified then all
       -p http-port  number of the http port to bind to
       -P ssl-port   number of the SSL port to bind to
       -r root-dir   root direcctory where the schema files are stored
       -s            enable the /shutdown GET command
       -v            verbose messages to stdout
```

Either one or both of `-p` and `-P` must be specified.  By default it will save things into the current directory unless `-r root-dir` is specified.

# Maven Configuration

Maven packages are published via [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.j256.simple-schema-reg/simple-schema-reg/badge.svg?style=flat-square)](https://mvnrepository.com/artifact/com.j256.simple-schema-reg/simple-schema-reg/latest)

``` xml
<dependency>
	<groupId>com.j256.simple-schema-reg</groupId>
	<artifactId>simple-schema-reg</artifactId>
	<version>0.4</version>
</dependency>
```

# Dependencies

Simple-Schema-Reg has a direct dependency on Jetty (9.4.56.v20240826 right now) for its web-server and Gson (2.8.5) for it's JSON processing.

# ChangeLog Release Notes

See the [![ChangeLog](https://img.shields.io/github/v/release/j256/simple-schema-reg?label=changelog)](https://github.com/j256/simple-schema-reg/blob/master/src/main/javadoc/doc-files/changelog.md)
