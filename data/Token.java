package data;

public class Token {
    public enum TokenTipo { 
        Identificador, PalabraReservada, NumEntero, OpMAS, OpMENOS, OpMULTI, CMPMEN, CMPMAY, Asignacion, PuntoComa,
        LlaveAbre, LlaveCierra, ParentAbre, ParentCierra, EOF, Invalido,
        Class, Boolean, Int, While, True, False
    }   

    public final TokenTipo tipo;
    public final String valor;
    public int codigo;

    public Token(TokenTipo tipo, String valor) {
        this.tipo = tipo;
        this.valor = valor;
        this.codigo = 0;
    }

    public String toString() {
        return "tipo token" + tipo + ": " + valor;
    }
}