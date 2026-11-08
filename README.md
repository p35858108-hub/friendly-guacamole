# Siri — Asistente Android estilo Apple Intelligence (Jetpack Compose)

## Puesta en marcha
1. Abre la carpeta en Android Studio (Ladybug o superior). Si falta `gradlew`, Studio lo genera (Gradle 8.9).
2. Añade tu clave en `local.properties` (no la subas a git):
   ```
   GEMINI_API_KEY=tu_clave
   ```
3. Instala la app, ábrela y pulsa **Set as default assistant** →
   *Ajustes > Apps > Apps predeterminadas > App de asistente digital* → **Siri**.
4. Mantén pulsado Inicio / desliza desde una esquina inferior.

## Arquitectura
```
service/
  SiriVoiceInteractionService  rol de asistente del sistema (BIND_VOICE_INTERACTION)
  SiriSessionService/Session   en cada invocación lanza AssistantActivity con startAssistantActivity()
  StubRecognitionService       obligatorio para que Android liste la app como asistente
AssistantActivity              ventana translúcida + blur real del fondo (API 31+), dictado por RecognizerIntent
AssistantViewModel             StateFlow<AssistantUiState> + Channel<AssistantEvent>
data/GeminiRepository          REST generateContent, timeout de 15 s, todo error -> Result.failure
util/WebSearchFallback         ACTION_VIEW google.com/search?q=… -> fallback ACTION_WEB_SEARCH
ui/
  GlowModifiers   Modifier.intelligenceGlow(): borde con sweep-gradient giratorio + bloom en 3 capas
  MorphingInputBar orbe 64dp -> barra "Search or Ask" con springs (dampingRatio 0.68)
  ResponseCard     tarjeta con entrada elástica, arrastre con resistencia y dismiss por velocidad
  SiriOverlay      composición de capas y animación de entrada
```

## Tolerancia a fallos
`GeminiRepository.ask()` nunca lanza excepciones (salvo cancelación): sin clave, sin red, HTTP != 2xx,
respuesta bloqueada/vacía, JSON inválido o timeout devuelven `Result.failure`. El ViewModel emite
`OpenWebSearch(query)` con el texto exacto, la Activity abre el navegador y se cierra.
