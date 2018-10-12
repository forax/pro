import static com.github.forax.pro.Pro.*;
import static com.github.forax.pro.builder.Builders.*;

pro.
  loglevel("verbose").
  exitOnError(true)

compiler.
  lint("all,-varargs,-overloads").
  moduleDependencyPath(path("deps", "../../target/main/artifact/", "../../deps"))
          
run(compiler, packager)

/exit
