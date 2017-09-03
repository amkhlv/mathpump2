name := """mathpump2"""

version := "1.0"

scalaVersion := "2.11.11"

scalacOptions += "-feature"

libraryDependencies ++= Seq(
  "com.typesafe.akka" % "akka-actor_2.11" % "2.5.4",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.11" % "test",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test",
  "org.scalafx" % "scalafx_2.11" % "8.0.102-R11",
  "com.rabbitmq" % "amqp-client" % "4.2.0",
  "log4j" % "log4j" % "1.2.17"
)

