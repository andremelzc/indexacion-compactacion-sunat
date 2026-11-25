# Indexación y compactación de datos a gran escala

**Caso práctico:** locales anexos de contribuyentes, con datos reales del Padrón Reducido de SUNAT.
**Curso:** Programación Concurrente y Paralela · UNMSM · Grupo 12 · Semestre 2026-II

Un repositorio de archivos binarios en disco, con registros de longitud variable, que se puede **indexar** (para buscar en una fracción de milisegundo) y **compactar** (para eliminar físicamente los registros dados de baja). Ambos procesos usan **hilos** y se ejecutan en un **servidor** que se controla con una llamada externa: una página web o la línea de comandos. Todo está hecho con el JDK de Java, sin librerías de terceros.

## Estado actual

| Parte | Estado |
|---|---|
| Código base (carga, INDEX, búsqueda, bajas, PACK, medición con hilos) | Hecho y probado en local |
| Servidor HTTP y página web | Hecho y probado en local |
| Datos reales | Hecho (padrón de locales anexos, 779,251 registros) |
| Despliegue en la nube | **Pendiente** (falta decidir el proveedor) |
| Mediciones en la máquina de la nube | **Pendiente** |
| Presentación final y ensayo | **Pendiente** |

El detalle, con casillas y reparto del trabajo, está en [docs/04-plan-y-estado.md](docs/04-plan-y-estado.md).

## Cómo probarlo en tu computadora

Necesitas el JDK de Java instalado (`javac -version` debe funcionar). Desde esta carpeta:

```
javac -d out src\*.java
java -cp out Servidor 8080 demo
```

En Linux o macOS escribe `src/*.java`. Luego abre `http://localhost:8080` en el navegador. La clave es `demo`. En la página, el orden es: **Cargar datos**, **Crear índices**, buscar, dar de baja y **Compactar archivo**.

Si prefieres el menú de consola, en lugar del servidor ejecuta `java -cp out Main`.

Más detalle en [docs/06-guia-de-uso.md](docs/06-guia-de-uso.md).

## Mapa de la documentación

| Documento | Para qué sirve |
|---|---|
| [01 · Qué es el proyecto](docs/01-que-es-el-proyecto.md) | El problema, la solución, qué pide el curso y cómo se cumple |
| [02 · El dataset](docs/02-dataset.md) | De dónde salen los datos, qué contienen y sus límites |
| [03 · Cómo funciona](docs/03-como-funciona.md) | Formato de los archivos, INDEX, PACK, servidor y órdenes |
| [04 · Plan y estado](docs/04-plan-y-estado.md) | Qué está terminado, qué falta, quién hace qué |
| [05 · La nube](docs/05-nube.md) | Opciones de proveedor investigadas y pasos de despliegue |
| [06 · Guía de uso](docs/06-guia-de-uso.md) | Cómo usar la página y la consola, y qué significa cada campo |
| [07 · Mediciones](docs/07-mediciones.md) | Resultados preliminares y tablas para llenar con la nube |
| [08 · Para la defensa](docs/08-defensa.md) | Preguntas que cada integrante debe poder responder |

## Estructura de carpetas

```
src/      código Java (Main, Servidor, Trabajo, Registro, ArchivoDatos, Indice, Compactador)
web/      la página web (un solo archivo, sin librerías)
datos/    el archivo de texto de SUNAT (padron_reducido_local_anexo.txt), incluido en el repo
docs/     la documentación
salida/   se crea sola al cargar los datos: locales.dat, índices y metadatos
out/      se crea al compilar
```

## Reglas del proyecto

- Datos reales, no simulados.
- Caso práctico real, no académico.
- De cero y sin librerías: solo el JDK (`RandomAccessFile`, `Thread` y el servidor HTTP que trae el JDK).
- Programación en disco, no en memoria.
- Código lo más simple posible.
