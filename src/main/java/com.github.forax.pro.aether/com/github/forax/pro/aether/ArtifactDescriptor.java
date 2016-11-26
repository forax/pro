package com.github.forax.pro.aether;

import java.nio.file.Path;

public interface ArtifactDescriptor {
  String getGroupId();
  String getArtifactId();
  String getVersion();
  String getCoordId();
  Path getPath();
}
