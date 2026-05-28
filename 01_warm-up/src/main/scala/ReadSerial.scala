// ADS I Class Project
// Chisel Introduction
//
// Chair of Electronic Design Automation, RPTU in Kaiserslautern
// File created on 18/10/2022 by Tobias Jauch (@tojauch)

package readserial

import chisel3._
import chisel3.util._


/** controller class */
class Controller extends Module{
  
  val io = IO(new Bundle {
    val rxd    = Input(UInt(1.W))   // serial input line
    val cnt_s  = Input(UInt(1.W))   // signal FROM counter: goes 1 when 8 bits counted
    val cnt_en = Output(UInt(1.W))  // signal TO counter: enables counting
    val valid  = Output(UInt(1.W))  // goes HIGH for 1 cycle when byte is ready
    })

  // internal register to track whether we are currently receiving
  // false = idle/waiting, true = receiving data bits
  val receiving = RegInit(false.B)  // starts as false (idle state)

  // state machine
  when(reset.asBool) {
    // reset: abort any transmission, go back to idle
    receiving := false.B

  } .elsewhen(io.cnt_s === 1.U) {
    // counter says 8 bits done → stop receiving, next cycle is idle
    receiving := false.B

  } .elsewhen(io.rxd === 0.U && !receiving) {
    // we are idle AND we see a start bit (0) → start receiving
    receiving := true.B

  }
  // otherwise stay in current state

  // cnt_en is 1 while we are receiving
  io.cnt_en := receiving

  // valid is 1 for exactly one cycle — when counter signals done
  io.valid  := io.cnt_s

}


/** counter class */
class Counter extends Module{
  
  val io = IO(new Bundle {
    val cnt_en = Input(UInt(1.W))   // enable signal FROM controller
    val cnt_s  = Output(UInt(1.W))  // done signal TO controller (1 when count reaches 8)
    })

  // internal register to hold the current count (needs to count 0 to 8, so 4 bits wide)
  val count = RegInit(0.U(4.W))   // starts at 0

  // state machine
  when(reset.asBool) {
    // reset: clear the counter
    count := 0.U

  } .elsewhen(io.cnt_en === 1.U) {
    // counting is enabled
    when(count === 8.U) {
      // reached 8 → reset counter back to 0 for next transmission
      count := 0.U
    } .otherwise {
      // keep counting up
      count := count + 1.U
    }
  } .otherwise {
    // not enabled → reset counter
    count := 0.U
  }

  // cnt_s goes HIGH when we have just counted the 8th bit
  io.cnt_s := (count === 8.U).asUInt


}

/** shift register class */
class ShiftRegister extends Module{
  
  val io = IO(new Bundle {
    val rxd    = Input(UInt(1.W))   // serial input: one bit per clock
    val cnt_en = Input(UInt(1.W))   // only shift when receiving
    val data   = Output(UInt(8.W))  // 8-bit parallel output
    })

  // internal 8-bit register to accumulate the received bits
  val shiftReg = RegInit(0.U(8.W))  // starts empty

  // functionality
  when(io.cnt_en === 1.U) {
    // shift left by 1, bring new bit in at the LSB
    // MSB is received first so after 8 shifts the oldest bit ends up at MSB
    shiftReg := Cat(shiftReg(6, 0), io.rxd)
  }

  io.data := shiftReg
}

/** 
  * The last warm-up task deals with a more complex component. Your goal is to design a serial receiver.
  * It scans an input line (“serial bus”) named rxd for serial transmissions of data bytes. A transmission 
  * begins with a start bit ‘0’ followed by 8 data bits. The most significant bit (MSB) is transmitted first. 
  * There is no parity bit and no stop bit. After the last data bit has been transferred a new transmission 
  * (beginning with a start bit, ‘0’) may immediately follow. If there is no new transmission the bus line 
  * goes high (‘1’, this is considered the “idle” bus signal). In this case the receiver waits until the next 
  * transmission begins. The outputs of the design are an 8-bit parallel data signal and a valid signal. 
  * The valid signal goes high (‘1’) for one clock cycle after the last serial bit has been transmitted, 
  * indicating that a new data byte is ready.
  */
class ReadSerial extends Module{
  val io = IO(new Bundle {
    val rxd   = Input(UInt(1.W))    // serial input line
    val valid = Output(UInt(1.W))   // 1 for one cycle when byte is ready
    val data  = Output(UInt(8.W))   // 8-bit received byte
  })

  // Instantiate the three sub-modules
  val controller    = Module(new Controller)
  val counter       = Module(new Counter)
  val shiftRegister = Module(new ShiftRegister)

  // Connect Controller
  controller.io.rxd   := io.rxd           // give controller the serial line
  controller.io.cnt_s := counter.io.cnt_s // controller listens to counter's done signal

  // Connect Counter
  counter.io.cnt_en := controller.io.cnt_en  // controller tells counter when to count

  // Connect Shift Register
  shiftRegister.io.rxd    := io.rxd                // shift register reads serial line
  shiftRegister.io.cnt_en := controller.io.cnt_en  // only shift when receiving

  // Global outputs
  io.valid := controller.io.valid   // valid comes from controller
  io.data  := shiftRegister.io.data // data comes from shift register
}
