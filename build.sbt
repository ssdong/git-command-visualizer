enablePlugins(ScalaJSBundlerPlugin)

name := "git-command-visualizer"

scalaVersion := "2.13.2"

val slinkyVersion = "0.6.7"

npmDependencies in Compile += "react" -> "16.13.1"
npmDependencies in Compile += "react-dom" -> "16.13.1"
npmDependencies in Compile += "react-proxy" -> "1.1.8"

npmDevDependencies in Compile += "file-loader" -> "6.0.0"
npmDevDependencies in Compile += "style-loader" -> "1.2.1"
npmDevDependencies in Compile += "css-loader" -> "3.5.3"
npmDevDependencies in Compile += "html-webpack-plugin" -> "4.3.0"
npmDevDependencies in Compile += "copy-webpack-plugin" -> "5.1.1"
npmDevDependencies in Compile += "webpack-merge" -> "4.2.2"

libraryDependencies ++= Seq(
  "me.shadaj" %%% "slinky-core",
  "me.shadaj" %%% "slinky-web",
  "me.shadaj" %%% "slinky-native",
  "me.shadaj" %%% "slinky-hot"
).map(_ % slinkyVersion)

libraryDependencies += "org.scalatest" %%% "scalatest" % "3.1.1" % Test

scalacOptions ++= Seq(
  "-deprecation",
  "-Ymacro-annotations"
)

version in webpack := "4.43.0"
version in startWebpackDevServer := "3.11.0"

webpackResources := baseDirectory.value / "webpack" * "*"

webpackConfigFile in fastOptJS := Some(baseDirectory.value / "webpack" / "webpack-fastopt.config.js")
webpackConfigFile in fullOptJS := Some(baseDirectory.value / "webpack" / "webpack-opt.config.js")
webpackConfigFile in Test := Some(baseDirectory.value / "webpack" / "webpack-core.config.js")

webpackDevServerExtraArgs in fastOptJS := Seq("--inline", "--hot")
webpackBundlingMode in fastOptJS := BundlingMode.LibraryOnly()

requireJsDomEnv in Test := true

addCommandAlias("dev", ";fastOptJS::startWebpackDevServer;~fastOptJS")

addCommandAlias("build", "fullOptJS::webpack")
