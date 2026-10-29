# Entwicklungsnotizen

Interne Doku. Nicht fuer Nutzer gedacht — die lesen die `README.md`.

## Versionen

Alle verifiziert gegen [fabricmc.net/develop](https://fabricmc.net/develop)
bzw. gegen die jeweiligen Projekt-Repos. Nichts davon geraten.

| Baustein | Version | Woher |
|---|---|---|
| Minecraft | 1.21.11 | — |
| Yarn | 1.21.11+build.6 | hoechster Release-Build |
| Fabric Loader | 0.20.1 | |
| Fabric API | 0.141.6+1.21.11 | nur als Dev-Dependency, siehe unten |
| Loom | 1.19.3 | braucht Gradle >= 9.8.0 **und JVM 25** |
| Gradle | 9.8.2 | |
| MixinExtras | 0.5.5 | compileOnly + annotationProcessor + include |
| YACL | 3.8.2+1.21.11-fabric (`pHWDw3Vc`) | Modrinth-Version-ID |
| Mod Menu | 17.0.1 (`cLkYaVuL`) | Modrinth-Version-ID |
| quilt-parsers | 0.2.1 | YACLs versteckte Laufzeit-Abhaengigkeit |
| Clumps | 29.0.0.1 (`OgBE8Rz4`) | nur `modRuntimeOnly`, zum Testen |

**Java-Level:** Loom 1.19.3 baut mit JVM 25, der Mod-Code bleibt auf
Release 21 (`options.release = 21` im `build.gradle`). Beides ist Absicht und
kein Widerspruch.

**quilt-parsers:** Der Modrinth-Maven liefert YACLs transitive
Abhaengigkeiten nicht mit. Ohne das explizite `include(modImplementation(...))`
fuer `org.quiltmc.parsers:json` und `:gson` crasht der Start mit
`NoClassDefFoundError: org/quiltmc/parsers/json/gson/GsonWriter`.

**fabric-api:** Wird nirgends im Code benutzt. `ClientModInitializer` kommt
aus dem Loader, `ModMenuApi` aus Mod Menu. Deshalb steht es *nicht* in
`depends` der `fabric.mod.json` — YACL bringt seine eigene Deklaration mit.
Als Gradle-Dependency bleibt es, weil die Dev-Umgebung es braucht.

## Architektur

Zwei Mixins, beide client-only.

### `ExperienceOrbEntityRendererMixin`

Vier Eingriffe in `net.minecraft.client.render.entity.ExperienceOrbEntityRenderer`.

| Hook | Ziel | Steuert |
|---|---|---|
| `@Inject` HEAD, cancellable | `render` | Kugeln ganz ausblenden |
| `@ModifyArgs` | `MatrixStack.scale(FFF)` in `render` | Groesse |
| `@Redirect` | `OrderedRenderCommandQueue.submitCustom(...)` in `render` | Farbe, Deckkraft, Groesse nach Wert |
| `@Inject` RETURN | `updateRenderState` | XP-Wert in den State, Schattengroesse |
| `@ModifyConstant` intValue=7 | `getBlockLight` | Eigenleuchten |

### `ExperienceOrbEntityRenderStateMixin`

Haengt per `@Unique` ein `int`-Feld an den RenderState, zugaenglich ueber
das Interface `com.lumitier.xporbtweaks.duck.XpOrbValueHolder`. Keine
Injektion in Vanilla-Code, daher kollisionsfrei.

## Warum `submitCustom` umgeleitet wird

Der naheliegende Weg waere, die Farb-Locals in `render` zu patchen. Geht
nicht, aus drei Gruenden:

1. Vanilla berechnet `int p` (rot) und `int r` (blau) als Locals, aber
   **`int q = 255` (gruen) ist eine Konstante im Lambda**, nicht in `render`.
   Der Gruenkanal ist von aussen nicht erreichbar.
2. In `vertex()` liegen zwar alle drei Kanaele als Argumente vor, aber dort
   gibt es weder `state.age` noch den XP-Wert — also keine Basis fuer eine
   Farbe pro Kugel.
3. Ein Static als Zwischenspeicher scheidet aus: `submitCustom` stellt die
   Zeichenbefehle in eine Warteschlange. Bis das Lambda laeuft, sind
   laengst andere Kugeln eingereiht.

Deshalb wird der `submitCustom`-Aufruf als Ganzes ersetzt und das Quad selbst
gezeichnet. Alles davor (push, translate, Kameraausrichtung, scale) und
danach (pop, `super.render` mit Schatten und Outline) bleibt Vanilla.

**Das Lambda im gemappten Jar heisst `method_72982`** — ein
Intermediary-Name und damit ein unzuverlaessiges Injektionsziel. Nicht
darauf injizieren.

## Bridge-Methoden

`render`, `getBlockLight` und `updateRenderState` existieren in der Klasse
jeweils **zweimal**: einmal konkret, einmal als synthetische Bruecke von
`EntityRenderer`. Ein blosser Methodenname im `method =`-Attribut matcht
mehrdeutig und laesst den Start crashen. Deshalb stehen ueberall die vollen
Deskriptoren.

## Die Clumps-Luecke

`ExperienceOrbEntity.getOrbSize()` liefert ab **2477 XP** immer Stufe 10.
Clumps verschmilzt beliebig viele Kugeln zu einer und ruft `setValue(Summe)`
auf — ein Clump mit 50.000 XP ist also von einem mit 2.477 nicht zu
unterscheiden.

`VALUE` ist ein `TrackedData<Integer>`, wird also automatisch zum Client
gesynct. Der Client kennt den echten Wert, Vanilla nutzt ihn beim Rendern
nur nicht aus. Genau das holen `valueScale` und `valueColor` nach.

Die Skala ist **logarithmisch** (`log1p(value) / log1p(valueMax)`). Linear
waere bei XP-Mengen unbrauchbar: ein 50.000er Clump drueckt alles unter
5.000 auf denselben Punkt.

### Kein Konflikt mit Clumps

| | Clumps | diese Mod |
|---|---|---|
| Ziel-Klasse | `ExperienceOrb` (Entity) | `ExperienceOrbEntityRenderer`, `…RenderState` |
| Seite | Server-Logik | Client-Rendering |
| Methoden | `canMerge`, `merge`, `playerTouch`, `tryMergeToExisting` | `render`, `updateRenderState`, `getBlockLight` |

Keine gemeinsame Klasse, keine gemeinsame Methode.

Clumps' Merge-Logik haengt komplett an `ServerPlayer` und `ServerLevel` —
auf einem Server ohne Clumps bringt eine clientseitige Installation nichts.
Unsere Mod ist davon unabhaengig.

## Defaults vs. Vanilla

Die Defaults sind **nicht bit-identisch** zu Vanilla. Vanilla rechnet:

```
rot  = (sin(o) + 1) * 0.5 * 255          0…255
gruen= 255                               konstant
blau = (sin(o + 4PI/3) + 1) * 0.1 * 255  0…51, phasenversetzt
```

Drei unabhaengige Kanaele. Unser Modell blendet zwischen `color` und
`pulseColor` mit **einer** gemeinsamen Phase. Rot und Gruen treffen wir
exakt; Blau laeuft synchron mit Rot statt um 4PI/3 versetzt. Bei maximalem
Rot hat Vanilla Blau ~13, wir 51 — ein minimaler Farbstich, nebeneinander
erkennbar, allein betrachtet nicht.

Bewusste Entscheidung: Zwei frei waehlbare Farben sind das Feature. Wer
Vanilla will, setzt `Mod Enabled` auf Aus, dann laeuft der Originalcode.

## Wenn Minecraft aktualisiert wird

Mojang ist nach 1.21.11 auf Kalenderversionierung umgestellt (26.1, 26.2,
…). Reihenfolge beim Portieren:

1. `gradle.properties` auf neue Yarn-/Loader-/API-Versionen
2. Minecraft neu dekompilieren, `ExperienceOrbEntityRenderer` aufschlagen
3. **Zuerst pruefen:** Gibt es `submitCustom` noch mit derselben Signatur?
   Das ist der zerbrechlichste Hook.
4. Dann: Ist `scale(0.3F, …)` noch da? Steht in `getBlockLight` noch `+ 7`?
5. `shadowRadius` noch im RenderState?
6. UV-Berechnung im eigenen Lambda gegen die neue Vanilla-Variante
   abgleichen — vier Kacheln pro Zeile auf einem 64er Atlas.

## Build

```
.\gradlew build
```

Ergebnis: `build\libs\xporbtweaks-<version>.jar` (die **ohne** `-sources`).

Die Jar buendelt MixinExtras und quilt-parsers per jar-in-jar. Das `NOTICE`
liegt mit in der Jar.
