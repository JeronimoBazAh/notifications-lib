# notifications-lib

Librería Java 21 para enviar notificaciones (Email, SMS, Push, Slack) con **una única API**, agnóstica a
frameworks y **sin dependencias en runtime**. Cambiar de proveedor es cambiar una línea de configuración.

## Instalación

Todavía no está publicada en un repositorio remoto: instálala en tu repositorio local de Maven.

```bash
git clone https://github.com/JeronimoBazAh/notifications-lib.git && cd notifications-lib
mvn clean install
```

```xml
<dependency>
  <groupId>com.example</groupId>
  <artifactId>notifications-lib</artifactId>
  <version>1.0.0</version>
</dependency>
```

Gradle: `implementation("com.example:notifications-lib:1.0.0")`. Requiere Java 21+.

## Quick Start

```java
try (NotificationService service = NotificationService.builder()
        .provider(SendGridEmailProvider.simulated(Secret.fromEnv("SENDGRID_API_KEY")))
        .build()) {

    SendResult result = service.send(EmailNotification.builder()
            .from("no-reply@acme.com").to("ana@example.com")
            .subject("Welcome").textBody("Hi Ana!").build());

    if (!result.isSuccess()) {
        System.err.println(result.error().code() + ": " + result.error().getMessage());
    }
}
```

Demo ejecutable (todo simulado, sin red):

```bash
mvn clean package && java -jar target/notifications-lib-1.0.0.jar
# o con Docker
docker build -t notifications-lib . && docker run --rm notifications-lib
```

## Arquitectura

```
caller ──► NotificationService (fachada: sync / async / batch)
               └─► DeliveryPipeline (enrutar por canal → reintentar → failover → eventos)
                       └─► NotificationProvider<N>  (SPI)
                               └─► Transport  (JdkHttpTransport | SimulatedTransports | tu fake)
```

| Pieza | Responsabilidad |
|---|---|
| `Notification` + `Email/Sms/Push/SlackNotification` | Datos de cada canal. Inmutables y validados al construirse |
| `ChannelType` | Identifica un canal. Es un `record`, no un `enum`, para que cualquiera pueda definir canales nuevos |
| `NotificationProvider<N>` | **SPI**: lo único que implementa un proveedor o canal nuevo |
| `Transport` | Costura con la red. Los proveedores arman el request real y el transporte lo envía |
| `DeliveryPipeline` | Algoritmo de entrega de una notificación (interno) |
| `RetryPolicy` / `FailoverPolicy` | Cuándo reintentar y cuándo pasar al siguiente proveedor |
| `NotificationListener` | Observer: eventos `SENT`, `ATTEMPT_FAILED`, `FAILOVER`, `FAILED` |

Patrones aplicados: **Strategy** (proveedores intercambiables), **Builder** (notificaciones y servicio),
**Observer** (eventos), **Facade** (`NotificationService`), **Template Method** light en el contrato de test de proveedores.

## Configuración (solo código Java)

| Qué | Cómo |
|---|---|
| Proveedores | `.provider(...)`. Varios por canal: el primero es primario y los siguientes son fallback |
| Reintentos | `.retryPolicy(RetryPolicy.exponential(3, Duration.ofMillis(200)).withJitter(0.2))` |
| Failover | `.failoverPolicy(FailoverPolicy.ALWAYS \| SKIP_ON_REJECTION \| NEVER)` |
| Eventos | `.listener(event -> ...)` |
| Async | `.executor(...)`. Por defecto, hilos virtuales (Java 21) |
| Credenciales | `Secret.of("...")` o `Secret.fromEnv("VAR")` |
| Red real | `new SendGridEmailProvider(secret, new JdkHttpTransport())` en lugar de `.simulated(...)` |

### Ejemplo con varios proveedores por canal

