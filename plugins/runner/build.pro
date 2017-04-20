import static com.github.forax.pro.Pro.*;

set("pro.loglevel", "verbose");
set("pro.exitOnError", true);

set("compiler.lint", "all,-varargs,-overloads");
set("compiler.moduleDependencyPath", path("deps", "../../target/main/artifact/", "../../deps"));

run("compiler", "packager");

/exit
