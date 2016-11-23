package com.github.forax.pro.helper.parser;

import static com.github.forax.pro.helper.ModuleVisitor.ACC_OPEN;
import static com.github.forax.pro.helper.ModuleVisitor.ACC_STATIC;
import static com.github.forax.pro.helper.ModuleVisitor.ACC_TRANSITIVE;
import static com.github.forax.pro.helper.parser.ModuleParser.Operator.COMMA;
import static com.github.forax.pro.helper.parser.ModuleParser.Operator.EOF;
import static com.github.forax.pro.helper.parser.ModuleParser.Operator.IDENTIFIER;
import static com.github.forax.pro.helper.parser.ModuleParser.Operator.LCURLY;
import static com.github.forax.pro.helper.parser.ModuleParser.Operator.MODULE;
import static com.github.forax.pro.helper.parser.ModuleParser.Operator.RCURLY;
import static com.github.forax.pro.helper.parser.ModuleParser.Operator.SEMICOLON;
import static com.github.forax.pro.helper.parser.ModuleParser.Operator.WITH;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.github.forax.pro.helper.ModuleVisitor;

//@Deprecated
public class ModuleParser {
  enum Operator {
    IDENTIFIER,
    EOF,
    
    LCURLY("{"),
    RCURLY("}"),
    SEMICOLON(";"),
    COMMA(","),
    
    OPEN,
    TRANSITIVE,
    STATIC,
    
    MODULE,
    REQUIRES,
    EXPORTS,
    TO,
    OPENS,
    USES,
    PROVIDES,
    WITH
    ;
    
    private final String symbol;
    
    private Operator() {
      this.symbol = name().toLowerCase();
    }
    private Operator(String symbol) {
      this.symbol = symbol;
    }
    
    static Operator match(String token) {
      //System.out.println("match " + token);
      return MAP.getOrDefault(token, IDENTIFIER);
    }
    
    private static final Map<String, Operator> MAP;
    static {
      MAP = Arrays.stream(Operator.values())
          .skip(2)
          .collect(Collectors.toMap(op -> op.symbol, op -> op));
    }
  }
  
  private String token;
  private Operator operator;
  
  private final Reader reader;
  private final ModuleVisitor visitor;
  private final CharBuffer buffer = CharBuffer.allocate(8192);
  
  private ModuleParser(Reader reader, ModuleVisitor visitor) {
    this.reader = reader;
    this.visitor = visitor;
    buffer.limit(0);
  }

  private Operator nextToken() throws IOException {
    StringBuilder builder = new StringBuilder();
    boolean inSpace = true;
    for(;;) {
      if (!buffer.hasRemaining()) {
        buffer.clear();
        if (reader.read(buffer) == -1) {
          if (!inSpace) {
            token = builder.toString();
            return operator = Operator.match(token);
          }
          return operator = EOF;
        }
        buffer.flip();  
      }
      
      while(buffer.hasRemaining()) {
        char c = buffer.get();
        switch(c) {
        case ' ': case '\t': case '\r': case '\n':
          if (inSpace) {
            continue;
          }
          token = builder.toString();
          builder.setLength(0);
          break;
          
        case '{': case '}': case ';': case ',':
          if (!inSpace) {
            buffer.position(buffer.position() - 1);
            token = builder.toString();
            builder.setLength(0);
          } else {
            token = "" + c;
          }
          break;
          
        default:
          inSpace = false;
          builder.append(c);
          continue;
        }
        
        inSpace = true;
        return operator = Operator.match(token);
      }
    }
  }
  
  private void ensureToken(Operator op) {
    if (op != operator) {
      throw new IllegalStateException("parse error " + operator + " '" + token + "' but ask for " + op);
    }
  }
  
  public static void parse(Path moduleInfoPath, ModuleVisitor visitor) throws IOException {
    try(BufferedReader reader = Files.newBufferedReader(moduleInfoPath)) {
      parse(reader, visitor); 
    }
  }
  
  public static void parse(Reader reader, ModuleVisitor visitor) throws IOException {
    ModuleParser moduleParser = new ModuleParser(reader, visitor);
    moduleParser.parseModule();
  }

  private int parseModifiers() throws IOException {
    int modifiers = 0;
    for(;;) {
      switch(operator) {
      case OPEN:
        modifiers |= ACC_OPEN;
        break;
      case TRANSITIVE:
        modifiers |= ACC_TRANSITIVE;
        break;
      case STATIC:
        modifiers |= ACC_STATIC;
        break;
      default:
        return modifiers;
      }
      nextToken();
    }
  }
  
  private void parseModule() throws IOException {
    nextToken();
    int modifiers = parseModifiers();
    ensureToken(MODULE);
    nextToken();
    ensureToken(IDENTIFIER);
    visitor.visitModule(modifiers, token);
    nextToken();
    ensureToken(LCURLY);
    nextToken();
    
    for(;;) {
      switch(operator) {
      case REQUIRES:
        parseRequires();
        break;
      case EXPORTS:
        parseExportsOrOpens(ModuleVisitor::visitExports);
        break;
      case OPENS:
        parseExportsOrOpens(ModuleVisitor::visitOpens);
        break;
      case USES:
        parseUses();
        break;
      case PROVIDES:
        parseProvides();
        break;
      default:
        ensureToken(RCURLY);
        return;
      }
      
      nextToken();
      continue;
    }
  }
  
  private void parseRequires() throws IOException {
    nextToken();
    int modifiers = parseModifiers();
    ensureToken(IDENTIFIER);
    String name = token;
    nextToken();
    ensureToken(SEMICOLON);
    visitor.visitRequires(modifiers, name);
  }
  
  interface Visitee {
    void visit(ModuleVisitor visitor, String name, List<String> list);
  }
  
  private void parseExportsOrOpens(Visitee visitee) throws IOException {
    nextToken();
    ensureToken(IDENTIFIER);
    String packaze = token;
    nextToken();
    switch(operator) {
    case TO:
      nextToken();
      List<String> restricted = parseStringList();
      visitee.visit(visitor, packaze, restricted);
      return;
      
    default:
      ensureToken(SEMICOLON);
      visitee.visit(visitor, packaze, List.of());
    }
  }
  
  private void parseUses() throws IOException {
    nextToken();
    ensureToken(IDENTIFIER);
    visitor.visitUses(token);
    nextToken();
    ensureToken(SEMICOLON);
  }
  
  private void parseProvides() throws IOException {
    nextToken();
    ensureToken(IDENTIFIER);
    String service = token;
    nextToken();
    ensureToken(WITH);
    nextToken();
    List<String> providers = parseStringList();
    visitor.visitProvides(service, providers);
  }
  
  private List<String> parseStringList() throws IOException {
    ArrayList<String> list = new ArrayList<>();
    for(;;) {
      ensureToken(IDENTIFIER);
      list.add(token);
      nextToken();
      if (operator == COMMA) {
        nextToken();
        continue;
      }
      ensureToken(SEMICOLON);
      return list;
    }
  }
}
