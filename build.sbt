Global / cancelable := true

lazy val root = Project("hotpotato", file("."))
  .aggregate(core, benchmarks)
  .settings(
    scalaVersion := "2.13.1",
  )

lazy val core = moduleProject("core")
  .settings(
    libraryDependencies ++= Seq(
      "com.chuusai" %% "shapeless" % "2.3.3",
      "org.typelevel" %% "cats-core" % "2.0.0",
      "org.typelevel" %% "cats-effect" % "2.0.0",

      "dev.zio" %% "zio" % "1.0.0-RC13",
      "org.scalatest" %% "scalatest" % "3.0.8" % "test",
    ),
  )

lazy val benchmarks = moduleProject("benchmarks")
  .enablePlugins(JmhPlugin)
  .dependsOn(core % "compile->compile;compile->test")
  .settings(
    publish / skip := true,
  )

def moduleProject(name: String) = Project(name, file(s"modules/$name"))
  .settings(
    scalacOptions ++= Seq(
      "-language:higherKinds",
    ),
    scalaVersion := "2.13.1",
    //    addCompilerPlugin("io.tryp" % "splain" % "0.4.1" cross CrossVersion.patch),
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
    addCompilerPlugin("org.typelevel" % "kind-projector" % "0.10.3" cross CrossVersion.binary)
  )

