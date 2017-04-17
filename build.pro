import static com.github.forax.pro.Pro.*;
import com.github.forax.pro.helper.FileHelper;
import java.nio.file.Files;

set("pro.loglevel", "verbose");
set("pro.exitOnError", true);

FileHelper.deleteAllFiles(location("bootstrap"), true);
Files.createDirectory(location("bootstrap"));

set("compiler.lint", "all,-varargs,-overloads");
set("compiler.moduleExplodedSourcePath", location("bootstrap/modules"));

set("runner.modulePath", path("bootstrap/modules", "deps"));
set("runner.module", "com.github.forax.pro.bootstrap/com.github.forax.pro.bootstrap.Bootstrap");

run("compiler", "runner");

/exit
