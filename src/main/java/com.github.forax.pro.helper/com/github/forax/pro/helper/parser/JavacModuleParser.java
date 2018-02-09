package com.github.forax.pro.helper.parser;


import static org.objectweb.asm.Opcodes.ACC_OPEN;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACC_TRANSITIVE;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.UndeclaredThrowableException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.objectweb.asm.ModuleVisitor;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExportsTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.ModuleTree;
import com.sun.source.tree.ModuleTree.ModuleKind;
import com.sun.source.tree.OpensTree;
import com.sun.source.tree.ProvidesTree;
import com.sun.source.tree.RequiresTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TreeVisitor;
import com.sun.source.tree.UsesTree;
import com.sun.source.util.JavacTask;

@SuppressWarnings("restriction")
public class JavacModuleParser { 
  static class ModuleHandler {
    private static final String[] EMPTY_ARRAY = new String[0];
    
    private final ModuleClassVisitor moduleClassVisitor;
    private ModuleVisitor mv;
    
    ModuleHandler(ModuleClassVisitor moduleClassVisitor) {
      this.moduleClassVisitor = moduleClassVisitor;
    }
    
    private static void accept(TreeVisitor<?, ?> visitor, Tree node) {
      node.accept(visitor, null);
    }
    
    private static String qualifiedString(Tree tree) {
      switch (tree.getKind()) {
      case IDENTIFIER:
          return ((IdentifierTree) tree).getName().toString();
      case MEMBER_SELECT:
        MemberSelectTree select = (MemberSelectTree) tree;
        return qualifiedString(select.getExpression()) + '.' + select.getIdentifier().toString();
      default:
          throw new AssertionError(tree.toString());
      }
    }
    
    private static String[] toArray(List<? extends ExpressionTree> trees) {
      if (trees == null) {
        return EMPTY_ARRAY;
      }
      return trees.stream().map(ModuleHandler::qualifiedString).toArray(String[]::new);
    }
    
    @SuppressWarnings("static-method")
    public void visitCompilationUnit(CompilationUnitTree node, TreeVisitor<?, ?> visitor) {
      for(Tree decl: node.getTypeDecls()) {
        if (!(decl instanceof ModuleTree)) {  // skip unnecessary nodes: imports, etc
          continue;
        }
        accept(visitor, decl);
      }
    }
    
    public void visitModule(ModuleTree node, TreeVisitor<?, ?> visitor) {
      String name = qualifiedString(node.getName());
      int flags = node.getModuleType() == ModuleKind.OPEN? ACC_OPEN: 0;
      
      mv = moduleClassVisitor.visitModule(name, flags, null);
      
      node.getDirectives().forEach(n -> accept(visitor, n));
    }

    public void visitRequires(RequiresTree node, @SuppressWarnings("unused") TreeVisitor<?, ?> __) {
      int modifiers = (node.isStatic()? ACC_STATIC: 0) | (node.isTransitive()? ACC_TRANSITIVE: 0);
      mv.visitRequire(qualifiedString(node.getModuleName()), modifiers, null);
    }
    
    public void visitExports(ExportsTree node, @SuppressWarnings("unused") TreeVisitor<?, ?> __) {
      mv.visitExport(qualifiedString(node.getPackageName()), 0, toArray(node.getModuleNames()));
    }

    public void visitOpens(OpensTree node, @SuppressWarnings("unused") TreeVisitor<?, ?> __) {
      mv.visitOpen(qualifiedString(node.getPackageName()), 0, toArray(node.getModuleNames()));
    }
    
    public void visitUses(UsesTree node, @SuppressWarnings("unused") TreeVisitor<?, ?> __) {
      mv.visitUse(qualifiedString(node.getServiceName()));
    }
    
    public void visitProvides(ProvidesTree node, @SuppressWarnings("unused") TreeVisitor<?, ?> __) {
      mv.visitProvide(qualifiedString(node.getServiceName()), toArray(node.getImplementationNames()));
    }
    
    
    interface Visitee {
      void visit(ModuleHandler handler, Tree node, TreeVisitor<?, ?> visitor);
      
      static Visitee of(Method method) {
        return (handler, node, visitor) -> {
          try {
            method.invoke(handler, node, visitor);
          } catch (IllegalAccessException e) {
            throw new AssertionError(e);
          } catch(InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
              throw (RuntimeException)cause;
            }
            if (cause instanceof Error) {
              throw (Error)cause;
            }
            throw new UndeclaredThrowableException(cause);
          }
        };
      }
    }
    
    static final Map<String, Visitee> METHOD_MAP;
    static {
      METHOD_MAP =
        Arrays.stream(ModuleHandler.class.getMethods())
        .filter(m -> m.getDeclaringClass() == ModuleHandler.class)
        .collect(Collectors.toMap(Method::getName, ModuleHandler.Visitee::of));
    }
  }
  
  public static void parse(Path moduleInfoPath, ModuleClassVisitor moduleClassVisitor) throws IOException {
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    try(StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)) {
      Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjects(moduleInfoPath);
      CompilationTask task = compiler.getTask(null, fileManager, null, null, null, compilationUnits);
      JavacTask javacTask = (JavacTask)task;
      Iterable<? extends CompilationUnitTree> units= javacTask.parse();
      CompilationUnitTree unit = units.iterator().next();

      ModuleHandler moduleHandler = new ModuleHandler(moduleClassVisitor);
      TreeVisitor<?,?> visitor = (TreeVisitor<?,?>)Proxy.newProxyInstance(TreeVisitor.class.getClassLoader(), new Class<?>[]{ TreeVisitor.class},
          (proxy, method, args) -> {
            ModuleHandler.METHOD_MAP
            .getOrDefault(method.getName(), (handler, node, v) -> { 
              throw new AssertionError("invalid node " + node.getClass());
            })
            .visit(moduleHandler, (Tree)args[0], (TreeVisitor<?,?>)proxy);
            return null;
          });

      unit.accept(visitor, null);
    }
  }
}
