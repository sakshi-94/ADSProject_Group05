package readserial
import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class ReadSerialTester extends AnyFlatSpec with ChiselScalatestTester {

  "ReadSerial" should "work" in {
    test(new ReadSerial).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

      // send one bit and advance one clock cycle
      def sendBit(bit: Int): Unit = {
        dut.io.rxd.poke(bit.U)
        dut.clock.step(1)
      }

      // send start bit + 8 data bits MSB first
      // valid appears on the SAME cycle as the last bit is clocked in
      // so we check valid immediately after sendByte returns
      def sendByte(byte: Int): Unit = {
        sendBit(0)  // start bit
        for (i <- 7 to 0 by -1) {
          val bit = (byte >> i) & 1
          sendBit(bit)
        }
      }

      // Test 1: Basic transmission — send 0xA5 (1010_0101 = 165)
      dut.io.rxd.poke(1.U)   // bus starts idle
      dut.clock.step(2)

      sendByte(0xA5)

      // valid appears on the same cycle as the last bit
      // NO extra clock.step() needed
      dut.io.valid.expect(1.U)
      dut.io.data.expect(0xA5.U)

      dut.clock.step(1)
      dut.io.valid.expect(0.U)  // valid only high for 1 cycle

      // Test 2: Send 0x00 (all zeros)
      dut.io.rxd.poke(1.U)
      dut.clock.step(2)

      sendByte(0x00)

      dut.io.valid.expect(1.U)
      dut.io.data.expect(0x00.U)

      dut.clock.step(1)
      dut.io.valid.expect(0.U)

      // Test 3: Send 0xFF (all ones)
      dut.io.rxd.poke(1.U)
      dut.clock.step(2)

      sendByte(0xFF)

      dut.io.valid.expect(1.U)
      dut.io.data.expect(0xFF.U)

      dut.clock.step(1)
      dut.io.valid.expect(0.U)

      // Test 4: Two transmissions back to back (no idle gap)
      dut.io.rxd.poke(1.U)
      dut.clock.step(2)

      sendByte(0x55)   // 0101_0101
      dut.io.valid.expect(1.U)
      dut.io.data.expect(0x55.U)

      // go idle for one cycle so controller resets to idle state
      // before it can detect the next start bit
      dut.io.rxd.poke(1.U)
      dut.clock.step(1)

      sendByte(0xAA)   // 1010_1010
      dut.io.valid.expect(1.U)
      dut.io.data.expect(0xAA.U)
      // immediately send next byte — no idle gap
      sendByte(0xAA)   // 1010_1010
      dut.io.valid.expect(1.U)
      dut.io.data.expect(0xAA.U)

      // Test 5: Reset during transmission
      dut.io.rxd.poke(1.U)
      dut.clock.step(2)

      // start a transmission then reset mid-way
      sendBit(0)   // start bit
      sendBit(1)   // a few data bits
      sendBit(0)
      sendBit(1)

      // trigger reset
      dut.reset.poke(true.B)
      dut.clock.step(1)
      dut.io.valid.expect(0.U)   // valid must be 0 after reset
      dut.reset.poke(false.B)

      // clean transmission after reset
      dut.io.rxd.poke(1.U)
      dut.clock.step(2)

      sendByte(0x3C)   // 0011_1100
      dut.io.valid.expect(1.U)
      dut.io.data.expect(0x3C.U)
    }
  }
}