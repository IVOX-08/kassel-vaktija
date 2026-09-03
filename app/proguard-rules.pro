# R8-Regeln für die Verkaufsversion.
#
# Ab Version 1.2.1 ist die Verkleinerung EINGESCHALTET (isMinifyEnabled = true). Google Play misst
# den Anteil verschleierten Codes und warnt unterhalb von 25 %; ohne R8 lag er bei 1 %.
#
# Jede Regel hier steht für einen konkreten Weg, auf dem die App sonst kaputtgeht. Es sind keine
# Vorsichtsregeln "für alle Fälle" — wer eine löschen will, muss die Stelle im Code widerlegen, die
# darüber steht.


# --------------------------------------------------------------------------------------------
# 1 · Aufzählungen, deren NAMEN gespeichert werden
# --------------------------------------------------------------------------------------------
# Das ist die gefährlichste Stelle im ganzen Projekt, und sie fällt beim Testen NICHT auf: R8 darf
# die Konstanten einer Aufzählung umbenennen. An diesen Stellen wird aber genau dieser Name als
# Text weggeschrieben und später wieder eingelesen:
#
#   QuranReaderPrefs      value.name  -> SharedPreferences,   QuranScript.valueOf(...)
#   AlarmSettings         name        -> DataStore,           AdhanSound.valueOf(...)
#   SettingsRepository    name        -> DataStore,           ThemeMode.valueOf(...)
#   AlarmScheduler        sound.name  -> Intent-Extra,        gelesen im Empfänger
#   TrackerNotifier       prayer.name -> Intent-Extra,        Prayer.valueOf(...)
#   CommunityRepository   status.name -> FIRESTORE
#
# Wird umbenannt, dann steht in der Datenbank plötzlich "a" statt "active", die gespeicherte
# Einstellung des Nutzers passt zu nichts mehr, und ein laufender Weckruf findet sein Gebet nicht.
# Auf einem frisch installierten Testgerät sieht man davon nichts — es fällt erst bei Leuten auf,
# die die App schon hatten.
#
# Deshalb: unsere Aufzählungen bleiben vollständig, wie sie sind. Das kostet ein paar Kilobyte.
-keep enum de.igbdsandzakkassel.vaktija.** { *; }


# --------------------------------------------------------------------------------------------
# 2 · kotlinx.serialization
# --------------------------------------------------------------------------------------------
# Die Hadith- und Koran-Dateien in assets/ werden über erzeugte Serialisierer gelesen. Die findet
# der Compiler über den Klassennamen; verschwindet die Klasse oder ihr Companion, bleibt der
# Bildschirm leer.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *; }
-keep,includedescriptorclasses class de.igbdsandzakkassel.vaktija.**$$serializer { *; }
-keepclassmembers class de.igbdsandzakkassel.vaktija.** {
    *** Companion;
}
-keepclasseswithmembers class de.igbdsandzakkassel.vaktija.** {
    kotlinx.serialization.KSerializer serializer(...);
}
# Die @Serializable-Klassen selbst (Feldnamen sind die Schlüssel in der JSON-Datei).
-keepclassmembers @kotlinx.serialization.Serializable class de.igbdsandzakkassel.vaktija.** {
    <fields>;
}


# --------------------------------------------------------------------------------------------
# 3 · Klassennamen in Intent-Extras und Meldungen
# --------------------------------------------------------------------------------------------
# Die Empfänger und Dienste stehen im Manifest und werden dadurch automatisch behalten. Ihre
# Konstanten (EXTRA_*, ACTION_*) sind Zeichenketten und von der Verschleierung ohnehin unberührt --
# hier ist nichts zusätzlich nötig, und das ist bewusst festgehalten, damit niemand aus Sorge
# pauschal das halbe Paket behält und die Verschleierung damit wieder auf 1 % drückt.


# --------------------------------------------------------------------------------------------
# 4 · Was NICHT eingeschaltet werden darf: shrinkResources
# --------------------------------------------------------------------------------------------
# Nur zur Erinnerung an der Stelle, an der man danach sucht -- die Einstellung selbst steht in
# build.gradle.kts:
#
# Die Adhan- und Hinweistöne werden NICHT über R.raw.xyz angesprochen, sondern zur Laufzeit über
# ihren Namen gesucht:
#     resources.getIdentifier(soundResName, "raw", packageName)   (AdhanForegroundService,
#                                                                  NewsNotifier)
# Für den Ressourcen-Verkleinerer sieht deshalb JEDE Tondatei unbenutzt aus. Er würde sie
# wegwerfen, die App bliebe zur Gebetszeit einfach stumm, und im Log stünde nichts Auffälliges.
# shrinkResources bleibt aus. Das hat mit der Play-Warnung nichts zu tun -- die betrifft den Code,
# nicht die Ressourcen.
