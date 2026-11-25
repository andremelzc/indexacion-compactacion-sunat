# 08 · Para la defensa

[← Volver al README](../README.md)

Cada integrante debería poder responder estas preguntas, aunque no haya escrito esa parte. Las respuestas son orientativas: explíquenlas con sus propias palabras. El detalle está en [03 · Cómo funciona](03-como-funciona.md).

## Sobre el proyecto

**¿Qué problema resuelve?**
Buscar locales en un archivo grande sin recorrerlo entero, y recuperar el espacio de los registros dados de baja. Todo sobre datos reales de SUNAT.

**¿Por qué no usar una base de datos?**
El curso pide construirlo de cero y sin librerías, trabajando en disco. Lo que se hace aquí es, en pequeño, lo que una base de datos hace por dentro: archivos de registros, índices y compactación.

**¿Por qué estos datos?**
Son reales y públicos, tienen campos de largo variable y dos claves naturales (RUC y UBIGEO). No es el padrón completo, sino el archivo de locales anexos (779,251 registros).

## Sobre los índices

**¿Por qué las entradas del índice miden todas 16 bytes?**
Porque así la entrada número *k* está en la posición `16·k`, y se puede hacer búsqueda binaria directamente sobre el archivo sin cargarlo en memoria.

**¿Cómo se ordena un archivo que no cabe en memoria?**
Con ordenamiento externo: se parte en bloques de 100,000 entradas, cada bloque se ordena y se guarda en un archivo temporal, y después se mezclan todos los temporales en el índice final.

**¿Qué pasa con los RUC que no son numéricos?**
Son 9 filas. Se guardan, pero no entran al índice por RUC, que por eso tiene 779,242 entradas.

**¿Por qué un RUC devuelve varios registros?**
Porque un contribuyente puede tener muchos locales anexos. El índice guarda una entrada por local.

## Sobre PACK y los hilos

**¿Cómo divide PACK el trabajo entre hilos sin romper registros?**
Corta el archivo en rangos, y cada corte cae al inicio de un registro (se leen solo las cabeceras para encontrarlo). Cada hilo cuenta los bytes que va a conservar, una suma de prefijos dice dónde empieza a escribir cada uno, y todos copian a la vez en zonas que no se pisan.

**¿Por qué hay que reconstruir los índices después de PACK?**
Porque al compactar, los registros cambian de posición y las posiciones guardadas en el índice dejan de ser válidas.

**¿Por qué dar de baja no libera espacio?**
Solo cambia el primer byte del registro, de 0 a 1. El espacio se recupera recién con PACK.

**¿Por qué la mejora con más hilos es limitada?**
Porque no todo se paraleliza: dividir el archivo y la mezcla final de los bloques ordenados se hacen en un solo hilo, y el disco tiene un límite de velocidad. Es la idea de la ley de Amdahl. Además, usar más hilos que núcleos no da más paralelismo. Las cifras están en [07](07-mediciones.md).

**¿Por qué los hilos escriben a la vez en el mismo archivo sin problemas?**
Porque cada uno escribe solo en su zona, calculada de antemano con la suma de prefijos. Las zonas no se solapan.

## Sobre la nube y el servidor

**¿Qué corre en la nube y qué en la computadora del usuario?**
En la nube están el programa y los datos; ahí se ejecutan INDEX y PACK. En la computadora del usuario solo hay un navegador o una consola que manda la orden y muestra el resultado.

**¿Dónde está la «llamada externa» que pide el enunciado?**
En la orden HTTP al servidor, que se puede mandar desde la página o desde la consola con `curl`. Lanza el proceso en el servidor.

**¿Por qué el servidor hace un solo trabajo a la vez?**
Porque mientras corre uno, los archivos están cambiando. Si otro trabajo o una búsqueda leyeran al mismo tiempo, verían datos a medias.

**¿Por qué HTTP y no otra cosa?**
Porque el JDK trae un servidor HTTP, así que no hace falta ninguna librería, y cualquier cliente (navegador, `curl`) puede usarlo.

**¿Qué tan seguro es?**
Poco: la clave viaja sin cifrar. Sirve para la demostración. La protección real es abrir el puerto solo a las IP del grupo y cambiar la clave.

## Sobre las limitaciones (conviene decirlas antes de que las pregunten)

- Se usa el archivo de locales anexos, no el padrón completo.
- Si el servidor se cae justo mientras PACK reemplaza el archivo, hay que recuperar `locales.dat.nuevo` a mano.
- Los números de la nube son los que se midieron en la máquina elegida; las pruebas locales son preliminares.
- Un campo no puede pasar de 255 bytes ni un registro de 65,535.

## Preguntas que nos conviene hacerle al profesor

- ¿Basta una máquina virtual en la nube con el programa y los datos, o espera otro tipo de servicio?
- ¿Es obligatorio un disparador en particular (servicio de Windows, procedimiento almacenado) o basta una llamada HTTP?
- ¿Es suficiente el archivo de locales anexos o necesita el padrón completo?
