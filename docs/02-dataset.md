# 02 · El dataset

[← Volver al README](../README.md)

## De dónde salen los datos

- **Fuente:** SUNAT (Superintendencia Nacional de Aduanas y de Administración Tributaria), Padrón Reducido de RUC, archivo de **locales anexos** (`padron_reducido_local_anexo.zip`).
- **Datos reales:** son registros públicos de contribuyentes, no simulados.
- **Archivo usado:** `datos/padron_reducido_local_anexo.txt` (50,496,158 bytes).
- **Fecha de descarga:** ____________ (completar)
- **Enlace de descarga:** ____________ (completar). El enlace exacto no pudo verificarse desde el entorno donde se preparó el proyecto; el archivo lo descargó un integrante del grupo.

El **padrón completo** de contribuyentes (alrededor de 14 millones de filas) no se pudo descargar, así que se trabaja solo con el archivo de locales anexos. Es un conjunto real y lo bastante grande para el caso, pero no es el padrón entero. Conviene decirlo así en la presentación.

## Formato del archivo de origen

- Texto plano, una fila por local, campos separados por `|`, cada línea termina también con `|`.
- La **primera línea** es el encabezado y se omite al cargar.
- Codificación **ISO-8859-1** (por eso las tildes se ven mal si se abre con otra codificación).

## Los 12 campos

| # | Campo | Ejemplo |
|---|---|---|
| 1 | RUC | 20100070970 |
| 2 | UBIGEO | 150110 |
| 3 | TIPO DE VÍA | AV. |
| 4 | NOMBRE DE VÍA | TUPAC AMARU |
| 5 | CÓDIGO DE ZONA | P.J. |
| 6 | TIPO DE ZONA | PAMPAS DE COMAS |
| 7 | NÚMERO | 4492 |
| 8 | KILÓMETRO | - |
| 9 | INTERIOR | - |
| 10 | LOTE | - |
| 11 | DEPARTAMENTO | - |
| 12 | MANZANA | - |

Un guion `-` significa que el campo está vacío. En las filas que revisamos, las columnas «CÓDIGO DE ZONA» y «TIPO DE ZONA» parecen traer los valores cambiados (la primera, una abreviatura; la segunda, el nombre de la zona). El proyecto guarda los campos **tal como vienen**, sin interpretarlos.

## Cifras

| Dato | Valor |
|---|---|
| Registros (filas de datos) | 779,251 |
| RUC distintos | 441,610 |
| Tamaño del TXT | 50,496,158 bytes |
| Tamaño de `locales.dat` (formato binario) | 52,054,541 bytes |
| Filas con RUC que no es numérico | 9 |

Un mismo RUC puede tener muchos locales. Por eso la búsqueda por RUC devuelve varios registros (por ejemplo, el RUC 20100070970 tiene 1,021).

## Calidad de los datos

Nueve filas tienen un RUC con caracteres que no son dígitos. Esas filas **se cargan y se guardan**, pero no entran al índice por RUC: por eso ese índice tiene 779,242 entradas y no 779,251. El índice por UBIGEO sí las incluye todas.

## Por qué este conjunto sirve para el proyecto

- Es **real** y público, como pide el curso.
- Tiene **campos de longitud variable** (calles, zonas, nombres), lo que justifica un formato binario con registros de tamaño distinto.
- Tiene **dos claves naturales** para indexar: RUC y UBIGEO.
- Es lo bastante grande para que buscar sin índice sea notablemente más lento que con índice (ver [07 · Mediciones](07-mediciones.md)).

## Cómo reemplazar o actualizar los datos

1. Descargar el nuevo archivo de SUNAT y extraer el TXT.
2. Guardarlo como `datos/padron_reducido_local_anexo.txt` (o cambiar la ruta en `Main.java`, constante `TXT`).
3. Ejecutar **Cargar datos** y luego **Crear índices**. Si el formato de las columnas cambió, hay que revisar `Registro.java`.
