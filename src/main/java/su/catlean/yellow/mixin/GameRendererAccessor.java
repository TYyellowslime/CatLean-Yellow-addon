package su.catlean.yellow.mixin;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Access to GameRenderer's private post-effect setter. */
@Mixin(GameRenderer.class)
public interface GameRendererAccessor {
    @Invoker("setPostEffect")
    void invokeSetPostEffect(Identifier effectId);
}
