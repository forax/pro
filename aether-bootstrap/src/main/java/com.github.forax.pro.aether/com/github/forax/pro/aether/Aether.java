package com.github.forax.pro.aether;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.artifact.JavaScopes;

public class Aether {
  private final RepositorySystem system;
  private final DefaultRepositorySystemSession session;
  private final List<RemoteRepository> remoteRepositories;
  
  private Aether(RepositorySystem system, DefaultRepositorySystemSession session, List<RemoteRepository> remoteRepositories) {
    this.system = system;
    this.session = session;
    this.remoteRepositories = remoteRepositories;
  }

  public static Aether create(Path mavenLocalRepository, List<URI> remoteRepositories) {
    // respository system
    var locator = MavenRepositorySystemUtils.newServiceLocator();
    locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
    locator.addService(TransporterFactory.class, FileTransporterFactory.class);
    locator.addService(TransporterFactory.class, HttpTransporterFactory.class);

    locator.setErrorHandler(new DefaultServiceLocator.ErrorHandler() {
      @Override
      public void serviceCreationFailed(Class<?> type, Class<?> impl, Throwable exception) {
        exception.printStackTrace();
      }
    });
    RepositorySystem system = locator.getService(RepositorySystem.class);

    // session
    var session = MavenRepositorySystemUtils.newSession();
    var localRepository = new LocalRepository(mavenLocalRepository.toString());
    session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepository));

    // central repository
    var central = new RemoteRepository.Builder("central", "default",
        "http://central.maven.org/maven2/").build();
    
    // remote repositories
    var remotes = Stream.concat(
          remoteRepositories.stream().map(uri -> new RemoteRepository.Builder(null, "default", uri.toString()).build()),
          Stream.of(central)
        )
        .collect(Collectors.toList());
    
    return new Aether(system, session, remotes);
  }
  
  @SuppressWarnings("static-method")
  public ArtifactQuery createArtifactQuery(String artifactCoords) {
    var artifact = new DefaultArtifact(artifactCoords);
    return new ArtifactQuery(artifact);
  }
  
  @SuppressWarnings("static-method")
  public ArtifactInfo createArtifactInfo(String artifactCoords) {
    var artifact = new DefaultArtifact(artifactCoords);
    return new ArtifactInfo(artifact);
  }

  public Set<ArtifactInfo> dependencies(ArtifactQuery query)throws IOException {
    var artifact = query.artifact;

    var collectRequest = new CollectRequest();
    collectRequest.setRoot(new Dependency(artifact, ""));
    collectRequest.setRepositories(remoteRepositories);

    CollectResult collectResult;
    try {
      collectResult = system.collectDependencies(session, collectRequest);
    } catch (DependencyCollectionException e) {
      throw new IOException(e);
    }

    var dependencies = new LinkedHashSet<ArtifactInfo>();
    collectResult.getRoot().accept(new DependencyVisitor() {
      @Override
      public boolean visitLeave(DependencyNode node) {
        return true;
      }

      @Override
      public boolean visitEnter(DependencyNode node) {
        var dependency = node.getDependency();
        var scope = dependency.getScope();
        //System.out.println("dependency node: " + dependency.getArtifact() + " optional " + dependency.isOptional() + " scope " + scope);
        if (dependency.isOptional()) {  
          // skip optional dependency
          return false;
        }
        if (!scope.isEmpty() && !scope.equals(JavaScopes.COMPILE) && !scope.equals(JavaScopes.RUNTIME)) {
          // skip test, provided and system dependency
          return false;
        }
        var artifact = node.getArtifact();
        dependencies.add(new ArtifactInfo(artifact));
        return true;
      }
    });
    return dependencies;
  }
   
  public List<ArtifactDescriptor> download(List<ArtifactInfo> unresolvedArtifacts) throws IOException {
    var repositories = this.remoteRepositories;
    var artifactRequests = unresolvedArtifacts.stream()
        .map(dependency -> {
          ArtifactRequest artifactRequest = new ArtifactRequest();
          artifactRequest.setArtifact(dependency.artifact);
          artifactRequest.setRepositories(repositories);
          return artifactRequest;
        })
        .collect(Collectors.toList());

    List<ArtifactResult> artifactResults;
    try {
      artifactResults = system.resolveArtifacts(session, artifactRequests);
    } catch (ArtifactResolutionException e) {
      throw new IOException(e);
    }
    
    return artifactResults.stream()
      .map(result -> new ArtifactDescriptor(result.getArtifact()))
      .collect(Collectors.toList());
  }
}
