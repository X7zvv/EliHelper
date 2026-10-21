# EliHelper (Phase 2) - Fabric, Minecraft 26.1.2, Java 25

## Bauen
1. `fabric_api_version` in gradle.properties eintragen (https://fabricmc.net/develop/, Version 26.1.2).
2. JDK 25: `./gradlew build` (Jar in build/libs/) oder `./gradlew runClient` zum Testen.

## Bedienung
- Menue: rechte Shift (in Steuerung > EliHelper aenderbar), dieselbe Taste schliesst es.
- Befehls-Hotkeys: Steuerung > EliHelper > "Befehl-Slot 1-4" (standardmaessig ohne Taste).
  Zuordnung Slot -> Befehl, Bestaetigung (Doppeldruck) und Buttons stehen in config/elihelper.json:
  {"label": "/afk", "command": "afk", "slot": 2, "confirm": true}
- Farbcodes: Text eingeben, Farben klicken, Zeichenzaehler beachtet Limit (rename 35 / sign 30, in der Config aenderbar).
  Praefix ("&") und Hex-Format ("&#%s") ebenfalls in der Config, falls HugoSMP andere Codes nutzt.
- Statistik: liest NUR System-Chatzeilen. `payoutRegex` ist ein geratener Standard - mit einer echten
  Auszahlungs-Chatzeile anpassen (Gruppe "amount" = Betrag).
- Minuten-Test: 60 s Echtzeit. Hochrechnung rechnet Auszahlung / aktueller Multi * Ziel-Multi.

## Noch nicht enthalten
Sell-/Order-Makro (Automatisierung, erst nach Klaerung der Serverregeln), Marktdaten von hugosmp-market.net,
Auktionen, Giveaways, Preisalarm, lila Button-Stil, 1.21.11-Zweig.
