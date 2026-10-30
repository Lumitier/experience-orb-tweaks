package com.lumitier.xporbtweaks.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.lumitier.xporbtweaks.XpOrbTweaks;
import com.lumitier.xporbtweaks.duck.XpOrbValueHolder;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.ExperienceOrbEntityRenderer;
import net.minecraft.client.render.entity.state.ExperienceOrbEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ExperienceOrbEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Eingriffe in den Vanilla-Renderer der Erfahrungskugeln.
 *
 * Alle Ziele sind gegen den dekompilierten 1.21.11-Code (Yarn 1.21.11+build.6)
 * und gegen javap auf dem gemergten Jar verifiziert. Der Vanilla-Code:
 *
 *   render(state, matrices, queue, camera):
 *     int i = state.size;                         -> UV-Kachel der Textur
 *     float o = state.age / 2.0F;                 -> Pulsphase
 *     int p = (sin(o) + 1) * 0.5 * 255;           (rot, pulsierend)
 *     int q = 255;                                (gruen, konstant)
 *     int r = (sin(o + 4PI/3) + 1) * 0.1 * 255;   (blau, pulsierend)
 *     matrixStack.scale(0.3F, 0.3F, 0.3F);        -> Groesse
 *     queue.submitCustom(matrices, LAYER, lambda) -> vier Vertices
 *     super.render(...)                           -> Schatten, Outline
 *
 *   getBlockLight(entity, pos):
 *     return clamp(super.getBlockLight(...) + 7, 0, 15)
 *
 * WARUM submitCustom umgeleitet statt einzelne Konstanten gepatcht wird:
 * Der Gruenkanal ist eine feste 255 INNERHALB des Lambdas, und die Pulsphase
 * liegt in vertex() gar nicht vor. Fuer eine frei waehlbare Farbe, die pro
 * Kugel aus age berechnet wird, braucht es beides. Das Lambda traegt im
 * gemappten Jar den Namen method_72982 - ein unzuverlaessiges Injektionsziel.
 * Deshalb wird der submitCustom-Aufruf als Ganzes ersetzt: alles davor (push,
 * translate, Kameraausrichtung, scale) und danach (pop, super.render mit
 * Schatten) bleibt Vanilla und damit kompatibel mit anderen Mods.
 *
 * Die Allokation des eigenen Lambdas entspricht exakt dem, was Vanilla an
 * dieser Stelle ohnehin tut - kein zusaetzlicher Druck auf den GC.
 *
 * WICHTIG - Bridge-Methoden: render, getBlockLight und updateRenderState gibt
 * es zusaetzlich als synthetische Bruecken mit gleichem Namen. Deshalb ueberall
 * der volle Deskriptor; ein blosser Methodenname wuerde mehrdeutig matchen.
 */
@Mixin(ExperienceOrbEntityRenderer.class)
public class ExperienceOrbEntityRendererMixin {

	// ------------------------------------------------------------------
	// Kugeln komplett ausblenden
	// ------------------------------------------------------------------

	@Inject(
		method = "render(Lnet/minecraft/client/render/entity/state/ExperienceOrbEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
		at = @At("HEAD"),
		cancellable = true
	)
	private void xporbtweaks$maybeSkip(ExperienceOrbEntityRenderState state, MatrixStack matrices,
	                                   OrderedRenderCommandQueue queue, CameraRenderState camera,
	                                   CallbackInfo ci) {
		if (XpOrbTweaks.enabled && !XpOrbTweaks.renderOrbs) {
			ci.cancel();
		}
	}

	// ------------------------------------------------------------------
	// Groesse: Vanilla skaliert mit 0.3F, wir setzen den Wert direkt.
	// ------------------------------------------------------------------

