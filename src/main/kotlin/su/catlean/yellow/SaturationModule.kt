package su.catlean.yellow

import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier
import su.catlean.api.addon.feature.AddonModule
import su.catlean.yellow.mixin.GameRendererAccessor
import kotlin.math.roundToInt

/**
 * Applies a world-wide saturation post effect.
 *
 * CatLean's 0.1.3 addon API exposes basic boolean/float settings, but it does not
 * expose conditional visibility for settings. The slider is therefore always
 * listed in the module settings; the effect itself only runs when the toggle is on.
 */
object SaturationModule : AddonModule("Saturation", "visuals", listOf("saturation")) {
    private var worldSaturation by setting("Насыщенность мира", false)
    private var saturationValue by setting("Насыщенность", 1.35f, 0.0f..3.0f)

    private val effectId = Identifier.fromNamespaceAndPath("yellow", "saturation")

    private var effectApplied = false
    private var previousEffect: Identifier? = null

    /** Called from the PostPass mixin to update the shader uniform without reloading the world. */
    @JvmStatic
    fun saturationAmount(): Float {
        // The requested slider step is 0.05. CatLean API 0.1.3 stores a float range only,
        // so quantize the value here to preserve that step in the rendered result.
        return ((saturationValue * 20.0f).roundToInt() / 20.0f).coerceIn(0.0f, 3.0f)
    }

    override fun onPlayerUpdate() {
        val minecraft = Minecraft.getInstance()
        val renderer = minecraft.gameRenderer

        if (minecraft.player == null || minecraft.level == null || !worldSaturation) {
            restorePreviousEffect()
            return
        }

        if (!effectApplied) {
            previousEffect = renderer.currentPostEffect()
            effectApplied = true
        }

        if (renderer.currentPostEffect() != effectId) {
            (renderer as Any as GameRendererAccessor).invokeSetPostEffect(effectId)
        }
    }

    override fun onDisable() {
        restorePreviousEffect()
    }

    private fun restorePreviousEffect() {
        if (!effectApplied) return

        val renderer = Minecraft.getInstance().gameRenderer
        if (renderer.currentPostEffect() == effectId) {
            val previous = previousEffect
            if (previous != null) {
                (renderer as Any as GameRendererAccessor).invokeSetPostEffect(previous)
            } else {
                renderer.clearPostEffect()
            }
        }

        previousEffect = null
        effectApplied = false
    }
}
