package com.github.forax.pro.aether;

import java.nio.file.Path;
import org.eclipse.aether.artifact.Artifact;

public class ArtifactDescriptor {
  private final Artifact artifact;

  ArtifactDescriptor(Artifact artifact) {
    this.artifact = artifact;
  }

  public String getGroupId() {
    return artifact.getGroupId();
  }

  public String getArtifactId() {
    return artifact.getArtifactId();
  }

  public String getVersion() {
    return artifact.getVersion();
  }

  public Object getArtifactKey() {
    return artifact.getGroupId() + ':' + artifact.getArtifactId();
  }

  public Path getPath() {
    return artifact.getFile().toPath();
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof ArtifactDescriptor)) {
      return false;
    }
    ArtifactDescriptor descriptor = (ArtifactDescriptor) o;
    return artifact.getGroupId().equals(descriptor.artifact.getGroupId())
        && artifact.getArtifactId().equals(descriptor.artifact.getArtifactId());
  }

  @Override
  public int hashCode() {
    return artifact.getGroupId().hashCode() ^ artifact.getArtifactId().hashCode();
  }

  @Override
  public String toString() {
    return getGroupId() + ':' + getArtifactId() + ':' + getVersion();
  }
}
