package com.lumitier.xporbtweaks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Mod-weiter Zustand + Config.
 *
 * Statische Felder, damit die Renderer-Mixins im Hot-Path ohne Lookup und ohne
 * Allokation darauf zugreifen koennen. Die YACL-Oberflaeche schreibt direkt
 * hinein (Live-Wirkung), save()/load() persistiert als JSON.
 */
public final class XpOrbTweaks {

	public static final String MOD_ID = "xporbtweaks";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private XpOrbTweaks() {}

	// ------------------------------------------------------------------
	// Defaults
	//
	// Vanilla skaliert mit 0.3F, setzt Alpha hart auf 128 (0x80), addiert +7
	// auf das Blocklicht und teilt age durch 2.0F. Die Farbe laeuft in Vanilla
	// zwischen Gruen und einem gelblichen Ton - das bilden color/pulseColor ab.
	// ------------------------------------------------------------------

	public static final int DEFAULT_COLOR       = 0x8000FF00;
	public static final int DEFAULT_PULSE_COLOR = 0x80FFFF33;

	/** Mod global an/aus. */
	public static volatile boolean enabled = true;

	/** Erfahrungskugeln ueberhaupt zeichnen. */
	public static volatile boolean renderOrbs = true;

	/** Eigenleuchten: Aufschlag auf das Blocklicht, 0..15. Vanilla ist 7. */
	public static volatile int glow = 7;

	/** Absolute Groesse. Vanilla ist 0.30. */
	public static volatile float scale = 0.30F;

	/** Schatten mit der Groesse mitskalieren. */
	public static volatile boolean scaleShadow = true;

	/** Farbpulsieren an/aus. */
	public static volatile boolean shouldPulse = true;

	/** Pulsgeschwindigkeit. 10 entspricht Vanilla (Divisor 2.0). */
	public static volatile int pulseSpeed = 10;

	/** Grundfarbe als ARGB. Der Alphakanal ist die Deckkraft. */
	public static volatile int color = DEFAULT_COLOR;

	/** Zielfarbe des Pulsierens als ARGB. */
	public static volatile int pulseColor = DEFAULT_PULSE_COLOR;

	/** Regenbogenmodus: ueberschreibt beide Farben. */
	public static volatile boolean rainbow = false;

	/** Deckkraft im Regenbogenmodus, 0..255. */
	public static volatile int rainbowOpacity = 255;

	/** Regenbogengeschwindigkeit. 10 entspricht ca. 5 Sekunden pro Runde. */
	public static volatile int rainbowSpeed = 10;

	// ------------------------------------------------------------------
	// Wertabhaengige Darstellung (die Clumps-Luecke)
	//
	// getOrbSize() liefert ab 2477 XP immer Stufe 10. Mit Clumps traegt eine
	// einzelne Kugel beliebig viel XP, sieht aber ab dieser Grenze immer gleich
	// aus. Beides hier haengt am echten Wert statt an der Texturstufe und
	// funktioniert deshalb auch ohne Clumps - nur faellt es ohne kaum auf.
	// ------------------------------------------------------------------

	public static final int DEFAULT_VALUE_COLOR = 0x80FF3300;

	/** Grosse Kugeln zusaetzlich vergroessern. */
	public static volatile boolean valueScale = false;

	/** Maximaler Zusatzfaktor beim oberen Ende des Wertebereichs. */
	public static volatile float valueScaleMax = 2.0F;

	/** Farbe nach XP-Wert einfaerben. */
	public static volatile boolean valueColor = false;

	/** Zielfarbe am oberen Ende des Wertebereichs. */
	public static volatile int valueColorHigh = DEFAULT_VALUE_COLOR;

	/** Wert, ab dem das obere Ende erreicht ist. 2477 ist Vanillas Deckel. */
	public static volatile int valueMax = 2477;

	/** 0..1, wie weit eine Kugel im konfigurierten Wertebereich liegt. */
	public static float valueFraction(int value) {
		int max = clampInt(valueMax, 10, 100000);
		if (value <= 0) {
			return 0.0F;
		}
		// Logarithmisch: XP-Mengen wachsen exponentiell, linear waere die
		// Skala schon bei einem mittelgrossen Clump am Anschlag.
		float f = (float) (Math.log1p(value) / Math.log1p(max));
		return clamp(f, 0.0F, 1.0F);
	}

	/** Groessenfaktor nach Wert, 1.0 .. valueScaleMax. */
	public static float valueScaleFactor(int value) {
		float max = clamp(valueScaleMax, 1.0F, 5.0F);
		return 1.0F + (max - 1.0F) * valueFraction(value);
	}

	// ------------------------------------------------------------------
	// Farbberechnung fuer den Renderer
	// ------------------------------------------------------------------

	private static int lerp8(int a, int b, float t) {
		return clampInt((int) (a + (b - a) * t), 0, 255);
	}

