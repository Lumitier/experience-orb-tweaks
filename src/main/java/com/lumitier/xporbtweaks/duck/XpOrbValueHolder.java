package com.lumitier.xporbtweaks.duck;

/**
 * Wird per Mixin auf ExperienceOrbEntityRenderState aufgesetzt.
 *
 * Vanilla legt im RenderState nur die Texturstufe (0..10) ab, nicht den echten
 * XP-Wert. Mit Clumps ist das zu wenig: ab 2477 XP liefert getOrbSize() immer
 * Stufe 10, ein Clump mit 50.000 XP ist von einem mit 2.477 nicht zu
 * unterscheiden. Hier wird der Rohwert zusaetzlich mitgefuehrt.
 */
public interface XpOrbValueHolder {

	int xporbtweaks$getValue();

	void xporbtweaks$setValue(int value);
}
