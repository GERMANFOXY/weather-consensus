# Weather Consensus

Native Android-App (Kotlin + Jetpack Compose), die Wetterdaten von mehreren echten APIs abruft, normalisiert, vergleicht und daraus eine **Konsens-Prognose** berechnet.

**Keine Mock- oder Demo-Daten** — alle Werte stammen live von den konfigurierten Wetterdiensten.

## Unterstützte APIs

| Anbieter        | API-Key nötig | Website |
|-----------------|---------------|---------|
| OpenWeatherMap  | Ja            | https://openweathermap.org/api |
| WeatherAPI.com  | Ja            | https://www.weatherapi.com/ |
| Tomorrow.io     | Ja            | https://www.tomorrow.io/ |
| Open-Meteo      | **Nein**      | https://open-meteo.com/ |

Ohne API-Keys funktioniert mindestens **Open-Meteo**. Fehlende Keys werden in der App angezeigt; es gibt keinen Fallback auf Fake-Daten.

## Voraussetzungen

- Android Studio Ladybug (2024.2+) oder neuer
- JDK 17+
- Android SDK (API 35)
- Gradle 8.9 (Wrapper enthalten)

## API-Keys eintragen

1. Kopiere die Beispiel-Datei:

   ```bash
   cp secrets.properties.example secrets.properties
   ```

2. Trage deine Keys in `secrets.properties` ein (Projektroot):

   ```properties
   OPENWEATHERMAP_API_KEY=dein_openweathermap_key
   WEATHERAPI_API_KEY=dein_weatherapi_key
   TOMORROW_IO_API_KEY=dein_tomorrow_io_key
   ```

3. `secrets.properties` ist in `.gitignore` und wird **nicht** ins Repository committed.

4. Keys werden zur Build-Zeit in `BuildConfig` geladen und **nie im UI angezeigt**.

5. Nach dem Ändern der Keys: App neu bauen (`Build > Rebuild Project`).

## Android SDK Pfad

Stelle sicher, dass `local.properties` im Projektroot existiert:

```properties
sdk.dir=C\:\\Users\\DEIN_USER\\AppData\\Local\\Android\\Sdk
```

Android Studio legt diese Datei beim Öffnen des Projekts automatisch an.

## App starten

### Mit Android Studio

1. Projektordner `weather-consensus` öffnen
2. Gradle Sync abwarten
3. Emulator oder Gerät verbinden (min. Android 8.0 / API 26)
4. Run ▶ auf `app`

### Mit Gradle (Kommandozeile)

```bash
# Windows
gradlew.bat assembleDebug

# APK installieren (Gerät verbunden)
gradlew.bat installDebug
```

## Funktionen

- **Stadt-Suche** über Open-Meteo Geocoding (Fallback: OpenWeatherMap Geocoding)
- **GPS-Standort** optional (mit Nutzerberechtigung)
- **Aktuelles Wetter** + **24-Stunden-Prognose**
- **Konsens-Berechnung** mit gewichtetem Durchschnitt
- **Ausreißer-Erkennung** (MAD-basiert)
- **Vertrauensanzeige**: „Sehr stabil“, „Leicht abweichend“, „Unsicher“
- **Anbieter-Vergleich** mit Einzelwerten pro Quelle
- **Einstellungen** für Anbieter-Gewichtungen (DataStore)
- **Caching** (15 Minuten TTL, In-Memory)
- **Parallele API-Abfragen** mit Kotlin Coroutines

## Architektur

```
app/src/main/java/com/weatherconsensus/
├── data/
│   ├── api/           # Retrofit-Interfaces & DTOs
│   ├── cache/         # Wetter-Cache
│   ├── client/        # Geocoding & Provider-Clients
│   ├── config/        # API-Key-Konfiguration (BuildConfig)
│   ├── location/      # GPS-Standort
│   ├── network/       # Retrofit/OkHttp Setup
│   ├── preferences/   # Gewichtungen (DataStore)
│   └── repository/    # Datenzugriff
├── domain/
│   ├── consensus/     # Konsens-Algorithmus
│   ├── model/         # Domänenmodelle
│   └── normalization/ # Normalisierung & Wetter-Kategorien
└── ui/
    ├── components/    # Wiederverwendbare UI-Bausteine
    ├── screen/        # Home & Settings
    ├── theme/         # Material 3 Theme
    └── viewmodel/     # ViewModels
```

## Normalisierte Einheiten

| Wert           | Einheit |
|----------------|---------|
| Temperatur     | °C      |
| Wind           | km/h    |
| Niederschlag   | mm oder % |
| Luftfeuchtigkeit | %     |
| Wetterzustand  | Einheitliche Kategorien (Klar, Regen, …) |

## Fehlerbehandlung

- Fehlende API-Keys → Hinweis-Banner (nur für Entwickler)
- Anbieter nicht erreichbar → z. B. „OpenWeatherMap nicht erreichbar“
- Alle Quellen fehlgeschlagen → Fehlermeldung, keine Fake-Daten
- Mindestens eine Quelle erfolgreich → Konsens wird berechnet

## Lizenz

Privates Projekt — siehe Repository-Inhaber.
