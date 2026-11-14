package su.catlean.yellow

import su.catlean.api.addon.feature.AddonCommand
import su.catlean.api.addon.feature.AddonCommandContext

/** Toggles Neuro sample recording. */
object NeuroCommand : AddonCommand("neuro") {
    override fun execute(context: AddonCommandContext): Int {
        NeuroRecorderModule.toggleRecord(context)
        return 1
    }
}

/** Saves the current recording as neuro_datasets/default.csv. */
object NeuroSaveCommand : AddonCommand("neuro-save") {
    override fun execute(context: AddonCommandContext): Int {
        NeuroRecorderModule.saveDefaultDataset(context)
        return 1
    }
}

/** Loads neuro_datasets/default.csv into NeuroRotation. */
object NeuroLoadCommand : AddonCommand("neuro-load") {
    override fun execute(context: AddonCommandContext): Int {
        NeuroRecorderModule.loadDefaultDataset(context)
        return 1
    }
}