```java
NotificationService service = NotificationService.builder()
        .provider(new SendGridEmailProvider(Secret.fromEnv("SENDGRID_API_KEY"), new JdkHttpTransport())) // primario
        .provider(new MailgunEmailProvider(Secret.fromEnv("MAILGUN_API_KEY"), "mg.acme.com", new JdkHttpTransport())) // fallback
        .provider(new TwilioSmsProvider("AC...", Secret.fromEnv("TWILIO_TOKEN"), "+15005550006", new JdkHttpTransport()))
        .provider(new FcmPushProvider("my-project", () -> Secret.of(tokenRefresher.currentToken()), new JdkHttpTransport()))
        .retryPolicy(RetryPolicy.exponential(3, Duration.ofMillis(200)).withJitter(0.2))
        .failoverPolicy(FailoverPolicy.SKIP_ON_REJECTION)
        .listener(event -> metrics.count(event.type(), event.provider()))
        .build();
```

## Proveedores soportados

| Canal | Proveedor | API modelada |
|---|---|---|
| Email | `SendGridEmailProvider` | v3 `POST /mail/send`, Bearer, `202` + header `X-Message-Id` |
| Email | `MailgunEmailProvider` | `POST /v3/{domain}/messages`, Basic, form, `200` + `{"id"}` |
| SMS | `TwilioSmsProvider` | `POST /Accounts/{sid}/Messages.json`, Basic, form, `201` + `sid` |
| Push | `FcmPushProvider` | HTTP v1 `messages:send`, OAuth2 Bearer (token por `Supplier`, vence ~1h) |
| Slack | `SlackChatProvider` | `chat.postMessage`. Responde HTTP 200 con `ok:false` ante errores y se maneja |

Los proveedores construyen el request tal como lo espera cada API (método, URL, auth, formato y respuesta de éxito).
Por defecto el demo envía a través de `SimulatedTransports`; `JdkHttpTransport` es el transporte real.

## API principal

- `NotificationService`: `send(n)`, `sendAsync(n)`, `sendAll(list)`, `close()`
- `SendResult`: `isSuccess()`, `provider()`, `providerMessageId()`, `attempts()`, `error()`, `orElseThrow()`
- `MessageTemplate`: `render(vars)` / `renderHtml(vars)` (escapa HTML), con `{{variable}}`
- Errores: `ValidationException`, `SendException`, `ErrorCode`

```java
MessageTemplate t = MessageTemplate.of("Hi {{name}}, your code is {{code}}");
String text = t.render(Map.of("name", "Ana", "code", "4821")); // falta una variable -> ValidationException
```

## Manejo de errores

- **Notificación inválida**: `ValidationException` al construirla, con **todas** las violaciones juntas.
  Una notificación inválida no puede existir.
- **Fallo de envío**: nunca se lanza; vuelve como dato en `SendResult.error()` (`ErrorCode` + `isRetryable()`).
  Esto simplifica `sendAsync` y los lotes. Si prefieres excepciones, usa `send(n).orElseThrow()`.

| HTTP | `ErrorCode` | ¿Reintenta? |
|---|---|---|
| 401 / 403 | `AUTHENTICATION` | No |
| 429 | `RATE_LIMITED` | Sí |
| 5xx / error de red | `PROVIDER_UNAVAILABLE` | Sí |
| otro 4xx | `REJECTED_BY_PROVIDER` | No |

## Cómo agregar un canal o proveedor (sin tocar código existente)

```java
record WhatsAppNotification(String id, String to, String text) implements Notification {
    public ChannelType channel() { return ChannelType.of("whatsapp"); }
    public Instant createdAt() { return Instant.now(); }
    public Map<String, String> metadata() { return Map.of(); }
}

class WhatsAppProvider implements NotificationProvider<WhatsAppNotification> {
    public String name() { return "whatsapp-cloud"; }
    public ChannelType channel() { return ChannelType.of("whatsapp"); }
    public Class<WhatsAppNotification> notificationType() { return WhatsAppNotification.class; }
    public ProviderResponse send(WhatsAppNotification n) throws SendException { /* ... */ }
}

NotificationService.builder().provider(new WhatsAppProvider()).build();
```

Para un proveedor nuevo de un canal existente, implementa `NotificationProvider<EmailNotification>`
(o el tipo que corresponda) y reutiliza `ProviderSupport.execute(...)` para clasificar errores HTTP.
Después, crea una subclase de `ProviderContractTest`: todo proveedor nuevo debe pasarla.
`ExtensibilityTest` demuestra este flujo completo agregando un canal desde fuera de la librería.

