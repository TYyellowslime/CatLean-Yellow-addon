package su.catlean.yellow

import net.minecraft.client.Minecraft
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec3
import su.catlean.api.addon.feature.AddonAuraRotation
import su.catlean.api.addon.feature.Rotation
import java.io.File
import kotlin.math.atan2
import kotlin.math.round
import kotlin.math.sqrt
import kotlin.random.Random

object NeuroRotation : AddonAuraRotation() {
    private const val MAX_DATASET_SAMPLES = 100_000
    private val dataset = mutableListOf<NeuroSample>()

    fun loadDataset(name: String): Boolean {
        val safeName = sanitizeDatasetName(name) ?: return false

        return try {
            val directory = File(Minecraft.getInstance().gameDirectory, "neuro_datasets")
            val file = File(directory, "$safeName.csv")
            if (!file.isFile) return false

            val loaded = mutableListOf<NeuroSample>()
            file.useLines(Charsets.UTF_8) { lines ->
                lines.drop(1).take(MAX_DATASET_SAMPLES).forEach { line ->
                    val parts = line.split(',')
                    if (parts.size < 5) return@forEach

                    val targetYaw = parts[0].toFloatOrNull() ?: return@forEach
                    val targetPitch = parts[1].toFloatOrNull() ?: return@forEach
                    val distance = parts[2].toFloatOrNull() ?: return@forEach
                    val actualYaw = parts[3].toFloatOrNull() ?: return@forEach
                    val actualPitch = parts[4].toFloatOrNull() ?: return@forEach

                    if (distance >= 0f && listOf(targetYaw, targetPitch, distance, actualYaw, actualPitch).all { it.isFinite() }) {
                        loaded += NeuroSample(targetYaw, targetPitch, distance, actualYaw, actualPitch)
                    }
                }
            }

            if (loaded.isEmpty()) return false

            dataset.clear()
            dataset.addAll(loaded)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun getGcd(): Float {
        val sensitivity = Minecraft.getInstance().options.sensitivity().get().toFloat()
        val factor = sensitivity * 0.6f + 0.2f
        return factor * factor * factor * 8.0f * 0.15f
    }

    private fun applyGcd(current: Rotation, targetYaw: Float, targetPitch: Float): Rotation {
        val gcd = getGcd()
        if (!gcd.isFinite() || gcd <= 0.0001f) {
            return Rotation(targetYaw, targetPitch.coerceIn(-90f, 90f))
        }

        val deltaYaw = Mth.wrapDegrees(targetYaw - current.yaw)
        val deltaPitch = targetPitch - current.pitch
        val fixedYaw = current.yaw + round(deltaYaw / gcd) * gcd
        val fixedPitch = (current.pitch + round(deltaPitch / gcd) * gcd).coerceIn(-90f, 90f)

        return Rotation(fixedYaw, fixedPitch)
    }

    override fun getRotation(
        target: Entity,
        currentRotation: Rotation,
        attackTick: Boolean
    ): Rotation {
        val point = getPoint(target)
        val eye = player.eyePosition

        val dx = point.x - eye.x
        val dy = point.y - eye.y
        val dz = point.z - eye.z

        val horizontalDistance = sqrt(dx * dx + dz * dz)
        val totalDistance = eye.distanceTo(point).toFloat()
        val desiredYaw = Mth.wrapDegrees(Math.toDegrees(atan2(dz, dx)) - 90.0).toFloat()
        val desiredPitch = (-Math.toDegrees(atan2(dy, horizontalDistance))).toFloat()

        val targetDeltaYaw = Mth.wrapDegrees(desiredYaw - currentRotation.yaw)
        val targetDeltaPitch = desiredPitch - currentRotation.pitch

        if (dataset.isNotEmpty()) {
            val nearest = dataset
                .asSequence()
                .map { sample ->
                    val diffYaw = Mth.wrapDegrees(sample.targetDeltaYaw - targetDeltaYaw)
                    val diffPitch = sample.targetDeltaPitch - targetDeltaPitch
                    val diffDistance = (sample.distance - totalDistance) * 4.0f
                    val score = diffYaw * diffYaw + diffPitch * diffPitch + diffDistance * diffDistance
                    sample to score
                }
                .sortedBy { it.second }
                .take(5)
                .toList()

            if (nearest.isNotEmpty()) {
                var totalWeight = 0f
                var weightedYaw = 0f
                var weightedPitch = 0f

                for ((sample, score) in nearest) {
                    val weight = 1.0f / (score + 0.05f)
                    totalWeight += weight
                    weightedYaw += sample.actualDeltaYaw * weight
                    weightedPitch += sample.actualDeltaPitch * weight
                }

                if (totalWeight > 0f && totalWeight.isFinite()) {
                    val baseYawStep = weightedYaw / totalWeight
                    val basePitchStep = weightedPitch / totalWeight
                    val blend = if (attackTick) 0.45f else 0.25f
                    val stepYaw = baseYawStep * (1f - blend) + (targetDeltaYaw * 0.42f) * blend
                    val stepPitch = basePitchStep * (1f - blend) + (targetDeltaPitch * 0.35f) * blend

                    return applyGcd(
                        currentRotation,
                        currentRotation.yaw + stepYaw,
                        currentRotation.pitch + stepPitch
                    )
                }
            }
        }

        val jitterYaw = Random.nextFloat() * 0.4f - 0.2f
        val jitterPitch = Random.nextFloat() * 0.3f - 0.15f
        val fallbackYaw = currentRotation.yaw + targetDeltaYaw * 0.38f + jitterYaw
        val fallbackPitch = currentRotation.pitch + targetDeltaPitch * 0.32f + jitterPitch

        return applyGcd(currentRotation, fallbackYaw, fallbackPitch)
    }

    override fun getPoint(target: Entity): Vec3 = target.boundingBox.center

    override fun needRayCast() = true

    override fun needExtend() = true
}
