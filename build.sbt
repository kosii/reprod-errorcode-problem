name := "reprod-errorcode-problem"

version := "1.0"

scalaVersion := "2.11.8"

resolvers += Resolver.sonatypeRepo("releases")


libraryDependencies ++= Seq(
  "org.scalikejdbc"    %% "scalikejdbc"        % "2.4.2",
  "org.scalikejdbc"    %% "scalikejdbc-config" % "2.4.2",
  "org.postgresql"     %  "postgresql"         % "9.4.1208",
  "com.aerospike"      %  "aerospike-client"   % "latest.integration"
)