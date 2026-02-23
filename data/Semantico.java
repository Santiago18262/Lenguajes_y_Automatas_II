package data;

import java.util.ArrayList;
import java.util.List;

public class Semantico {

    private final List<Token> listaTokens;
    private StringBuilder mensajesError = new StringBuilder();
    private String nombreClase;

    /** Tabla de símbolos */
    public static class Simbolo {
        public final String nombre;
        public final String tipo;
        public String valor;
        public final int direccion;

        public Simbolo(String n, String t, String v, int d) {
            this.nombre = n;
            this.tipo = t;
            this.valor = v;
            this.direccion = d;
        }

        public String getNombre() { return nombre; }
        public String getTipo() { return tipo; }
    }

    // Tabla de símbolos 
    private final List<Simbolo> tablaSimbolos = new ArrayList<>();
    private int nextDir = 0;

    public Semantico(List<Token> tokens) {
        this.listaTokens = tokens;
    }

    public List<Simbolo> getTablaSimbolos() { return tablaSimbolos; }
    public String getErrores() { return mensajesError.toString(); }

    /** Ejecuta el análisis semántico reutilizando el Parser:
     * El Parser valida sintaxis y, al reconocer reglas, invoca acciones semánticas aquí
     * (declarar, usar, asignacionArit, asignacionBool, validarCondicionWhile, etc.).
     */
    public boolean analizar() {
        mensajesError = new StringBuilder();
        tablaSimbolos.clear();
        nextDir = 0;

        Parser p = new Parser(listaTokens);
        p.analizar(this);

        return mensajesError.length() == 0;
    }

    public void registrarNombreClase(String nombre) {
        this.nombreClase = nombre;
    }

    /** Registra una variable en la tabla de símbolos validando redeclaraciones */
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

    /** Valida que la condición del while sea una expresión booleana correcta. */
    public void validarCondicionWhile(List<Token> exprTokens) {
        if (!validarExprBool(exprTokens)) {
            registrarError("La condición del ciclo 'while' debe ser una expresión booleana válida.");
        }
    }

    /** Verifica que una variable exista antes de usarse en una expresión o instrucción. */
    public void usar(String nombre) {
        if (!existeSimbolo(nombre)) {
            registrarError("Uso de variable no declarada: " + nombre);
        }
    }

    /**
     * Asignación aritmética: id = Expresion;
     * - id debe ser int
     * - la expresión debe ser aritmética (solo términos int y operadores aritméticos)
     */
    public void asignacionArit(String nombreVar, List<Token> exprTokens) {
        Simbolo var = buscarSimbolo(nombreVar);
        if (var == null) {
            registrarError("Variable no declarada: " + nombreVar);
            return;
        }

        if (!"int".equals(var.tipo)) {
            registrarError("Tipos incompatibles en asignación a '" + nombreVar + "': " + var.tipo + " := int");
            return;
        }

        if (!validarExprArit(exprTokens)) {
            registrarError("La expresión asignada a '" + nombreVar + "' no es aritmética válida.");
            return;
        }

        setValor(nombreVar, exprAString(exprTokens));
    }

    /**
     * Asignación booleana: id = ExpresionBooleana;
     * - id debe ser boolean
     * - la expresión debe evaluarse a boolean (true/false o comparación relacional)
     */
    public void asignacionBool(String nombreVar, List<Token> exprTokens) {
        Simbolo var = buscarSimbolo(nombreVar);
        if (var == null) {
            registrarError("Variable no declarada: " + nombreVar);
            return;
        }

        if (!"boolean".equals(var.tipo)) {
            registrarError("Tipos incompatibles en asignación a '" + nombreVar + "': " + var.tipo + " := boolean");
            return;
        }

        if (!validarExprBool(exprTokens)) {
            registrarError("La expresión asignada a '" + nombreVar + "' no es booleana válida.");
            return;
        }

        setValor(nombreVar, exprAString(exprTokens));
    }

    // ===========================
    // Validación de expresiones 
    // ===========================

    /** Expresión aritmética: Expresion OP Expresion */
    private boolean validarExprArit(List<Token> expr) {
        boolean esperoTermino = true;
        for (Token t : expr) {
            if (esPuntoComa(t)) break;
            if (esperoTermino) {
                String tipo = tipoDeTermino(t);
                if (tipo == null || "boolean".equals(tipo)) {
                    registrarError("Término inválido en expresión aritmética");
                    return false;
                }
                esperoTermino = false;
            } else {
            // Debe de contener un operador + - *
            if (!esOperadorArit(t)) {
                registrarError("Operador inválido en expresión aritmética");
                return false;
            }
            esperoTermino = true; // Despues del operador espero término
            }
        }
        if (esperoTermino) {
            registrarError("Expresión aritmética incompleta");
            return false;
        }

        return true;
    }

    /** Expresión booleana: Expresion CMP Expresion | true | false */
    private boolean validarExprBool(List<Token> expr) {
        // True or False
        if (expr.size() == 1 && (expr.get(0).codigo == Parser.C_TRUE || expr.get(0).codigo == Parser.C_FALSE)) {
            return true;
        }

        // Se busca el comparador
        int iCmp = -1;
        for (int i = 0; i < expr.size(); i++) {
            if (esComparador(expr.get(i))) {
                iCmp = i;
                break;
            }
        }

        // El comparador no puede ser el primer ni ultimo token
        if (iCmp == -1) {
            registrarError("Falta comparador (< o >) en expresión booleana");
            return false;
        }

        if (iCmp == 0 || iCmp == expr.size() - 1) {
            registrarError("Comparación incompleta en expresión booleana");
            return false;
        }

        List<Token> izquierda = expr.subList(0, iCmp);
        List<Token> derecha = expr.subList(iCmp + 1, expr.size());

        // Validar ambos lados como expresiones aritméticas (int)
        if (!validarExprArit(izquierda)) {
            registrarError("Error en comparación: el lado izquierdo no es una expresión aritmética válida (int).");
            return false;
        }

        if (!validarExprArit(derecha)) {
            registrarError("Error en comparación: el lado derecho no es una expresión aritmética válida (int).");
            return false;
        }

        return true;
    }

    // Operaciones sobre la tabla de símbolos
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

    /** Utilidades para validación de expresiones 
     * Devuelve el tipo semántico de un token usado como término (o null si no aplica). */
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

    /** Tamaño “simulado” por tipo para calcular direcciones. */
    private int sizeOf(String tipo) {
        switch (tipo) {
            case "int": return 2;
            case "boolean": return 1;
            default: return 0;
        }
    }

    /** Convierte una lista de tokens a texto, para guardar la expresión asignada. */
    private String exprAString(List<Token> expr) {
        StringBuilder sb = new StringBuilder();
        for (Token t : expr) {
            if (t == null) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(t.valor);
        }
        return sb.toString().trim();
    }

    /** Agrega un error al mensaje de errorres */
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