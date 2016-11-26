package com.github.forax.pro.aether;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
  private final RemoteRepository mavenCentral;
  
  private Aether(RepositorySystem system, DefaultRepositorySystemSession session, RemoteRepository mavenCentral) {
    this.system = system;
    this.session = session;
    this.mavenCentral = mavenCentral;
  }

  public static Aether create(Path mavenLocalRepository) {
    // respository system
    DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
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
    DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
    LocalRepository localRepo = new LocalRepository(mavenLocalRepository.toString());
    session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));

    // repository
    RemoteRepository mavenCentral = new RemoteRepository.Builder("central", "default",
        "http://central.maven.org/maven2/").build();
    
    return new Aether(system, session, mavenCentral);
  }

  public Set<String> dependencies(String unresolvedArtifact)throws IOException {
    Artifact artifact = new DefaultArtifact(unresolvedArtifact);

    CollectRequest collectRequest = new CollectRequest();
    collectRequest.setRoot(new Dependency(artifact, ""));
    collectRequest.setRepositories(List.of(mavenCentral));

    CollectResult collectResult;
    try {
      collectResult = system.collectDependencies(session, collectRequest);
    } catch (DependencyCollectionException e) {
      throw new IOException(e);
    }

    LinkedHashSet<String> dependencies = new LinkedHashSet<>();
    collectResult.getRoot().accept(new DependencyVisitor() {
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
        String coordId = coordId(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());
        dependencies.add(coordId);
        return true;
      }
    });
    return dependencies;
  }
   
  public Map<String, ArtifactDescriptor> download(List<String> unresolvedArtifacts) throws IOException {
    System.out.println("download unresolvedArtifacts " + unresolvedArtifacts);

    List<RemoteRepository> repositories = List.of(mavenCentral);
    Map<ArtifactRequest, String> artifactRequests = unresolvedArtifacts.stream()
        .collect(Collectors.toMap(artifactName -> {
          DefaultArtifact artifact = new DefaultArtifact(artifactName);

          ArtifactRequest artifactRequest = new ArtifactRequest();
          artifactRequest.setArtifact(artifact);
          artifactRequest.setRepositories(repositories);

          return artifactRequest;
        }, artifactName -> artifactName));

    List<ArtifactResult> artifactResults;
    try {
      artifactResults = system.resolveArtifacts(session, artifactRequests.keySet());
    } catch (ArtifactResolutionException e) {
      throw new IOException(e);
    }
    
    return artifactResults.stream()
      .collect(Collectors.<ArtifactResult, String, ArtifactDescriptor>toMap(
          result -> artifactRequests.get(result.getRequest()),
          result -> createArtifactDescriptor(result.getArtifact())));
  }
  
  static ArtifactDescriptor createArtifactDescriptor(Artifact artifact) {
    String groupId = artifact.getGroupId();
    String artifactId = artifact.getArtifactId();
    String version = artifact.getVersion();
    String coordId = coordId(groupId, artifactId, version);
    Path path = artifact.getFile().toPath();
    return new ArtifactDescriptor() {
      @Override
      public String getArtifactId() {
        return artifactId;
      }
      @Override
      public String getGroupId() {
        return groupId;
      }
      @Override
      public String getVersion() {
        return version;
      }
      @Override
      public String getCoordId() {
        return coordId;
      }
      @Override
      public Path getPath() {
        return path;
      }
      
      @Override
      public String toString() {
        return coordId + '(' + path + ')';
      }
    };
  }

  static String coordId(String groupId, String artifactId, String version) {
    return groupId + ':' + artifactId + ':' + version;
  }
}
