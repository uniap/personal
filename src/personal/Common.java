
package personal;

import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Common {
    
    public static String[] splitCommandLine(String cl) {
        String tokens[] = new String[2];
        
        int firstSpace = cl.indexOf(" ");
        if (firstSpace != -1) {
            tokens[0] = cl.substring(0, firstSpace).trim();
            tokens[1] = cl.substring(firstSpace).trim();
        }
        else {
            tokens[0] = cl.trim();
            tokens[1] = "";
        }
        return tokens;
    }
    
    public static int mapTipoDato(String td) {
        int tj;
        switch(td) {
            case "INT":
                tj = java.sql.Types.INTEGER;
                break;
                
            case "STR":
                tj = java.sql.Types.VARCHAR;
                break;
                
            case "DATE":
                tj = java.sql.Types.DATE;
                break;
                
            case "DEC":
                tj = java.sql.Types.DECIMAL;
                break;
                
            default: 
                tj = 0;
                System.out.println("ERROR, TIPO DE DATO NO MAPEADO! " + td);
        }
        return tj;
    }
    
    public static Date stringToDate(String sdate) {
        Date date = null;
        String dateFormats[] = { "dd-MM-yy", "dd/MM/yy", "dd-MM-yyyy", "dd/MM/yyyy" };
        
        for (String dateFormat : dateFormats) {
            SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);
            try {
                date = new Date(formatter.parse(sdate).getTime());
                break;
            } catch (ParseException e) { }   
        }
        
        if (date == null) {
            if (sdate.equalsIgnoreCase("hoy")) {
                java.util.Date fechah = new java.util.Date();
                date = new Date(fechah.getTime());
            }
            else if (sdate.equalsIgnoreCase("ayer")) {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DATE, -1);
                date = new Date(cal.getTimeInMillis());
            }
        }
        return date;
    }    
    
    public static String getValorPorAbreviatura(Connection con, String tabla, String campo, String abreviatura) throws SQLException {
        String retorno = null;
        Statement qcta = con.createStatement();
        ResultSet rsqcta = qcta.executeQuery("SELECT " + campo + " FROM " + tabla + " WHERE ABREVIATURA_CL = '" + abreviatura + "'");
        if (rsqcta.next()) {
            retorno = rsqcta.getString(1);
        }
        return retorno;
    }    
    
    private static ResultSet runQuery(Connection con, String sql) throws SQLException {
        Statement stmt = con.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        rs.next();        
        return rs;
    }
    
    public static double runQuerySingleDouleValue(Connection con, String sql) throws SQLException {
        ResultSet rs = Common.runQuery(con, sql);
        return rs.getDouble(1);
    }        
    
    public static int runQuerySingleIntValue(Connection con, String sql) throws SQLException {
        ResultSet rs = Common.runQuery(con, sql);
        return rs.getInt(1);
    }    
}
