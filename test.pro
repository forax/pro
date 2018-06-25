import static com.github.forax.pro.Pro.*
import static com.github.forax.pro.builder.Builders.*

// pro.loglevel("debug")
tester.timeout(99)
tester.parallel(false)

// run "pro" tests
run(tester)

// run "pro" tests again, now in parallel
tester.parallel(true)
run(tester)

// run "plugins" tests
tester.moduleExplodedTestPath(
  list(
    path("plugins/tester/target/test/exploded")
  )
)
run(tester)

/exit
