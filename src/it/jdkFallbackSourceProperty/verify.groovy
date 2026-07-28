// Verifies that Clover fell back to the maven.compiler.source property as its source level.

File buildLog = new File(basedir, "build.log")
assert buildLog.exists()

String log = buildLog.text
assert log.contains("No <jdk> defined, using the compiler's source level: 1.8")

// the source level shall be passed to the instrumenter
assert log.contains("parameter = [--source]")
assert log.contains("parameter = [1.8]")

assert new File(basedir, "target/clover/src-instrumented/Simple.java").exists()
assert new File(basedir, "target/clover/classes/Simple.class").exists()
assert new File(basedir, "target/clover/clover.db").exists()

return true
