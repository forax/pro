package com.github.forax.pro.aether;

import org.eclipse.aether.artifact.Artifact;

public class ArtifactInfo {
  final Artifact artifact;

  ArtifactInfo(Artifact artifact) {
    this.artifact = artifact;
  }
  
  public String getArtifactKey() {
    return artifact.getGroupId() + ':' + artifact.getArtifactId();
  }
  
  public String getArtifactCoords() {
    return artifact.getGroupId() + ':' + artifact.getArtifactId() + ':' + artifact.getVersion();
  }
  
  @Override
  public boolean equals(Object o) {
    if (!(o instanceof ArtifactInfo)) {
      return false;
    }
    var info = (ArtifactInfo)o;
    return artifact.getGroupId().equals(info.artifact.getGroupId()) &&
           artifact.getArtifactId().equals(info.artifact.getArtifactId());
  }
  
  @Override
  public int hashCode() {
    return artifact.getGroupId().hashCode() ^ artifact.getArtifactId().hashCode();
  }
  
  @Override
  public String toString() {
    return getArtifactKey();
  }
}
