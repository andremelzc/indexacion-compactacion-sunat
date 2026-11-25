# 05 · La nube

[← Volver al README](../README.md)

Qué se pidió: el proceso a gran escala se ejecuta **en el servidor**, los datos están **en una máquina fuerte en la nube**, y una **llamada externa** lo dispara.

Este documento reúne las opciones investigadas y los pasos para desplegar. **Todavía no se eligió proveedor ni se desplegó nada.** Las ofertas y los precios cambian con frecuencia: lo de abajo se consultó en la web el **16/11/2026** y hay que volver a comprobarlo antes de decidir.

## Qué necesita el proyecto

- Una máquina con **varios núcleos** (para que se note la mejora con hilos) y **disco propio** (los datos son archivos y se leen con `RandomAccessFile`).
- Java (JDK) instalado y un puerto abierto (8080 por defecto).
- Unos 200 MB de disco para los datos, los índices y los temporales.
- Solo se necesita durante la prueba y la demostración: se puede encender y apagar.

## Opciones investigadas

| Opción | Lo que se encontró | Contra |
|---|---|---|
| **Azure for Students** | 100 USD de crédito por 12 meses, **sin tarjeta**, verificando el correo universitario (o con el paquete de estudiantes de GitHub). Hay verificación manual subiendo un documento | La verificación falla a algunos estudiantes si su dominio no está registrado. Una comparación indica opciones de cómputo limitadas: hay que ver qué tamaños deja crear |
| **Google Cloud (GCP)** | 300 USD por 90 días para cuentas nuevas | Pide tarjeta. Al terminar, los recursos se detienen |
| **AWS** | Cuentas nuevas: plan gratuito con hasta 200 USD en créditos (100 al registrarse y hasta 100 más), por 6 meses o hasta agotar los créditos | Al terminar se cierra la cuenta y se pierden los recursos. No se verificó si pide tarjeta ni qué tamaños de máquina permite el plan gratuito |
| **Railway** | Plataforma donde se sube el programa. Prueba de 30 días con 5 USD. En el plan gratuito, hasta 1 vCPU y 0.5 GB de RAM por servicio. Volumen de 0.5 GB en el plan gratuito (5 GB en Hobby) | Pocos núcleos para mostrar la mejora con hilos. Más potencia es de pago (unos 20 USD por vCPU al mes) |
| **Oracle Cloud (siempre gratis)** | Máquinas ARM gratuitas que no vencen | Una fuente reporta que redujeron la asignación a la mitad (de 4 núcleos y 24 GB a 2 y 12 GB), sin confirmar en la documentación oficial. Hay reportes de falta de capacidad al crear la máquina |
| **VPS de pago por hora** | Hetzner (sin prueba gratis, el más barato; subió precios en 2026; un plan de 4 vCPU y 8 GB ronda 8.50 € al mes según una comparación de junio de 2026), Linode/Akamai (100 USD por 60 días con método de pago), Vultr (créditos variables, método de pago; **cobra por hora aunque esté apagado**), DigitalOcean (las fuentes se contradicen sobre su oferta para estudiantes: no contar con ella) | Casi todos piden tarjeta. Si se paga, son pocos dólares por unas horas de uso |

## Orden sugerido

1. **Azure for Students**, si el correo de alguien califica: no pide tarjeta.
2. **GCP o AWS**, si alguien tiene tarjeta.
3. **Un VPS de pago por hora** (con al menos 4 núcleos) si nada de lo anterior funciona. Para pocas horas de prueba el gasto es mínimo.
4. **Railway u Oracle** como alternativas, sabiendo sus límites.

El código es el mismo en todos; solo cambian los pasos de crear la máquina y subir los datos.

## Otras formas de plantear la nube

- **Funciones sin servidor y almacenamiento de datos por separado.** Es una arquitectura válida, pero para este proyecto trae problemas: los datos dejarían de ser archivos locales (se leerían por red, y las búsquedas binarias harían muchas peticiones), las funciones tienen un tiempo máximo de ejecución (un INDEX o un PACK sobre archivos grandes es un proceso largo), y hablar con esos servicios suele pedir librerías, lo que choca con «de cero». Si el profesor espera varios servicios, hay que adaptar el acceso a los datos.
- **Contenedor:** más trabajo de preparación y los datos tendrían que vivir en almacenamiento aparte.

Ver las preguntas al profesor en [04](04-plan-y-estado.md).

## Desplegar en una máquina Linux (guía general)

Son los pasos habituales. **No se han probado en ninguna nube todavía**: quien despliegue debe ajustar y anotar los pasos reales aquí.

1. **Crear la máquina** con Ubuntu LTS y varios núcleos. Abrir el puerto **8080** solo para las IP del grupo (en el firewall, grupo de seguridad o regla de red del proveedor).
2. **Conectarse por SSH** a la máquina.
3. **Instalar Java y unzip:**
   ```
   sudo apt update
   sudo apt install -y default-jdk-headless unzip
   ```
4. **Subir el proyecto** desde tu computadora (cambia usuario e IP):
   ```
   scp Proyecto_Final_Indexacion_Compactacion_SUNAT.zip usuario@IP:~
   ```
5. **Descomprimir, compilar y arrancar** con una clave nueva:
   ```
   unzip Proyecto_Final_Indexacion_Compactacion_SUNAT.zip
   cd indexacion-compactacion-sunat
   javac -d out src/*.java
   nohup java -cp out Servidor 8080 UNA_CLAVE_NUEVA > servidor.log 2>&1 &
   ```
   El `nohup` y el `&` lo dejan corriendo aunque cierres la sesión SSH. Los mensajes quedan en `servidor.log`.
6. **Probar:** abrir `http://IP:8080` en el navegador y usar la página, o `curl` desde otra computadora.
7. **Medir** con el botón de medición (ver [07](07-mediciones.md)).
8. **Detener la máquina desde el portal del proveedor** al terminar (ver el cuidado de costos).

Se probó con JDK 21 (OpenJDK en Ubuntu). El código solo usa clases básicas del JDK.

## Costos y cuidados

- Apagar la máquina **desde el portal** y no solo desde dentro del sistema. Con Vultr y otros que cobran por máquina creada, hay que destruirla.
- Con créditos de 100 a 200 USD, una máquina grande encendida todo el mes los consume: encenderla solo para probar y medir.
- Al vencer la cuenta o borrar la máquina se pierde todo: guardar el código, los datos y los resultados en las computadoras del grupo.
- Abrir el puerto solo a las IP del grupo y cambiar la clave por defecto.
- Subir los datos con `scp` desde una computadora es lo más seguro. No está confirmado que SUNAT permita descargar desde la máquina de la nube.
