
package personal;

import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParamPool extends ArrayList {
       
    public void addParamList(String slist) { // P:NOMBRE:TIPO P:NOMBRE:TIPO ...
           
        for (String param : slist.split(" ")) {
            if (param.startsWith("R")) {
                continue;
            }
            
            // P:IMPORTE:DEC
            Pattern ps = Pattern.compile("P:([A-Z_]+):([A-Z_]+)"); 
            Matcher ms = ps.matcher(param);
            if (ms.matches()) {
                String id = ms.group(1);
                String tipoDato = ms.group(2);
                int td = Common.mapTipoDato(tipoDato);
                this.add(new Parametro(id, td));
            }
            else {
                // P:CUENTA.CUENTA:STR:ALIAS
                Pattern pr = Pattern.compile("P:([A-Z_]+)\\.([A-Z_]+):([A-Z_]+):([A-Z0-9_]+)"); 
                Matcher mr = pr.matcher(param);
                if (mr.matches()) {
                    String tabla = mr.group(1);
                    String campo = mr.group(2);
                    String tipoDato = mr.group(3);
                    String id = mr.group(4);
                    int td = Common.mapTipoDato(tipoDato);
                    this.add(new Parametro(id, td, tabla, campo));
                }
                else {
                    System.out.println("ERROR: sintaxis de parametro incorrecto. '" + param + "'");
                }
            }
        }
    }
    
    private Parametro getParamPorNombre(String n) {
        Parametro found = null;
        for (Object aThi : this) {
            Parametro p = (Parametro) aThi;
            if (p.getNombre().equalsIgnoreCase(n)) {
                found = p;
            }
        }
        return found;
    }
    
    public String getStringParamValue(String paramName) {
        Parametro p = this.getParamPorNombre(paramName);
        return p.getValor();
    }
    
    public double getDoubleParamValue(String paramName) {
        Parametro p = this.getParamPorNombre(paramName);
        return Double.parseDouble(p.getValor());
    }    
    
    public int getIntParamValue(String paramName) {
        Parametro p = this.getParamPorNombre(paramName);
        return Integer.parseInt(p.getValor());
    }
    
    public Date getDateParamValue(String paramName) {
        Date valor;
        try {
            SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy");
            
            Parametro p = this.getParamPorNombre(paramName);
            java.util.Date date = format.parse(p.getValor());
            valor = new Date(date.getTime());
        } catch (ParseException ex) {
            valor = null;
        }
        return valor;
    }        
}
