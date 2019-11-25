lazy val root = Project("hotpotato", file("."))
  .dependsOn(core, benchmarks, docs)
  .aggregate(core, benchmarks, docs)
  .settings(
    publish / skip := true,
    commonSettings,
  )

lazy val core = moduleProject("core")
  .enablePlugins(BoilerplatePlugin)
  .settings(
    libraryDependencies ++= Seq(
      "com.chuusai" %% "shapeless" % "2.3.3",
      "org.typelevel" %% "cats-core" % "2.0.0",
      "org.typelevel" %% "cats-effect" % "2.0.0",
      "org.typelevel" %% "cats-laws" % "2.0.0" % Test,
      "org.typelevel" %% "discipline-scalatest" % "1.0.0-RC1" % Test,
      // FIXME optional dep!
      "dev.zio" %% "zio" % "1.0.0-RC17",
      "dev.zio" %% "zio-interop-cats" % "2.0.0.0-RC9",
      "org.scalatest" %% "scalatest" % "3.0.8" % "test",
    ),
  )

lazy val docs = project
  .dependsOn(core % "compile->compile;compile->test")
  .enablePlugins(MicrositesPlugin)
  .settings(
    commonSettings,
    publish / skip := true,
  )
  .settings(
    mdocIn := file("docs/docs"),
    micrositeName := "Hotpotato",
    micrositeDescription := "Typesafe error handling like you mean it",
    micrositeUrl := "https://jatcwang.github.io",
    micrositeBaseUrl := "/hotpotato",
    micrositeDocumentationUrl := "/hotpotato/docs",
    micrositeAuthor := "Jacob Wang",
    micrositeHomepage := "https://jatcwang.github.io/hotpotato",
    micrositeTwitterCreator := "@jatcwang",
    micrositeGithubOwner := "jatcwang",
    micrositeGithubRepo := "hotpotato",
    micrositeCompilingDocsTool := WithMdoc,
    micrositePushSiteWith := GitHub4s,
    micrositeGithubToken := sys.env.get("GITHUB_TOKEN"),
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
      commonSettings,
    )

lazy val commonSettings = Seq(
  scalaVersion := "2.13.1",
  crossScalaVersions := List("2.12.10", "2.13.1"),
  scalacOptions ++= Seq(
    "-language:higherKinds",
  ),
  //    addCompilerPlugin("io.tryp" % "splain" % "0.4.1" cross CrossVersion.patch),
  addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
  addCompilerPlugin("org.typelevel" % "kind-projector" % "0.10.3" cross CrossVersion.binary),
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
