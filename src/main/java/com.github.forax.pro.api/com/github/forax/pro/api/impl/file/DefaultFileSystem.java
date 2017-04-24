package com.github.forax.pro.api.impl.file;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Objects;
import java.util.Set;

public class DefaultFileSystem extends FileSystem {
  private final FileSystem delegate;
  private final DefaultFileSystemProvider fileSystemProvider;

  DefaultFileSystem(DefaultFileSystemProvider fileSystemProvider,FileSystem delegate) {
    this.fileSystemProvider = Objects.requireNonNull(fileSystemProvider);
    this.delegate = Objects.requireNonNull(delegate);
  }

  @Override
  public FileSystemProvider provider() {
    return fileSystemProvider;
  }
  
  @Override
  public Path getPath(String first, String... more) {
    return fileSystemProvider.getPath(delegate, first, more);
  }
  
  @Override
  public void close() throws IOException {
    delegate.close();
  }

  @Override
  public Iterable<FileStore> getFileStores() {
    return delegate.getFileStores();
  }

  @Override
  public PathMatcher getPathMatcher(String syntaxAndPattern) {
    return delegate.getPathMatcher(syntaxAndPattern);
  }

  @Override
  public Iterable<Path> getRootDirectories() {
    return delegate.getRootDirectories();
  }

  @Override
  public String getSeparator() {
    return delegate.getSeparator();
  }

  @Override
  public UserPrincipalLookupService getUserPrincipalLookupService() {
    return delegate.getUserPrincipalLookupService();
  }

  @Override
  public boolean isOpen() {
    return delegate.isOpen();
  }

  @Override
  public boolean isReadOnly() {
    return delegate.isReadOnly();
  }

  @Override
  public WatchService newWatchService() throws IOException {
    return delegate.newWatchService();
  }

  @Override
  public Set<String> supportedFileAttributeViews() {
    return delegate.supportedFileAttributeViews();
  }
}
