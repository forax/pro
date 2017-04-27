import static com.github.forax.pro.Pro.*

set("pro.loglevel", "verbose");

// set("tester.overrideArguments", list("--help"))

// run "pro" tests
run("tester")

// run "plugins" tests
set("tester.moduleExplodedTestPath", list(path(
  // "plugins/runner/target/test/exploded",
  "plugins/tester/target/test/exploded"
)))
run("tester")

/exit
