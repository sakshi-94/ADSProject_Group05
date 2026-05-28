// ADS I Class Project
// Chisel Introduction
//
// Chair of Electronic Design Automation, RPTU in Kaiserslautern
// File created on 18/10/2022 by Tobias Jauch (@tojauch)

package adder

import chisel3._
import chisel3.util._


/** 
  * Half Adder Class 
  * 
  * Your task is to implement a basic half adder as presented in the lecture.
  * Each signal should only be one bit wide (inputs and outputs).
  * There should be no delay between input and output signals, we want to have
  * a combinational behaviour of the component.
  */
class HalfAdder extends Module {

  val io = IO(new Bundle {
    val a = Input(UInt(1.W))
    val b = Input(UInt(1.W))
    val s = Output(UInt(1.W))
    val co = Output(UInt(1.W))
  })
  io.s := io.a ^ io.b
  io.co := io.a & io.b
}

/** 
  * Full Adder Class 
  * 
  * Your task is to implement a basic full adder. The component's behaviour should 
  * match the characteristics presented in the lecture. In addition, you are only allowed 
  * to use two half adders (use the class that you already implemented) and basic logic 
  * operators (AND, OR, ...).
  * Each signal should only be one bit wide (inputs and outputs).
  * There should be no delay between input and output signals, we want to have
  * a combinational behaviour of the component.
  */
class FullAdder extends Module{

  val io = IO(new Bundle {
    val a = Input(UInt(1.W))
    val b = Input(UInt(1.W))
    val ci = Input(UInt(1.W))
    val s = Output(UInt(1.W))
    val co = Output(UInt(1.W))
    })
  val ha1 = Module(new HalfAdder)
  val ha2 = Module(new HalfAdder)

  ha1.io.a := io.a //module input a is connected to ha1's input a
  ha1.io.b := io.b //module input b is connected to ha1's input b

  ha2.io.a := ha1.io.s //ha1's sum output is connected to ha2's input a
  ha2.io.b := io.ci // ci iss connected to ha2's input b

  io.s := ha2.io.s

  io.co := ha1.io.co | ha2.io.co
}

/** 
  * 4-bit Adder class 
  * 
  * Your task is to implement a 4-bit ripple-carry-adder. The component's behaviour should 
  * match the characteristics presented in the lecture.  Remember: An n-bit adder can be 
  * build using one half adder and n-1 full adders.
  * The inputs and the result should all be 4-bit wide, the carry-out only needs one bit.
  * There should be no delay between input and output signals, we want to have
  * a combinational behaviour of the component.
  */
class FourBitAdder extends Module{

  val io = IO(new Bundle {
    val a = Input(UInt(4.W))
    val b = Input(UInt(4.W))
    val result = Output(UInt(4.W))
    val co_3 = Output(UInt(1.W))
    })

  val ha = Module(new HalfAdder)
  ha.io.a := io.a(0)
  ha.io.b := io.b(0)
  val s0 = ha.io.s
  val co_0 = ha.io.co

  val fa1 = Module(new FullAdder)
  fa1.io.a := io.a(1)
  fa1.io.b := io.b(1)
  fa1.io.ci := co_0
  val s1 = fa1.io.s
  val co_1 = fa1.io.co

  val fa2 = Module(new FullAdder)
  fa2.io.a := io.a(2)
  fa2.io.b := io.b(2)
  fa2.io.ci := co_1
  val s2 = fa2.io.s
  val co_2 = fa2.io.co

  val fa3 = Module(new FullAdder)
  fa3.io.a := io.a(3)
  fa3.io.b := io.b(3)
  fa3.io.ci := co_2
  val s3 = fa3.io.s

  io.result := Cat(s3, s2, s1, s0)
  io.co_3 := fa3.io.co
}