	/** Lineare Interpolation zweier ARGB-Werte. */
	public static int lerpArgb(int c1, int c2, float t) {
		int a = lerp8((c1 >>> 24) & 0xFF, (c2 >>> 24) & 0xFF, t);
		int r = lerp8((c1 >> 16)  & 0xFF, (c2 >> 16)  & 0xFF, t);
		int g = lerp8((c1 >> 8)   & 0xFF, (c2 >> 8)   & 0xFF, t);
		int b = lerp8(c1          & 0xFF, c2          & 0xFF, t);
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	// Der Regenbogen-Farbton haengt nur an der Uhrzeit, nicht am einzelnen Orb.
	// Einmal pro Millisekunde rechnen reicht. Nur vom Render-Thread benutzt.
	private static long rainbowLastMs = Long.MIN_VALUE;
	private static int rainbowCached = 0xFFFFFF;

	/** Aktueller Regenbogen-Farbwert als 0xRRGGBB. */
	public static int rainbowRgb() {
		long now = System.currentTimeMillis();
		if (now != rainbowLastMs) {
			int speed = clampInt(rainbowSpeed, 1, 50);
			float period = 50000.0F / speed;
			float hue = (now % (long) period) / period;
			rainbowCached = java.awt.Color.HSBtoRGB(hue, 1.0F, 1.0F) & 0xFFFFFF;
			rainbowLastMs = now;
		}
		return rainbowCached;
	}

	/**
	 * Endgueltige Farbe einer Kugel als ARGB.
	 *
	 * Reihenfolge: Regenbogen schlaegt Pulsieren, Pulsieren schlaegt Grundfarbe.
	 */
	public static int resolveColor(float age, int value) {
		int base;
		if (rainbow) {
			base = (clampInt(rainbowOpacity, 0, 255) << 24) | rainbowRgb();
		} else {
			int sp = clampInt(pulseSpeed, 0, 50);
			if (!shouldPulse || sp == 0) {
				base = color;
			} else {
				// Vanilla: age / 2.0F. pulseSpeed 10 trifft das exakt.
				float divisor = 20.0F / sp;
				float t = ((float) Math.sin(age / divisor) + 1.0F) * 0.5F;
				base = lerpArgb(color, pulseColor, t);
			}
		}

		if (valueColor) {
			base = lerpArgb(base, valueColorHigh, valueFraction(value));
		}
		return base;
	}

	// ------------------------------------------------------------------
	// Clamp-Helfer
	// ------------------------------------------------------------------

	public static float clamp(float v, float min, float max) {
		if (Float.isNaN(v)) {
			return min;
		}
		return v < min ? min : (v > max ? max : v);
	}

	public static int clampInt(int v, int min, int max) {
		return v < min ? min : (v > max ? max : v);
	}

	/** Alle Werte in gueltige Bereiche zwingen. */
	public static void sanitize() {
		scale          = clamp(scale, 0.05F, 2.0F);
		glow           = clampInt(glow, 0, 15);
		pulseSpeed     = clampInt(pulseSpeed, 0, 50);
		rainbowOpacity = clampInt(rainbowOpacity, 0, 255);
		rainbowSpeed   = clampInt(rainbowSpeed, 1, 50);
		valueScaleMax  = clamp(valueScaleMax, 1.0F, 5.0F);
		valueMax       = clampInt(valueMax, 10, 100000);
	}

	// ------------------------------------------------------------------
	// Persistenz (GSON kommt mit Minecraft)
	// ------------------------------------------------------------------

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private static Path configPath() {
		return FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + ".json");
	}

	/** Reines Daten-Transferobjekt fuer JSON. */
	private static final class Data {
		boolean enabled = true;
		boolean renderOrbs = true;
		int glow = 7;
		float scale = 0.30F;
		boolean scaleShadow = true;
		boolean shouldPulse = true;
		int pulseSpeed = 10;
		int color = DEFAULT_COLOR;
		int pulseColor = DEFAULT_PULSE_COLOR;
		boolean rainbow = false;
		int rainbowOpacity = 255;
		int rainbowSpeed = 10;
		boolean valueScale = false;
		float valueScaleMax = 2.0F;
		boolean valueColor = false;
		int valueColorHigh = DEFAULT_VALUE_COLOR;
		int valueMax = 2477;
	}

	public static void load() {
		Path p = configPath();
		try {
			if (Files.exists(p)) {
				try (Reader r = Files.newBufferedReader(p)) {
					Data d = GSON.fromJson(r, Data.class);
					if (d != null) {
						enabled        = d.enabled;
						renderOrbs     = d.renderOrbs;
						glow           = d.glow;
						scale          = d.scale;
						scaleShadow    = d.scaleShadow;
						shouldPulse    = d.shouldPulse;
						pulseSpeed     = d.pulseSpeed;
						color          = d.color;
						pulseColor     = d.pulseColor;
						rainbow        = d.rainbow;
						rainbowOpacity = d.rainbowOpacity;
						rainbowSpeed   = d.rainbowSpeed;
						valueScale     = d.valueScale;
						valueScaleMax  = d.valueScaleMax;
						valueColor     = d.valueColor;
						valueColorHigh = d.valueColorHigh;
						valueMax       = d.valueMax;
					}
				}
			}
		} catch (Exception e) {
			LOGGER.warn("[XP Orb Tweaks] Config konnte nicht geladen werden, nutze Defaults.", e);
		}
		sanitize();
		save();
	}

	public static void save() {
		sanitize();
		Path p = configPath();
		try {
			Files.createDirectories(p.getParent());
			Data d = new Data();
			d.enabled        = enabled;
			d.renderOrbs     = renderOrbs;
			d.glow           = glow;
			d.scale          = scale;
			d.scaleShadow    = scaleShadow;
			d.shouldPulse    = shouldPulse;
			d.pulseSpeed     = pulseSpeed;
			d.color          = color;
			d.pulseColor     = pulseColor;
			d.rainbow        = rainbow;
			d.rainbowOpacity = rainbowOpacity;
			d.rainbowSpeed   = rainbowSpeed;
			d.valueScale     = valueScale;
			d.valueScaleMax  = valueScaleMax;
			d.valueColor     = valueColor;
			d.valueColorHigh = valueColorHigh;
			d.valueMax       = valueMax;
			try (Writer w = Files.newBufferedWriter(p)) {
				GSON.toJson(d, w);
			}
		} catch (Exception e) {
			LOGGER.warn("[XP Orb Tweaks] Config konnte nicht gespeichert werden.", e);
		}
	}
}
