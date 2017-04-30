package com.github.forax.pro.plugin.modulefixer.impl;

import static org.objectweb.asm.Opcodes.ASM6;
import static org.objectweb.asm.Opcodes.D2L;
import static org.objectweb.asm.Opcodes.DADD;
import static org.objectweb.asm.Opcodes.DALOAD;
import static org.objectweb.asm.Opcodes.DCONST_0;
import static org.objectweb.asm.Opcodes.DCONST_1;
import static org.objectweb.asm.Opcodes.DDIV;
import static org.objectweb.asm.Opcodes.DMUL;
import static org.objectweb.asm.Opcodes.DNEG;
import static org.objectweb.asm.Opcodes.DREM;
import static org.objectweb.asm.Opcodes.DSUB;
import static org.objectweb.asm.Opcodes.F2D;
import static org.objectweb.asm.Opcodes.F2L;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.I2D;
import static org.objectweb.asm.Opcodes.I2L;
import static org.objectweb.asm.Opcodes.INVOKEDYNAMIC;
import static org.objectweb.asm.Opcodes.L2D;
import static org.objectweb.asm.Opcodes.LADD;
import static org.objectweb.asm.Opcodes.LALOAD;
import static org.objectweb.asm.Opcodes.LAND;
import static org.objectweb.asm.Opcodes.LCONST_0;
import static org.objectweb.asm.Opcodes.LCONST_1;
import static org.objectweb.asm.Opcodes.LDC;
import static org.objectweb.asm.Opcodes.LDIV;
import static org.objectweb.asm.Opcodes.LMUL;
import static org.objectweb.asm.Opcodes.LNEG;
import static org.objectweb.asm.Opcodes.LOR;
import static org.objectweb.asm.Opcodes.LREM;
import static org.objectweb.asm.Opcodes.LSHL;
import static org.objectweb.asm.Opcodes.LSHR;
import static org.objectweb.asm.Opcodes.LSUB;
import static org.objectweb.asm.Opcodes.LUSHR;
import static org.objectweb.asm.Opcodes.LXOR;
import static org.objectweb.asm.Opcodes.MULTIANEWARRAY;

import java.util.List;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.analysis.Interpreter;
import org.objectweb.asm.tree.analysis.Value;

public final class ConstInterpreter extends Interpreter<ConstInterpreter.ConstValue> {
  public static final class ConstValue implements Value {
    static final ConstValue ONE_SLOT = new ConstValue("", 1);
    static final ConstValue TWO_SLOT = new ConstValue("", 2);
    
    static ConstValue slot(int size) { return size == 1? ONE_SLOT: TWO_SLOT; }
    static ConstValue string(String constString) { return new ConstValue(constString, 1); }
    
    final String constString;
    final int slot;

    private ConstValue(String constString, int slot) {
      this.constString = constString;
      this.slot = slot;
    }
    
    @Override
    public int getSize() {
      return slot;
    }
    
    public String getConstString() {
      return constString;
    }
  }
  
  public ConstInterpreter() {
    super(ASM6);
  }

  @Override
  public ConstValue newValue(Type type) {
    if (type == Type.VOID_TYPE) {
      return null;
    }
    return type == null? ConstValue.ONE_SLOT:  ConstValue.slot(type.getSize());
  }

  @Override
  public ConstValue newOperation(AbstractInsnNode insn) {
    switch (insn.getOpcode()) {
    case LCONST_0:
    case LCONST_1:
    case DCONST_0:
    case DCONST_1:
      return ConstValue.TWO_SLOT;
    case LDC:
      Object cst = ((LdcInsnNode) insn).cst;
      if (cst instanceof Type) {  // constant class
        return ConstValue.string(((Type)cst).getInternalName());
      }
      return cst instanceof Long || cst instanceof Double ? ConstValue.TWO_SLOT: ConstValue.ONE_SLOT;
    case GETSTATIC:
      return ConstValue.slot(Type.getType(((FieldInsnNode) insn).desc).getSize());
    default:
      return ConstValue.ONE_SLOT;
    }
  }

  @Override
  public ConstValue copyOperation(AbstractInsnNode insn, ConstValue value) {
    return value;
  }

  @Override
  public ConstValue unaryOperation(AbstractInsnNode insn, ConstValue value) {
    switch (insn.getOpcode()) {
    case LNEG:
    case DNEG:
    case I2L:
    case I2D:
    case L2D:
    case F2L:
    case F2D:
    case D2L:
      return ConstValue.TWO_SLOT;
    case GETFIELD:
      return ConstValue.slot(Type.getType(((FieldInsnNode) insn).desc).getSize());
    default:
      return ConstValue.ONE_SLOT;
    }
  }

  @Override
  public ConstValue binaryOperation(AbstractInsnNode insn, ConstValue value1, ConstValue value2) {
    switch (insn.getOpcode()) {
    case LALOAD:
    case DALOAD:
    case LADD:
    case DADD:
    case LSUB:
    case DSUB:
    case LMUL:
    case DMUL:
    case LDIV:
    case DDIV:
    case LREM:
    case DREM:
    case LSHL:
    case LSHR:
    case LUSHR:
    case LAND:
    case LOR:
    case LXOR:
      return ConstValue.TWO_SLOT;
    default:
      return ConstValue.ONE_SLOT;
    }
  }

  @Override
  public ConstValue ternaryOperation(AbstractInsnNode insn, ConstValue value1, ConstValue value2, ConstValue value3) {
    return ConstValue.ONE_SLOT;
  }

  @Override
  public ConstValue naryOperation(AbstractInsnNode insn, List<? extends ConstValue> values) {
    int opcode = insn.getOpcode();
    if (opcode == MULTIANEWARRAY) {
      return ConstValue.ONE_SLOT;
    } else {
      String desc = (opcode == INVOKEDYNAMIC) ? ((InvokeDynamicInsnNode) insn).desc
          : ((MethodInsnNode) insn).desc;
      return ConstValue.slot(Type.getReturnType(desc).getSize());
    }
  }

  @Override
  public void returnOperation(AbstractInsnNode insn, ConstValue value, ConstValue expected) {
    // empty
  }

  @Override
  public ConstValue merge(ConstValue d, ConstValue w) {
    if (d.constString == "" || w.constString == "") {
      return ConstValue.slot(Math.min(d.slot, w.slot));
    }
    if (d.constString.equals(w.constString)) {
      return d;
    }
    return ConstValue.ONE_SLOT;
  }
}