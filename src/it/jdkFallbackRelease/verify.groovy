// Verifies that Clover fell back to the maven-compiler-plugin's <release> element as its source level.

File buildLog = new File(basedir, "build.log")
assert buildLog.exists()

String log = buildLog.text
assert log.contains("No <jdk> defined, using the compiler's release level: 11")

// the source level shall be passed to the instrumenter
assert log.contains("parameter = [--source]")
assert log.contains("parameter = [11]")

// the class using 'var' shall be instrumented and compiled
assert new File(basedir, "target/clover/src-instrumented/UsesLocalVariableTypeInference.java").exists()
assert new File(basedir, "target/clover/classes/UsesLocalVariableTypeInference.class").exists()
assert new File(basedir, "target/clover/clover.db").exists()

return true
