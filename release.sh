function test {
  export JAVA_HOME=$1

  mvn release:prepare
}

test "c:\Program Files\Java\jdk-9.0.4"