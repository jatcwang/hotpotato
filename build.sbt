val root = Project("root", file("."))
  .settings(
    name := "coproduct-error",
    version := "0.1",
    scalaVersion := "2.12.8",
    libraryDependencies ++= Seq(
      "com.chuusai" %% "shapeless" % "2.3.3",
      "org.typelevel" %% "cats-core" % "1.6.0",

      "org.scalatest" %% "scalatest" % "3.0.7" % "test",
    ),
    scalacOptions ++= Seq(
      "-language:higherKinds",
      "-Ypartial-unification",
    ),
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.0-M4"),
    addCompilerPlugin("org.spire-math" % "kind-projector" % "0.9.9" cross CrossVersion.binary)
  //addCompilerPlugin("io.tryp" % "splain" % "0.4.0" cross CrossVersion.patch)
  )
