// remove "open" from module header and use: exports integration.pro to junit.platform.commons;
open module integration.pro {
  requires junit.jupiter.api;
  requires junit.platform.commons;
  requires opentest4j;

  requires com.github.forax.pro;
  requires com.github.forax.pro.aether;
}
