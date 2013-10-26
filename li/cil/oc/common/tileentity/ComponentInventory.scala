package li.cil.oc.common.tileentity

import li.cil.oc.api.driver
import li.cil.oc.api.network.environment.ManagedEnvironment
import li.cil.oc.server.driver.Registry
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.world.World

trait ComponentInventory extends Inventory with Environment with Persistable {
  protected val components = Array.fill[Option[ManagedEnvironment]](getSizeInventory)(None)

  def world: World

  // ----------------------------------------------------------------------- //

  def installedMemory = inventory.foldLeft(0)((sum, stack) => sum + (stack match {
    case Some(item) => Registry.driverFor(item) match {
      case Some(driver: driver.Memory) => driver.amount(item)
      case _ => 0
    }
    case _ => 0
  }))

  // ----------------------------------------------------------------------- //

  override def onConnect() {
    super.onConnect()
    components collect {
      case Some(environment) => node.network.connect(node, environment.node)
    }
  }

  override def onDisconnect() {
    super.onDisconnect()
    components collect {
      case Some(environment) => environment.node.network.remove(environment.node)
    }
  }

  // ----------------------------------------------------------------------- //

  override def load(nbt: NBTTagCompound) = {
    super.load(nbt) // Load items before we can read their tags.
    inventory.zipWithIndex collect {
      case (Some(stack), slot) => (stack, slot)
    } foreach {
      case (stack, slot) =>
        components(slot) = Registry.driverFor(stack) match {
          case Some(driver) =>
            Option(driver.createEnvironment(stack)) match {
              case Some(environment) =>
                environment.load(driver.nbt(stack))
                Some(environment)
              case _ => None
            }
          case _ => None
        }
    }
  }

  override def save(nbt: NBTTagCompound) = {
    inventory.zipWithIndex collect {
      case (Some(stack), slot) => (stack, slot)
    } foreach {
      case (stack, slot) => components(slot) match {
        case Some(environment) =>
          // We're guaranteed to have a driver for entries.
          environment.save(Registry.driverFor(stack).get.nbt(stack))
        case _ => // Nothing special to save.
      }
    }
    super.save(nbt) // Save items after updating their tags.
  }

  // ----------------------------------------------------------------------- //

  def getInventoryStackLimit = 1

  override protected def onItemAdded(slot: Int, item: ItemStack) = if (!world.isRemote) {
    Registry.driverFor(item) match {
      case Some(driver) => Option(driver.createEnvironment(item)) match {
        case Some(environment) =>
          components(slot) = Some(environment)
          environment.load(driver.nbt(item))
          node.network.connect(node, environment.node)
        case _ => // No environment (e.g. RAM).
      }
      case _ => // No driver.
    }
  }

  override protected def onItemRemoved(slot: Int, item: ItemStack) = if (!world.isRemote) {
    // Uninstall component previously in that slot.
    components(slot) match {
      case Some(environment) =>
        // Note to self: we have to remove the node from the network *before*
        // saving, to allow file systems to close their handles before they
        // are saved (otherwise hard drives would restore all handles after
        // being installed into a different computer, even!)
        components(slot) = None
        environment.node.network.remove(environment.node)
        Registry.driverFor(item).foreach(driver => environment.save(driver.nbt(item)))
      case _ => // Nothing to do.
    }
  }
}