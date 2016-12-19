package com.github.forax.pro.daemon.impl;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.forax.pro.api.Plugin;
import com.github.forax.pro.daemon.DaemonService;
import com.github.forax.pro.daemon.impl.Daemon.WorkQueue.Result;

public class Daemon implements DaemonService {
  static class PathRegistry {
    private final HashMap<Path, Set<Plugin>> pathRegistry = new HashMap<>();  
    
    public void register(Path path, Plugin plugin) {
      pathRegistry.computeIfAbsent(path, __ -> new HashSet<>()).add(plugin);
    }
    
    public Set<Plugin> findInRegistry(Path path) {
      for(Path parent = path; parent != null; parent = parent.getParent()) {
        Set<Plugin> plugins = pathRegistry.get(parent);
        if (plugins != null) {
          return plugins;
        }
      }
      return Set.of();
    }

    public Set<Path> paths() {
      return pathRegistry.keySet();
    }
  }
  
  static class WorkQueue<E> {
    private boolean overflow;
    private final LinkedHashSet<E> set = new LinkedHashSet<>();
    
    public enum Result { OVERFLOW, NORMAL }
    
    public void put(E element) {
      synchronized(set) {
        if (set.add(element) && set.size() == 1) {
          set.notify();
        }
      }
    }
    
    public void overflow() {
      synchronized(set) {
        overflow = true;
        set.clear();
        set.notify();
      }
    }
    
    public Result drain(List<? super E> dest) throws InterruptedException {
      synchronized(set) {
        while (!overflow && set.isEmpty()) {
          set.wait();
        }
        if (overflow) {
          overflow = false;
          return Result.OVERFLOW;
        }
        dest.addAll(set);
        set.clear();
        return Result.NORMAL;
      }
    }
  }
  
  private static final Daemon INSTANCE = new Daemon();
  
  Daemon() {
    // enforce singleton
  }
  
  public static Daemon provider() {
    return INSTANCE;
  }
  
  // non mutable
  private final ArrayBlockingQueue<Runnable> commandQueue = new ArrayBlockingQueue<>(64);
  private final WorkQueue<Path> pathQueue = new WorkQueue<>();
  
  // changed by the main thread
  private Thread thread;
  
  // changed at each run, mutated only by the daemon thread
  private WatchService watcher;
  private Thread watcherThread;
  private List<Plugin> plugins = List.of();
  private PluginFacade pluginFacade;
  private PathRegistry pathRegistry = new PathRegistry();
  
  private void mainLoop() {
    ArrayList<Path> paths = new ArrayList<>();
    LinkedHashSet<Plugin> scheduledPlugins = new LinkedHashSet<>();
    for(;;) {
      Result result;
      try {
        result = pathQueue.drain(paths);
      } catch (InterruptedException e) {
        // command requested
        result = null;
      }
      
      if (result == Result.OVERFLOW) {
        // TODO
        continue;
      }
      
      // paths modified
      if (result == Result.NORMAL) {
        paths.forEach(path -> {
          System.out.println("mainLoop: path " + path);
          Set<Plugin> contextPlugins = pathRegistry.findInRegistry(path);
          scheduledPlugins.addAll(contextPlugins);
        });
        paths.clear();
      }
      
      // commands
      Runnable runnable;
      while((runnable = commandQueue.poll()) != null) {
        try {
          runnable.run();
        } catch(ExitException __) {
          return;
        }
      }
      
      System.out.println("scheduledPlugins " + scheduledPlugins);
      
      // run plugins
      if(!scheduledPlugins.isEmpty()) {
        List<Plugin> selectedPlugins = plugins.stream().filter(scheduledPlugins::contains).collect(Collectors.toList());
        scheduledPlugins.clear();
        for(Plugin plugin: selectedPlugins) {
          int errorCode = pluginFacade.execute(plugin);
          if (errorCode != 0) {
            break;
          }
        }
      }
    }
  }
  
  
  enum WatchKeyKind {
    DIRECTORY_CREATE,
    DIRECTORY_MODIFY,
    ;

    static final Map<Kind<?>, WatchKeyKind> KIND_MAP =
        Map.of(
            ENTRY_CREATE, DIRECTORY_CREATE,
            ENTRY_MODIFY, DIRECTORY_MODIFY);
  }
  
