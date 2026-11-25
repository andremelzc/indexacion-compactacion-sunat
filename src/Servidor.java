import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Locale;

// Servidor del proyecto: recibe ordenes por HTTP y ejecuta el proceso pesado en la maquina donde corre
// (la maquina de la nube). Solo usa el JDK (com.sun.net.httpserver), sin librerias externas.
//
// Uso:  java -cp out Servidor [puerto] [clave]       (puerto 8080 y clave "demo" por defecto)
// Se arranca desde la carpeta "proyecto": ahi estan las carpetas salida/, datos/ y web/.
//
// Ordenes (todas llevan la clave en la cabecera X-Clave, o en ?clave=...):
//   GET  /                      la pagina web (no pide clave)
//   GET  /estado                estado general del repositorio
//   GET  /estado/<id>           como va un trabajo
//   GET  /buscar?ruc=...        busqueda con indice (o ?ubigeo=...; con &comparar=si tambien mide el recorrido secuencial)
//   POST /cargar                carga el TXT de SUNAT a locales.dat
//   POST /index?hilos=N         INDEX()
//   POST /pack?hilos=N          PACK() y reindexado
//   POST /baja?ruc=...          da de baja los locales de un RUC (o ?dep=15 para un departamento)
//   POST /benchmark             prueba con 1, 2, 4 y 8 hilos
public class Servidor implements HttpHandler {

    static String clave = "demo";
    static ArchivoDatos datos;
    static HashMap<Integer, Trabajo> trabajos = new HashMap<Integer, Trabajo>();
    static Trabajo actual = null;   // el trabajo que esta corriendo (null = libre)
    static int contador = 0;

    public static void main(String[] args) throws Exception {
        int puerto = 8080;
        if (System.getenv("PORT") != null) {
            puerto = Integer.parseInt(System.getenv("PORT"));
        }
        if (args.length > 0) {
            puerto = Integer.parseInt(args[0]);
        }
        if (args.length > 1) {
            clave = args[1];
        }

        new File("salida").mkdirs();
        datos = new ArchivoDatos(Main.DATOS, Main.META);

        HttpServer servidor = HttpServer.create(new InetSocketAddress(puerto), 0);
        servidor.createContext("/", new Servidor());
        servidor.start();
        System.out.println("Servidor escuchando en el puerto " + puerto + " (clave: " + clave + ")");
    }

    // Solo un trabajo largo a la vez: mientras uno corre, los archivos estan cambiando.
    static synchronized Trabajo iniciar(String tipo, int hilos, long valor) {
        if (actual != null) {
            return null;
        }
        contador++;
        Trabajo t = new Trabajo(contador, tipo, hilos, valor);
        trabajos.put(t.id, t);
        actual = t;
        t.start();
        return t;
    }

    static synchronized void terminar(Trabajo t) {
        if (actual == t) {
            actual = null;
        }
    }

    // Se llama por cada peticion que llega.
    public void handle(HttpExchange ex) throws IOException {
        try {
            String metodo = ex.getRequestMethod();
            String ruta = ex.getRequestURI().getPath();
            String consulta = ex.getRequestURI().getQuery();
            System.out.println(metodo + " " + ruta + (consulta == null ? "" : "?" + consulta));

            if (metodo.equals("OPTIONS")) {
                Headers h = ex.getResponseHeaders();
                h.set("Access-Control-Allow-Origin", "*");
                h.set("Access-Control-Allow-Headers", "X-Clave");
                h.set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
                ex.sendResponseHeaders(204, -1);
                ex.close();
                return;
            }

            if (ruta.equals("/favicon.ico")) {      // los navegadores lo piden solos; no hay icono
                ex.sendResponseHeaders(204, -1);
                ex.close();
                return;
            }

            if (ruta.equals("/") || ruta.equals("/index.html")) {
                File pagina = new File("web/index.html");
                if (!pagina.exists()) {
                    responder(ex, 404, "text/plain", "No se encuentra web/index.html");
                } else {
                    responder(ex, 200, "text/html", leerArchivo(pagina));
                }
                return;
            }

            // Todo lo demas necesita la clave
            String recibida = ex.getRequestHeaders().getFirst("X-Clave");
            if (recibida == null) {
                recibida = param(consulta, "clave");
            }
            if (recibida == null || !recibida.equals(clave)) {
                responder(ex, 401, "application/json", error("clave incorrecta"));
                return;
            }

            if (ruta.equals("/estado") && metodo.equals("GET")) {
                responder(ex, 200, "application/json", estadoGeneral());

            } else if (ruta.startsWith("/estado/") && metodo.equals("GET")) {
                int id = Integer.parseInt(ruta.substring(8));
                Trabajo t = trabajos.get(id);
                if (t == null) {
                    responder(ex, 404, "application/json", error("no existe ese trabajo"));
                } else {
                    responder(ex, 200, "application/json", describir(t));
                }

            } else if (ruta.equals("/buscar") && metodo.equals("GET")) {
                buscar(ex, consulta);

            } else if (metodo.equals("POST") && (ruta.equals("/cargar") || ruta.equals("/index")
                    || ruta.equals("/pack") || ruta.equals("/baja") || ruta.equals("/benchmark"))) {
                lanzar(ex, ruta, consulta);

            } else {
                responder(ex, 404, "application/json", error("orden desconocida: " + metodo + " " + ruta));
            }
        } catch (NumberFormatException e) {
            responder(ex, 400, "application/json", error("un parametro debe ser un numero"));
        } catch (Exception e) {
            responder(ex, 500, "application/json", error(e.toString()));
        }
    }

