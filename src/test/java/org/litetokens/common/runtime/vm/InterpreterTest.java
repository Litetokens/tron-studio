/*
 * java-litetokens is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * java-litetokens is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.litetokens.common.runtime.vm;

import static org.junit.Assert.assertTrue;

import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import java.io.*;
import org.litetokens.core.config.args.Args;
import org.litetokens.protos.Protocol.Transaction;
import org.litetokens.common.runtime.vm.program.InternalTransaction;
import org.litetokens.common.runtime.vm.VM;
import org.litetokens.common.runtime.vm.program.Program;
import org.litetokens.common.runtime.vm.program.invoke.ProgramInvokeMockImpl;

@Slf4j
public class InterpreterTest {

  private ProgramInvokeMockImpl invoke;
  private Program program;

  @BeforeClass
  public static void init() {
    Args.getInstance().setDebug(true);
  }

  @Test
  public void testVMException() {
    VM vm = new VM();
    invoke = new ProgramInvokeMockImpl();
    byte[] op = { 0x5b, 0x60, 0x00, 0x56 };
    // 0x5b      - JUMPTEST
    // 0x60 0x00 - PUSH 0x00
    // 0x56      - JUMP to 0
    Transaction xlt = Transaction.getDefaultInstance();
    InternalTransaction interXlt = new InternalTransaction(xlt);
    program = new Program(op, invoke, interXlt);

    boolean result = false;

    try {
      while (!program.isStopped()) {
        vm.step(program);
      }
    } catch (Program.OutOfEnergyException e) {
      result = true;
    }

    assertTrue(result);
  }

  @Test
  public void JumpSingleOperation() {
    VM vm = new VM();
    invoke = new ProgramInvokeMockImpl();
    byte[] op = { 0x56 };
    // 0x56      - JUMP
    Transaction xlt = Transaction.getDefaultInstance();
    InternalTransaction interXlt = new InternalTransaction(xlt);
    program = new Program(op, invoke, interXlt);

    boolean result = false;

    try {
      while (!program.isStopped()) {
        vm.step(program);
      }
    } catch (Program.StackTooSmallException e) {
      // except to get stack too small exception for Jump
      result = true;
    }

    assertTrue(result);
  }

  @Test
  public void JumpToInvalidDestination() {
    VM vm = new VM();
    invoke = new ProgramInvokeMockImpl();
    byte[] op = { 0x60, 0x20,0x56 };
    // 0x60      - PUSH1
    // 0x20      - 20
    // 0x56      - JUMP
    Transaction xlt = Transaction.getDefaultInstance();
    InternalTransaction interXlt = new InternalTransaction(xlt);
    program = new Program(op, invoke, interXlt);

    boolean result = false;

    try {
      while (!program.isStopped()) {
        vm.step(program);
      }
    } catch (Program.BadJumpDestinationException e) {
      // except to get BadJumpDestinationException for Jump
      Assert.assertTrue(e.getMessage().contains("Operation with pc isn't 'JUMPDEST': PC[32];"));
      result = true;
    }

    assertTrue(result);
  }

  @Test
  public void JumpToLargeNumberDestination() {
    VM vm = new VM();
    invoke = new ProgramInvokeMockImpl();
    byte[] op = { 0x64, 0x7f, 0x7f, 0x7f, 0x7f, 0x7f,0x56 };
    // 0x60              - PUSH5
    // 0x7F7F7F7F7F      - 547599908735
    // 0x56              - JUMP
    Transaction xlt = Transaction.getDefaultInstance();
    InternalTransaction interXlt = new InternalTransaction(xlt);
    program = new Program(op, invoke, interXlt);

    boolean result = false;

    try {
      while (!program.isStopped()) {
        vm.step(program);
      }
    } catch (Program.BadJumpDestinationException e) {
      // except to get BadJumpDestinationException for Jump
      Assert.assertTrue(e.getMessage().contains("Operation with pc isn't 'JUMPDEST': PC[-1];"));
      result = true;
    }

    assertTrue(result);
  }

  @AfterClass
  public static void destroy(){
    Args.clearParam();
  }
}