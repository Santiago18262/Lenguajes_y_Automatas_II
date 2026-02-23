package data;

import java.util.ArrayList;
import java.util.List;

public class Semantico {
    private List<Token> listaTokens;
    private StringBuilder mensajesError = new StringBuilder();
    private String nombreClase; 

    // Clase para los Símbolos de la tabla
    public static class Simbolo {
        public final String nombre;
        public final String tipo;
        public String valor;
        public final int direccion;
        
        public Simbolo(String n, String t, String v, int d) {
            this.nombre=n; this.tipo=t; this.valor=v; this.direccion=d;
        }
		public String getNombre() {
			return nombre;
		}
		public String getTipo() {
			return tipo;
		}
    }

    // Para la tabla de Símbolos
    private final List<Simbolo> tablaSimbolos = new ArrayList<>();
    private int nextDir = 0;

    public Semantico(List<Token> tokens) { this.listaTokens = tokens; }
    public List<Simbolo> getTablaSimbolos(){ return tablaSimbolos; }
    public String getErrores(){ return mensajesError.toString(); }

    public boolean analizar() {
        this.mensajesError = new StringBuilder(); // Limpiar errores previos
        this.tablaSimbolos.clear();               // Limpiar tabla previa
        
        // Creamos un parser "fantasma" para que recorra la lógica
        // y dispare los eventos semánticos (declarar, usar, etc.)
        Parser p = new Parser(listaTokens);
        p.analizar(this); // 'this' es este objeto semántico
        
        return mensajesError.length() == 0;
    }

    public void registrarNombreClase(String nombre) {
        this.nombreClase = nombre;
    }

     /** Registrar una declaración: int x; / boolean b; */
    public void declarar(String nombre, String tipo) {
        if (nombre.equals(nombreClase)) {
            registrarError("El identificador '" + nombre + "' ya está usado como nombre de la clase.");
            return;
        }
        if (existeSimbolo(nombre)) {
            registrarError("Redeclaración de variable: " + nombre);
            return;
        }
        tablaSimbolos.add(new Simbolo(nombre, tipo, "", nextDir));
        nextDir += sizeOf(tipo);
    }

    /** Para la regla: while (Expresion Booleana) */
    public void validarCondicionWhile(List<Token> exprTokens) {
        if (!validarExprBool(exprTokens)) {
            registrarError("La condición del ciclo 'while' debe ser una expresión booleana válida.");
        }
    }

    /** Registrar el uso de una variable (en términos o lado izquierdo). */
    public void usar(String nombre) {
        if (!existeSimbolo(nombre)) {
            registrarError("Uso de variable no declarada: " + nombre);
        }
    }
    
    /** Validar asignación aritmética: id = Expresion;  (Expresion es int) */
    public void asignacionArit(String nombreVar, List<Token> exprTokens) {
        Simbolo var = buscarSimbolo(nombreVar);
        if (var == null) {
            registrarError("Variable no declarada: " + nombreVar);
            return;
        }

        // La variable destino debe ser int
        if (!"int".equals(var.tipo)) {
            registrarError("Tipos incompatibles en asignación a '" + nombreVar + "': " + var.tipo + " := int");
            return;
        }

        // Validar que la expresión sea aritmética válida (solo int)
        if (!validarExprArit(exprTokens)) {
            return; // el error ya se registró dentro
        }

        // Guardar texto de la expresión
        setValor(nombreVar, exprAString(exprTokens));
    }

    /** Validar asignación booleana: id = ExpresionBooleana; (resultado boolean) */
    public void asignacionBool(String nombreVar, List<Token> exprTokens) {
        Simbolo var = buscarSimbolo(nombreVar);
        if (var == null) {
            registrarError("Variable no declarada: " + nombreVar);
            return;
        }

        // La variable destino debe ser boolean
        if (!"boolean".equals(var.tipo)) {
            registrarError("Tipos incompatibles en asignación a '" + nombreVar + "': " + var.tipo + " := boolean");
            return;
        }

        // Validar que la expresión booleana sea válida
        if (!validarExprBool(exprTokens)) {
            return; // el error ya se registró dentro
        }

        setValor(nombreVar, exprAString(exprTokens));
    }

    // =========================================================
    // 2) Validaciones internas de expresiones
    // =========================================================

    /**
     * Expresión aritmética válida para tu gramática:
     *   Termino (OP Termino)*
     * Donde Termino es:
     *   Identificador (de tipo int) | NumEntero
     */
    private boolean validarExprArit(List<Token> expr) {
        if (expr == null || expr.isEmpty()) {
            registrarError("Expresión aritmética vacía");
            return false;
        }

        boolean esperoTermino = true;

        for (Token t : expr) {
            if (esPuntoComa(t)) break; // por seguridad

            if (esperoTermino) {
                String tipo = tipoDeTermino(t);
                if (tipo == null) {
                    registrarError("Término inválido en expresión aritmética");
                    return false;
                }
                if ("boolean".equals(tipo)) {
                    registrarError("Término inválido en expresión aritmética");
                    return false;
                }
                // si es identificador y no existe, ya se registró error en tipoDeTermino
                esperoTermino = false;
            } else {
                if (!esOperadorArit(t)) {
                    registrarError("Operador aritmético inválido o faltante");
                    return false;
                }
                esperoTermino = true;
            }
        }

        if (esperoTermino) { // terminó esperando término => expresión incompleta
            registrarError("Expresión aritmética incompleta");
            return false;
        }

        return true;
    }

