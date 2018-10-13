package com.github.forax.pro.plugin.modulefixer.impl;

import static org.objectweb.asm.Opcodes.ASM7;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.TypePath;

public final class EmptyClassVisitor extends ClassVisitor {
  static final AnnotationVisitor EMPTY_ANNOTATION_VISITOR = new AnnotationVisitor(ASM7) {
    @Override
    public AnnotationVisitor visitAnnotation(String name, String desc) {
      return this;
    }
  };
  
  private static final FieldVisitor EMPTY_FIELD_VISITOR = new FieldVisitor(ASM7) {
    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
      return EMPTY_ANNOTATION_VISITOR;
    }
    @Override
    public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
      return EMPTY_ANNOTATION_VISITOR;
    }
  };
  
  private static final MethodVisitor EMPTY_METHOD_VISITOR = new MethodVisitor(ASM7) {
    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
      return EMPTY_ANNOTATION_VISITOR;
    }
    @Override
    public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
      return EMPTY_ANNOTATION_VISITOR;
    }
    @Override
    public AnnotationVisitor visitAnnotationDefault() {
      return EMPTY_ANNOTATION_VISITOR;
    }
    @Override
    public AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
      return EMPTY_ANNOTATION_VISITOR;
    }
    @Override
    public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start,
        Label[] end, int[] index, String desc, boolean visible) {
      return EMPTY_ANNOTATION_VISITOR;
    }
    @Override
    public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
      return EMPTY_ANNOTATION_VISITOR;
    }
    @Override
    public AnnotationVisitor visitTryCatchAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
      return EMPTY_ANNOTATION_VISITOR;
    }
  };
  
  private static final EmptyClassVisitor EMPTY_CLASS_VISITOR = new EmptyClassVisitor();
  
  public static EmptyClassVisitor getInstance() {
    return EMPTY_CLASS_VISITOR;
  }
  
  private EmptyClassVisitor() {
    super(ASM7);
  }

  @Override
  public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
    return EMPTY_ANNOTATION_VISITOR;
  }

  @Override
  public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
    return EMPTY_FIELD_VISITOR;
  }

  @Override
  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    return EMPTY_METHOD_VISITOR;
  }
}