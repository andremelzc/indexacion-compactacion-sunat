// Un local anexo de SUNAT.
// Campos (en este orden): RUC, UBIGEO, TIPO DE VIA, NOMBRE DE VIA, CODIGO DE ZONA,
// TIPO DE ZONA, NUMERO, KILOMETRO, INTERIOR, LOTE, DEPARTAMENTO, MANZANA
public class Registro {

    public static final int NUM_CAMPOS = 12;
    public static final String[] NOMBRES = {
        "RUC", "UBIGEO", "TIPO DE VIA", "NOMBRE DE VIA", "CODIGO DE ZONA", "TIPO DE ZONA",
        "NUMERO", "KILOMETRO", "INTERIOR", "LOTE", "DEPARTAMENTO", "MANZANA"
    };

    public String[] campos = new String[NUM_CAMPOS];
    public boolean borrado = false;  // true = marcado para eliminar
    public int tamano = 0;           // cuantos bytes ocupa en el archivo

    // Convierte el registro a bytes para guardarlo en disco.
    // Formato: [1 byte borrado][2 bytes tamano total][largo campo 1][campo 1][largo campo 2][campo 2]...
    public byte[] aBytes() throws Exception {
        byte[][] datos = new byte[NUM_CAMPOS][];
        int total = 3;
        for (int i = 0; i < NUM_CAMPOS; i++) {
            datos[i] = campos[i].getBytes("ISO-8859-1");
            total = total + 1 + datos[i].length;
        }

        byte[] b = new byte[total];
        if (borrado) {
            b[0] = 1;
        } else {
            b[0] = 0;
        }
        b[1] = (byte) (total >> 8);
        b[2] = (byte) total;

        int pos = 3;
        for (int i = 0; i < NUM_CAMPOS; i++) {
            b[pos] = (byte) datos[i].length;
            pos++;
            for (int j = 0; j < datos[i].length; j++) {
                b[pos] = datos[i][j];
                pos++;
            }
        }
        tamano = total;
        return b;
    }

    // Hace lo contrario: arma el registro a partir de los bytes leidos del disco.
    public static Registro desdeBytes(byte[] b) throws Exception {
        Registro r = new Registro();
        r.borrado = (b[0] == 1);
        r.tamano = b.length;

        int pos = 3;
        for (int i = 0; i < NUM_CAMPOS; i++) {
            int largo = b[pos] & 0xFF;
            pos++;
            r.campos[i] = new String(b, pos, largo, "ISO-8859-1");
            pos = pos + largo;
        }
        return r;
    }

    // Devuelve el valor numerico de un campo (para indexar por RUC o UBIGEO).
    // Si el campo no es un numero (hay unos pocos RUC sucios en el archivo), devuelve -1.
    public long clave(int campo) {
        String texto = campos[campo].trim();
        if (texto.length() == 0 || texto.length() > 15) {
            return -1;
        }
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (c < '0' || c > '9') {
                return -1;
            }
        }
        return Long.parseLong(texto);
    }

    public String toString() {
        String s = "";
        for (int i = 0; i < NUM_CAMPOS; i++) {
            s = s + campos[i];
            if (i < NUM_CAMPOS - 1) {
                s = s + " | ";
            }
        }
        return s;
    }
}
