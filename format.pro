import static com.github.forax.pro.Pro.*

set("pro.loglevel", "verbose")
set("pro.exitOnError", true)

// --dry-run
//   Prints the paths of the files whose contents would change if the formatter were run normally.
// --set-exit-if-changed
//       Return exit code 1 if there are any formatting changes.

// set("formatter.rawArguments", list("--dry-run", "--set-exit-if-changed")) // jshell does not exit on error?!
set("formatter.rawArguments", list("--dry-run"))

// --replace
//   Send formatted output back to files, not stdout.

// set("formatter.rawArguments", list("--replace"))

run("formatter")

/exit errorCode()
