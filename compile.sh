function test {
  export JAVA_HOME=$1

  mvn compile
  # mvn release:prepare
  # mvn release:perform
}

test "c:\Program Files\Java\jdk-9.0.4"