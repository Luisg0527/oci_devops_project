# Contexto del Proyecto — OCI DevOps / "Sprintly"

> Documento de contexto integral: propósito, dominio, requerimientos, módulos (tasks),
> arquitectura, stack y operación. Generado a partir del estado real del repositorio.

---

## 1. Resumen ejecutivo

Plataforma de **gestión de proyectos y seguimiento DevOps** construida sobre la plantilla
*Oracle MyToDoRegion (MTDR) Spring Workshop*, extendida con:

- Gestión ágil completa: **proyectos → sprints → tareas**, equipos, usuarios y roles.
- **Métricas DevOps tipo DORA**: despliegues, incidentes, tiempos de recuperación y KPIs.
- **Asistente de IA "Sprintly"**: chatbot con LLM (Groq · Llama 3.3) + **RAG** (embeddings Gemini + Qdrant).
- **Bot de Telegram** para interacción con tareas.
- Despliegue **cloud-native en Oracle Cloud (OCI)** con Kubernetes (OKE), IaC (Terraform) y CI/CD.

**Repositorio:** monorepo. Backend Spring Boot + frontend React empaquetados en un único artefacto (JAR → imagen Docker).

| Dato | Valor |
|---|---|
| Rama principal | `main` |
| Estructura | `MtdrSpring/backend` (app), `MtdrSpring/terraform` (IaC), `MtdrSpring/utils` (scripts) |
| Artefacto | `MyTodoList-0.0.1-SNAPSHOT.jar` |
| Namespace K8s | `mtdrworkshop` |
| Equipo DB | `EQUIPO65` |

---

## 2. Modelo de dominio (entidades)

| Entidad | Descripción | Estados / Enums clave |
|---|---|---|
| **User** | Usuario del sistema; vinculable a Telegram | `ACTIVE`, `INACTIVE`, `LOCKED` |
| **Role** | Rol/permiso del usuario | — |
| **Team** | Equipo de trabajo | — |
| **Project** | Proyecto gestionado, con manager y horas totales | `ACTIVE`, `ON_HOLD`, `COMPLETED`, `CANCELLED` |
| **ProjectMember** | Relación usuario↔proyecto (clave compuesta) | — |
| **Sprint** | Iteración con fechas inicio/fin | `PLANNED`, `ACTIVE`, `CLOSED` |
| **ProjectSprint** | Relación proyecto↔sprint (clave compuesta) | — |
| **Task** | Tarea: título, descripción, asignación, horas | Stage: `BACKLOG/SPRINT/COMPLETED` · Status: `PENDING/IN_PROGRESS/DONE/CANCELLED/REOPENED` · Priority: `LOW/MEDIUM/HIGH` |
| **TaskStatusHistory** | Historial de cambios de estado de tarea | — |
| **TaskSprintHistory** | Historial de movimientos de tarea entre sprints | — |
| **Deployment** | Registro de despliegue (métrica DevOps) | Env: `DEV/QA/STAGING/PRODUCTION` · Status: `SUCCESS/FAILED/IN_PROGRESS` · `recoveryTimeMin` |
| **Incident** | Incidente operativo | Severity: `LOW/MEDIUM/HIGH/CRITICAL` · `occurredAt`/`resolvedAt` |
| **KpiType** / **KpiValue** | Definición y valores de KPIs (categoría, unidad) | — |
| **BotInteraction** | Interacción registrada del bot de Telegram | — |
| **AuditLog** | Bitácora de auditoría de acciones | — |
| **LlmAnalysis** | Resultado de análisis generado por LLM | — |
| **UserCredential** | Credenciales para login (separadas del User) | — |

> Patrón común: **soft-delete** (`IS_DELETED`/`DELETED_AT`) y timestamps (`CREATED_AT`/`UPDATED_AT`).
> Esquema gestionado por la BD (`spring.jpa.hibernate.ddl-auto=none`), no por Hibernate.

---

## 3. Requerimientos funcionales

Derivados de los controladores REST existentes (`com.ociproject.controller`):

1. **Autenticación y seguridad** (`AuthController`) — login, refresh token (JWT stateless).
2. **Gestión de usuarios** (`UserController`) y **roles** (`RoleController`).
3. **Gestión de equipos** (`TeamController`).
4. **Gestión de proyectos** (`ProjectController`) — CRUD, miembros, manager.
5. **Gestión de sprints** (`SprintController`) — planificación y ciclo de vida.
6. **Gestión de tareas** (`TaskController`) — CRUD, asignación, estados, time tracking.
7. **Tablero / dashboard** (`DashboardController`) — métricas agregadas, workload.
8. **Reportes** (`ReportController`).
9. **Métricas DevOps**:
   - Despliegues (`DeploymentController`).
   - Incidentes (`IncidentController`).
   - KPIs: tipos (`KpiTypeController`) y valores (`KpiValueController`).
