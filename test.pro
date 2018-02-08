import static com.github.forax.pro.Pro.*
import static com.github.forax.pro.builder.Builders.*

// pro.loglevel("debug")
tester.timeout(99)

// run "pro" tests
run(tester)

// run "plugins" tests
tester.moduleExplodedTestPath(
  list(
    path("plugins/tester/target/test/exploded")
  )
)
run(tester)

/exit
