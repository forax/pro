package com.github.forax.pro.api.impl.file;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

public class DefaultFileSystemProvider extends FileSystemProvider {
  private static final Supplier<Path> DEFAULT_PREFIX_SUPPLIER = () -> Paths.get(".");
  
  private final FileSystemProvider delegate;
  private Supplier<Path> prefixSupplier = DEFAULT_PREFIX_SUPPLIER;
  
  public DefaultFileSystemProvider(FileSystemProvider delegate) {
    System.err.println("initialized !!!!!!!!!!!!!!!!!!!!!!!!!!!");
    this.delegate = Objects.requireNonNull(delegate);
  }
  
  public void setPrefixSupplier(Supplier<Path> prefixSupplier) {
    this.prefixSupplier = Objects.requireNonNull(prefixSupplier);
  }
  
  public Supplier<Path> getPrefixSupplier() {
    return prefixSupplier;
  }
  
  @Override
  public Path getPath(URI uri) {
    Path path = delegate.getPath(uri);
    return translatePath(path);
  }

  Path getPath(FileSystem fileSystem, String first, String[] more) {
    Path path = fileSystem.getPath(first, more);
    return translatePath(path);
  }
  
  private Path translatePath(Path path) {
    if (prefixSupplier == DEFAULT_PREFIX_SUPPLIER || path.isAbsolute()) {
      return path;
    }
    return prefixSupplier.get().resolve(path);
  }
  
  @Override
  @SuppressWarnings("resource")
  public FileSystem getFileSystem(URI uri) {
    FileSystem fileSystem =  delegate.getFileSystem(uri);
    return translateFileSystem(uri, fileSystem);
  }

  @Override
  @SuppressWarnings("resource")
  public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
    FileSystem fileSystem = delegate.newFileSystem(uri, env);
    return translateFileSystem(uri, fileSystem);
  }
  
  private FileSystem translateFileSystem(URI uri, FileSystem fileSystem) {
    if ("file".equalsIgnoreCase(uri.getScheme())) {
      return new DefaultFileSystem(this, fileSystem); 
    }
    return fileSystem;
  }
  
  @Override
  public void checkAccess(Path path, AccessMode... modes) throws IOException {
    delegate.checkAccess(path, modes);
  }
  @Override
  public void copy(Path source, Path target, CopyOption... options) throws IOException {
    delegate.copy(source, target, options);
  }
  @Override
  public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
    delegate.createDirectory(dir, attrs);
  }
  @Override
  public void delete(Path path) throws IOException {
    delegate.delete(path);
  }
  @Override
  public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
    return delegate.getFileAttributeView(path, type, options);
  }
  @Override
  public FileStore getFileStore(Path path) throws IOException {
    return delegate.getFileStore(path);
  }
  @Override
  public String getScheme() {
    return delegate.getScheme();
  }
  @Override
  public boolean isHidden(Path path) throws IOException {
    return delegate.isHidden(path);
  }
  @Override
  public boolean isSameFile(Path path, Path path2) throws IOException {
    return delegate.isSameFile(path, path2);
  }
  @Override
  public void move(Path source, Path target, CopyOption... options) throws IOException {
    delegate.move(source, target, options);
  }
  @Override
  public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
    return delegate.newByteChannel(path, options, attrs);
  }
  @Override
  public DirectoryStream<Path> newDirectoryStream(Path dir, Filter<? super Path> filter) throws IOException {
    return delegate.newDirectoryStream(dir, filter);
  }
  @Override
  public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> attributes, LinkOption... options) throws IOException {
    return delegate.readAttributes(path, attributes, options);
  }
  @Override
  public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
    return delegate.readAttributes(path, attributes, options);
  }
  @Override
  public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
    delegate.setAttribute(path, attribute, value, options);
  }
}