	@WrapOperation(
		method = "render(Lnet/minecraft/client/render/entity/state/ExperienceOrbEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;scale(FFF)V")
	)
	private void xporbtweaks$scale(MatrixStack matrices, float x, float y, float z,
	                               Operation<Void> original) {
		if (!XpOrbTweaks.enabled) {
			original.call(matrices, x, y, z);
			return;
		}
		float s = XpOrbTweaks.clamp(XpOrbTweaks.scale, 0.05F, 2.0F);
		original.call(matrices, s, s, s);
	}

	// ------------------------------------------------------------------
	// Farbe: eigener Zeichenaufruf statt des Vanilla-Lambdas.
	// ------------------------------------------------------------------

	@WrapOperation(
		method = "render(Lnet/minecraft/client/render/entity/state/ExperienceOrbEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;submitCustom(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue$Custom;)V")
	)
	private void xporbtweaks$submit(OrderedRenderCommandQueue self,
	                                MatrixStack submitMatrices, RenderLayer layer,
	                                OrderedRenderCommandQueue.Custom vanilla,
	                                Operation<Void> original,
	                                ExperienceOrbEntityRenderState state, MatrixStack matrices,
	                                OrderedRenderCommandQueue queue, CameraRenderState camera) {
		if (!XpOrbTweaks.enabled) {
			original.call(self, submitMatrices, layer, vanilla);
			return;
		}

		// UV-Kachel exakt wie Vanilla: 4x3 Kacheln a 16px auf einem 64er Atlas.
		final int tile = state.size;
		final float u0 = (tile % 4 * 16 + 0)  / 64.0F;
		final float u1 = (tile % 4 * 16 + 16) / 64.0F;
		final float v0 = (tile / 4 * 16 + 0)  / 64.0F;
		final float v1 = (tile / 4 * 16 + 16) / 64.0F;
		final int light = state.light;

		final int value = ((XpOrbValueHolder) state).xporbtweaks$getValue();

		final int argb = XpOrbTweaks.resolveColor(state.age, value);
		final int ca = (argb >>> 24) & 0xFF;
		final int cr = (argb >> 16)  & 0xFF;
		final int cg = (argb >> 8)   & 0xFF;
		final int cb = argb          & 0xFF;

		// Groesse nach XP-Wert. Nicht ueber matrixStack.scale, weil der Aufruf
		// vor submitCustom liegt und dort der RenderState nicht vorliegt -
		// stattdessen direkt an den Eckpunkten, um die Quadmitte (0, 0.25).
		final float g = XpOrbTweaks.valueScale ? XpOrbTweaks.valueScaleFactor(value) : 1.0F;
		final float x0 = -0.5F * g;
		final float x1 =  0.5F * g;
		final float y0 = 0.25F + (-0.25F - 0.25F) * g;
		final float y1 = 0.25F + ( 0.75F - 0.25F) * g;

		original.call(self, submitMatrices, layer, (OrderedRenderCommandQueue.Custom) (entry, consumer) -> {
			xporbtweaks$vertex(consumer, entry, x0, y0, cr, cg, cb, ca, u0, v1, light);
			xporbtweaks$vertex(consumer, entry, x1, y0, cr, cg, cb, ca, u1, v1, light);
			xporbtweaks$vertex(consumer, entry, x1, y1, cr, cg, cb, ca, u1, v0, light);
			xporbtweaks$vertex(consumer, entry, x0, y1, cr, cg, cb, ca, u0, v0, light);
		});
	}

	@Unique
	private static void xporbtweaks$vertex(VertexConsumer consumer, MatrixStack.Entry entry,
	                                       float x, float y,
	                                       int red, int green, int blue, int alpha,
	                                       float u, float v, int light) {
		consumer.vertex(entry, x, y, 0.0F)
			.color(red, green, blue, alpha)
			.texture(u, v)
			.overlay(OverlayTexture.DEFAULT_UV)
			.light(light)
			.normal(entry, 0.0F, 1.0F, 0.0F);
	}

	// ------------------------------------------------------------------
	// Schatten mitskalieren. shadowRadius liegt im RenderState und wird in
	// updateRenderState aus dem Renderer uebernommen - danach greifen wir zu.
	// ------------------------------------------------------------------

	@Inject(
		method = "updateRenderState(Lnet/minecraft/entity/ExperienceOrbEntity;Lnet/minecraft/client/render/entity/state/ExperienceOrbEntityRenderState;F)V",
		at = @At("RETURN")
	)
	private void xporbtweaks$updateState(ExperienceOrbEntity entity, ExperienceOrbEntityRenderState state,
	                                     float tickDelta, CallbackInfo ci) {
		// Den echten XP-Wert mitfuehren. VALUE ist TrackedData, also auch auf
		// dem Client korrekt - auch bei einem von Clumps verschmolzenen Orb.
		((XpOrbValueHolder) state).xporbtweaks$setValue(entity.getValue());

		if (!XpOrbTweaks.enabled || !XpOrbTweaks.scaleShadow) {
			return;
		}
		float f = XpOrbTweaks.clamp(XpOrbTweaks.scale, 0.05F, 2.0F) / 0.3F;
		if (XpOrbTweaks.valueScale) {
			f *= XpOrbTweaks.valueScaleFactor(entity.getValue());
		}
		state.shadowRadius *= f;
	}

	// ------------------------------------------------------------------
	// Eigenleuchten: Vanilla addiert +7 auf das Blocklicht.
	// ------------------------------------------------------------------

	@ModifyExpressionValue(
		method = "getBlockLight(Lnet/minecraft/entity/ExperienceOrbEntity;Lnet/minecraft/util/math/BlockPos;)I",
		at = @At(value = "CONSTANT", args = "intValue=7")
	)
	private int xporbtweaks$glow(int original) {
		if (!XpOrbTweaks.enabled) {
			return original;
		}
		return XpOrbTweaks.clampInt(XpOrbTweaks.glow, 0, 15);
	}
}
