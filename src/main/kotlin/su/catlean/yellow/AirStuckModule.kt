package su.catlean.yellow

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import su.catlean.api.addon.feature.AddonModule
import su.catlean.api.event.events.network.SendPacket
import su.catlean.api.event.events.player.PlayerUpdateEvent
import su.catlean.gofra.Flow

object AirStuckModule : AddonModule("AirStuck", "movement", listOf("airstuck")) {

    private var height by setting("Height", 2.0f, 0.1f..30.0f)

    private fun isHighEnough(): Boolean {
        if (player.onGround()) return false

        val pos = player.position()
        val startY = pos.y
        val blockX = Mth.floor(pos.x)
        val blockZ = Mth.floor(pos.z)
        val startBlockY = Mth.floor(startY)
        val endBlockY = Mth.floor(startY - height)

        val blockPos = BlockPos.MutableBlockPos()

        for (y in startBlockY downTo endBlockY) {
            blockPos.set(blockX, y, blockZ)
            val state = world.getBlockState(blockPos)
            val shape = state.getCollisionShape(world, blockPos)

            if (!shape.isEmpty) {
                val blockTopY = y + shape.max(Direction.Axis.Y)
                val distance = startY - blockTopY
                if (distance >= 0.0) {
                    return distance >= height
                }
            }
        }

        return true
    }

    @Flow
    private fun onPacketSend(event: SendPacket) {
        if (Minecraft.getInstance().player == null || Minecraft.getInstance().level == null) return

        if (event.packet is ServerboundMovePlayerPacket && isHighEnough()) {
            event.cancel()
        }
    }

    @Flow
    @Suppress("UNUSED_PARAMETER")
    private fun onPlayerUpdate(event: PlayerUpdateEvent) {
        if (Minecraft.getInstance().player == null || Minecraft.getInstance().level == null) return

        if (isHighEnough()) {
            player.deltaMovement = Vec3.ZERO
            player.abilities.flying = false
            player.resetFallDistance()
        }
    }

    override fun onDisable() {
        if (Minecraft.getInstance().player != null) {
            player.deltaMovement = Vec3.ZERO
        }
    }
}

