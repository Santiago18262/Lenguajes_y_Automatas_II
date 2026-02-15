package data;

import java.util.ArrayList;
import java.util.List;

public class Scanner {
    private final List<Token> listaTokens = new ArrayList<>();
    private int indiceActual = 0;
    private String codigoFuente, mensajeError;
    private boolean hayError;

    // Listado de palabras reservadas
    private static String[] PALABRAS_RESERVADAS = {
        "class", "boolean", "int", "while", "true", "false"
    };

    public Scanner(String codigo) {
        hayError = false;
        codigoFuente = (codigo != null) ? codigo : "";
        analizar();
    }

    // Recorre el texto y hace la lista de tokens
    private void analizar() {
        int posicion = 0, longitud = codigoFuente.length(); // Posición actual y longitud total del código fuente

        while (posicion < longitud) {
            char caracter = codigoFuente.charAt(posicion);

            // 1) Saltar espacios en blanco
            if (Character.isWhitespace(caracter)) { 
                posicion++; 
                continue; 
            }

            // 2) Identificador o reservada: [a-zA-Z][a-zA-Z0-9]*
            if (Character.isLetter(caracter)) {
                int j = posicion + 1;
                while (j < longitud && Character.isLetterOrDigit(codigoFuente.charAt(j))) j++;
                String palabra = codigoFuente.substring(posicion, j);

                Token.TokenTipo tipoReservada = obtenerTipoReservada(palabra);
                if (tipoReservada != Token.TokenTipo.Invalido) {
                    listaTokens.add(new Token(tipoReservada, palabra));
                } else {
                    listaTokens.add(new Token(Token.TokenTipo.Identificador, palabra));
                }
                posicion = j;
                continue;
            }

            // 3) Número entero 
            if (Character.isDigit(caracter)) {
                int j = posicion + 1;
                while (j < longitud && Character.isDigit(codigoFuente.charAt(j))) {
                    j++;
                }

                String palabra = codigoFuente.substring(posicion, j);
                listaTokens.add(new Token(Token.TokenTipo.NumEntero, palabra));
                posicion = j;
                continue;
            }

            // 4) Procesar simbolos individuales: + - * < > = ; { } ( ) 
            if (procesarSimbolos(caracter)) { 
                posicion++; 
                continue;
            }

            // 5) Símbolo desconocido: marcar el caracter invalido
            if (!Character.isWhitespace(caracter)) {
                listaTokens.add(new Token(Token.TokenTipo.Invalido, String.valueOf(caracter)));
            }
            posicion++;
        }

        // EOF
        listaTokens.add(new Token(Token.TokenTipo.EOF, ""));
    }

    // Símbolos permitidos pegados a palabras/números
    private boolean esSimboloValido(char c) {
        switch (c) {
            case '+': case '-': case '*':
            case '<': case '>': case '=':
            case ';': case '{': case '}':
            case '(': case ')': 
                return true;
            default:
                return false;
        }
    }

    // Si el lexema está en la lista, es PalabraReservada; sino Invalido
    private Token.TokenTipo obtenerTipoReservada(String lexema) {
        for (int i = 0; i < PALABRAS_RESERVADAS.length; i++) {
            if (PALABRAS_RESERVADAS[i].equals(lexema))
                return Token.TokenTipo.PalabraReservada;
        }
        return Token.TokenTipo.Invalido;
    }

    // OP: + - *
    // CMP: < > =
    // Otros símbolos: ; { } ( )
    private boolean procesarSimbolos(char c) {
        switch (c) {
            case '+': listaTokens.add(new Token(Token.TokenTipo.OpMAS, "+"));        return true;
            case '-': listaTokens.add(new Token(Token.TokenTipo.OpMENOS, "-"));      return true;
            case '*': listaTokens.add(new Token(Token.TokenTipo.OpMULTI, "*"));      return true;
            case '<': listaTokens.add(new Token(Token.TokenTipo.CMPMEN, "<"));       return true;
            case '>': listaTokens.add(new Token(Token.TokenTipo.CMPMAY, ">"));       return true;
            case '=': listaTokens.add(new Token(Token.TokenTipo.Asignacion, "="));   return true; 
            case ';': listaTokens.add(new Token(Token.TokenTipo.PuntoComa, ";"));    return true;
            case '{': listaTokens.add(new Token(Token.TokenTipo.LlaveAbre, "{"));    return true;
            case '}': listaTokens.add(new Token(Token.TokenTipo.LlaveCierra, "}"));  return true;
            case '(': listaTokens.add(new Token(Token.TokenTipo.ParentAbre, "("));   return true;
            case ')': listaTokens.add(new Token(Token.TokenTipo.ParentCierra, ")")); return true;
            default:  return false;
        }
    }

    // Entrega tokens en el orden en que fueron generados
    public Token siguienteToken() {
        if (indiceActual < listaTokens.size()) {
            Token t = listaTokens.get(indiceActual++);
            if (t.tipo == Token.TokenTipo.Invalido) {
                hayError = true;
                mensajeError = "Error léxico: token inválido -> " + t.valor;
            }
            return t;
        }
        return new Token(Token.TokenTipo.EOF, "");
    }

    public String getMensajeError() { return mensajeError; }
    public boolean hayError()       { return hayError; }
}