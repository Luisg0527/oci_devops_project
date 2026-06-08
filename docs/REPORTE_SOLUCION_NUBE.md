<div align="center">

# Tecnológico de Monterrey

**Instituto Tecnológico y de Estudios Superiores de Monterrey**
**Campus Monterrey**

<br>

**Actividad de Clase: Implantación de una Solución en la Nube**
**Arquitectura Escalable, Tolerante a Fallos y Optimización de Recursos**

<br><br>

Iñigo Gonzalez A01723229
Victor Martinez A01723093
Paolo Gaya A01722922
Miguel Angel Alvarez A01722925
Jinhyuk Park A01286288
Luis Garza Gómez Morin A00839388

<br><br>

Desarrollo e implantación de sistemas de software (Gpo 106)

<br><br>

7 de junio de 2026

</div>

<div style="page-break-after: always;"></div>

---

## Introducción

El presente reporte documenta la implantación en la nube de nuestro sistema de gestión de proyectos *MyTodoList / Sprintly*, desplegado sobre Oracle Cloud Infrastructure (OCI). Más allá de tener una aplicación funcional, el objetivo de esta actividad fue dotar a la solución de las características que se esperan de un sistema productivo: que pueda escalar ante la demanda, que tolere fallos sin caerse, que sea observable, y que consuma los recursos de forma responsable y trazable. A lo largo del documento explicamos qué encontramos al inicio, qué decisiones tomamos y qué quedó implementado, procurando justificar cada cambio en lugar de solo enumerarlo.

---

## Diseño de arquitectura escalable y tolerante a fallos

Nuestra aplicación es un servicio de Spring Boot que expone una API REST y que integra varios servicios de respaldo: una base de datos Oracle Autonomous (ATP), un modelo de lenguaje (Groq), un servicio de *embeddings* (Google Gemini) y una base de datos vectorial (Qdrant). Desde el diseño procuramos que la aplicación fuera **sin estado** (*stateless*): la sesión del usuario vive en el token JWT que viaja en cada petición y no en la memoria del servidor. Esta decisión, que puede parecer menor, es la que habilita todo lo demás, pues permite crear y destruir réplicas de la aplicación libremente sin perder la sesión de nadie.

Sobre esa base, el despliegue en Oracle Kubernetes Engine (OKE) corre con dos réplicas distribuidas entre nodos distintos mediante restricciones de topología, de manera que la caída de un nodo no deja al servicio sin atención. La tolerancia a fallos también se trabajó a nivel de la lógica del asistente de IA: si la base vectorial o el servicio de *embeddings* no responden, el sistema no falla, sino que recurre a un mecanismo de respaldo que construye el contexto a partir de un *snapshot* completo de la organización. El usuario nunca percibe la interrupción. A esto se suma el uso de una base de datos gestionada, que aporta respaldos automáticos, y de un *pool* de conexiones que absorbe los picos de carga.

---

## Balanceo de carga y auto-escalamiento

El balanceo de carga se resuelve a través de un *Load Balancer* de OCI, que reparte el tráfico entrante entre las réplicas disponibles de la aplicación. El punto débil que identificamos fue la ausencia de auto-escalamiento: el sistema mantenía un número fijo de réplicas sin importar la carga, lo cual es ineficiente tanto en momentos de saturación como en momentos de inactividad.

Para corregirlo incorporamos un *HorizontalPodAutoscaler*, que ajusta automáticamente el número de réplicas de la aplicación entre dos y cinco según el uso de CPU, tomando como objetivo un setenta por ciento de utilización. De este modo, el sistema crece cuando la demanda lo exige y se contrae cuando la actividad baja, sin intervención manual. Cabe señalar que este mecanismo solo es posible porque previamente definimos los límites de recursos de cada contenedor, requisito indispensable para que Kubernetes pueda tomar decisiones de escalado informadas.

---

## Optimización de recursos y rendimiento