    /**
     * Expresión booleana válida para tu gramática:
     *   true | false | Termino CMP Termino
     * Donde Termino debe ser numérico (int).
     */
    private boolean validarExprBool(List<Token> expr) {
        if (expr == null || expr.isEmpty()) {
            registrarError("Expresión booleana vacía");
            return false;
        }

        // Caso: true / false
        if (expr.size() == 1 && (expr.get(0).codigo == Parser.C_TRUE || expr.get(0).codigo == Parser.C_FALSE)) {
            return true;
        }

        // En lugar de asumir que el comparador está en la posición 1, 
        // lo buscamos en la lista, porque ahora puede venir "x + 5 < y"
        int iCmp = -1;
        for (int i = 0; i < expr.size(); i++) {
            if (esComparador(expr.get(i))) {
                iCmp = i;
                break;
            }
        }

        if (iCmp == -1) {
            registrarError("Comparador inválido en expresión booleana");
            return false;
        }

        // Obtenemos los términos que están justo al lado del comparador
        Token izq = expr.get(iCmp - 1);
        Token der = expr.get(iCmp + 1);

        String tipoIzq = tipoDeTermino(izq);
        String tipoDer = tipoDeTermino(der);

        if ("boolean".equals(tipoIzq) && "boolean".equals(tipoDer)) {
            registrarError("Error de tipos: Los operandos de una comparación relacional no pueden ser booleanos");
            return false;
        }

        if (tipoIzq == null || "boolean".equals(tipoIzq) || tipoDer == null || "boolean".equals(tipoDer)) {
            registrarError("Error de tipos: No se pueden comparar tipos no numéricos");
            return false;
        }

        // Si tu Parser garantiza la gramática exacta, con esto basta.
        // Si pudiera venir algo como: a < b + 1, entonces aquí habría que extenderlo.
        return true;
    }

    // =========================================================
    // 3) Helpers de tabla de símbolos
    // =========================================================

    private boolean existeSimbolo(String nombre) {
        for (Simbolo s : tablaSimbolos) {
            if (s.nombre.equals(nombre)) return true;
        }
        return false;
    }

    private Simbolo buscarSimbolo(String nombre) {
        for (Simbolo s : tablaSimbolos) {
            if (s.nombre.equals(nombre)) return s;
        }
        return null;
    }

    private void setValor(String nombre, String valor) {
        for (Simbolo s : tablaSimbolos) {
            if (s.nombre.equals(nombre)) {
                s.valor = valor;
                return;
            }
        }
    }

    // =========================================================
    // 4) Helpers de tipos / tokens
    // =========================================================

    private String tipoDeTermino(Token t) {
        if (t == null) return null;

        if (t.codigo == Parser.C_NUMENTERO) return "int";

        if (t.codigo == Parser.C_TRUE || t.codigo == Parser.C_FALSE) return "boolean";

        if (t.codigo == Parser.C_IDENTIFICADOR) {
            Simbolo s = buscarSimbolo(t.valor);
            if (s == null) {
                registrarError("Uso de variable no declarada: " + t.valor);
                return null;
            }
            return s.tipo;
        }

        return null;
    }

    private boolean esOperadorArit(Token t) {
        if (t == null) return false;
        return t.codigo == Parser.C_OPMAS || t.codigo == Parser.C_OPMENOS || t.codigo == Parser.C_OPMULTI;
    }

    private boolean esComparador(Token t) {
        if (t == null) return false;
        return t.codigo == Parser.C_CMPMAY || t.codigo == Parser.C_CMPMEN;
    }

    private boolean esPuntoComa(Token t) {
        return t != null && t.codigo == Parser.C_PUNTOCOMA;
    }

    private int sizeOf(String tipo) {
        switch (tipo) {
            case "int": return 2;
            case "boolean": return 1;
            default: return 0;
        }
    }

    private String exprAString(List<Token> expr) {
        StringBuilder sb = new StringBuilder();
        for (Token t : expr) {
            if (t == null) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(t.valor);
        }
        return sb.toString().trim();
    }

    private void registrarError(String msg) {
        if (msg == null || msg.isEmpty()) return;

        int len = mensajesError.length();
        if (len > 0) {
            int lastNl = mensajesError.lastIndexOf("\n");
            String last = (lastNl >= 0) ? mensajesError.substring(lastNl + 1) : mensajesError.toString();
            if (msg.equals(last)) return;
            mensajesError.append('\n');
        }
        mensajesError.append(msg);
    }
}