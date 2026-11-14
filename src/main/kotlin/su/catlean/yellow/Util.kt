package su.catlean.yellow

import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.network.chat.Component

val player: LocalPlayer
    get() = requireNotNull(Minecraft.getInstance().player) { "Player is not available" }

val world: ClientLevel
    get() = requireNotNull(Minecraft.getInstance().level) { "World is not available" }

fun message(s: Any) {
    Minecraft.getInstance().player?.sendSystemMessage(Component.literal(s.toString()))
}
