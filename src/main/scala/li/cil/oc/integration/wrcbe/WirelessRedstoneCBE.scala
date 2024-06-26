package li.cil.oc.integration.wrcbe

import codechicken.wirelessredstone.core.RedstoneEther
import li.cil.oc.integration.util.WirelessRedstone.WirelessRedstoneSystem
import li.cil.oc.server.component.RedstoneWireless

import scala.collection.JavaConversions.asScalaBuffer
import scala.language.reflectiveCalls

object WirelessRedstoneCBE extends WirelessRedstoneSystem {
  def addTransmitter(rs: RedstoneWireless) {
    if (rs.wirelessOutput && rs.wirelessFrequency > 0) {
      RedstoneEther.server.addTransmittingDevice(rs)
    }
  }

  def removeTransmitter(rs: RedstoneWireless) {
    if (rs.wirelessFrequency > 0) {
      RedstoneEther.server.removeTransmittingDevice(rs)
    }
  }

  def addReceiver(rs: RedstoneWireless) {
    RedstoneEther.server.addReceivingDevice(rs)
    if (rs.wirelessFrequency > 0) {
      rs.wirelessInput = RedstoneEther.server.isFreqOn(rs.wirelessFrequency)
    }
  }

  def removeReceiver(rs: RedstoneWireless) {
    RedstoneEther.server.removeReceivingDevice(rs)
  }

  def updateOutput(rs: RedstoneWireless) {
    if (rs.wirelessOutput) {
      addTransmitter(rs)
    }
    else {
      removeTransmitter(rs)
    }
  }

  def getInput(rs: RedstoneWireless) = rs.wirelessInput

  def resetRedstone(rs: RedstoneWireless): Unit = {
    val ff = RedstoneEther.server.getTransmittingDevicesOnFreq(rs.wirelessFrequency).filter(f => {
      (f.getPosition() == rs.getPosition()) && (f.getDimension() == rs.getDimension)
    }).foreach {
      RedstoneEther.server.removeTransmittingDevice(_)
    }
  }
}
