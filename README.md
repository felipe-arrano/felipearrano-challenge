# Tenpo Challenge - API REST en Spring Boot

## 1. Descripción del proyecto

Esta API REST fue desarrollada como solución al desafío técnico propuesto por **Tenpo**. Sus funcionalidades principales son:

* **Cálculo con Porcentaje Externo:** Expone un endpoint (`GET /api/v1/calculations/sum-with-percentage`) que suma dos números y aplica un porcentaje adicional obtenido de un servicio externo (simulado). Esta operación incluye manejo de resiliencia (**Retry**, **Circuit Breaker**) y una caché distribuida (**Redis**) con **fallback** para asegurar la disponibilidad y eficiencia.
  
    * Caché del porcentaje y resilencia:
        * El porcentaje obtenido del servicio externo es almacenado en una caché **Redis**.
        * El valor de la caché se utiliza en el caso de que el servicio externo falle 3 veces (3 intentos).
        * Si no hay un valor previamente almacenado en caché, la API debe responder un error HTTP adecuado.
        * El valor de la cache es válido por 30 minutos.
        * (Bonus) Se implementa el patrón **Circuit Breaker** para aportar aún más residencia.


* **Auditoría y Consulta de Historial:** Registra de forma asíncrona los detalles de todas las llamadas a la API en una base de datos **PostgreSQL**. Expone un endpoint (`GET /api/v1/history`) para consultar este historial de forma paginada. Al ser una funcionalidad asíncrona no afecta el tiempo de respuesta del servicio (endpoint) principal invocado, ni tampoco lo afecta si el registro falla.


La aplicación también implementa los siguiente requerimientos: 
* **Limitación de tasa de llamadas a la API:** La API soporta 3 RPM por defecto. Si se excede este umbral la aplicación devuelve un error HTTP 429 Too Many Request y un mensaje descriptivo.
* **Manejo centralizado de errores:** Se manejan los errores 4XX y 5XX de manera centralizada y estandarizada.
* **Documentación vía Swagger:** Se documenta y detalla el uso de los endpoints de la API.
* **Contenerizacion con Docker:** Se incluyen las instrucciones para contenerizar la aplicación para que pueda ser replicada y escalada.


## 2. Instrucciones para ejecutar el servicio y la base de datos localmente

**Pre-requisitos:**

* Docker instalado y corriendo.
* Docker Compose instalado.
* Git (para clonar el repositorio).

**Pasos:**

1.  **Clonar el repositorio:**
    ```bash
    git clone https://github.com/felipe-arrano/felipearrano-challenge.git
    ```

1.  **Ingresar al repositorio:**
    ```bash
    cd felipearrano-challenge 
    ```

2.  **Construir y levantar los contenedores (API, PostgreSQL, Redis):**
    ```bash
    docker-compose up --build -d
    ```
    * Esto construirá la imagen de la API (si es necesario) y levantará todos los servicios en segundo plano.
    * La API estará disponible en: `http://localhost:8080`
    * La base de datos PostgreSQL estará accesible (para la API) en el puerto `5432` interno de Docker.
    * Redis estará accesible (para la API) en el puerto `6379` interno de Docker.

3.  **Verificar el estado:**
    
Puedes usar `docker ps` para confirmar que los contenedores `challenge-api`, `postgres` y `redis` están corriendo. Con esto ya puedes probar la aplicación.


## 3. Detalles sobre cómo interactuar con la API

La forma más sencilla y completa de interactuar con la API es a través de su documentación interactiva **Swagger UI**.

* **Acceso a Swagger UI:** Una vez levantados los contenedores (ver sección anterior), abre tu navegador y ve a:
  [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

Allí encontrarás la descripción detallada de cada endpoint, los parámetros requeridos, los tipos de datos esperados y podrás ejecutar peticiones de prueba directamente desde la interfaz.

**Endpoints Principales:**

* `GET /api/v1/calculations/sum-with-percentage`:
    * **Propósito:** Calcula la suma de dos números más un porcentaje externo.
    * **Query Params:** `num1` (BigDecimal >= 0), `num2` (BigDecimal >= 0).
* `GET /api/v1/history`:
    * **Propósito:** Obtiene el historial paginado de llamadas a la API.
    * **Query Params:** `page` (int >= 0, default 0), `size` (int >= 1, default 10).


## 4. Justificaciones técnicas tomadas

Las decisiones técnicas clave durante el desarrollo se tomaron buscando cumplir los requisitos del desafío y aplicar buenas prácticas:

* **Stack Reactivo (Spring WebFlux, R2DBC):** Se eligió para cumplir el requisito bonus y por ser adecuado para aplicaciones I/O-bound, mejorando la escalabilidad y eficiencia en el uso de hilos comparado con el stack tradicional MVC/Servlet.


* **Arquitectura Hexagonal:** Se implementó para desacoplar la lógica de negocio de los detalles de infraestructura (web, datos, caché), facilitando la mantenibilidad, testabilidad y la posibilidad de intercambiar componentes (ej. cambiar el tipo de base de datos o caché).


* **Redis para Caché:** Se optó por Redis como caché distribuida para cumplir el requisito de **escalabilidad en entornos multi-réplica**, asegurando la consistencia del porcentaje cacheado entre instancias, en lugar de usar una caché en memoria local.


* **Resilience4j:** Se utilizó por ser la librería estándar en el ecosistema Spring para implementar patrones de resiliencia (Retry, Circuit Breaker, Rate Limiter) de forma robusta y configurable.


* **Logging Asíncrono con AOP:** Se implementó para cumplir el requisito de no impactar la latencia de las respuestas principales, usando `@Async` y separando la lógica de logging transversalmente con AOP (`@Aspect`).


* **Docker y Docker Compose:** Se usaron para cumplir el requisito de despliegue contenerizado y facilitar la creación de un entorno de ejecución completo y reproducible (API + DB + Caché).


* **Externalización de Configuración:** Parámetros clave del mock y TTLs se movieron a `application.yml` (usando `@ConfigurationProperties`) para mayor flexibilidad y facilidad de prueba/demostración.


* **Base de Datos y Acceso:** Se usó PostgreSQL y R2DBC según lo especificado, aprovechando el acceso reactivo a la base de datos.


* **Documentación API:** Se usó `springdoc-openapi` por su fácil integración con Spring Boot/WebFlux para generar documentación estándar OpenAPI v3 y la interfaz Swagger UI.