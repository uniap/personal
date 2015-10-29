
package personal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class Esquema {
    ArrayList<Comando> comandos;
    ArrayList<SqlStatement> sqls;
    ArrayList<Metodo> mets;
    Connection dbCon;
    
    public static final boolean INVALID = false;
    public static final boolean VALID = true;    
    
    class Metodo {
        String nomClase, nomMetodo;

        public Metodo(String cla, String met) {
            this.nomClase = cla;
            this.nomMetodo = met;
        }

        public String getNomClase() {
            return nomClase;
        }

        public String getNomMetodo() {
            return nomMetodo;
        }
    }
    
    class SqlStatement {
        String sql;
        ArrayList<Integer> refs;

        SqlStatement() {
            refs = new ArrayList();
        }

        void addReferencia(int i) {
            this.refs.add(i);
        }

        public void setSql(String sql) {
            this.sql = sql;
        }

        public String getSqlSyntax() {
            return sql;
        }

        public ArrayList<Integer> getRefs() {
            return refs;
        }
    }
    
    class Comando {
        String identificador;
        ParamPool params;
        ArrayList<SqlStatement> sqls;    
        Metodo met;

        public Comando(String id, ParamPool p) {
            this.identificador = id;
            params = p;
            sqls = new ArrayList();
        }

        public ParamPool getParams() {
            return params;
        }

        int getCtdParametros() {
            int ctd = 0;
            if (params != null)
                ctd = params.size();
            return ctd;
        }

        public void setMet(Metodo met) {
            this.met = met;
        }

        public Metodo getMet() {
            return met;
        }

        public ArrayList<SqlStatement> getSqlList() {
            return sqls;
        }

        public void addSqlStatement(SqlStatement sql) {
            this.sqls.add(sql);
        }

        public String getIdentificador() {
            return this.identificador;
        }

        public void setIdentificador(String id) {
            this.identificador = id;
        }

        public String getHelp(Connection con, String args) throws SQLException {
            String row0 = "";
            String row1 = "";
            int ctdArgs;
            if (args.trim().isEmpty()) {
                ctdArgs = 0;
            }
            else {
                String sargs[] = args.split(" ");
                ctdArgs = sargs.length;
            }
            int i=0;
            if (this.params != null) {
                for (Parametro a : (ArrayList<Parametro>) this.params) {
                    if (i==0) {
                        row0 += "<td>" + this.getIdentificador();
                        row1 = "<td>";
                    }
                    if (i < ctdArgs) {
                        row0 += " " + a.nombre + " ";
                    }
                    else if (i == ctdArgs) {
                        row0 += "</td><td><strong>" + a.nombre + "</strong></td><td>";
                        row1 += "</td><td>" + this.getValidValues(con, a) + "</td><td>";
                    }
                    else {
                        row0 += a.nombre + " ";
                    }
                    i++;
                }
            }
            row0 += "</td>";
            return "<html><table border=0><tr>" + row0 + "</tr><tr>" + row1 + "</tr></html>";
        }
        
        private String getValidValues(Connection con, Parametro p) {
            String result = "";
            
            if (p.getTipoArg() == Parametro.REFERENCIA) {
                try {
                    Statement stmt = con.createStatement();
                    ResultSet rs = stmt.executeQuery("SELECT * FROM " + p.getTablaFk());
                    while (rs.next()) {
                        result += "<strong>" + rs.getString("abreviatura_cl") + "</strong>-" + rs.getString("label_cl") + "<br>";
                    }
                } catch (SQLException ex) {
                    result = ex.toString();
                }
            }
            else {
                result = "STATIC";
            }
            
            return result;
        }

        public boolean validarArgumentos(String cml) {
            boolean checkArgs = INVALID;

            String tokens[] = cml.split(" ");
            if (this.getCtdParametros() == (tokens.length - 1)) {
                checkArgs = VALID;
            }
            return checkArgs;
        }    

        public int getFirstInvalidArgument(String cml) {
            int nro = -1;

            String args = Common.splitCommandLine(cml)[1];

            return nro;
        }
    }    
    
    public Esquema(String source, Connection co) throws FileNotFoundException, IOException, SQLException {
        this.comandos = new ArrayList();
        this.sqls = new ArrayList();
        this.mets = new ArrayList();
        this.dbCon = co;
        
        BufferedReader in = new BufferedReader(new FileReader(new File(source)));
        String line;
        while((line = in.readLine()) != null) {
            if (line.isEmpty())
                continue;
            
            String tokens[] = line.split("=");
            switch (tokens[0]) {
                case "MET":
                    // MET=0;Negocio.ajusteSaldo
                    String tokens4[] = tokens[1].split(";");
                    String temp = tokens4[1]; // Negocio.ajusteSaldo P:CUENTA:STR P:SALDO:DEC
                    String t1[] = temp.split(" ");
                    String tt[] = t1[0].split("\\.");
                    
                    Metodo m = new Metodo(tt[0], tt[1]);                    
                    this.mets.add(m);
                    break;
                
                case "SQL":
                    SqlStatement sql = new SqlStatement();
                    String tokens3[] = tokens[1].split(";");
                    String sqlTxt = tokens3[1];
                    int desde, hasta;
                    do {
                        desde = sqlTxt.indexOf(':');
                        if (desde != -1) {
                            hasta = desde + 2; // MEJORAR
                            if (hasta == -1) {
                                hasta = sqlTxt.indexOf(')', desde);
                            }
                            String sref = sqlTxt.substring(desde, hasta).trim();
                            int ref = Integer.parseInt(sref.substring(1));
                            sql.addReferencia(ref);
                            sqlTxt = sqlTxt.replaceFirst(sref, "?");
                        }
                    } while (desde != -1);
                    sql.setSql(sqlTxt);
                    this.sqls.add(sql);
                    break;
                    
                case "COMANDO":
                    String tokens5[] = Common.splitCommandLine(tokens[1]);
                    String cid = tokens5[0];
                    String list = tokens5[1];
                    
                    if (!list.isEmpty()) {
                        ParamPool pp = new ParamPool();
                        pp.addParamList(list);

                        Comando cmd = new Comando(cid, pp);

                        String tokens2[] = list.split(" ");
                        for (String t : tokens2) {
                            String paramToks[] = t.split(":");

                            if (paramToks[0].equals("R")) {
                                // R:SQL:x o R:MET:x
                                int ref = Integer.parseInt(paramToks[2]);
                                switch (paramToks[1]) {
                                    case "SQL":
                                        cmd.addSqlStatement(this.sqls.get(ref));
                                        break;
                                    case "MET":
                                        cmd.setMet(this.mets.get(ref));
                                        break;
                                }
                            }
                        }       
                        comandos.add(cmd);
                    }
                    else {
                        comandos.add(new Comando(cid, null)); // COMANDO SIN ARGUMENTOS
                    }
                    break;
            }
        }    
    }
    
    public String showHelp() {
        String comando;
        String tabla1="", tabla2="";
        int j=0;
        for (Comando c: this.comandos) {
            comando = "<tr><td>" + c.getIdentificador();
            
            ParamPool pp = c.getParams();
            for (int i=0; i<c.getCtdParametros(); i++) {
                Parametro p = (Parametro) pp.get(i);
                comando += " <" + p.getNombre() + ">";
            }
            comando += "</td></tr>";
            if (j <= this.comandos.size() / 2) {
                tabla1 += comando;
            }
            else {
                tabla2 += comando;
            }
            j++;
        }
        
        return "<table><tr><td><table>" + tabla1 + "</table></td><td><table>" + tabla2  + "</table></td></tr></table>";
    }
    
    private Comando getCommando(String cmdId) {
        for (Comando c : comandos) {
            if (c.getIdentificador().equalsIgnoreCase(cmdId)) {
                return c;
            }
        }
        return null;
    }
    
    public String getCmdHelp(String cml) throws SQLException {
        String tokens[] = Common.splitCommandLine(cml);
        
        String cmd = tokens[0];
        String args = tokens[1];
        
        Comando c = this.getCommando(cmd);
        
        return c.getHelp(dbCon, args);
    }
    
    public boolean validarComando(String cml, boolean chArgs) {
        boolean checkId = INVALID;
        boolean checkArgs = INVALID;
        
        String tokens0[] = cml.split(" ");
        for (Comando c : comandos) {
            if (c.getIdentificador().equalsIgnoreCase(tokens0[0])) {
                checkId = VALID;
                if (chArgs) {
                    checkArgs = c.validarArgumentos(cml);
                }
                else {
                    checkArgs = VALID;
                }
                break;
            }
        }
        return checkId && checkArgs;
    }
    
    public String processCommand(String cl) {
        String out = "";
        
        String tokens[] = cl.split(" ");
        
        try {
            if (tokens[0].equals("HELP")) {
                return this.showHelp();
            }
            
            Comando c = this.getCommando(tokens[0]);
            
            ParamPool pool = c.getParams();
            for (int i=1; i<tokens.length; i++) {
                Parametro p = (Parametro) pool.get(i-1);
                p.setValor(tokens[i]);
            }
            
            Metodo m = c.getMet();
            if (m != null) {  
              Class handlerClass = Class.forName(m.getNomClase());                                
              Class [ ] paramList = new Class [ ]  { Connection.class, ParamPool.class } ; 
              Object handler = handlerClass.newInstance();
              Method me = handlerClass.getMethod(m.getNomMetodo(), paramList);
              out = (String) me.invoke(handler, this.dbCon, pool); 
            }
                      for (SqlStatement sql : c.getSqlList()) {
                if (sql.getSqlSyntax().startsWith("S")) {
                    Statement stmt = this.dbCon.createStatement();
                    try (ResultSet rs = stmt.executeQuery(sql.getSqlSyntax())) {
                        ResultSetMetaData rsmd = rs.getMetaData();
                        DataMatrix dw = new DataMatrix(rsmd.getColumnCount());
                        dw.setHdrColor(Personal.HDR_COLOR);
                        for (int i=0; i<rsmd.getColumnCount(); i++) {
                            dw.addColumn(rsmd.getColumnName(i+1), rsmd.getColumnType(i+1));
                        }                        
                        while (rs.next()) {
                            int row = dw.addRow();
                            for (int i=0; i<rsmd.getColumnCount(); i++) {
                                switch(rsmd.getColumnType(i+1)) {
                                    case java.sql.Types.INTEGER:
                                        dw.setItemInt(row, i, rs.getInt(i+1));
                                        break;
                                        
                                    case java.sql.Types.DECIMAL:
                                        dw.setItemDecimal(row, i, rs.getDouble(i+1));
                                        break;

                                    case java.sql.Types.DATE:
                                        dw.setItemDate(row, i, rs.getDate(i+1));
                                        break;
                                        
                                    default:
                                        dw.setItemString(row, i, rs.getString(i+1));
                                        break;
                                }
                            }
                        }
                        out = dw.getHTMLTable();
                    }                
                }
                else {
                    try (PreparedStatement pstmt = this.dbCon.prepareStatement(sql.getSqlSyntax())) {
                        ParameterMetaData pmtdt = pstmt.getParameterMetaData();
                        ArrayList<Parametro> args = c.getParams();
                        ArrayList<Integer> refs = sql.getRefs();
                        for (int i=0; i<refs.size(); i++) {
                            switch(pmtdt.getParameterType(i+1)) {
                                case java.sql.Types.VARCHAR:
                                case java.sql.Types.CHAR:
                                    Parametro arg = args.get(refs.get(i));
                                    if (arg.getTipoArg() == Parametro.REFERENCIA) {
                                        String valor = Common.getValorPorAbreviatura(this.dbCon, arg.getTablaFk(), arg.getCampoFk(), tokens[refs.get(i)+1]);
                                        pstmt.setString(i+1, valor);
                                    }
                                    else {
                                        pstmt.setString(i+1, tokens[refs.get(i)+1]);
                                    }
                                    break;
                                    
                                case java.sql.Types.INTEGER:
                                    pstmt.setInt(i+1, Integer.parseInt(tokens[refs.get(i)+1]));
                                    break;
                                    
                                case java.sql.Types.DATE:
                                    pstmt.setDate(i+1, Common.stringToDate(tokens[refs.get(i)+1]));
                                    break;
                                    
                                case java.sql.Types.DECIMAL:
                                    //pstmt.setDouble(i+1, Double.parseDouble(tokens[refs.get(i)+1]));
                                    String snum = tokens[refs.get(i)+1].replaceAll(",", ".");
                                    pstmt.setDouble(i+1, Double.parseDouble(snum));
                                    break;
                            }
                        }
                        pstmt.execute();
                    }
                    Imputador.impurtarMovimientos(this.dbCon);
                    this.dbCon.commit();
                }
            }    
        }
        catch(SQLException | ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchMethodException | SecurityException | IllegalArgumentException | InvocationTargetException e) {
            try {
                dbCon.rollback();
            } catch (SQLException ex) {
                out += ex.toString();
            }
            out += e.toString();
        }
        return out;
    }        
}