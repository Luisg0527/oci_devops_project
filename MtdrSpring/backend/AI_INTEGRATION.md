# Integración de IA — Sprintly RAG

Documento de referencia técnica para el flujo de IA del backend.
Cubre: chat asistente, vectorización de la base, retrieval, despliegue.

**Última actualización:** 2026-05-19
**Branch:** `dev-gustambo`

---

## 1. Stack en uso

| Pieza | Proveedor / Tech | Modelo / versión | Rol |
|---|---|---|---|
| Chat completions (LLM) | **Groq** API | `llama-3.3-70b-versatile` | Genera la respuesta final del asistente |
| Embeddings | **Google Gemini** API | `text-embedding-004` (768d) | Convierte texto → vector para RAG |
| Vector DB | **Qdrant** self-hosted | `qdrant/qdrant:v1.12.4` | Almacena vectores + payload, búsqueda por similitud |
| Fuente de verdad | **Oracle ATP 19c** | (existente) | Source of truth; no guarda vectores |
| Framework HTTP | Spring 6 `RestClient` | Spring Boot 3.5.6 | Cliente genérico para Groq/Gemini/Qdrant |

**No usamos** Spring AI (`spring-ai-*`). Los clientes están hechos a mano sobre `RestClient`.

---

## 2. Arquitectura

```
┌────────────────────────────────────────────────────────────────────┐
│  ORACLE ATP 19c  (source of truth — datos del negocio)             │
│  PROJECTS · SPRINTS · TASKS · USERS · TEAMS · KPI_VALUES ·         │
│  DEPLOYMENTS · INCIDENTS · BOT_INTERACTIONS                        │
└────────────────────────────────────────────────────────────────────┘
        │                                                ▲
        │ Hibernate POST_INSERT/UPDATE/DELETE            │ findById hydrate
        ▼                                                │
┌────────────────────────────────────────────────────────────────────┐
│  SPRING BOOT (pod en OKE, replicas=2)                              │
│                                                                    │
│  HibernateChangeListener ──► EmbeddingChangeEvent (Spring event)   │
│                                            │                       │
│                              @TransactionalEventListener(AFTER_COMMIT)
│                              @Async("embeddingExecutor")           │
│                                            ▼                       │
│                              EmbeddingEventDispatcher              │
│                                            │                       │
│                                            ▼                       │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │ EmbeddingService                                             │  │
│  │   handle(event) | reindexAll() | hydrate(hits)               │  │
│  │     │                                                         │  │
│  │     ├─► CanonicalTextBuilder.text(entity)                    │  │
│  │     ├─► EmbeddingClient.embed(text)         ─►  GEMINI       │  │
│  │     └─► QdrantVectorStore.upsert(...)       ─►  QDRANT       │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                    │
│  AiChatController (/ai/chat)                                       │
│     1. embed(query)                            ─►  GEMINI          │
│     2. Qdrant.search(qvec, topK=20)            ─►  QDRANT          │
│     3. EmbeddingService.hydrate(hits)          ─►  ORACLE          │
│     4. AiContextService.buildMiniSnapshotJson()                    │
│     5. GroqClient.complete(messages)           ─►  GROQ            │
│     6. BotInteractionService.save(...)                             │
└────────────────────────────────────────────────────────────────────┘
                                  │
                  ┌───────────────┴───────────────┐
                  ▼                               ▼
┌─────────────────────────────┐   ┌─────────────────────────────────┐
│  QDRANT (pod en OKE)        │   │  APIs externas (Internet)       │
│  StatefulSet · 1 replica    │   │  - Groq: chat completions       │
│  PVC 10Gi (OCI Block)       │   │  - Gemini: embeddings           │
│  Collection project_context │   │                                 │
│  768d · Cosine distance     │   │                                 │
└─────────────────────────────┘   └─────────────────────────────────┘
```

---

## 3. Vectorización: dónde y cuándo

### 3.1 Bootstrap (una vez al startup)

