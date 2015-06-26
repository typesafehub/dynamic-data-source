dynamic-data-source
===

[![Build Status](https://api.travis-ci.org/typesafehub/dynamic-data-source.png?branch=master)](https://travis-ci.org/typesafehub/dynamic-data-source)

This is a small library that provides an abstract class that may be subclassed for the purposes of dynamically resolving a database endpoint.

Dynamically resolving a database endpoint is important for [Reactive](http://www.reactivemanifesto.org/) systems as no resilient program should assume that a database is available to it at any time. Databases can come and go, networks fail etc.

The library may be used by any JVM based language that is using JDBC.

To use from your sbt build:

```scala
libraryDependencies += "com.typesafe.dynamicdatasource" %% "dynamic-data-source" % "1.0.0"
```
