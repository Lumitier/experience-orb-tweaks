package com.lumitier.xporbtweaks.client;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client-Entrypoint.
 *
 * Phase 1: Nur Lade-Nachweis, noch keine Funktion.
 * Die Renderer-Hooks kommen in Phase 2.
 */
public class XpOrbTweaksClient implements ClientModInitializer {

	public static final String MOD_ID = "xporbtweaks";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeClient() {
		com.lumitier.xporbtweaks.XpOrbTweaks.load();
		LOGGER.info("[XP Orb Tweaks] Client-Init ok. (Phase 1: Geruest + Config, noch keine Renderer-Hooks.)");
	}
}
