module com.github.forax.pro.daemon.imp {
  requires com.github.forax.pro.daemon;
  requires com.github.forax.pro.api;
  requires com.github.forax.pro.helper;
  
  provides com.github.forax.pro.daemon.Daemon
    with com.github.forax.pro.daemon.imp.ImpDaemon;
}