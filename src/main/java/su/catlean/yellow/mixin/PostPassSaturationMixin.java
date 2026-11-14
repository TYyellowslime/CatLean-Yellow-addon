package su.catlean.yellow.mixin;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import su.catlean.yellow.SaturationModule;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;

/** Keeps the post-effect uniform in sync with the CatLean slider. */
@Mixin(PostPass.class)
public abstract class PostPassSaturationMixin {
    @Shadow @Final
    private Map<String, GpuBuffer> customUniforms;

    @Unique
    private float yellow$lastSaturationAmount = Float.NaN;

    @Inject(method = "addToFrame", at = @At("HEAD"))
    private void yellow$updateSaturationUniform(
            FrameGraphBuilder graphBuilder,
            Map<Identifier, ResourceHandle<RenderTarget>> resourceHandles,
            com.mojang.blaze3d.buffers.GpuBufferSlice projectionMatrix,
            CallbackInfo callbackInfo
    ) {
        GpuBuffer oldBuffer = this.customUniforms.get("SaturationConfig");
        if (oldBuffer == null) {
            return;
        }

        float amount = SaturationModule.saturationAmount();
        if (Float.isFinite(this.yellow$lastSaturationAmount)
                && Math.abs(this.yellow$lastSaturationAmount - amount) < 0.0001f) {
            return;
        }

        // A std140 float block is padded to 16 bytes. Create a replacement uniform buffer
        // instead of using raw OpenGL calls, so this also works with Minecraft's newer backend.
        ByteBuffer initialData = ByteBuffer.allocateDirect(16).order(ByteOrder.nativeOrder());
        initialData.putFloat(amount);
        initialData.position(0);

        GpuBuffer newBuffer = RenderSystem.getDevice().createBuffer(
                () -> "Yellow saturation uniform",
                GpuBuffer.USAGE_UNIFORM,
                initialData
        );

        this.customUniforms.put("SaturationConfig", newBuffer);
        oldBuffer.close();
        this.yellow$lastSaturationAmount = amount;
    }
}