10. **Asistente IA** (`AiChatController`) — chat con contexto RAG.
11. **Análisis LLM** (`LlmAnalysisController`) — análisis automatizado de datos del proyecto.
12. **Bot de Telegram** (`BotController`) — gestión de tareas vía chat.
13. **Auditoría** (`AuditLogController`) — consulta de bitácora.

**Documentación de API:** Swagger UI en `/swagger-ui.html` · OpenAPI en `/v3/api-docs`.

---

## 4. Requerimientos no funcionales

| Categoría | Requerimiento |
|---|---|
| **Seguridad** | JWT (access 1h / refresh 24h), Spring Security, credenciales en K8s Secrets, soft-delete y auditoría. |
| **Disponibilidad** | 2–5 réplicas con HPA (CPU 70%), probes startup/liveness/readiness, rollout sin downtime. |
| **Escalabilidad** | HorizontalPodAutoscaler + topology spread por host; pool UCP (15 init / 10 min / 30 max). |
| **Observabilidad** | Spring Actuator + Micrometer/Prometheus en `/actuator/prometheus`. |
| **Rendimiento IA** | Executor dedicado para upserts de embeddings; límites de contexto (25k chars) y timeouts por servicio. |
| **Reproducibilidad** | Infra 100% en Terraform; build determinista vía Maven + Docker. |
| **Calidad** | Super-Linter en CI; pruebas unitarias por servicio (JUnit + Mockito). |
| **Portabilidad** | Artefacto único contenerizado; config por variables de entorno. |

---

## 5. Módulos / Tasks (estado del trabajo)

Módulos **implementados** (presentes en `src/main/java`):

- [x] Núcleo de gestión: usuarios, roles, equipos, proyectos, sprints, tareas.
- [x] Historial de tareas (estado y sprint) + time tracking.
- [x] Métricas DevOps: deployments, incidents, KPIs.
- [x] Seguridad JWT + login con credenciales separadas.
- [x] Auditoría (AuditLog) y manejo global de excepciones.
- [x] Chatbot IA "Sprintly" (Groq Llama 3.3) con pipeline RAG.
- [x] RAG: embeddings Gemini, Qdrant vector store, bootstrap e indexación por eventos de Hibernate.
- [x] Bot de Telegram (long-polling).
- [x] Frontend React (dashboard manager, backlog, proyectos, reportes, gestión de equipo, login).
- [x] Dashboard y reportes.
- [x] Suite de pruebas unitarias de servicios (13 clases de test).
- [x] CI (Super-Linter) + Build Spec de OCI DevOps + IaC Terraform.

> No existe `README.md` con backlog formal en el repo; el estado anterior se infiere del código y del historial git.
> Pendiente sugerido: documentar endpoints, agregar pruebas de integración y completar el README.

---

## 6. Arquitectura

```
[ Telegram ]      [ Navegador / React SPA ]
      |                     |
      v                     v
            OCI Load Balancer (IP_HASH)
                     |
        ┌────────────────────────────┐
        │  OKE (Kubernetes) — ns      │
        │  mtdrworkshop               │
        │                             │
        │  Deployment (2–5 pods)      │
        │   Spring Boot 3.5 / Java 21 │
        │   - REST API + Swagger      │
        │   - Frontend estático       │
        │   - Telegram bot            │
        │   - Actuator/Prometheus     │
        │        |          |         │
        │        v          v         │
        │   Qdrant      (HTTP→ Groq,  │
        │  (vectores)    Gemini APIs) │
        └────────────────────────────┘
                     |
                     v
        Oracle Autonomous DB (ATP, wallet)
```

- **Frontend** se compila con `frontend-maven-plugin` y se copia a `static/` del JAR → servido por Spring Boot.
- **RAG**: cambios en entidades → `HibernateChangeListener` → `EmbeddingEventDispatcher` → Gemini embeddings → Qdrant (`project_context`). El chat consulta Qdrant para construir contexto del LLM.

---

## 7. Stack tecnológico (resumen)

