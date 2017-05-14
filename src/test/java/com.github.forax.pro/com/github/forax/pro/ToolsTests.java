package com.github.forax.pro;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.List;
import org.junit.jupiter.api.Test;

@SuppressWarnings("static-method")
class ToolsTests {

  @Test
  void fileName() throws Exception {
    assertAll("file name is `test.txt`",
        () -> assertEquals("test.txt", Tools.fileName(new URI("http://example.com/a/b/c/test.txt"))),
        () -> assertEquals("test.txt", Tools.fileName(new URI("https://example.com/a/b/c/test.txt"))),
        () -> assertEquals("test.txt", Tools.fileName(new URI("http://example.com/test.txt?p=v"))),
        () -> assertEquals("test.txt", Tools.fileName(new URI("http://example.com/test.txt#anchor"))),
        () -> assertEquals("test.txt", Tools.fileName(new URI("http://example.com/test.txt?p=v#tag"))),
        () -> assertEquals("", Tools.fileName(new URI("http://example.com/#/z/test.txt?a=b")))
    );
  }

  @Test
  void downloadUsingFileProtocol() throws Exception {
    Path tmp = Files.createTempDirectory("pro-download-file-");
    Path obj = Files.createTempFile("pro-download", "-obj");
    List<String> lines = List.of("line 1", "line 2", "line 3");
    Files.write(obj, lines);
    FileTime nineK = FileTime.fromMillis(9000);
    Files.setLastModifiedTime(obj, nineK);
    Path ect = Tools.download(obj.toUri(), tmp, "ect", __ -> true);
    assertEquals("ect", ect.getFileName().toString());
    assertEquals(nineK, Files.getLastModifiedTime(ect));
    assertLinesMatch(lines, Files.readAllLines(ect));
  }

  @Test
  void downloadUsingHttpsProtocol() throws Exception {
    Path temp = Files.createTempDirectory("pro-download-https-");
    Path free = Tools.download(new URI("https://www.gnu.org/licenses/fdl.txt"), temp);
    assertEquals("fdl.txt", free.getFileName().toString());
    assertTrue(Files.readAllLines(free).stream().anyMatch(line -> line.contains("free")));
  }

  // could be removed if https://github.com/junit-team/junit5/issues/845 is addressed
  private <T> T error(String message) {
    fail(message);
    return null;
  }

  @Test
  void downloadOnlyLoadsOnce() throws Exception {
    Path temp = Files.createTempDirectory("pro-download-once-");
    Path one = Tools.download(new URI("https://www.gnu.org/licenses/fdl.txt"), temp, "one", path -> error("one"));
    BasicFileAttributes oneBasics = Files.readAttributes(one, BasicFileAttributes.class);
    Path two = Tools.download(new URI("https://www.gnu.org/licenses/fdl.txt"), temp, "two", path -> error("two"));
    BasicFileAttributes twoBasics = Files.readAttributes(two, BasicFileAttributes.class);
    assertEquals(oneBasics.size(), twoBasics.size());
    assertEquals(oneBasics.lastModifiedTime(), twoBasics.lastModifiedTime());
    FileTime twoCreationTime = twoBasics.creationTime();
    // download to "two" again - should trigger "useTimeStamp" predicate evaluation
    boolean[] useTimeStamp = { false };
    Path three = Tools.download(new URI("https://www.gnu.org/licenses/fdl.txt"), temp, "two", path -> {
      useTimeStamp[0] = true;
      return true;
    });
    assertTrue(useTimeStamp[0], "useTimeStamp not evaluated");
    BasicFileAttributes threeBasics = Files.readAttributes(three, BasicFileAttributes.class);
    assertEquals(two, three);
    assertEquals(oneBasics.size(), threeBasics.size());
    assertEquals(oneBasics.lastModifiedTime(), threeBasics.lastModifiedTime());
    assertEquals(twoCreationTime, threeBasics.creationTime());
    // reset last modified time to the future -- will trigger download
    Files.setLastModifiedTime(two, FileTime.fromMillis(Long.MAX_VALUE - 1));
    Path four = Tools.download(new URI("https://www.gnu.org/licenses/fdl.txt"), temp, "two", path -> error("four"));
    BasicFileAttributes fourBasics = Files.readAttributes(four, BasicFileAttributes.class);
    assertEquals(two, four);
    assertEquals(oneBasics.size(), fourBasics.size());
    assertEquals(oneBasics.lastModifiedTime(), fourBasics.lastModifiedTime());
  }

  @Test
  void copyCompareAndDeleteTree() throws Exception {
    // create tree
    Path tmp = Files.createTempDirectory("pro-deleteTree-");
    Path tm2 = Files.createTempDirectory(tmp,"nested-");
    Path txt = Files.createTempFile(tm2, "test",".txt");
    assertTrue(Files.exists(txt));
    // copy tree
    Path cpy = Files.createTempDirectory("pro-copyTree-").resolve("target");
    Tools.copyTree(tmp, cpy);
    assertTrue(Tools.compareTree(tmp, cpy));
    // delete tree
    Tools.deleteTree(tmp, false);
    assertTrue(Files.exists(tmp));
    assertFalse(Files.exists(tm2));
    assertFalse(Files.exists(txt));
    Tools.deleteTree(tmp, true);
    assertFalse(Files.exists(tmp));
    // delete copy
    Tools.deleteTree(cpy, true);
    assertFalse(Files.exists(cpy));
  }

  @Test
  void compare() {
    Path deps = Paths.get("deps");
    assertTrue(Tools.compareTree(deps, deps));
    Path plugins = Paths.get("plugins");
    assertFalse(Tools.compareTree(deps, plugins));
  }
}
