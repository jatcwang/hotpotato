lazy val root = Project("hotpotato", file("."))
  .dependsOn(core, benchmarks, docs)
  .aggregate(core, benchmarks, docs, testoptionaldependency, testoptionaldependencymissing)
  .settings(
    publish / skip := true,
    commonSettings,
  )

val zioDep             = "dev.zio" %% "zio" % "1.0.0-RC17"
val zioCatsInteroptDep = "dev.zio" %% "zio-interop-cats" % "2.0.0.0-RC10"
val catsEffectDep      = "org.typelevel" %% "cats-effect" % "2.1.2"
val catsCoreDep        = "org.typelevel" %% "cats-core" % "2.1.1"
lazy val core = moduleProject("core")
  .enablePlugins(BoilerplatePlugin)
  .settings(
    libraryDependencies ++= Seq(
      "com.chuusai" %% "shapeless" % "2.3.3",
      catsCoreDep,
      catsEffectDep % "optional",
      zioDep % "optional",
      zioCatsInteroptDep % "optional",
      "org.typelevel" %% "cats-laws" % "2.1.1" % Test,
      "org.typelevel" %% "discipline-scalatest" % "1.0.0-RC2" % Test,
    ),
  )

lazy val testoptionaldependencymissing = moduleProject("testoptionaldependencymissing")
  .dependsOn(core)
  .settings(
    commonSettings,
    publish / skip := true,
  )

lazy val testoptionaldependency = moduleProject("testoptionaldependency")
  .dependsOn(core)
  .settings(
    commonSettings,
    publish / skip := true,
    libraryDependencies ++= Seq(
      zioDep,
      zioCatsInteroptDep,
      catsCoreDep,
      catsEffectDep,
    ),
  )

lazy val docs = project
  .dependsOn(core % "compile->compile;compile->test")
  .enablePlugins(MicrositesPlugin)
  .settings(
    commonSettings,
    publish / skip := true,
    libraryDependencies ++= Seq(
      zioDep,
      zioCatsInteroptDep,
      catsEffectDep,
    ),
  )
  .settings(
    mdocIn := file("docs/docs"),
    micrositeName := "Hotpotato",
    micrositeDescription := "Typesafe error handling like you mean it",
    micrositeUrl := "https://jatcwang.github.io",
    micrositeBaseUrl := "/hotpotato",
    micrositeDocumentationUrl := s"${micrositeBaseUrl.value}/docs/intro",
    micrositeAuthor := "Jacob Wang",
    micrositeHomepage := "https://jatcwang.github.io/hotpotato",
    micrositeTwitterCreator := "@jatcwang",
    micrositeGithubOwner := "jatcwang",
    micrositeGithubRepo := "hotpotato",
    micrositeCompilingDocsTool := WithMdoc,
    micrositeHighlightTheme := "a11y-light",
    micrositePushSiteWith := GitHub4s,
    micrositeGithubToken := sys.env.get("GITHUB_TOKEN"),
  )

lazy val benchmarks = moduleProject("benchmarks")
  .enablePlugins(JmhPlugin)
  .dependsOn(core % "compile->compile;compile->test")
  .settings(
    libraryDependencies ++= Seq(
      zioDep,
    ),
    publish / skip := true,
  )

def moduleProject(name: String) =
  Project(s"hotpotato-$name", file(s"modules/$name"))
    .settings(
      commonSettings,
    )
    .settings(
      libraryDependencies ++= Seq(
        "org.scalatest" %% "scalatest" % "3.1.1" % "test",
      ),
    )

lazy val commonSettings = Seq(
  scalaVersion := "2.13.1",
  crossScalaVersions := List("2.12.10", "2.13.1"),
  scalacOptions --= {
    Seq("-Wself-implicit") ++ (
      if (sys.env.get("CI").isDefined) {
        Seq.empty
      } else {
        Seq("-Xfatal-warnings")
      }
    )
  },
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
