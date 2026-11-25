# 06 · Guía de uso

[← Volver al README](../README.md)

## Requisitos

- El **JDK** de Java instalado. Comprueba con `javac -version` en una consola.
- Un navegador moderno para la página.
- Para la consola: `curl`. En Windows 10 y 11 viene incluido; en PowerShell escribe `curl.exe` (a secas, `curl` es otra cosa).

## Arrancar el servidor

Desde la carpeta del proyecto (la que contiene `src`, `datos` y `web`):

```
javac -d out src\*.java
java -cp out Servidor 8080 demo
```

- En Linux o macOS: `src/*.java`.
- `8080` es el puerto y `demo` la clave. Si el puerto está ocupado, usa otro (por ejemplo `8090`).
- La consola queda ocupada: ahí aparece cada orden que llega. Para detenerlo: Ctrl + C.
- **Hay que arrancarlo desde esa carpeta**, porque busca `datos/`, `salida/` y `web/` en la carpeta actual.
- Si prefieres el menú de consola, sin servidor: `java -cp out Main`.

Abre `http://localhost:8080`.

## Orden para probar de cero

1. **Cargar datos** (un par de segundos).
2. **Crear índices** con 2 hilos.
3. Buscar el RUC `20100070970`, marcando «Comparar con recorrer el archivo completo».
4. **Dar de baja un departamento**, con el código `15`.
5. **Compactar archivo** con 4 hilos: la tira del archivo se encoge.
6. **Medir con 1, 2, 4 y 8 hilos** (es lo que más tarda).

La página resalta en azul el paso que toca y lo explica en una línea.

## Qué significa cada parte de la página

### Arriba
- **Clave:** la contraseña con que se arrancó el servidor. Sin ella no se aceptan órdenes.
- **Conectado / Sin conexión / Clave incorrecta:** si la página logra hablar con el servidor.

### El archivo
- **El número grande:** lo que ocupa hoy `locales.dat` en el disco del servidor.
- **La tira de colores:** el archivo dibujado como una barra. **Azul**, «En uso»: registros válidos. **Rayado rojo**, «Bajas sin compactar»: registros dados de baja que todavía ocupan espacio. **Hueco punteado**, «Liberado por PACK»: espacio recuperado al compactar. La escala se toma del mayor tamaño que la página ha visto en ese navegador.
- **Registros activos / Bajas sin compactar / Índices creados:** el mismo estado, en cifras.
- **Siguiente paso:** el proceso que conviene ejecutar ahora.
- **Actualizar:** vuelve a consultar el estado. La página también lo hace sola cada 15 segundos.

### Procesos en el servidor
Todos se ejecutan en el servidor, no en tu computadora.
- **Cargar datos:** lee el TXT de SUNAT y construye `locales.dat`. Borra los índices anteriores.
- **Crear índices (con N hilos):** INDEX(). Ordena por RUC y por UBIGEO para poder buscar sin recorrer el archivo entero.
- **Compactar archivo (con N hilos):** PACK(). Elimina de verdad las bajas y vuelve a crear los índices.
- **Dar de baja por RUC:** marca todos los locales de ese RUC. No achica el archivo.
- **Dar de baja un departamento:** marca todos los locales de ese departamento. El código son los dos primeros dígitos del UBIGEO; el 15 es Lima. No achica el archivo.
- **Medir con 1, 2, 4 y 8 hilos:** repite la carga, los índices y la compactación con cada cantidad de hilos, y muestra una tabla y un gráfico. Al terminar recarga los datos originales.

> Las bajas **no achican el archivo**: solo lo marcan. Lo que lo achica es **Compactar archivo**.

### Buscar con el índice
- **RUC / UBIGEO:** el campo por el que se busca. **Valor:** el número a buscar.
- **Comparar con recorrer el archivo completo:** hace la búsqueda también sin índice, para mostrar la diferencia de tiempo.
- **Resultado:** cuántos locales se encontraron, en cuánto tiempo, las barras de comparación y una tabla con RUC, UBIGEO y dirección (primeros 10). Debajo, «Ver la orden equivalente en consola» muestra el `curl` que hace lo mismo.

### A la derecha
- **Trabajos terminados:** el resultado de cada proceso, con hora y duración. La medición de hilos aparece como tabla y gráfico. Cada uno incluye la orden equivalente en consola.
- **La barra fija de abajo:** aparece mientras corre un proceso y muestra avisos, como «Escribe el RUC…».

Mientras un proceso corre, los botones se bloquean: el servidor solo hace un trabajo a la vez.

## Usar la consola (curl)

La misma operación que hace la página se puede lanzar así. Cambia `localhost:8080` por la IP de la máquina cuando esté en la nube y `demo` por la clave.

| Qué hace | Comando |
|---|---|
| Estado | `curl -H "X-Clave: demo" http://localhost:8080/estado` |
| Cargar datos | `curl -X POST -H "X-Clave: demo" http://localhost:8080/cargar` |
| INDEX con 4 hilos | `curl -X POST -H "X-Clave: demo" "http://localhost:8080/index?hilos=4"` |
| Cómo va un trabajo | `curl -H "X-Clave: demo" http://localhost:8080/estado/1` |
| Buscar por RUC | `curl -H "X-Clave: demo" "http://localhost:8080/buscar?ruc=20100070970"` |
| Buscar por UBIGEO | `curl -H "X-Clave: demo" "http://localhost:8080/buscar?ubigeo=250101"` |
| Comparar índice y recorrido completo | `curl -H "X-Clave: demo" "http://localhost:8080/buscar?ruc=20100070970&comparar=si"` |
| Dar de baja un RUC | `curl -X POST -H "X-Clave: demo" "http://localhost:8080/baja?ruc=20100070970"` |
| Dar de baja un departamento | `curl -X POST -H "X-Clave: demo" "http://localhost:8080/baja?dep=15"` |
| PACK con 8 hilos | `curl -X POST -H "X-Clave: demo" "http://localhost:8080/pack?hilos=8"` |
| Medición con 1, 2, 4 y 8 hilos | `curl -X POST -H "X-Clave: demo" http://localhost:8080/benchmark` |

Las órdenes largas responden con un identificador, por ejemplo `{"id":2,"tipo":"index"}`. Se consulta `/estado/2` hasta que diga `terminado`. Si la página está abierta, detecta el trabajo que lanzaste por consola y lo muestra.

La consola muestra el resultado como texto (JSON). Por eso la página existe: es donde el resultado se ve bien. Ambas formas son la misma llamada externa al servidor.

## Si algo no funciona

| Síntoma | Qué revisar |
|---|---|
| `javac` dice «Invalid filename» | Estás en otra carpeta. Entra a la que contiene `src`, `datos` y `web` |
| La página dice «Sin conexión» | El servidor no está corriendo, o el puerto no es el mismo |
| «Clave incorrecta» | La clave de la página no coincide con la del arranque |
| «No se encuentra datos/padron_reducido_local_anexo.txt» | Arrancaste el servidor desde otra carpeta, o falta el archivo en `datos/` |
| «Hay un trabajo en curso» | Espera a que termine el que está corriendo |
| «Primero hay que crear los índices» | Ejecuta **Crear índices** |
| El puerto está ocupado | Usa otro puerto al arrancar |