[`EmbeddingBootstrap.onReady()`](src/main/java/com/ociproject/service/embedding/EmbeddingBootstrap.java)
se ejecuta cuando Spring termina de arrancar (`ApplicationReadyEvent`) y solo
si la colección Qdrant está vacía. Recorre Oracle y vectoriza todo:

```
EmbeddingService.reindexAll()
  ↳ projectRepo.findAll(),   filtra IS_DELETED='Y'
  ↳ sprintRepo.findAll(),    ...
  ↳ taskRepo.findAll(),      ...
  ↳ userRepo.findAll(),      ...
  ↳ teamRepo.findAll(),      ...
  ↳ kpiValueRepo.findAll(),  ...
  ↳ deploymentRepo.findAll()
  ↳ incidentRepo.findAll()
chunks de 100 → Gemini batchEmbedContents → Qdrant upsertBatch
```

Si Qdrant ya tiene puntos, se salta. Idempotente.

### 3.2 Incremental (en cada cambio)

[`HibernateChangeListener`](src/main/java/com/ociproject/service/embedding/HibernateChangeListener.java)
se registra en el `EntityManagerFactory` y escucha eventos nativos de
Hibernate — **no se modifica ninguna entidad JPA**.

```
Cualquier save/update/delete vía JPA
  → Hibernate dispara POST_INSERT / POST_UPDATE / POST_DELETE
  → HibernateChangeListener.classify(entity)
  → publishEvent(EmbeddingChangeEvent.upsert | .delete)
  → (TX commit)
  → EmbeddingEventDispatcher.onChange()  ← @Async, hilo embedding-N
  → EmbeddingService.handle(event)
      ↳ repo.findById(id)                  ← relee fresco
      ↳ textBuilder.text(entity)
      ↳ embeddingClient.embed(text)        ← Gemini
      ↳ vectorStore.upsert(...)            ← Qdrant
```

**Soft-delete** (entity.deleted = true) se traduce automáticamente a
`Kind.DELETE` para que el vector se elimine al mismo tiempo que el usuario
ve la entidad como borrada.

### 3.3 Entidades vectorizadas

| Entidad | SourceType | Texto canónico (resumen) |
|---|---|---|
| `Project` | `PROJECT` | Nombre, estado, manager, descripción (truncada 1500) |
| `Sprint` | `SPRINT` | Nombre, estado, fechas, total_hours |
| `Task` | `TASK` | Título, estado, prioridad, proyecto, sprint, assignee, fechas, descripción |
| `User` | `USER` | Nombre, rol, equipo, status |
| `Team` | `TEAM` | Nombre, descripción |
| `KpiValue` | `KPI_VALUE` | Tipo, scope, valor, contexto, fecha |
| `Deployment` | `DEPLOYMENT` | Versión, env, status, proyecto, fecha |
| `Incident` | `INCIDENT` | Tipo, severidad, proyecto, fechas, descripción |

Construcción de textos: [`CanonicalTextBuilder`](src/main/java/com/ociproject/service/embedding/CanonicalTextBuilder.java).

### 3.4 Point IDs determinísticos

```java
QdrantVectorStore.pointId(type, id)
  = UUID.nameUUIDFromBytes((type.name() + ":" + id).getBytes())
```

Eso significa que el upsert es idempotente: el mismo `(SourceType, sourceId)`
produce siempre el mismo UUID en Qdrant. Re-correr el bootstrap no duplica,
sobrescribe.

---

## 4. Retrieval: el flujo `/ai/chat`

[`AiChatController.chat()`](src/main/java/com/ociproject/controller/AiChatController.java)

