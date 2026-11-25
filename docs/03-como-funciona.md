# 03 · Cómo funciona

[← Volver al README](../README.md)

## Vista general

```
 Computadora del usuario                    Máquina del servidor (la nube)
 ┌───────────────────┐   orden HTTP         ┌───────────────────────────────────┐
 │ Navegador (página)│ ───────────────────► │ Servidor Java (puerto 8080)       │
 │        o          │                      │   ├─ entrega la página web        │
 │ curl (consola)    │ ◄─────────────────── │   ├─ recibe las órdenes           │
 └───────────────────┘   estado y resultado │   └─ lanza INDEX / PACK / buscar  │
                                            │ Disco: locales.dat, índices,      │
                                            │        locales.meta               │
                                            └───────────────────────────────────┘
```

La computadora del usuario no hace trabajo pesado: manda la orden y muestra la respuesta. El servidor es quien lee y escribe los archivos y usa los hilos.

## Los archivos de datos

Todo vive en la carpeta `salida/`, que se crea sola.

### `locales.dat`: los registros

Una secuencia de registros de longitud variable. Cada registro tiene este formato:

| Parte | Tamaño | Contenido |
|---|---|---|
| Marca de borrado | 1 byte | 0 = activo, 1 = dado de baja |
| Tamaño total | 2 bytes | Cuántos bytes ocupa el registro entero |
| Campos (12 veces) | 1 byte + texto | Largo del campo y su texto en ISO-8859-1 |

Como cada campo lleva su largo, un campo puede medir de 0 a 255 bytes. Para llegar al registro siguiente se suma el tamaño total.

### `ruc.idx` y `ubigeo.idx`: los índices

Archivos de **entradas de 16 bytes**: 8 bytes de clave y 8 bytes con la posición del registro en `locales.dat`. Están **ordenados por clave**. Como todas las entradas miden lo mismo, la entrada número *k* está exactamente en la posición `16·k`, y eso permite la búsqueda binaria directa sobre el archivo.

### `locales.meta`: los metadatos

Un archivo de texto con el nombre del archivo de datos, la cantidad de registros, la cantidad de campos y el nombre de cada uno.

## INDEX(): crear un índice

1. **Dividir** el archivo de datos en N rangos, uno por hilo. El corte cae siempre al inicio de un registro, y para encontrarlo solo se leen las cabeceras (no el contenido).
2. **Cada hilo** lee su rango, arma bloques de 100,000 entradas, ordena cada bloque con un merge sort propio y lo guarda en un archivo temporal. Así nunca hace falta tener todo en memoria (ordenamiento externo).
3. **Un solo hilo** mezcla todos los temporales en el índice final, tomando siempre la clave más pequeña de las que están a la mano.
4. Se borran los temporales.

**Por qué la mejora con hilos es limitada:** los pasos 1 y 3 y la lectura del disco no se reparten entre hilos. Es la idea de la ley de Amdahl: la parte que no se paraleliza pone un tope a la ganancia.

## Buscar con el índice

Se hace búsqueda binaria sobre el archivo de índice con `RandomAccessFile`, sin cargarlo en memoria: se salta a la mitad, se compara la clave, y se descarta una mitad en cada paso. Se obtienen las posiciones de todos los registros con esa clave, se leen con acceso directo y se ignoran los dados de baja. Para el UBIGEO existe además la búsqueda por rango, que se usa para dar de baja un departamento entero (claves desde `dep·10000` hasta `(dep+1)·10000`).

## Dar de baja

Solo se cambia el **primer byte** del registro, de 0 a 1. El registro sigue ocupando espacio hasta que se haga PACK. La baja por RUC y la baja por departamento usan los índices para encontrar las posiciones.

## PACK(): compactar

