rem === Do not instrument code. Just compile, run tests and install ===
echo "Compiling without Clover"
mvn clean install
pause

rem === Instrument server-side code only, run tests in a browser (or simulator like htmlunit) ===
echo "Instrumenting server code with Clover"
mvn -Pwith.clover.serveronly clean install
echo "See report in target/site/clover/index.html"
pause

rem === Instrument all code, run tests in JVM using mocking framework ===
echo "Instrumenting server, client and shared code with Clover"
mvn -Pwith.clover.everything clean install
echo "See report in target/site/clover/index.html"
pause