package com.lumitier.xporbtweaks.config;

import com.lumitier.xporbtweaks.XpOrbTweaks;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.ColorControllerBuilder;
import dev.isxander.yacl3.api.controller.FloatSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * YACL-Config-Screen.
 *
 * Aufbau: zwei Kategorien.
 *   "General"         - nur der Hauptschalter
 *   "Experience Orbs" - alle Einstellungen, in aufklappbaren Gruppen
 *
 * Verifiziert gegen das YACL-Jar 3.8.2+1.21.11-fabric (javap):
 *   OptionGroup.createBuilder().name(..).collapsed(boolean).option(..).build()
 *   ColorControllerBuilder.create(Option&lt;java.awt.Color&gt;).allowAlpha(boolean)
 *   IntegerSliderControllerBuilder.create(Option&lt;Integer&gt;)
 *   FloatSliderControllerBuilder.create(Option&lt;Float&gt;)
 *   BooleanControllerBuilder.create(Option&lt;Boolean&gt;).yesNoFormatter().coloured(boolean)
 *
 * Alle Bindings schreiben DIREKT in die statischen Felder von XpOrbTweaks
 * (Live-Wirkung ohne Neustart). save() schreibt die JSON-Datei.
 */
public final class XpOrbTweaksScreen {

	private XpOrbTweaksScreen() {}

	private static Text key(String id) {
		return Text.translatable("xporbtweaks.option." + id);
	}

	private static OptionDescription desc(String id) {
		return OptionDescription.createBuilder()
			.text(Text.translatable("xporbtweaks.option." + id + ".desc"))
			.build();
	}

	private static Option<Boolean> toggle(String id, boolean def,
	                                      java.util.function.Supplier<Boolean> getter,
	                                      java.util.function.Consumer<Boolean> setter) {
		return Option.<Boolean>createBuilder()
			.name(key(id))
			.description(desc(id))
			.binding(def, getter, setter)
			.controller(opt -> BooleanControllerBuilder.create(opt).yesNoFormatter().coloured(true))
			.build();
	}

	private static Option<Float> slider(String id,
	                                    float def, float min, float max, float step,
	                                    java.util.function.Supplier<Float> getter,
	                                    java.util.function.Consumer<Float> setter) {
		return Option.<Float>createBuilder()
			.name(key(id))
			.description(desc(id))
			.binding(def, getter, setter)
			.controller(opt -> FloatSliderControllerBuilder.create(opt).range(min, max).step(step))
			.build();
	}

	private static Option<Integer> intSlider(String id,
	                                         int def, int min, int max, int step,
	                                         java.util.function.Supplier<Integer> getter,
	                                         java.util.function.Consumer<Integer> setter) {
		return Option.<Integer>createBuilder()
			.name(key(id))
			.description(desc(id))
			.binding(def, getter, setter)
			.controller(opt -> IntegerSliderControllerBuilder.create(opt).range(min, max).step(step))
			.build();
	}

