package com.lumitier.xporbtweaks.client;

import com.lumitier.lumilib.api.LumiLibApi;
import com.lumitier.lumilib.api.LumiMod;
import com.lumitier.lumilib.api.SettingsPage;
import com.lumitier.xporbtweaks.XpOrbTweaks;
import com.lumitier.xporbtweaks.config.XpOrbTweaksScreen;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.text.Text;

/**
 * Anmeldung bei LumiLib.
 *
 * WARUM EINE EIGENE KLASSE - das ist der wichtige Teil:
 * LumiLib ist OPTIONAL. Stuende der Verweis darauf im Initialisierer der
 * Mod, wuerde die Java-Laufzeit beim Start versuchen, LumiLibs Klassen
 * aufzuloesen - und ohne LumiLib startet die Mod gar nicht mehr. Liegt
 * der Verweis dagegen in einer eigenen Klasse, wird sie erst geladen,
 * wenn jemand sie anfasst. Vorher wird abgefragt, ob LumiLib da ist.
 *
 * NAME, VERSION UND BESCHREIBUNG KOMMEN AUS DER fabric.mod.json:
 * Sie stehen dort ohnehin, Mod Menu und Modrinth lesen dieselben Werte.
 * Sie hier noch einmal einzutragen hiesse, zwei Stellen gleich halten zu
 * muessen - und nach dem naechsten Versionssprung stuende im Hub die
 * alte Nummer.
 *
 * ZUM ICON: nichts zu tun. LumiLib liest es aus dem Jar dieser Mod.
 */
public final class LumiLibCompat {

	private LumiLibCompat() {}

	public static void register() {
		ModContainer container = FabricLoader.getInstance()
			.getModContainer(XpOrbTweaks.MOD_ID).orElse(null);

		String version = container == null ? ""
			: container.getMetadata().getVersion().getFriendlyString();
		String name = container == null ? "Experience Orb Tweaks"
			: container.getMetadata().getName();
		String summary = container == null ? ""
			: container.getMetadata().getDescription();

		LumiLibApi.register(new LumiMod(
			XpOrbTweaks.MOD_ID,
			Text.literal(name),
			version,
			Text.literal(summary),
			// Kein eigenes Sprite - LumiLib nimmt das Icon aus dem Jar.
			null,
			// Der bestehende YACL-Bildschirm. Sobald LumiLib Schieberegler
			// und Farbwaehler kann, wird daraus eine SettingsPage und die
			// Einstellungen stehen direkt im Hub.
			// YACL bleibt als Rueckfallebene - Mod Menus Zahnrad
			// oeffnet weiterhin diesen Bildschirm.
			XpOrbTweaksScreen::create,
			LumiLibCompat::settings));

		// Server-Regeln (LumiLibControlPlugin). Eigener Fang: Mit einer
		// LumiLib aelter als 1.3.0 gibt es die Methode nicht - dann soll die
		// Mod trotzdem im Hub stehen, nur eben ohne Server-Steuerung.
		try {
			LumiLibApi.onServerRulesChanged(() ->
				XpOrbTweaks.serverDisabled = LumiLibApi.isDisabledByServer(XpOrbTweaks.MOD_ID));
		} catch (Throwable t) {
			XpOrbTweaks.LOGGER.warn("[XP Orb Tweaks] LumiLib is too old for server control (needs 1.3.0+).");
		}
	}

	/**
	 * Die Einstellungen, wie LumiLibs Hub sie zeichnet.
	 *
	 * DIESELBEN FELDER wie der YACL-Bildschirm, nur anders beschrieben.
	 * Beide Wege schreiben direkt in die statischen Felder von
	 * XpOrbTweaks und rufen dasselbe save() - es gibt also nur EINEN
	 * Stand, egal ueber welchen Weg man ihn aendert.
	 */
	private static SettingsPage settings() {
		return SettingsPage.builder(XpOrbTweaks.MOD_ID)
			.section("general")
				.toggle("mod_enabled",
					() -> XpOrbTweaks.enabled, v -> XpOrbTweaks.enabled = v, true)
				.toggle("render_orbs",
					() -> XpOrbTweaks.renderOrbs, v -> XpOrbTweaks.renderOrbs = v, true)
				.intSlider("glow", 0, 15, 1,
					() -> XpOrbTweaks.glow, v -> XpOrbTweaks.glow = v, 7)
			.section("scale")
				.slider("scale", 0.05F, 2.0F, 0.01F,
					() -> XpOrbTweaks.scale, v -> XpOrbTweaks.scale = v, 0.30F)
				.toggle("scale_shadow",
					() -> XpOrbTweaks.scaleShadow, v -> XpOrbTweaks.scaleShadow = v, true)
			.section("pulse")
				.toggle("should_pulse",
					() -> XpOrbTweaks.shouldPulse, v -> XpOrbTweaks.shouldPulse = v, true)
				.intSlider("pulse_speed", 0, 50, 1,
					() -> XpOrbTweaks.pulseSpeed, v -> XpOrbTweaks.pulseSpeed = v, 10)
			.section("color")
				.color("color",
					() -> XpOrbTweaks.color, v -> XpOrbTweaks.color = v, XpOrbTweaks.DEFAULT_COLOR)
				.color("pulse_color",
					() -> XpOrbTweaks.pulseColor, v -> XpOrbTweaks.pulseColor = v, XpOrbTweaks.DEFAULT_PULSE_COLOR)
			.section("rainbow")
				.toggle("rainbow",
					() -> XpOrbTweaks.rainbow, v -> XpOrbTweaks.rainbow = v, false)
				.intSlider("rainbow_opacity", 0, 255, 1,
					() -> XpOrbTweaks.rainbowOpacity, v -> XpOrbTweaks.rainbowOpacity = v, 255)
				.intSlider("rainbow_speed", 1, 50, 1,
					() -> XpOrbTweaks.rainbowSpeed, v -> XpOrbTweaks.rainbowSpeed = v, 10)
			.section("value")
				.toggle("value_scale",
					() -> XpOrbTweaks.valueScale, v -> XpOrbTweaks.valueScale = v, false)
				.slider("value_scale_max", 1.0F, 5.0F, 0.1F,
					() -> XpOrbTweaks.valueScaleMax, v -> XpOrbTweaks.valueScaleMax = v, 2.0F)
				.toggle("value_color",
					() -> XpOrbTweaks.valueColor, v -> XpOrbTweaks.valueColor = v, false)
				.color("value_color_high",
					() -> XpOrbTweaks.valueColorHigh, v -> XpOrbTweaks.valueColorHigh = v, XpOrbTweaks.DEFAULT_VALUE_COLOR)
				.intSlider("value_max", 10, 100000, 10,
					() -> XpOrbTweaks.valueMax, v -> XpOrbTweaks.valueMax = v, 2477)
			.save(XpOrbTweaks::save)
			.build();
	}
}
