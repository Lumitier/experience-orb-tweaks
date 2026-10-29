package com.lumitier.xporbtweaks.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Haengt den Config-Screen ans Zahnrad in der Mod-Liste. */
public final class ModMenuIntegration implements ModMenuApi {

	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return XpOrbTweaksScreen::create;
	}
}