```
POST /ai/chat
  Headers: Authorization: Bearer <JWT>
  Body: { "message": "...", "history": [...] }

  ┌─────────────────────────────────────────────────────────────┐
  │ buildContext(query)                                         │
  │   if (rag enabled && Gemini configured):                    │
  │     ┌─────────────────────────────────────────┐             │
  │     │ qvec = gemini.embed(query)              │             │
  │     │ hits = qdrant.search(qvec, top-20)      │             │
  │     │ docs = oracle.hydrate(hits, top-8)      │             │
  │     │ mini = aiContext.buildMiniSnapshotJson()│             │
  │     │ return ContextBundle("rag", mini, docs) │             │
  │     └─────────────────────────────────────────┘             │
  │   catch (anything):                                         │
  │     return ContextBundle("snapshot", fullSnapshot)  ← fallback
  └─────────────────────────────────────────────────────────────┘
  ┌─────────────────────────────────────────────────────────────┐
  │ buildMessages(request, user, ctx)                           │
  │   [system] Sprintly prompt + mini-snapshot + top-8 docs     │
  │   [history] últimos 10 turnos (cap)                         │
  │   [user] mensaje actual                                     │
  └─────────────────────────────────────────────────────────────┘
  ┌─────────────────────────────────────────────────────────────┐
  │ groqClient.complete(messages)  ─►  Groq llama-3.3-70b       │
  └─────────────────────────────────────────────────────────────┘
  ┌─────────────────────────────────────────────────────────────┐
  │ botInteractionService.save(...)  ─►  BOT_INTERACTIONS       │
  └─────────────────────────────────────────────────────────────┘

Response: { "reply": "...", "model": "llama-3.3-70b-versatile", "elapsedMs": 1234 }
```

**Fallback transparente:** si Gemini falla, Qdrant no responde, o cualquier
parte del pipeline RAG truena, el controller cae automáticamente al
comportamiento original (snapshot completo de la org). El usuario nunca ve
una caída del RAG. Log: `mode=rag` o `mode=snapshot`.

---

## 5. Configuración

Las propiedades viven en
[`application.properties`](src/main/resources/application.properties).
Todas se sobrescriben con env vars (formato `${VAR:default}`).

### 5.1 Variables de entorno

| Var | Default | Notas |
|---|---|---|
| `GROQ_API_KEY` | _(vacío)_ | Required para chat |
| `GEMINI_API_KEY` | _(vacío)_ | Required para RAG. Sin esto → fallback automático |
| `QDRANT_URL` | `http://qdrant:6333` | En local: `http://localhost:6333` |
| `QDRANT_COLLECTION` | `project_context` | |
| `QDRANT_API_KEY` | _(vacío)_ | Solo si Qdrant tiene auth habilitado |
| `RAG_ENABLED` | `true` | Toggle global de RAG |
| `RAG_BOOTSTRAP` | `true` | Toggle del bulk index inicial |

### 5.2 Propiedades Spring (no requieren cambio normal)

```properties
gemini.api-url=https://generativelanguage.googleapis.com/v1beta
gemini.model=models/text-embedding-004
gemini.dimensions=768
gemini.batch-size=100
gemini.timeout-ms=15000

qdrant.timeout-ms=10000

rag.top-k=20             # candidatos pedidos a Qdrant
rag.hydrated-limit=8     # cuántos llegan al prompt del LLM
```

### 5.3 Tuning rápido

- **Calidad baja:** sube `rag.top-k` (20 → 40) y `rag.hydrated-limit` (8 → 12)
- **Latencia alta:** baja `rag.hydrated-limit` y `groq.max-context-chars`
- **Costo Gemini alto (improbable en free tier):** baja `gemini.batch-size` y
  apaga `RAG_BOOTSTRAP` en producción tras el primer arranque

---

## 6. Desarrollo local

### 6.1 Prerequisitos

- JDK **21** (no 25 — Lombok 1.18.36 no lo soporta)
- Maven 3.9+
- Docker Desktop
- Wallet de Oracle ATP en `src/main/resources/Wallet_reacttodoqk8zv/`

### 6.2 Receta (PowerShell)

```powershell
# Terminal 1: Qdrant local
docker run -d --name qdrant `
  -p 6333:6333 -p 6334:6334 `
  -v qdrant_storage:/qdrant/storage `
  qdrant/qdrant:v1.12.4

# Terminal 2: backend
$env:GROQ_API_KEY   = "gsk_..."
$env:GEMINI_API_KEY = "AIzaSy..."
$env:QDRANT_URL     = "http://localhost:6333"
$env:JAVA_HOME      = "C:\Program Files\Java\jdk-21"
$env:PATH           = "$env:JAVA_HOME\bin;$env:PATH"

