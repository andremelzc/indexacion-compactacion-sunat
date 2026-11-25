# 01 · Qué es el proyecto

[← Volver al README](../README.md)

## El problema

SUNAT publica los **locales anexos** de los contribuyentes: los establecimientos adicionales (sucursales, almacenes, agencias) que declaran además de su domicilio fiscal. Son cientos de miles de filas. Con ese volumen aparecen dos problemas reales:

1. **Buscar es lento.** Para encontrar los locales de un RUC hay que leer el archivo entero, registro por registro.
2. **Borrar deja huecos.** Cuando un local se da de baja, el espacio que ocupaba sigue en el archivo. Con el tiempo el archivo crece con datos muertos.

## La solución

Un repositorio propio de datos en disco, con dos operaciones:

- **INDEX():** construye un archivo de índice por campo (RUC y UBIGEO), ordenado, que permite buscar por búsqueda binaria directamente en el disco, sin cargar nada en memoria. Se obtiene una búsqueda en tiempo O(log n).
- **PACK():** elimina físicamente los registros marcados como dados de baja y deja el archivo compacto. Como las posiciones cambian, después se vuelven a crear los índices.

Ambas se ejecutan **en paralelo con hilos**, porque con archivos grandes el tiempo importa.

## Caso práctico

Consultar los locales de un contribuyente por su **RUC**, o todos los locales de una zona por su **UBIGEO**; dar de baja los locales de un contribuyente que cierra o de un departamento completo; y recuperar el espacio con PACK. Todo sobre datos reales (ver [02 · El dataset](02-dataset.md)).

## Dónde corre: la nube

El enunciado pide procesamiento a gran escala **en la nube**: el proceso se ejecuta en un servidor, con los datos en una máquina fuerte, y se dispara con una llamada externa. Por eso el proyecto incluye un **servidor HTTP** y una **página web** como cliente. La computadora del usuario solo manda la orden y muestra el resultado; el trabajo pesado ocurre en el servidor. La arquitectura está en [03 · Cómo funciona](03-como-funciona.md) y el estado del despliegue en [04 · Plan y estado](04-plan-y-estado.md).

## Qué se pidió y cómo se cumple

| Pedido | Cómo se cumple | Estado |
|---|---|---|
| Repositorio con registros y campos de longitud variable, un índice por campo y metadatos | `locales.dat`, `ruc.idx`, `ubigeo.idx`, `locales.meta` | Hecho |
| INDEX() en paralelo | Lectura por rangos con un hilo cada uno, ordenamiento externo por bloques y mezcla | Hecho |
| PACK() en paralelo | Conteo por rangos, suma de prefijos y copia en paralelo | Hecho |
| Datos reales | Padrón Reducido de SUNAT, locales anexos | Hecho |
| Caso práctico real | Consulta y baja de locales por RUC y por zona | Hecho |
| De cero, sin librerías | Solo JDK | Hecho |
| Programación en disco | `RandomAccessFile`, índices y datos siempre en archivos | Hecho |
| Procesamiento a gran escala en la nube | Servidor HTTP listo; falta desplegarlo | **Pendiente** |
| Disparo por llamada externa (consola, servicio) | Órdenes HTTP; se usan desde la página o con `curl` | Hecho en local |
| Datos en una máquina fuerte en la nube | Falta subir los datos a la máquina | **Pendiente** |

Esta tabla está hecha con lo que el grupo resumió del enunciado. Si el documento original pide algo más, hay que agregarlo aquí.

## Lo que el proyecto no es

- No es una base de datos general: tiene un solo tipo de registro y dos índices.
- No usa el padrón completo de ~14 millones de filas, sino el archivo de locales anexos (ver el dataset).
- No es un servicio de producción: la comunicación es HTTP sin cifrar y la seguridad se limita a una clave (ver [03 · Cómo funciona](03-como-funciona.md)).

## Glosario mínimo

- **RUC:** número de 11 dígitos con que SUNAT identifica a cada contribuyente.
- **UBIGEO:** código de seis dígitos de una zona geográfica (departamento, provincia y distrito). Los dos primeros dígitos son el departamento; el 15 es Lima.
- **Índice:** archivo ordenado que dice en qué posición del archivo de datos está cada registro.
- **Baja:** marcar un registro como borrado sin quitarlo todavía del archivo.
- **PACK:** eliminar físicamente las bajas.
- **Hilo:** tarea que corre en paralelo con otras dentro del mismo programa.
