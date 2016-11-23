import static com.github.forax.pro.Pro.*;

set("packager.moduleMetadata", list(
     "com.foo.acme.amodule@1.0",
     "com.foo.acme.anothermodule@2.0/com.foo.acme.anotherpackage.Main"
   ));

run("compiler", "packager")

/exit
