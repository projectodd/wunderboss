# WunderBoss

This is a prototype of the next-generation TorqueBox platform

## Building

Make sure you're using Java 7 or above.

    mvn install -s support/settings.xml

## Running integration tests

Make sure `phantomjs` is available on your $PATH -
http://phantomjs.org/download.html

    cd integration-tests/
    mvn test -s support/settings.xml


## Hello World example

Make sure you have a recent JRuby installed.

    jruby test.rb

This boots up a hello world Rack application on http://localhost:8080/