  private static void watcherLoop(WatchService watcher, WorkQueue<Path> pathQueue, Set<Path> roots) {
    // scan roots
    HashMap<Path, Boolean> rootMap = new HashMap<>();
    for(Path root: roots) {
      boolean exists = Files.exists(root);
      if (exists) {
        registerSubDirectories(root, watcher);
      }
      rootMap.put(root, exists);
    }
    
    WatchKey key;
    for(;;) {
      try {
        key = watcher.poll(15, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        try {
          watcher.close();
        } catch(IOException __) {
          // do nothing
        }
        return;
      }
      
      if (key != null) {

        do {
          System.out.println("watch " + key);
          for(WatchEvent<?> event: key.pollEvents()) {
            WatchKeyKind kind = WatchKeyKind.KIND_MAP.get(event.kind());
            switch(kind) {
            case DIRECTORY_CREATE:
            case DIRECTORY_MODIFY:
              Path localPath = (Path)event.context();
              Path directory = (Path)key.watchable();
              Path path = directory.resolve(localPath);
              System.out.println("path " + path);
              switch(kind) {
              case DIRECTORY_CREATE:
                if (Files.isDirectory(path)) {
                  System.out.println("register new directory " + path);
                  registerSubDirectories(path, watcher);
                  pathQueue.put(path); 
                }
                continue;
              case DIRECTORY_MODIFY:
                pathQueue.put(path);
                continue;
              default:
                throw new AssertionError("invalid kind " + kind);
              }
            default:
              throw new AssertionError("invalid kind " + kind);
            }
          }
          key.reset();

          try {
            key = watcher.poll(1, TimeUnit.SECONDS);
          } catch (InterruptedException e) {
            try {
              watcher.close();
            } catch(IOException __) {
              // do nothing
            }
            return;
          }

        } while(key != null);

      }
      
      // scan roots
      for(Map.Entry<Path, Boolean> entry: rootMap.entrySet()) {
        Path root = entry.getKey();
        boolean available = entry.getValue();
        boolean exists = Files.exists(root);
        if (!available && exists) {
          System.out.println("create root directory " + root);
          registerSubDirectories(root, watcher);
          pathQueue.put(root); 
        }
        entry.setValue(exists);
      } 
    }  
  }
  
  @Override
  public void start() {
    if (thread != null) {
      throw new IllegalStateException("a thread is alreday running");
    }
    thread = new Thread(this::mainLoop);
    thread.start();
  }
  
  @Override
  public boolean isStarted() {
    return thread != null;
  }
  
  @SuppressWarnings("serial")
  static class ExitException extends RuntimeException {
    ExitException() {
      // empty
    }
  }
  
  private void send(Runnable runnable) {
    commandQueue.offer(runnable);
    thread.interrupt();
  }
  
  @Override
  public void stop() {
    if (thread == null) {
      throw new IllegalStateException("no thread is running");
    }
    send(() -> {
      // stop watcher thread
      if (watcherThread != null) {
        watcherThread.interrupt();
        join(watcherThread);
      }
      watcher = null;
      watcherThread = null;
      
      // stop daemon thread
      throw new ExitException();
    });
    
    join(thread); 
    thread = null;
  }
  
  private static void join(Thread thread) {
    try {
      thread.join();
    } catch (InterruptedException e) {
      throw new AssertionError(e);
    }
  }
  
  private static void registerSubDirectories(Path path, WatchService watcher) {
    try(Stream<Path> stream = Files.walk(path)) {
      stream
        .filter(Files::isDirectory)
        .forEach(directory -> {
          System.out.println("register directory " + directory);
          
          try {
            directory.register(watcher, ENTRY_CREATE, ENTRY_MODIFY);
          } catch (IOException e) {
            System.err.println("watcher: " + e);
          }
          
         });
    } catch(IOException e) {
      System.err.println("watcher: " + e);
    }
  }
  
  @Override
  public void run(List<Plugin> plugins, PluginFacade pluginFacade) {
    if (thread == null) {
      throw new IllegalStateException("no thread was started");
    }
    
    send(() -> {
      // stop watcher thread
      if (this.watcherThread != null) {
        this.watcherThread.interrupt();
        join(watcherThread);
      }
      this.watcherThread = null;
      
      WatchService watcher;
      try {
        watcher = FileSystems.getDefault().newWatchService();
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
      
      PathRegistry pathRegistry = new PathRegistry();
      plugins.forEach(plugin -> pluginFacade.watch(plugin, path -> pathRegistry.register(path, plugin)));
      
      // start a new watcher thread
      Thread watcherThread = new Thread(() -> watcherLoop(watcher, pathQueue, pathRegistry.paths()));
      watcherThread.start();
      
      this.plugins = plugins;
      this.pluginFacade = pluginFacade;
      this.pathRegistry = pathRegistry;
      this.watcher = watcher;
      this.watcherThread = watcherThread;
    });
  }
}