| Capa | Tecnologías |
|---|---|
| **Backend** | Java 21, Spring Boot 3.5.6 (Web, Data JPA, Security, Validation, Actuator), Lombok, springdoc-openapi 2.8.8 |
| **Auth** | Spring Security + JWT (jjwt 0.12.6) |
| **Frontend** | React 17, react-scripts 5, TypeScript 4, Material UI 5, react-router 5, moment |
| **IA / RAG** | Groq (`llama-3.3-70b-versatile`), Gemini embeddings (`gemini-embedding-001`, 768d), Qdrant 1.12.4 |
| **Mensajería** | Telegram Bots 9.1.0 (long-polling) |
| **Datos** | Oracle Autonomous Database (ATP) + UCP, Oracle JDBC ojdbc11 23.9 |
| **Infra OCI** | OKE, OCIR, API Gateway, VCN, Object Storage, Load Balancer |
| **IaC** | Terraform (provider `hashicorp/oci` 4.42.0) |
| **CI/CD** | OCI DevOps Build Service (`build_spec.yaml`, GraalVM), GitHub Actions (Super-Linter v7), Maven, Docker, kubectl |
| **Observabilidad** | Micrometer + Prometheus, Spring Actuator |
| **Pruebas** | JUnit 5, Mockito, spring-security-test |

---

## 8. CI/CD y despliegue

- **GitHub Actions** (`.github/workflows/linter.yml`): Super-Linter en push/PR a `main` (Java, JS/JSX, CSS, YAML, XML, Terraform fmt+tflint, Dockerfile hadolint, Bash).
- **OCI DevOps Build** (`build_spec.yaml`): instala GraalVM, login a OCIR (`mx-queretaro-1.ocir.io`), ejecuta `backend/build.sh`.
- **Build** (`build.sh`): `mvn clean package` → `docker build` → `docker push` a OCIR (`todolistapp-springboot:0.1`).
- **Deploy** (`deploy.sh`): genera manifiesto con `sed`, `kubectl apply` + `rollout restart/status` (timeout 120s); inyección Istio opcional.
- **Infra** (`MtdrSpring/setup.sh` / `destroy.sh` + `terraform/`): provisiona OKE, ATP, VCN, OCIR, Object Storage, API Gateway.

**Manifiestos K8s:** `todolistapp-springboot.yaml` (Deployment + 2 Services + HPA) y `qdrant.yaml` (StatefulSet + Service + PVC 10Gi).

---

## 9. Configuración (variables de entorno clave)

| Variable | Uso |
|---|---|
| `db_url`, `db_user`, `dbpassword` | Conexión Oracle ATP (wallet en `/mtdrworkshop/creds`) |
| `OCI_REGION` | Región OCI |
| `ui_username`, `ui_password` | Credenciales de admin del frontend |
| `GROQ_API_KEY` | Chatbot LLM (secret `groq-api`) |
| `GEMINI_API_KEY` | Embeddings (secret `gemini-api`) |
| `QDRANT_URL`, `QDRANT_COLLECTION` | Vector store (`http://qdrant:6333`, `project_context`) |
| `RAG_ENABLED`, `RAG_BOOTSTRAP` | Activación e indexación inicial del RAG |
| `app.jwt.expiration-ms` / `refresh-expiration-ms` | 3 600 000 / 86 400 000 |
| `telegram.bot.name` | `botbootbotboot_bot` |

---

## 10. Operación local y comandos

```bash
# Build completo (frontend + backend + JAR)
cd MtdrSpring/backend && ./mvnw clean package

# Ejecutar local (requiere wallet ATP en src/main/resources)
java -jar target/MyTodoList-0.0.1-SNAPSHOT.jar

# Frontend en modo dev (proxy a :8080)
cd src/main/frontend && npm start

# Qdrant local
docker run -p 6333:6333 qdrant/qdrant:v1.12.4

# Provisionar / destruir infra OCI
cd MtdrSpring && ./setup.sh   # ./destroy.sh
```

- **Pruebas:** `./mvnw test` (servicios con JUnit 5 + Mockito).
- **API docs (local):** `http://localhost:8080/swagger-ui.html`.

---

## 11. Notas y deuda técnica

- **`README.md` vacío** — falta documentación de arranque y de API.
- Imagen Docker basada en `openjdk:22-jdk` mientras el build de CI usa GraalVM 22; alinear versiones.
- `gemini-embedding-001` reemplazó a `text-embedding-004` (deprecado 2026-01): **re-indexar Qdrant** tras cambios de modelo.
- Faltan **pruebas de integración** (solo unitarias de servicio) y validación E2E del frontend.
- Secretos gestionados vía K8s Secrets; verificar que no haya credenciales en archivos versionados (`PS_ByPass.txt`, `at.cfg`).
