package su.catlean.yellow

import su.catlean.api.addon.CatLeanAddon
import su.catlean.api.addon.CatLeanApi

object YellowAddon : CatLeanAddon {
    override fun onCatLeanAddon(api: CatLeanApi) {
        api.registry.registerModule(AirStuckModule)
        api.registry.registerModule(SaturationModule)
        api.registry.registerModule(SpearSwapModule)
        api.registry.registerCommand(NeuroCommand)
        api.registry.registerCommand(NeuroSaveCommand)
        api.registry.registerCommand(NeuroLoadCommand)
        api.registry.registerAuraRotation(NeuroRotation)
        api.events.subscribe(NeuroRecorderModule)

        api.logger("Yellow").info("Yellow addon loaded")
    }
}
