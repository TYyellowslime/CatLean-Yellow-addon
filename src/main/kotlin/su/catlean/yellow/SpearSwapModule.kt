package su.catlean.yellow

import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket
import net.minecraft.tags.ItemTags
import su.catlean.api.addon.feature.AddonModule
import su.catlean.api.event.events.client.InputEvent
import su.catlean.api.event.events.client.TickEvent
import su.catlean.gofra.Flow

/**
 * Performs a quick hotbar swap to a spear and then restores the exact slot
 * that was selected before the key was pressed.
 *
 * The key is configurable through an enum setting because CatLean Addon API
 * 0.1.3 does not expose a dedicated keybind setting type.
 */
enum class SpearSwapKey(val keyCode: Int) {
    A(65), B(66), C(67), D(68), E(69), F(70), G(71), H(72), I(73),
    J(74), K(75), L(76), M(77), N(78), O(79), P(80), Q(81), R(82),
    S(83), T(84), U(85), V(86), W(87), X(88), Y(89), Z(90),
    NUM_0(48), NUM_1(49), NUM_2(50), NUM_3(51), NUM_4(52),
    NUM_5(53), NUM_6(54), NUM_7(55), NUM_8(56), NUM_9(57),
    SPACE(32), LEFT_SHIFT(340), LEFT_CONTROL(341), LEFT_ALT(342)
}

object SpearSwapModule : AddonModule("SpearSwap", "misc", listOf("spearswap", "spear-swap")) {
    private var activationKey by setting("Клавиша", SpearSwapKey.G)
    private var returnDelayTicks by setting("Задержка возврата (тики)", 1, 1..5)

    private var originalSlot: Int? = null
    private var ticksUntilReturn = 0
    private var swapPlayer: LocalPlayer? = null

    @Flow
    private fun onInput(event: InputEvent) {
        if (event.device != InputEvent.Device.Keyboard || event.action != InputEvent.Action.Press) return
        if (event.key != activationKey.keyCode) return

        val minecraft = Minecraft.getInstance()
        if (minecraft.gui.screen() != null) return

        val player = minecraft.player ?: return
        if (originalSlot != null) return // Do not start another swap until the previous one is restored.

        val selectedSlot = player.inventory.selectedSlot
        val spearSlot = (0 until 9).firstOrNull { slot ->
            player.inventory.getItem(slot).`is`(ItemTags.SPEARS)
        }

        if (spearSlot == null) {
            player.sendOverlayMessage(Component.literal("SpearSwap: копьё не найдено в хотбаре."))
            return
        }

        if (spearSlot == selectedSlot) return

        originalSlot = selectedSlot
        swapPlayer = player
        ticksUntilReturn = returnDelayTicks.coerceAtLeast(1)
        selectHotbarSlot(player, spearSlot)
    }

    @Flow
    @Suppress("UNUSED_PARAMETER")
    private fun onClientTick(event: TickEvent) {
        val slotToRestore = originalSlot ?: return
        val minecraft = Minecraft.getInstance()
        val player = minecraft.player

        // Never apply a pending slot from an old world/player to a new session.
        if (player == null || player !== swapPlayer) {
            clearPendingSwap()
            return
        }

        ticksUntilReturn--
        if (ticksUntilReturn <= 0) {
            selectHotbarSlot(player, slotToRestore)
            clearPendingSwap()
        }
    }

    override fun onDisable() {
        restoreOriginalSlot()
    }

    private fun restoreOriginalSlot() {
        val slot = originalSlot ?: return
        val minecraft = Minecraft.getInstance()
        val player = minecraft.player
        if (player != null && player === swapPlayer) {
            selectHotbarSlot(player, slot)
        }
        clearPendingSwap()
    }

    private fun selectHotbarSlot(player: LocalPlayer, slot: Int) {
        val safeSlot = slot.coerceIn(0, 8)
        player.inventory.selectedSlot = safeSlot
        // Send immediately so both sides observe the swap even when the return is only one tick later.
        player.connection.send(ServerboundSetCarriedItemPacket(safeSlot))
    }

    private fun clearPendingSwap() {
        originalSlot = null
        ticksUntilReturn = 0
        swapPlayer = null
    }
}
