package com.simibubi.create.foundation.mixin.fabric;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;

/**
 * porting_lib's "extensions" module gives RenderTarget a RenderTargetExtensions interface whose
 * enableStencil()/disableStencil() default implementations just throw ("this should be overridden
 * via mixin. what?") - the override mixin was meant to ship alongside it, but the last 1.21.1
 * build actually published for that module (3.1.0-beta.54, confirmed via the artifact's own
 * maven-metadata.xml: no later 1.21.1 version of "extensions" exists) never includes a
 * RenderTargetMixin in its mixin config. Catnip's UIRenderHelper calls enableStencil() on every
 * one of its custom render targets during RenderSystem.finishInitialization, so this throws on
 * every single game boot. Implemented here directly instead of waiting on the upstream library,
 * following the same depth/stencil combined texture approach NeoForge's own RenderTarget patch
 * uses (checked by disassembling a NeoForge-patched RenderTarget.class): swap the depth
 * attachment's texture format between plain GL_DEPTH_COMPONENT and combined GL_DEPTH24_STENCIL8,
 * then (re)attach it to both the depth and stencil attachment points.
 */
@Mixin(RenderTarget.class)
public abstract class RenderTargetMixin {

	@Unique
	private boolean create$stencilEnabled;

	@Shadow
	public int depthBufferId;

	@Shadow
	public int width;

	@Shadow
	public int height;

	@Shadow
	public boolean useDepth;

	@Shadow
	public int frameBufferId;

	@Shadow
	public abstract void checkStatus();

	public void enableStencil() {
		if (this.create$stencilEnabled || !this.useDepth)
			return;
		this.create$stencilEnabled = true;
		create$recreateDepthTexture(true);
	}

	public void disableStencil() {
		if (!this.create$stencilEnabled)
			return;
		this.create$stencilEnabled = false;
		create$recreateDepthTexture(false);
	}

	public boolean isStencilEnabled() {
		return this.create$stencilEnabled;
	}

	@Unique
	private void create$recreateDepthTexture(boolean stencil) {
		GlStateManager._bindTexture(this.depthBufferId);
		GlStateManager._texParameter(3553, 10241, 9728);
		GlStateManager._texParameter(3553, 10240, 9728);
		GlStateManager._texParameter(3553, 34892, 0);
		GlStateManager._texParameter(3553, 10242, 33071);
		GlStateManager._texParameter(3553, 10243, 33071);
		if (stencil) {
			GlStateManager._texImage2D(3553, 0, 36013, this.width, this.height, 0, 34041, 36269, null);
		} else {
			GlStateManager._texImage2D(3553, 0, 6402, this.width, this.height, 0, 6402, 5126, null);
		}

		GlStateManager._glBindFramebuffer(36160, this.frameBufferId);
		GlStateManager._glFramebufferTexture2D(36160, 36096, 3553, this.depthBufferId, 0);
		if (stencil) {
			GlStateManager._glFramebufferTexture2D(36160, 36128, 3553, this.depthBufferId, 0);
		}
		this.checkStatus();
	}
}
