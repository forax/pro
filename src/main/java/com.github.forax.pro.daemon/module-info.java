module com.github.forax.pro.daemon {
  requires com.github.forax.pro.api;
  requires com.github.forax.pro.helper;
  exports com.github.forax.pro.daemon;
  
  provides com.github.forax.pro.daemon.DaemonService
    with com.github.forax.pro.daemon.impl.Daemon;
}