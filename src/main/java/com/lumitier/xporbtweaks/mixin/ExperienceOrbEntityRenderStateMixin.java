package com.lumitier.xporbtweaks.mixin;

import com.lumitier.xporbtweaks.duck.XpOrbValueHolder;
import net.minecraft.client.render.entity.state.ExperienceOrbEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Haengt den echten XP-Wert an den RenderState.
 *
 * Reines Datenfeld, keine Injektion in Vanilla-Code - damit kollisionsfrei
 * gegenueber jeder anderen Mod, die denselben RenderState benutzt.
 */
@Mixin(ExperienceOrbEntityRenderState.class)
public class ExperienceOrbEntityRenderStateMixin implements XpOrbValueHolder {

	@Unique
	private int xporbtweaks$value;

	@Override
	public int xporbtweaks$getValue() {
		return this.xporbtweaks$value;
	}

	@Override
	public void xporbtweaks$setValue(int value) {
		this.xporbtweaks$value = value;
	}
}
