name := """mathpump2"""

version := "1.0"

scalaVersion := "2.11.6"

scalacOptions += "-feature"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.11",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.11" % "test",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test",
  "org.scalafx" % "scalafx_2.11" % "8.0.60-R9",
  "com.rabbitmq" % "amqp-client" % "3.5.7",
  "log4j" % "log4j" % "1.2.17"
)

