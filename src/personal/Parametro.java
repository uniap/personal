
package personal;

public class Parametro {
    String nombre, valor, tablaFk, campoFk;
    int tipoArg, tipoDato;

    static final int ESTATICO = 0;
    static final int REFERENCIA = 1;

    public Parametro(String n, int td) {
        this.nombre = n;
        this.tipoArg = Parametro.ESTATICO;
        this.tipoDato = td;
        this.tablaFk = this.campoFk = null;
    }

    public Parametro(String n, int td, String tabla, String campo) {
        this.nombre = n;
        this.tipoArg = Parametro.REFERENCIA;
        this.tablaFk = tabla;
        this.campoFk = campo;
        this.tipoDato = td;
    }

    public String getNombre() {
        return nombre;
    }

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }

    public String getTablaFk() {
        return tablaFk;
    }

    public String getCampoFk() {
        return campoFk;
    }        

    public int getTipoArg() {
        return tipoArg;
    }
}    
