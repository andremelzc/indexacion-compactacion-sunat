# 07 · Mediciones

[← Volver al README](../README.md)

Aquí van los tiempos del proyecto. Hay dos cosas distintas:

- **Pruebas preliminares:** hechas en computadoras locales. Sirven para comprobar que todo funciona y para ver el comportamiento, pero **no son los resultados de la nube**.
- **Resultados en la nube:** se llenan cuando el servidor esté desplegado. **Son los que se presentan.**

La prueba de rendimiento (botón «Medir con 1, 2, 4 y 8 hilos» o `POST /benchmark`) hace, para cada cantidad de hilos y tres veces, lo siguiente: carga los datos, crea los dos índices, da de baja todos los locales de Lima (departamento 15) y compacta. Se toma el **mejor** de los tres tiempos. «Aceleración» es el tiempo con 1 hilo dividido entre el tiempo con N hilos.

## Resultados en la nube (llenar)

**Máquina:** ____________ · **Núcleos:** ____ · **RAM:** ____ · **Disco:** ____ · **Fecha:** ____

| Hilos | INDEX (ms) | Aceleración | PACK (ms) | Aceleración |
|---|---|---|---|---|
| 1 | | | | |
| 2 | | | | |
| 4 | | | | |
| 8 | | | | |

| Búsqueda | Con índice (ms) | Recorrido completo (ms) | Mejora |
|---|---|---|---|
| RUC 20100070970 | | | |
| UBIGEO 250101 | | | |

Anotar también cuántas veces se corrió la medición y si los resultados fueron parecidos.

## Pruebas preliminares

### Computadora de un integrante

**Núcleos:** ____ (completar) · **Sistema:** ____ (completar) · una corrida

| Hilos | INDEX (ms) | Aceleración | PACK (ms) | Aceleración |
|---|---|---|---|---|
| 1 | 954.7 | 1.00 | 149.8 | 1.00 |
| 2 | 653.1 | 1.46 | 147.6 | 1.01 |
| 4 | 509.8 | 1.87 | 129.4 | 1.16 |
| 8 | 470.2 | 2.03 | 118.9 | 1.26 |

El índice tarda la mitad con 8 hilos. PACK mejora poco, porque su tiempo es muy corto y depende del disco.

### Entorno de pruebas del desarrollo

2 núcleos, Linux, OpenJDK 21 · una corrida

| Hilos | INDEX (ms) | Aceleración | PACK (ms) | Aceleración |
|---|---|---|---|---|
| 1 | 587.5 | 1.00 | 118.9 | 1.00 |
| 2 | 430.5 | 1.36 | 102.2 | 1.16 |
| 4 | 460.9 | 1.27 | 118.4 | 1.00 |
| 8 | 483.0 | 1.22 | 126.9 | 0.94 |

Con solo 2 núcleos, pasar de 2 hilos no ayuda. Es lo esperable: más hilos que núcleos no da más paralelismo.

### Búsqueda con índice contra recorrido completo (entorno de pruebas)

| Búsqueda | Locales | Con índice | Recorrido completo |
|---|---|---|---|
| RUC 20100070970 | 1,021 | 3.8 ms | 177.7 ms |
| UBIGEO 250101 | 6,789 | 3.1 ms | 173.4 ms |

La búsqueda con índice no es instantánea cuando hay muchos locales, porque hay que leer cada uno. La diferencia con el recorrido completo es mayor cuando el RUC tiene pocos locales (en una prueba anterior, un RUC con pocos locales se buscó en 0.05 ms contra 206 ms).

### Efecto de PACK (entorno de pruebas)

Dando de baja el departamento 15 (Lima):

| Dato | Valor |
|---|---|
| Locales dados de baja | 313,833 (40 % de los registros, 41 % de los bytes) |
| Registros conservados | 465,418 |
| Tamaño de `locales.dat` | 52,054,541 → 30,676,274 bytes |

## Cómo presentar las cifras

- Usar **solo** las de la tabla de la nube en la presentación como resultado.
- Citar las preliminares como «pruebas locales», con la máquina indicada.
- No inventar ni redondear a favor. Si la mejora con hilos es modesta, explicarla con la parte del proceso que no se paraleliza (ver [03](03-como-funciona.md)).
