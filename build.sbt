val root = Project("root", file("."))
  .settings(
    name := "coproduct-error",
    version := "0.1",
    scalaVersion := "2.13.1",
    libraryDependencies ++= Seq(
      "com.chuusai" %% "shapeless" % "2.3.3",
      "org.typelevel" %% "cats-core" % "2.0.0",

      "dev.zio" %% "zio" % "1.0.0-RC13",
      "org.scalatest" %% "scalatest" % "3.0.8" % "test",
    ),
    scalacOptions ++= Seq(
      "-language:higherKinds",
    ),
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
    addCompilerPlugin("org.typelevel" % "kind-projector" % "0.10.3" cross CrossVersion.binary)
  //addCompilerPlugin("io.tryp" % "splain" % "0.4.0" cross CrossVersion.patch)
  )