## Seguridad

- **`Secret`** oculta el valor en `toString()` y logs. Solo `reveal()` lo expone, al armar el header.
- No pongas claves en el código: usa variables de entorno (`Secret.fromEnv`) o un secret manager.
- Los `toString()` de las notificaciones no incluyen destinatarios ni contenido (PII fuera de los logs).
- Los transportes simulados nunca loguean headers (donde viajan las credenciales).
- `MessageTemplate.renderHtml` escapa los valores para evitar inyección de HTML.
- `JdkHttpTransport` aplica timeouts de conexión y de request.

## Decisiones de diseño y trade-offs

- **Un tipo por canal en vez de un `Map<String,Object>`**: las diferencias entre canales (subject del email,
  device token del push) las verifica el compilador. Costo: un record por canal.
- **`ChannelType` como record**: un enum es cerrado y obligaría a modificar la librería para agregar un canal.
  Costo: se pierde el `switch` exhaustivo.
- **Fallos de envío como dato**: es más cómodo para async y batch. Costo: el caller debe revisar `isSuccess()`.
- **`Transport` como costura**: los proveedores se prueban sin red y sin framework de mocks. Se reemplaza lo
  simulado por `JdkHttpTransport` sin tocar el proveedor.
- **Cero dependencias**: logging con `System.Logger`, JSON mínimo propio, tests con fakes escritos a mano.
  Costo: el parseo de respuestas es básico (regex sobre ids). Para respuestas más ricas conviene Jackson.
- **Failover configurable**: por defecto ante cualquier fallo se pasa al siguiente proveedor (maximiza entrega).
  `SKIP_ON_REJECTION` evita insistir con contenido que un proveedor ya rechazó.
- **Hilos virtuales para async**: el envío es I/O bloqueante, que es justo lo que mejor escalan.
- **Pipeline separado del servicio (SRP)**: el servicio gestiona configuración y concurrencia; el pipeline, el algoritmo de entrega.

## Tests

`mvn clean test` (90 tests). Incluye:

- Servicio: retry/backoff, failover, políticas, eventos, listeners con error, interrupción, async y batch.
- **Contract test** (`ProviderContractTest`): lo pasan los 5 proveedores (éxito + 401/429/503/400 + error de red).
- Validación del modelo, `Secret`, `RetryPolicy` (jitter), `MessageTemplate`, `Json`, `FormBody`.
- `JdkHttpTransport` contra un servidor HTTP local del JDK (sin red externa).
- `ExtensibilityTest`: agrega un canal nuevo **sin modificar la librería**.

## Qué falta (roadmap)

- **Idempotency keys**: SendGrid, Twilio y FCM no las ofrecen de forma nativa; habría que deduplicar de nuestro lado.
- Circuit breaker por proveedor, para dejar de golpear uno que lleva N fallos seguidos.
- Rate limiting del lado cliente y soporte de `Retry-After` en respuestas 429.
- Adjuntos de email y payloads específicos de plataforma en push (APNs/Android).
- Parseo de respuestas con Jackson y métricas (Micrometer) sobre `NotificationListener`.
- Persistencia / outbox para garantizar entrega ante caídas del proceso.

## Uso de IA

Esta solución se desarrolló con **Claude (Anthropic)** como asistente.

- **Decisiones mías**: el enfoque de librería agnóstica, qué opcionales incluir (async, reintentos, validación,
  eventos, templates, Slack) y cuáles dejar en el roadmap, y la revisión crítica del diseño.
- **Propuesto e implementado por la IA**: el borrador de la arquitectura (SPI, `Transport`, pipeline), los proveedores,
  los tests y este README.
- **Proceso**: analizamos primero el enunciado, definimos la arquitectura y luego iteramos con una revisión crítica
  (SRP del servicio, tests con hilos, failover configurable, jitter, transporte real).
- **Verificación**: los 90 tests se ejecutaron con el lanzador de JUnit sobre JDK 21. La construcción con Maven
  (`mvn clean test`) la verifico yo localmente.

<!-- Ajusta esta sección a lo que realmente hiciste. -->