1. **Dividir** el archivo en N rangos, cortando al inicio de un registro.
2. **Contar:** cada hilo cuenta cuántos bytes de su rango se conservan (los activos).
3. **Suma de prefijos:** el lugar donde empieza a escribir cada hilo en el archivo nuevo es la suma de los bytes conservados por los hilos anteriores.
4. **Copiar en paralelo:** cada hilo escribe sus registros activos en su zona del archivo nuevo (`locales.dat.nuevo`). Las zonas no se pisan, así que no hace falta coordinarlos.
5. **Reemplazar:** el archivo nuevo sustituye al viejo y se actualizan los metadatos.
6. **Reindexar:** las posiciones cambiaron, así que se vuelven a crear los dos índices.

## El servidor

Está en `Servidor.java` (recibe las órdenes) y `Trabajo.java` (ejecuta los procesos largos en su propio hilo). Usa el servidor HTTP que trae el JDK, sin librerías.

### Órdenes

Todas llevan la clave en la cabecera `X-Clave` (o en `?clave=...`).

| Orden | Qué hace | Respuesta |
|---|---|---|
| `GET /` | La página web (no pide clave) | HTML |
| `GET /estado` | Estado del repositorio | JSON |
| `GET /estado/<id>` | Cómo va un trabajo | JSON |
| `GET /buscar?ruc=...` o `?ubigeo=...` | Busca con el índice. Con `&comparar=si` mide también el recorrido completo | JSON |
| `POST /cargar` | Carga el TXT a `locales.dat` | id del trabajo |
| `POST /index?hilos=N` | INDEX() | id del trabajo |
| `POST /pack?hilos=N` | PACK() y reindexado | id del trabajo |
| `POST /baja?ruc=...` o `?dep=...` | Da de baja por RUC o por departamento | id del trabajo |
| `POST /benchmark` | Mide INDEX y PACK con 1, 2, 4 y 8 hilos | id del trabajo |

Las órdenes largas responden enseguida con un identificador, por ejemplo `{"id":2,"tipo":"index"}`. Luego se consulta `GET /estado/2` hasta que el estado sea `terminado` (o `error`); ahí aparecen el resultado y los tiempos.

### Reglas

- **Un solo trabajo largo a la vez.** Si llega otro, responde 409.
- **No se puede buscar mientras corre un trabajo**, porque los archivos están cambiando (también 409).
- **Clave incorrecta o ausente:** 401. **Falta un paso previo** (por ejemplo, buscar sin índices): 400 con el motivo.

### Límites de seguridad

- La comunicación es **HTTP sin cifrar** y la clave viaja en cada petición. Sirve para una demostración, no para producción.
- Se permite llamar desde cualquier origen (cabecera CORS abierta). En la nube hay que abrir el puerto **solo a las IP del grupo** y cambiar la clave.
- Los identificadores de trabajo viven en memoria: al reiniciar el servidor se pierden.

## El código

| Archivo | Qué hace |
|---|---|
| `Main.java` | Menú de consola y la prueba de rendimiento |
| `Servidor.java` | Servidor HTTP: rutas, clave y bloqueo de trabajos |
| `Trabajo.java` | Un trabajo largo en su propio hilo |
| `Registro.java` | Un registro: pasar a bytes y volver, y sacar la clave |
| `ArchivoDatos.java` | Todo lo que toca `locales.dat` y los metadatos |
| `Indice.java` | INDEX() y la búsqueda binaria (incluye `HiloIndice`) |
| `Compactador.java` | PACK() (incluye `HiloContar` y `HiloCopiar`) |

## Limitaciones conocidas

- Si el servidor se cae justo mientras PACK reemplaza el archivo, `locales.dat` puede faltar y quedar solo `locales.dat.nuevo`, que contiene los datos completos. No hay recuperación automática.
- No hay control de acceso concurrente más allá de «un trabajo a la vez».
- Los campos no pueden pasar de 255 bytes ni los registros de 65,535 bytes.
- Se probó con el archivo de locales anexos, no con el padrón completo.