cd MtdrSpring/backend
mvn spring-boot:run `
  "-Dspring-boot.run.jvmArguments=-Dfrontend-maven-plugin.skip=true"
```

### 6.3 Cómo obtener las keys

- Groq: https://console.groq.com/keys (free tier ~30 RPM)
- Gemini: https://aistudio.google.com/app/apikey (free tier 1500 RPM)

### 6.4 Verificación rápida

Logs esperados al arrancar:

```
HibernateChangeListener registered for INSERT/UPDATE/DELETE.
Started Application in X seconds
Created Qdrant collection 'project_context' (dim=768, Cosine).
Bootstrap: indexed N/M TASK rows.
...
Bootstrap done: indexed N points in M ms.
```

Probar el chat (después de hacer login y obtener JWT):

```powershell
curl -X POST http://localhost:8080/ai/chat `
  -H "Authorization: Bearer $token" `
  -H "Content-Type: application/json" `
  -d '{\"message\":\"¿Cómo va el sprint actual?\",\"history\":[]}'
```

Logs del controller mostrarán: `AI chat OK userId=X mode=rag hits=20 (1234ms)`.

Si dice `mode=snapshot` → el RAG cayó al fallback, revisa keys/Qdrant.

### 6.5 Dashboard de Qdrant

http://localhost:6333/dashboard — ver colecciones, puntos, métricas.

---

## 7. Despliegue en OKE (producción)

### 7.1 Manifests involucrados

- [`qdrant.yaml`](src/main/resources/qdrant.yaml) — StatefulSet + Service + PVC 10Gi
- [`todolistapp-springboot.yaml`](src/main/resources/todolistapp-springboot.yaml) — backend con envs RAG ya configurados

### 7.2 Pasos para activar

```bash
# 1. Secret con la API key de Gemini
kubectl -n mtdrworkshop create secret generic gemini-api \
  --from-literal=apikey=AIzaSy...

# 2. Desplegar Qdrant en el cluster
kubectl -n mtdrworkshop apply -f qdrant.yaml

# 3. Redesplegar backend (build_spec.yaml + deploy.sh existentes lo hacen)
./MtdrSpring/backend/deploy.sh
```

### 7.3 Variables ya cableadas en el manifest del backend

```yaml
- name: GROQ_API_KEY     # secret: groq-api / key: apikey
- name: GEMINI_API_KEY   # secret: gemini-api / key: apikey
- name: QDRANT_URL       # http://qdrant:6333  (resolución vía Service)
- name: QDRANT_COLLECTION # project_context
- name: RAG_ENABLED      # true
- name: RAG_BOOTSTRAP    # true
```

### 7.4 Multi-réplica

El backend corre con `replicas: 2`. Diseño anticipado:

- Qdrant es la única fuente de vectores → ambas réplicas leen lo mismo
- `HibernateChangeListener` corre en la réplica que recibe el write → solo
  un upsert se dispara, evitando duplicación de trabajo
- `EmbeddingBootstrap` chequea `vectorStore.count() > 0` antes de re-indexar
  → si una réplica arranca primero y bootstrappea, la segunda lo detecta y
  no hace nada (idempotente además gracias a los UUIDs determinísticos)

### 7.5 Backup / DR

Qdrant tiene snapshot API. Recomendación: cron job semanal que llame
`POST /collections/project_context/snapshots` y suba el `.snapshot` a OCI
Object Storage. Si se pierde el PV, alternativa: borrar la collection y
re-ejecutar `EmbeddingBootstrap` (Oracle es source of truth).

---

## 8. Inventario de archivos nuevos

```
config/
├── AsyncConfig.java                ← @EnableAsync + pool "embeddingExecutor"
├── GeminiProperties.java
├── QdrantProperties.java
└── RagProperties.java