    // Arranca un trabajo largo (cargar, index, pack, baja, benchmark) despues de revisar que se pueda.
    static void lanzar(HttpExchange ex, String ruta, String consulta) throws Exception {
        String tipo = ruta.substring(1);
        int hilos = 2;
        if (param(consulta, "hilos") != null) {
            hilos = Integer.parseInt(param(consulta, "hilos"));
        }
        if (hilos < 1 || hilos > 64) {
            responder(ex, 400, "application/json", error("hilos debe estar entre 1 y 64"));
            return;
        }
        long valor = 0;

        if (tipo.equals("cargar") || tipo.equals("benchmark")) {
            if (!new File(Main.TXT).exists()) {
                responder(ex, 400, "application/json", error("no se encuentra " + Main.TXT));
                return;
            }
        } else if (!new File(Main.DATOS).exists()) {
            responder(ex, 400, "application/json", error("primero hay que cargar los datos (POST /cargar)"));
            return;
        }

        if (tipo.equals("baja")) {
            if (param(consulta, "ruc") != null) {
                tipo = "baja_ruc";
                valor = Long.parseLong(param(consulta, "ruc"));
                if (!new File(Main.IDX_RUC).exists()) {
                    responder(ex, 400, "application/json", error("primero hay que crear los indices (POST /index)"));
                    return;
                }
            } else if (param(consulta, "dep") != null) {
                tipo = "baja_dep";
                valor = Long.parseLong(param(consulta, "dep"));
                if (!new File(Main.IDX_UBIGEO).exists()) {
                    responder(ex, 400, "application/json", error("primero hay que crear los indices (POST /index)"));
                    return;
                }
            } else {
                responder(ex, 400, "application/json", error("falta ruc=... o dep=..."));
                return;
            }
        }

        Trabajo t = iniciar(tipo, hilos, valor);
        if (t == null) {
            responder(ex, 409, "application/json", error("hay un trabajo en curso, espera a que termine"));
        } else {
            responder(ex, 202, "application/json", "{\"id\":" + t.id + ",\"tipo\":\"" + t.tipo + "\"}");
        }
    }

    // Busqueda con el indice. Si hay un trabajo corriendo, se rechaza: los archivos estan cambiando.
    static void buscar(HttpExchange ex, String consulta) throws Exception {
        if (actual != null) {
            responder(ex, 409, "application/json", error("hay un trabajo en curso, espera a que termine"));
            return;
        }
        int campo = 0;
        String rutaIdx = Main.IDX_RUC;
        String texto = param(consulta, "ruc");
        if (texto == null) {
            campo = 1;
            rutaIdx = Main.IDX_UBIGEO;
            texto = param(consulta, "ubigeo");
        }
        if (texto == null) {
            responder(ex, 400, "application/json", error("falta ruc=... o ubigeo=..."));
            return;
        }
        if (!new File(rutaIdx).exists()) {
            responder(ex, 400, "application/json", error("primero hay que crear los indices (POST /index)"));
            return;
        }
        long valor = Long.parseLong(texto);

        long inicio = System.nanoTime();
        long[] posiciones = Indice.buscar(rutaIdx, valor);
        double ms = (System.nanoTime() - inicio) / 1000000.0;

        RandomAccessFile f = new RandomAccessFile(Main.DATOS, "r");
        String lista = "";
        int activos = 0;
        for (int i = 0; i < posiciones.length; i++) {
            Registro r = ArchivoDatos.leerEn(f, posiciones[i]);
            if (!r.borrado) {
                if (activos < 10) {
                    if (activos > 0) {
                        lista = lista + ",";
                    }
                    lista = lista + "\"" + escapar(r.toString()) + "\"";
                }
                activos++;
            }
        }
        f.close();

        String json = "{\"encontrados\":" + activos + ",\"bajas_ignoradas\":" + (posiciones.length - activos)
                + ",\"ms\":" + num(ms) + ",\"registros\":[" + lista + "]";
        if ("si".equals(param(consulta, "comparar"))) {
            inicio = System.nanoTime();
            long n = datos.buscarSecuencial(campo, valor);
            json = json + ",\"ms_secuencial\":" + num((System.nanoTime() - inicio) / 1000000.0)
                    + ",\"encontrados_secuencial\":" + n;
        }
        responder(ex, 200, "application/json", json + "}");
    }

