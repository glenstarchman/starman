resolvers += "Sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
addCompilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full)
// Run sbt xitrum-package to prepare for deploying to production environment
//addSbtPlugin("tv.cntt" % "xitrum-package" % "1.9")
// For precompiling Scalate templates in the compile phase of SBT
addSbtPlugin("com.mojolly.scalate" % "xsbt-scalate-generator" % "0.5.0")