service/embedding/
├── SourceType.java                 ← enum de tipos vectorizables
├── EmbeddingChangeEvent.java       ← evento Spring (UPSERT/DELETE)
├── EmbeddingClient.java            ← interfaz swappable
├── GeminiEmbeddingClient.java      ← impl. Gemini text-embedding-004
├── QdrantVectorStore.java          ← cliente HTTP de Qdrant
├── CanonicalTextBuilder.java       ← texto canónico por entidad
├── EmbeddingService.java           ← orquestador: handle / reindex / hydrate
├── EmbeddingEventDispatcher.java   ← @Async @TransactionalEventListener
├── HibernateChangeListener.java    ← hook a POST_INSERT/UPDATE/DELETE
└── EmbeddingBootstrap.java         ← bulk index al startup
```

## Archivos modificados (mínimo)

- `application.properties` — secciones Gemini + Qdrant + RAG (aditivo)
- `AiContextService.java` — añadido `buildMiniSnapshotJson()` (aditivo)
- `AiChatController.java` — flujo RAG con fallback
- `todolistapp-springboot.yaml` — envs nuevas
- **Sin cambios en:** `pom.xml`, entidades, repositorios, otros services/controllers

---

## 9. Decisiones técnicas y por qué

| Decisión | Razón |
|---|---|
| Qdrant self-hosted en OKE (no Cloud) | Latencia ~1ms intra-cluster, gratis siempre, sin trials que expiran |
| Gemini para embeddings | Free tier 1500 RPM, soporte nativo a español, 768d compatible con muchos modelos |
| HTTP nativo via `RestClient` (no SDKs) | Cero dependencias nuevas en `pom.xml`, control total |
| Hibernate `EventListenerRegistry` (no `@EntityListeners`) | No requiere modificar entidades; transparente para servicios y repositorios |
| `@TransactionalEventListener(AFTER_COMMIT)` + `@Async` | Si la TX hace rollback, el embedding nunca se publica → consistencia |
| Fallback al snapshot completo | Una caída de RAG no debe romper el chat existente |
| UUID determinístico por `(type, id)` | Upsert idempotente, re-bootstrap seguro |
| Oracle 19c **sin** tabla de vectores | 19c no tiene tipo VECTOR; Qdrant aporta indexación nativa |

---

## 10. Limitaciones conocidas / pendiente

- **No usamos Spring AI**: hay que mantener nuestros DTOs Groq/Gemini a mano.
  Migrar a `spring-ai-*` reduciría boilerplate (futuro refactor opcional).
- **Sin ACL en el retrieval**: actualmente Qdrant devuelve top-K sin filtrar
  por proyectos del usuario. El payload ya guarda `project_id` para activar
  el filtro cuando definamos la política de permisos.
- **Sin re-ranking**: usamos directamente el score coseno de Qdrant. Para
  precisión alta se podría agregar un re-rank (Cohere rerank API o reglas).
- **Texto canónico fijo**: cambios en `CanonicalTextBuilder` requieren
  re-indexar manualmente (`POST /actuator/...` o re-arrancar con la
  collection vacía).
- **Tasks históricas truncadas**: el `buildMiniSnapshotJson` no incluye
  tasks (delegamos a RAG). Si Gemini cae y caemos al fallback, sí incluye
  todo. Asimetría aceptada por simplicidad.

---

## 11. Operativa

### Re-indexar todo manualmente

No hay endpoint hoy. Opciones:

1. Borrar la collection desde el dashboard de Qdrant y reiniciar el pod →
   `EmbeddingBootstrap` re-indexa automáticamente.
2. Crear un endpoint `POST /admin/embeddings/reindex` (futuro).

### Apagar RAG temporalmente

```bash
kubectl -n mtdrworkshop set env deployment/todolistapp-springboot-deployment RAG_ENABLED=false
kubectl -n mtdrworkshop rollout restart deployment todolistapp-springboot-deployment
```

El `/ai/chat` vuelve al snapshot completo automáticamente.

### Monitoreo recomendado

- Log line `AI chat OK userId=X mode=rag hits=20 (Yms)` → métrica de modo y latencia
- Qdrant `/metrics` (Prometheus) → tamaño de collection, latencia de search
- Errores `Embedding ... failed: ...` → tasa de fallos del pipeline async