    static String estadoGeneral() throws Exception {
        boolean hayDatos = new File(Main.DATOS).exists();
        boolean hayIndices = new File(Main.IDX_RUC).exists() && new File(Main.IDX_UBIGEO).exists();
        String json = "{\"datos\":" + hayDatos + ",\"indices\":" + hayIndices;
        if (actual != null) {
            json = json + ",\"ocupado\":true,\"trabajo\":" + actual.id + ",\"tipo\":\"" + actual.tipo + "\"";
        } else {
            json = json + ",\"ocupado\":false";
            if (hayDatos) {
                long[] e = datos.estadisticas();
                json = json + ",\"activos\":" + e[0] + ",\"bajas\":" + e[1]
                        + ",\"tamano\":" + new File(Main.DATOS).length() + ",\"bytes_bajas\":" + e[2];
            }
        }
        return json + "}";
    }

    static String describir(Trabajo t) {
        return "{\"id\":" + t.id + ",\"tipo\":\"" + t.tipo + "\",\"estado\":\"" + t.estado + "\",\"hilos\":"
                + t.hilos + ",\"ms\":" + num(t.ms()) + ",\"resultado\":\"" + escapar(t.resultado) + "\"}";
    }

    static String error(String mensaje) {
        return "{\"error\":\"" + escapar(mensaje) + "\"}";
    }

    // Valor de un parametro de la direccion (?nombre=valor&otro=...). Devuelve null si no esta.
    static String param(String consulta, String nombre) {
        if (consulta == null) {
            return null;
        }
        String[] partes = consulta.split("&");
        for (int i = 0; i < partes.length; i++) {
            int igual = partes[i].indexOf('=');
            if (igual > 0 && partes[i].substring(0, igual).equals(nombre)) {
                try {
                    return URLDecoder.decode(partes[i].substring(igual + 1), "UTF-8");
                } catch (Exception e) {
                    return null;
                }
            }
        }
        return null;
    }

    // Prepara un texto para ponerlo dentro de un JSON.
    static String escapar(String s) {
        String r = "";
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"') {
                r = r + "\\\"";
            } else if (c == '\\') {
                r = r + "\\\\";
            } else if (c == '\n') {
                r = r + "\\n";
            } else if (c < 32) {
                r = r + " ";
            } else {
                r = r + c;
            }
        }
        return r;
    }

    static String num(double x) {
        return String.format(Locale.US, "%.3f", x);
    }

    static String leerArchivo(File f) throws IOException {
        byte[] b = new byte[(int) f.length()];
        FileInputStream in = new FileInputStream(f);
        int leidos = 0;
        while (leidos < b.length) {
            int n = in.read(b, leidos, b.length - leidos);
            if (n < 0) {
                break;
            }
            leidos = leidos + n;
        }
        in.close();
        return new String(b, "UTF-8");
    }

    static void responder(HttpExchange ex, int codigo, String tipo, String texto) throws IOException {
        byte[] b = texto.getBytes("UTF-8");
        Headers h = ex.getResponseHeaders();
        h.set("Content-Type", tipo + "; charset=utf-8");
        h.set("Access-Control-Allow-Origin", "*");
        ex.sendResponseHeaders(codigo, b.length);
        OutputStream o = ex.getResponseBody();
        o.write(b);
        o.close();
    }
}
