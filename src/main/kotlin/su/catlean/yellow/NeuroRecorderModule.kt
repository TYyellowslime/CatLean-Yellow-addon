package su.catlean.yellow

import net.minecraft.client.Minecraft
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.EntityHitResult
import su.catlean.api.addon.feature.AddonCommandContext
import su.catlean.api.event.events.player.PlayerUpdateEvent
import su.catlean.gofra.Flow
import java.io.File
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Event subscriber used by the Neuro commands.
 * It is subscribed once by YellowAddon and records only while recording is enabled.
 */
object NeuroRecorderModule {
    private const val DEFAULT_DATASET_NAME = "default"
    private const val MAX_SAMPLES = 100_000

    private data class RotationSnapshot(val yaw: Float, val pitch: Float)

    private val samples = ArrayList<NeuroSample>()
    private var recording = false
    private var previousRotation: RotationSnapshot? = null
    private var previousTarget: Entity? = null

    fun toggleRecord(context: AddonCommandContext) {
        recording = !recording
        previousRotation = null
        previousTarget = null

        if (recording) {
            samples.clear()
            context.message("§a[Neuro] §fЗапись включена. Наводитесь на цель и двигайте камерой.")
        } else {
            context.message("§e[Neuro] §fЗапись остановлена. Сэмплов: §e${samples.size}")
            context.message("§7Сохранить датасет: §f^neuro-save")
        }
    }

    fun saveDefaultDataset(context: AddonCommandContext) {
        val safeName = sanitizeDatasetName(DEFAULT_DATASET_NAME)
        if (safeName == null) {
            context.message("§c[Neuro] §fНекорректное имя датасета.")
            return
        }

        if (samples.isEmpty()) {
            context.message("§c[Neuro] §fНет данных для сохранения. Сначала запустите §e^neuro§f.")
            return
        }

        try {
            val directory = File(Minecraft.getInstance().gameDirectory, "neuro_datasets")
            if (!directory.exists() && !directory.mkdirs()) {
                context.message("§c[Neuro] §fНе удалось создать папку neuro_datasets.")
                return
            }

            val output = File(directory, "$safeName.csv")
            val snapshot = samples.toList()
            output.bufferedWriter(Charsets.UTF_8).use { writer ->
                writer.appendLine("targetDeltaYaw,targetDeltaPitch,distance,actualDeltaYaw,actualDeltaPitch")
                snapshot.forEach { sample ->
                    writer.append(sample.targetDeltaYaw.toString())
                    writer.append(',')
                    writer.append(sample.targetDeltaPitch.toString())
                    writer.append(',')
                    writer.append(sample.distance.toString())
                    writer.append(',')
                    writer.append(sample.actualDeltaYaw.toString())
                    writer.append(',')
                    writer.appendLine(sample.actualDeltaPitch.toString())
                }
            }

            context.message("§a[Neuro] §fСохранено §e${snapshot.size} §fсэмплов: §e${output.name}")
        } catch (exception: Exception) {
            context.message("§c[Neuro] §fОшибка сохранения: ${exception.javaClass.simpleName}")
        }
    }

    fun loadDefaultDataset(context: AddonCommandContext) {
        if (NeuroRotation.loadDataset(DEFAULT_DATASET_NAME)) {
            context.message("§a[Neuro] §fДатасет §edefault.csv §fзагружен.")
        } else {
            context.message("§c[Neuro] §fНе удалось найти или прочитать §edefault.csv§f.")
        }
    }

    @Flow
    @Suppress("UNUSED_PARAMETER")
    private fun onPlayerUpdate(event: PlayerUpdateEvent) {
        if (!recording || samples.size >= MAX_SAMPLES) return

        val minecraft = Minecraft.getInstance()
        val localPlayer = minecraft.player
        val level = minecraft.level
        if (localPlayer == null || level == null) {
            previousRotation = null
            previousTarget = null
            return
        }

        val target = (minecraft.hitResult as? EntityHitResult)?.entity
        if (target == null || target === localPlayer || target.isRemoved || !target.isAlive) {
            previousRotation = null
            previousTarget = null
            return
        }

        val current = RotationSnapshot(localPlayer.yRot, localPlayer.xRot)
        val previous = previousRotation
        if (previous != null && previousTarget === target) {
            val point = target.boundingBox.center
            val eye = localPlayer.eyePosition
            val dx = point.x - eye.x
            val dy = point.y - eye.y
            val dz = point.z - eye.z
            val horizontalDistance = sqrt(dx * dx + dz * dz)
            val distance = eye.distanceTo(point).toFloat()

            val desiredYaw = Mth.wrapDegrees(Math.toDegrees(atan2(dz, dx)) - 90.0).toFloat()
            val desiredPitch = (-Math.toDegrees(atan2(dy, horizontalDistance))).toFloat()
            val targetDeltaYaw = Mth.wrapDegrees(desiredYaw - current.yaw)
            val targetDeltaPitch = desiredPitch - current.pitch

            val actualDeltaYaw = Mth.wrapDegrees(current.yaw - previous.yaw)
            val actualDeltaPitch = current.pitch - previous.pitch

            samples += NeuroSample(
                targetDeltaYaw = targetDeltaYaw,
                targetDeltaPitch = targetDeltaPitch,
                distance = distance,
                actualDeltaYaw = actualDeltaYaw,
                actualDeltaPitch = actualDeltaPitch
            )
        }

        previousRotation = current
        previousTarget = target
    }
}