	/** Farbwaehler mit Hex-Feld und Alphakanal. */
	private static Option<java.awt.Color> colorPicker(String id, int def,
	                                                  java.util.function.Supplier<Integer> getter,
	                                                  java.util.function.Consumer<Integer> setter) {
		return Option.<java.awt.Color>createBuilder()
			.name(key(id))
			.description(desc(id))
			.binding(
				new java.awt.Color(def, true),
				() -> new java.awt.Color(getter.get(), true),
				v -> setter.accept(v.getRGB()))
			.controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true))
			.build();
	}

	public static Screen create(Screen parent) {
		return YetAnotherConfigLib.createBuilder()
			.title(Text.translatable("xporbtweaks.title"))

			// ================= Tab 1: General =================
			.category(ConfigCategory.createBuilder()
				.name(Text.translatable("xporbtweaks.category.general"))
				.option(toggle("mod_enabled", true,
					() -> XpOrbTweaks.enabled, v -> XpOrbTweaks.enabled = v))
				.build())

			// ================= Tab 2: Experience Orbs =================
			.category(ConfigCategory.createBuilder()
				.name(Text.translatable("xporbtweaks.category.orbs"))

				.group(OptionGroup.createBuilder()
					.name(Text.translatable("xporbtweaks.group.general"))
					.option(toggle("render_orbs", true,
						() -> XpOrbTweaks.renderOrbs, v -> XpOrbTweaks.renderOrbs = v))
					.option(intSlider("glow", 7, 0, 15, 1,
						() -> XpOrbTweaks.glow, v -> XpOrbTweaks.glow = v))
					.build())

				.group(OptionGroup.createBuilder()
					.name(Text.translatable("xporbtweaks.group.scale"))
					.option(slider("scale", 0.30F, 0.05F, 2.0F, 0.01F,
						() -> XpOrbTweaks.scale, v -> XpOrbTweaks.scale = v))
					.option(toggle("scale_shadow", true,
						() -> XpOrbTweaks.scaleShadow, v -> XpOrbTweaks.scaleShadow = v))
					.build())

				.group(OptionGroup.createBuilder()
					.name(Text.translatable("xporbtweaks.group.pulse"))
					.option(toggle("should_pulse", true,
						() -> XpOrbTweaks.shouldPulse, v -> XpOrbTweaks.shouldPulse = v))
					.option(intSlider("pulse_speed", 10, 0, 50, 1,
						() -> XpOrbTweaks.pulseSpeed, v -> XpOrbTweaks.pulseSpeed = v))
					.build())

				.group(OptionGroup.createBuilder()
					.name(Text.translatable("xporbtweaks.group.color"))
					.option(colorPicker("color", XpOrbTweaks.DEFAULT_COLOR,
						() -> XpOrbTweaks.color, v -> XpOrbTweaks.color = v))
					.option(colorPicker("pulse_color", XpOrbTweaks.DEFAULT_PULSE_COLOR,
						() -> XpOrbTweaks.pulseColor, v -> XpOrbTweaks.pulseColor = v))
					.build())

				.group(OptionGroup.createBuilder()
					.name(Text.translatable("xporbtweaks.group.rainbow"))
					.option(toggle("rainbow", false,
						() -> XpOrbTweaks.rainbow, v -> XpOrbTweaks.rainbow = v))
					.option(intSlider("rainbow_opacity", 255, 0, 255, 1,
						() -> XpOrbTweaks.rainbowOpacity, v -> XpOrbTweaks.rainbowOpacity = v))
					.option(intSlider("rainbow_speed", 10, 1, 50, 1,
						() -> XpOrbTweaks.rainbowSpeed, v -> XpOrbTweaks.rainbowSpeed = v))
					.build())

				.group(OptionGroup.createBuilder()
					.name(Text.translatable("xporbtweaks.group.value"))
					.option(toggle("value_scale", false,
						() -> XpOrbTweaks.valueScale, v -> XpOrbTweaks.valueScale = v))
					.option(slider("value_scale_max", 2.0F, 1.0F, 5.0F, 0.1F,
						() -> XpOrbTweaks.valueScaleMax, v -> XpOrbTweaks.valueScaleMax = v))
					.option(toggle("value_color", false,
						() -> XpOrbTweaks.valueColor, v -> XpOrbTweaks.valueColor = v))
					.option(colorPicker("value_color_high", XpOrbTweaks.DEFAULT_VALUE_COLOR,
						() -> XpOrbTweaks.valueColorHigh, v -> XpOrbTweaks.valueColorHigh = v))
					.option(intSlider("value_max", 2477, 10, 100000, 10,
						() -> XpOrbTweaks.valueMax, v -> XpOrbTweaks.valueMax = v))
					.build())

				.build())

			.save(XpOrbTweaks::save)
			.build()
			.generateScreen(parent);
	}
}
