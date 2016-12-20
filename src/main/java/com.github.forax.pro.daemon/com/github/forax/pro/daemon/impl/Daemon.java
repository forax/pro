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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import com.github.forax.pro.api.Config;
import com.github.forax.pro.api.Plugin;
import com.github.forax.pro.daemon.DaemonService;
import com.github.forax.pro.helper.Log;

public class Daemon implements DaemonService {
  static class Refresher {
    private boolean refresh;
    private final Object lock = new Object();
    
    public void waitARefresh() throws InterruptedException {
      synchronized(lock) {
        while(!refresh) {
          lock.wait();
        }
        refresh = false;
      }
    }
    
    public void notifyARefresh() {
      synchronized(lock) {
        refresh = true;
        lock.notify();
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
  private final Refresher refresher = new Refresher();
  
  // changed by the main thread
  private Thread thread;
  
  // changed at each run, mutated only by the daemon thread
  private Thread watcherThread;
  private List<Plugin> plugins = List.of();
  private Config config;
  
  private void mainLoop() {
    for(;;) {
      try {
        refresher.waitARefresh();
      } catch (InterruptedException e) {
        // command requested
        Runnable runnable;
        while((runnable = commandQueue.poll()) != null) {
          try {
            runnable.run();
          } catch(ExitException __) {
            return;
          }
        }
        
        continue;
      }
      
      // run plugins
      for(Plugin plugin: plugins) {
        int errorCode = execute(plugin, config);
        if (errorCode != 0) {
          break;
        } 
      }
    }
  }
  
  private static int execute(Plugin plugin, Config config) {
    int errorCode;
    try {
      errorCode = plugin.execute(config);
    } catch (IOException | /*UncheckedIOException |*/ RuntimeException e) {  //FIXME revisit RuntimeException !
      e.printStackTrace();
      String logLevel = config.get("loglevel", String.class).orElse("debug");
      Log log = Log.create(plugin.name(), logLevel);
      log.error(e);
      errorCode = 1; // FIXME
    }
    return errorCode;
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
  
  private static void watcherLoop(WatchService watcher, Refresher refresher, Log log, Set<Path> roots) {
    // scan roots
    HashMap<Path, Boolean> rootMap = new HashMap<>();
    for(Path root: roots) {
      boolean exists = Files.exists(root);
      if (exists) {
        registerSubDirectories(root, watcher, log);
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
      
      boolean modified = false;
      if (key != null) {

        do {
          for(WatchEvent<?> event: key.pollEvents()) {
            WatchKeyKind kind = WatchKeyKind.KIND_MAP.get(event.kind());
            switch(kind) {
            case DIRECTORY_CREATE:
            case DIRECTORY_MODIFY:
              Path localPath = (Path)event.context();
              Path directory = (Path)key.watchable();
              Path path = directory.resolve(localPath);
              log.verbose(path, p -> "create/modify path " + p);
              switch(kind) {
              case DIRECTORY_CREATE:
                if (Files.isDirectory(path)) {
                  registerSubDirectories(path, watcher, log);
                }
                modified = true;
                continue;
              case DIRECTORY_MODIFY:
                modified = true;
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
          registerSubDirectories(root, watcher, log);
          modified = true;
        }
        entry.setValue(exists);
      } 
      
      if (modified) {
        refresher.notifyARefresh();
      }
    }  
  }
  
  private static void registerSubDirectories(Path path, WatchService watcher, Log log) {
    try(Stream<Path> stream = Files.walk(path)) {
      stream
        .filter(Files::isDirectory)
        .forEach(directory -> {
          log.debug(directory, dir -> "register directory " + dir);
          
          try {
            directory.register(watcher, ENTRY_CREATE, ENTRY_MODIFY);
          } catch (IOException e) {
            log.error(e);
          }
         });
    } catch(IOException e) {
      log.error(e);
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
  
  
  
  @Override
  public void run(List<Plugin> plugins, Config config) {
    if (thread == null) {
      throw new IllegalStateException("no thread was started");
    }
    
    if (plugins.isEmpty()) {  // do nothing
      return;
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
      
      HashSet<Path> roots = new HashSet<>();
      plugins.get(0).watch(config, roots::add);
      
      // start a new watcher thread
      Log log = Log.create("daemon", config.get("loglevel", String.class).orElse("debug"));
      Thread watcherThread = new Thread(() -> watcherLoop(watcher, refresher, log, roots));
      watcherThread.start();
      
      this.plugins = plugins;
      this.config = config;
      this.watcherThread = watcherThread;
    });
  }
}