La optimización del rendimiento se abordó en varios frentes. En la capa de aplicación definimos *requests* y *limits* de CPU y memoria para cada contenedor, lo que permite al planificador de Kubernetes ubicar los *pods* de forma eficiente y evitar tanto el desperdicio como la contención. En la integración con los servicios de IA aprovechamos el procesamiento por lotes de los *embeddings* y establecimos topes en el pipeline de recuperación de contexto, acotando así el cómputo y el tamaño de la información enviada al modelo de lenguaje. Finalmente, el *pool* de conexiones a la base de datos reduce el costo de abrir y cerrar conexiones repetidamente. Cada una de estas medidas busca el mismo fin: hacer más con menos, sin comprometer la experiencia del usuario.

---

## Monitoreo, observabilidad y control

Una solución que no se puede observar no se puede gestionar. Por ello incorporamos un módulo de observabilidad basado en Spring Boot Actuator y Micrometer, que expone el estado de salud del servicio y un conjunto de métricas en formato Prometheus. Estas métricas pueden ser recolectadas automáticamente gracias a las anotaciones de descubrimiento que añadimos a los *pods*, y constituyen la base para configurar alertas sobre indicadores críticos como la latencia del asistente o la tasa de errores. Asimismo, ajustamos el manejo de *logs* para que se emitan como flujo de eventos hacia la salida estándar, conforme a las buenas prácticas de aplicaciones nativas de la nube, de manera que la plataforma pueda centralizarlos.

En cuanto al manejo de errores, nuestra integración con las APIs externas contempla excepciones específicas por cada servicio, control de tiempos de espera y degradación elegante ante fallos, de modo que el sistema responde de forma controlada en lugar de propagar errores al usuario. Como control de gobierno adicional, la API mantiene un esquema de versionado explícito, lo que permite evolucionar los contratos sin romper a los consumidores existentes.

---

## Estrategia de optimización de costos

El control del gasto fue una preocupación constante a lo largo de la actividad. Aprovechamos los recursos de capa gratuita siempre que fue posible: la base de datos Autonomous opera bajo el esquema *Always Free*, y tanto el modelo de lenguaje como el servicio de *embeddings* se consumen en sus niveles gratuitos, lo que elimina el costo de inferencia. La base de datos vectorial se aloja dentro del propio clúster, evitando contratar un servicio gestionado adicional.

Identificamos con claridad los dos recursos que sí generan costo recurrente —el *node pool* de cómputo y el *Load Balancer*— para poder vigilarlos y apagarlos primero. Como oportunidad de mejora documentada, dejamos señalada la posibilidad de migrar el cómputo a instancias Ampere de capa gratuita, lo que reduciría el gasto a prácticamente cero. El auto-escalamiento descrito anteriormente contribuye también a la contención de costos, al no mantener recursos encendidos cuando no se necesitan.

---

## Uso responsable de recursos: etiquetado y limpieza

Para garantizar la trazabilidad del gasto y una limpieza ordenada, etiquetamos todos los recursos facturables definidos por infraestructura como código con un conjunto común de *tags*: proyecto, ambiente, responsable, centro de costo y origen de gestión. Estas etiquetas permiten, desde la consola de análisis de costos de OCI, filtrar y agrupar el gasto del proyecto, así como localizar con precisión qué recursos deben eliminarse al cierre.

La limpieza se sistematizó mediante un *script* reproducible que primero inventaría los recursos a partir de sus etiquetas, luego elimina las cargas de trabajo del clúster —liberando el *Load Balancer* y el volumen persistente de la base vectorial, que de otro modo quedaría huérfano— y finalmente destruye la infraestructura provisionada con Terraform. De esta manera evitamos dejar residuos que generen costos silenciosos después de terminada la actividad.

---

## Conclusión

A lo largo de esta actividad transformamos una aplicación funcional en una solución con características nativas de la nube: escalable, tolerante a fallos, observable y consciente de su consumo. Encontramos que la mayor parte del valor no estuvo en escribir más código, sino en tomar decisiones de arquitectura acertadas —hacer la aplicación *stateless*, externalizar la configuración, desacoplar los servicios de respaldo— y en complementarlas con prácticas de operación responsables como el etiquetado y la limpieza. Quedan oportunidades de mejora claramente identificadas, principalmente en la profundización del monitoreo y en la migración del cómputo a capa gratuita, pero la solución implantada cumple con los criterios de escalabilidad, tolerancia a fallos, optimización y sostenibilidad planteados, y deja una ruta documentada para seguir madurando.
