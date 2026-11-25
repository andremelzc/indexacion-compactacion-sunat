# 04 · Plan y estado

[← Volver al README](../README.md)

Marcar `[x]` cuando algo se termine y anotar la fecha. Lo que no está marcado, **no está hecho**.

**Fecha de entrega:** ____________ (completar)

## Dónde estamos

El código está terminado y probado en local, y el servidor con la página web también. **Lo que falta es llevarlo a la nube**, medir allí y preparar la entrega final. Para llevarlo a la nube primero hay que decidir cuál usar (ver [05 · La nube](05-nube.md)).

## Hecho

### Fase 1 · Entender y diseñar
- [x] Leer el enunciado y entender qué se pide (INDEX, PACK, paralelismo)
- [x] Elegir un caso práctico real: locales anexos de contribuyentes
- [x] Conseguir datos reales (padrón de locales anexos de SUNAT; ver [02](02-dataset.md))
- [x] Definir las reglas: sin librerías, en disco, código simple

### Fase 2 · Código base
- [x] Formato binario de registros de longitud variable, con marca de borrado
- [x] Carga del TXT de SUNAT a `locales.dat`, con metadatos
- [x] INDEX() con ordenamiento externo y lectura por rangos en paralelo
- [x] Búsqueda binaria directa sobre el archivo de índice (por RUC y por UBIGEO)
- [x] Bajas por RUC y por departamento
- [x] PACK() paralelo (conteo, suma de prefijos, copia) y reindexado
- [x] Prueba de rendimiento con 1, 2, 4 y 8 hilos
- [x] Menú de consola
- [x] Comprobar los resultados de PACK y de los índices con una verificación independiente
- [x] Versión del avance del 50 % (sin PACK) y su presentación

### Fase 3 · Preparar el proyecto para un servidor
- [x] Servidor HTTP con las órdenes (`/cargar`, `/index`, `/pack`, `/baja`, `/buscar`, `/estado`, `/benchmark`)
- [x] Trabajos largos en segundo plano, con identificador y estado
- [x] Un solo trabajo a la vez, y clave de acceso
- [x] Página web que usa esas órdenes y muestra el resultado
- [x] Probado en una computadora local y con un navegador sin interfaz
- [x] Documentación del proyecto (este conjunto de archivos)

## Pendiente

### Fase 4 · La nube
- [ ] **Decidir el proveedor.** Depende de si el correo universitario califica para Azure for Students y de si alguien tiene tarjeta (ver [05](05-nube.md))
- [ ] Crear la cuenta y la máquina (varios núcleos), y abrir el puerto solo a las IP del grupo
- [ ] Subir el proyecto y el TXT, instalar JDK, compilar y arrancar el servidor con una clave nueva
- [ ] Probar la página y `curl` contra la IP de la máquina
- [ ] Anotar en [05](05-nube.md) los pasos reales que se siguieron

### Fase 5 · Mediciones
- [ ] Correr la medición de 1, 2, 4 y 8 hilos en la máquina de la nube (varias veces)
- [ ] Anotar los resultados en [07](07-mediciones.md), con las características de la máquina
- [ ] Decidir qué cifras se presentan: **solo las medidas en la nube**; las locales se citan como pruebas preliminares

### Fase 6 · Entrega
- [ ] Preguntar al profesor si una máquina virtual le basta o espera otro tipo de servicio (ver «Preguntas abiertas»)
- [ ] Actualizar la presentación con la parte de nube
- [ ] Ensayar la demostración completa
- [ ] Que cada integrante pueda responder las preguntas de [08](08-defensa.md)
- [ ] Copia final del código, los datos y la documentación fuera de la nube

## Reparto de lo que falta

El código ya está hecho; lo que queda se puede repartir así:

| Persona | Responsabilidad | Tareas |
|---|---|---|
| ____________ | La nube | Fase 4 completa. Documenta los pasos reales y cuida el costo (apagar la máquina al terminar) |
| ____________ | Mediciones y pruebas | Fase 5. Además prueba el proyecto en Windows y en otro navegador, y la consola con `curl` desde otra computadora |
| ____________ | Presentación y defensa | Actualiza las diapositivas, escribe el guion y organiza el ensayo. Reúne las preguntas para el profesor |
| Todos | | Leer [03](03-como-funciona.md) y [08](08-defensa.md), y explicarse el código entre ustedes |

## Preguntas abiertas

| Pregunta | Para quién | Estado |
|---|---|---|
| ¿Cuál es el proveedor de nube? | El grupo | Pendiente |
| ¿Basta una máquina virtual con el programa y los datos, disparada por una llamada externa, o se espera otro tipo de servicio (por ejemplo, servicios separados para los datos y para las funciones)? | El profesor | Pendiente |
| El enunciado menciona «servicio de Windows» y «procedimiento almacenado» como ejemplos de disparador. ¿Es obligatorio alguno o basta una llamada HTTP? | El profesor | Pendiente |
| ¿Hace falta el padrón completo o basta el archivo de locales anexos? | El profesor | Pendiente |

## Riesgos y cuidados

- **Costo:** apagar la máquina **desde el portal del proveedor** (no solo desde dentro del sistema) al terminar cada sesión. Algunos proveedores cobran aunque la máquina esté apagada, y ahí hay que destruirla.
- **Pérdida de datos:** al vencer o borrar la cuenta o la máquina se pierde lo que haya en ella. Mantener copia del código, los datos y la documentación en las computadoras del grupo.
- **Seguridad:** abrir el puerto solo a las IP del grupo y cambiar la clave. La clave viaja sin cifrar; sirve para la demostración.
- **Cifras:** no presentar como resultado de la nube lo que se midió en una computadora local.
- **Mejora por hilos:** depende de los núcleos reales y del disco de la máquina. Si la mejora es poca, explicarla (ver [03](03-como-funciona.md), la parte que no se paraleliza) en lugar de forzar cifras.

## Historia breve

1. Se entendió el enunciado y se eligió el caso y el dataset.
2. Se hizo una versión del avance del 50 %: carga, INDEX, búsqueda y bajas, sin PACK.
3. Se completó con PACK paralelo, reindexado y la prueba de rendimiento.
4. Se pidió llevarlo a la nube: se agregó el servidor HTTP, la página web y esta documentación.
5. Sigue: decidir la nube, desplegar y medir.
