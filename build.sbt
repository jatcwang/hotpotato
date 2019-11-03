lazy val root = Project("hotpotato", file("."))
  .aggregate(core, benchmarks)
  .settings(
    commonSettings,
  )

lazy val core = moduleProject("core")
  .settings(
    libraryDependencies ++= Seq(
      "com.chuusai" %% "shapeless" % "2.3.3",
      "org.typelevel" %% "cats-core" % "2.0.0",
      "org.typelevel" %% "cats-effect" % "2.0.0",
      // FIXME optional dep!
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

def moduleProject(name: String) =
  Project(s"hotpotato-$name", file(s"modules/$name"))
    .settings(
      scalacOptions ++= Seq(
        "-language:higherKinds",
      ),
      //    addCompilerPlugin("io.tryp" % "splain" % "0.4.1" cross CrossVersion.patch),
      addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
      addCompilerPlugin("org.typelevel" % "kind-projector" % "0.10.3" cross CrossVersion.binary),
    )
    .settings(
      commonSettings,
    )

lazy val commonSettings = Seq(
  scalaVersion := "2.13.1",
  crossScalaVersions := List("2.12.10", "2.13.1")
)

inThisBuild(
  List(
    organization := "com.github.jatcwang",
    homepage := Some(url("https://github.com/jatcwang/hotpotato")),
    licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    developers := List(
      Developer(
        "jatcwang",
        "Jacob Wang",
        "jatcwang@gmail.com",
        url("https://almostfunctional.com"),
      ),
    ),
  ),
)
