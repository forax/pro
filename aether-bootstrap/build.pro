import static com.github.forax.pro.Pro.*;
import static com.github.forax.pro.builder.Builders.*;

var version = Runtime.version().feature();

resolver.
    //checkForUpdate(true).
    dependencies(
      // Aether
      "org.eclipse.aether.transport.wagon=org.eclipse.aether:aether-transport-wagon:1.1.0",
      "org.eclipse.aether.transport.http=org.eclipse.aether:aether-transport-http:1.1.0",
      "org.eclipse.aether.transport.file=org.eclipse.aether:aether-transport-file:1.1.0",
      "org.eclipse.aether.connector.basic=org.eclipse.aether:aether-connector-basic:1.1.0",
      "org.eclipse.aether.impl=org.eclipse.aether:aether-impl:1.1.0",
      "org.eclipse.aether.util=org.eclipse.aether:aether-util:1.1.0",
      "org.eclipse.aether.spi=org.eclipse.aether:aether-spi:1.1.0",
      "org.eclipse.aether.api=org.eclipse.aether:aether-api:1.1.0",
      
      // Aether internal dependencies
      "org.apache.httpcomponents.httpclient=org.apache.httpcomponents:httpclient:4.5.6",
      "org.slf4j.api=org.slf4j:slf4j-api:1.8.0-beta2",
      "org.apache.maven.wagon.provider.api=org.apache.maven.wagon:wagon-provider-api:3.2.0",
      "org.apache.commons.codec=commons-codec:commons-codec:1.11",
      "org.apache.commons.logging.impl=org.slf4j:jcl-over-slf4j:1.8.0-beta2",
      "org.apache.httpcomponents.httpcore=org.apache.httpcomponents:httpcore:4.4.10",
      
      
      // Maven Aether Provider
      "org.apache.maven.aether.provider=org.apache.maven:maven-aether-provider:3.3.9",
      
      // Maven Aether Provider internal dependencies
      "org.codehaus.plexus.component.annotations=org.codehaus.plexus:plexus-component-annotations:1.7.1",
      "org.apache.commons.lang3=org.apache.commons:commons-lang3:3.8.1",
      "org.apache.maven.model=org.apache.maven:maven-model:3.5.4,org.apache.maven:maven-model-builder:3.5.4",
      "org.apache.maven.builder.support=org.apache.maven:maven-builder-support:3.5.4",
      "org.codehaus.plexus:plexus-utils=org.codehaus.plexus:plexus-utils:3.1.0",
      "com.google.guava=com.google.guava:guava:26.0-jre",
      "org.codehaus.plexus.interpolation=org.codehaus.plexus:plexus-interpolation:1.25",
      "org.apache.maven.artifact=org.apache.maven:maven-artifact:3.5.4,org.apache.maven:maven-repository-metadata:3.5.4"
    )

//compiler.sourceRelease(version) // avoid to generate nest-mate attributes
compiler.sourceRelease(10)

packager.modules(
      "com.github.forax.pro.aether@0." + version + "/com.github.forax.pro.aether.bootstrap.Main"
    )

frozer.rootModule("com.github.forax.pro.aether")

run(resolver, modulefixer, compiler, packager, /*runner,*/ frozer)

/exit
