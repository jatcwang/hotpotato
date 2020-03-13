# Hotpotato

[![Release](https://img.shields.io/nexus/r/com.github.jatcwang/hotpotato-core_2.13?server=https%3A%2F%2Foss.sonatype.org)](https://oss.sonatype.org/content/repositories/releases/com/github/jatcwang/hotpotato-core_2.13/)
[![(https://jatcwang.github.io/hotpotato/)](https://badges.gitter.im/gitterHQ/gitter.png)](https://gitter.im/jatcwang/hotpotato)

A type-safe and flexible error handling library for Scala, based on Shapeless Coproducts.

Head over to the [Microsite](https://jatcwang.github.io/hotpotato/) for documentation and examples!

# Quick Installation

```
libraryDependencies += "com.github.jatcwang" %% "hotpotato-core" % LATEST_VERSION
```

# Warning about using this in production codebases

This project was developed to explore what a correct, type-safe and ergonomic error handling would look like in Scala.
Due to its heavy use of shapeless machinery, using this in a production codebase may significantly increase your compile time.
Be warned!

# Contributing

All issues and PRs are very welcome (including major changes to the library design/API since we're at the early stages).

This project uses sbt-boilerplate plugin to generate functions of different arity.
If you cannot find a source file, it may be generated and lives under `core/src/main/boilerplate` 
(or other `boilerplate` directories)
