package com.github.forax.pro.aether;

import com.github.forax.pro.helper.util.StableList;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
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

public class Aether {
  private final RepositorySystem system;
  private final DefaultRepositorySystemSession session;
  private final List<RemoteRepository> remoteRepositories;

  private Aether(
      RepositorySystem system,
      DefaultRepositorySystemSession session,
      List<RemoteRepository> remoteRepositories) {
    this.system = system;
    this.session = session;
    this.remoteRepositories = remoteRepositories;
  }

  public static Aether create(Path mavenLocalRepository, List<URI> remoteRepositories) {
    // respository system
    DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
    locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
    locator.addService(TransporterFactory.class, FileTransporterFactory.class);
    locator.addService(TransporterFactory.class, HttpTransporterFactory.class);

    locator.setErrorHandler(
        new DefaultServiceLocator.ErrorHandler() {
          @Override
          public void serviceCreationFailed(Class<?> type, Class<?> impl, Throwable exception) {
            exception.printStackTrace();
          }
        });
    RepositorySystem system = locator.getService(RepositorySystem.class);

    // session
    DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
    LocalRepository localRepo = new LocalRepository(mavenLocalRepository.toString());
    session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));

    // central repository
    RemoteRepository central =
        new RemoteRepository.Builder("central", "default", "http://central.maven.org/maven2/")
            .build();

    // remote repositories
    List<RemoteRepository> remotes =
        StableList.from(remoteRepositories)
            .map(uri -> new RemoteRepository.Builder(null, "default", uri.toString()).build())
            .append(central);

    return new Aether(system, session, remotes);
  }

  @SuppressWarnings("static-method")
  public ArtifactQuery createArtifactQuery(String artifactCoords) {
    DefaultArtifact artifact = new DefaultArtifact(artifactCoords);
    return new ArtifactQuery(artifact);
  }

  public Set<ArtifactInfo> dependencies(ArtifactQuery query) throws IOException {
    Artifact artifact = query.artifact;

    CollectRequest collectRequest = new CollectRequest();
    collectRequest.setRoot(new Dependency(artifact, ""));
    collectRequest.setRepositories(remoteRepositories);

    CollectResult collectResult;
    try {
      collectResult = system.collectDependencies(session, collectRequest);
    } catch (DependencyCollectionException e) {
      throw new IOException(e);
    }

    LinkedHashSet<ArtifactInfo> dependencies = new LinkedHashSet<>();
    collectResult
        .getRoot()
        .accept(
            new DependencyVisitor() {
              @Override
              public boolean visitLeave(DependencyNode node) {
                return true;
              }

              @Override
              public boolean visitEnter(DependencyNode node) {
                if (node.getDependency().isOptional()) {
                  // skip optional dependency    !! TODO revisit
                  return false;
                }
                Artifact artifact = node.getArtifact();
                dependencies.add(new ArtifactInfo(artifact));
                return true;
              }
            });
    return dependencies;
  }

  public List<ArtifactDescriptor> download(List<ArtifactInfo> unresolvedArtifacts)
      throws IOException {
    System.out.println("download unresolvedArtifacts " + unresolvedArtifacts);

    List<RemoteRepository> repositories = this.remoteRepositories;
    List<ArtifactRequest> artifactRequests =
        unresolvedArtifacts
            .stream()
            .map(
                dependency -> {
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

    return artifactResults
        .stream()
        .map(result -> new ArtifactDescriptor(result.getArtifact()))
        .collect(Collectors.toList());
  }
}
