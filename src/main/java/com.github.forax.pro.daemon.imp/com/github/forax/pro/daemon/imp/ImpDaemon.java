package com.github.forax.pro.daemon.imp;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
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

import com.github.forax.pro.api.Config;
import com.github.forax.pro.api.Plugin;
import com.github.forax.pro.api.Command;
import com.github.forax.pro.api.helper.ProConf;
import com.github.forax.pro.daemon.Daemon;
import com.github.forax.pro.helper.Log;

/**
 * A small and simple implementation of a daemon.
 */
public class ImpDaemon implements Daemon {
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
  
  private static final ImpDaemon INSTANCE = new ImpDaemon();
  
  private ImpDaemon() {
    // enforce singleton
  }
  
  /**
   * Returns the singleton Daemon.
   * @return the singleton Daemon.
   */
  public static ImpDaemon provider() {
    return INSTANCE;
  }
  
  // non mutable
  private final ArrayBlockingQueue<Runnable> commandQueue = new ArrayBlockingQueue<>(64);
  private final Refresher refresher = new Refresher();
  
  // changed by the main thread
  private Thread thread;
  
  // changed at each run, mutated only by the daemon thread
  private Thread watcherThread;
  private List<Command> tasks = List.of();
  private Config config;
  
  private void mainLoop() {
    for(;;) {
      try {
        refresher.waitARefresh();
      } catch (@SuppressWarnings("unused") InterruptedException e) {
        // command requested
        Runnable runnable;
        while((runnable = commandQueue.poll()) != null) {
          try {
            runnable.run();
          } catch(@SuppressWarnings("unused") ExitException __) {
            return;
          }
        }
        
        continue;
      }
      
      // run tasks
      var errorCode = 0;
      for(var task: tasks) {
        errorCode = execute(task, config);
        if (errorCode != 0) {
          break;
        } 
      }
      if (errorCode == 0) {
        var log = Log.create("daemon", config.getOrThrow("pro", ProConf.class).loglevel());
        log.info(null, __ -> "DONE !");
      }
    }
  }
  
  private static int execute(Command task, Config config) {
    try {
      return task.execute(config);
    } catch (IOException | /*UncheckedIOException |*/ RuntimeException e) {  //FIXME revisit RuntimeException !
      e.printStackTrace();
      var logLevel = config.get("loglevel", String.class).orElse("debug");
      var log = Log.create(task.name(), logLevel);
      log.error(e);
      return 1; // FIXME
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
  
  private static void watcherLoop(WatchService watcher, Refresher refresher, Log log, Set<Path> roots) {
    // scan roots
    var rootMap = new HashMap<Path, Boolean>();
    for(var root: roots) {
      var exists = Files.exists(root);
      if (exists) {
        registerSubDirectories(root, watcher, log);
      }
      rootMap.put(root, exists);
    }
    
    WatchKey key;
    for(;;) {
      try {
        key = watcher.poll(15, TimeUnit.SECONDS);
      } catch (@SuppressWarnings("unused") InterruptedException e) {
        try {
          watcher.close();
        } catch(@SuppressWarnings("unused") IOException __) {
          // do nothing
        }
        return;
      }
      
      var modified = false;
      if (key != null) {

        do {
          for(var event: key.pollEvents()) {
            var kind = WatchKeyKind.KIND_MAP.get(event.kind());
            switch(kind) {
            case DIRECTORY_CREATE:
            case DIRECTORY_MODIFY: {
              var localPath = (Path)event.context();
              var directory = (Path)key.watchable();
              var path = directory.resolve(localPath);
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
            }
            default:
              throw new AssertionError("invalid kind " + kind);
            }
          }
          key.reset();

          try {
            key = watcher.poll(1, TimeUnit.SECONDS);
          } catch (@SuppressWarnings("unused") InterruptedException e) {
            try {
              watcher.close();
            } catch(@SuppressWarnings("unused") IOException __) {
              // do nothing
            }
            return;
          }

        } while(key != null);

      }
      
      // scan roots
      for(var entry: rootMap.entrySet()) {
        var root = entry.getKey();
        var available = entry.getValue();
        var exists = Files.exists(root);
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
    try(var stream = Files.walk(path)) {
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
      super(null, null, false, false);
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
  public void execute(List<Command> tasks, Config config) {
    if (thread == null) {
      throw new IllegalStateException("no thread was started");
    }
    
    // find first plugin
    var firstOpt = tasks.stream().filter(Plugin.class::isInstance).map(Plugin.class::cast).findFirst();
    if (!firstOpt.isPresent()) {  // do nothing
      return;
    }
    var firstPlugin = firstOpt.get();
    
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
      
      var roots = new HashSet<Path>();
      firstPlugin.watch(config, roots::add);
      
      // start a new watcher thread
      var log = Log.create("daemon", config.getOrThrow("pro", ProConf.class).loglevel());
      Thread watcherThread = new Thread(() -> watcherLoop(watcher, refresher, log, roots));
      watcherThread.start();
      
      this.tasks = tasks;
      this.config = config;
      this.watcherThread = watcherThread;
    });
  }
}
