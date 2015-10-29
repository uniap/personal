
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import personal.Common; 
import personal.DataMatrix;
import personal.Imputador;
import personal.ParamPool;
import personal.Personal;

public class Negocio {
        
    public String reportGrupos(Connection con, ParamPool params) {
        String content;
        try {
            int meses = 12;
            Calendar cal = new GregorianCalendar();
            
            DataMatrix dm = new DataMatrix(meses+2);
            dm.addColumn("GRUPO", java.sql.Types.VARCHAR);
            dm.showColsTotals(true);
            dm.setHdrColor(Personal.HDR_COLOR);
            cal.add(Calendar.MONTH, meses * -1);
            int last_ano = 0, last_mes = 0;
            ArrayList<Double> resumenes = new ArrayList();
            for (int m=0; m<meses+1; m++) {
                int mes = cal.get(Calendar.MONTH)+1;
                int ano = cal.get(Calendar.YEAR);
                dm.addColumn(mes + "_" + ano, java.sql.Types.DECIMAL);
                cal.add(Calendar.MONTH, 1);
                
                Statement stmtr = con.createStatement();
                ResultSet rs = stmtr.executeQuery("select importe_total from resumen_tarjeta where year(fecha_pago) = " + ano + " and month(fecha_pago) = " + mes + " and pagado='S'");
                if (rs.next()) {
                    resumenes.add(rs.getDouble(1));
                }
                else {
                    resumenes.add(0.0);
                }
                last_ano = ano;
                last_mes = mes;
            }
            
            String sql = "select m.GRUPO_CONSUMO, year(m.fecha), month(m.fecha), abs(sum(m.importe)) ";
            sql += "from movimiento m, cuenta c ";
            sql += "where m.cuenta = c.cuenta AND m.grupo_consumo NOT IN ('ININPUTABLE', 'INGRESOS') and c.tipo_cuenta <> 'TARJETA CREDITO'";
            sql += "group by m.GRUPO_CONSUMO, year(m.fecha), month(m.fecha) ";
            sql += "order by m.GRUPO_CONSUMO, year(m.fecha), month(m.fecha)";
            
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            String lastGrp = "";
            int row=0;
            
            while (rs.next()) {
                String grp = rs.getString(1);
                int anio = rs.getInt(2);
                int mes = rs.getInt(3);
                double total = rs.getDouble(4);
                
                if (!lastGrp.equals(grp)) {
                    row = dm.addRow();
                    dm.setItemString(row, 0, grp);
                    lastGrp = grp;
                }
                try {
                    dm.setItemDecimal(row, mes + "_" + anio, total);
                }
                catch(ArrayIndexOutOfBoundsException e) {
                    System.out.println(row + " " + mes + "_" + anio + " " + total);
                }
            }
            
            row = dm.addRow();
            dm.setItemString(row, 0, "TARJETA");
            for (int m=0; m<meses+1; m++) {
                dm.setItemDecimal(row, m+1, resumenes.get(m));
            }
            
            content = dm.getHTMLTable();
        } catch (SQLException ex) {
            content = ex.toString();
        }
        return content;
    }
    
    public String busqueda(Connection con, ParamPool params) {
        String result;
        try {
            String txt = params.getStringParamValue("TXT");
            Statement stmt = con.createStatement();
            String sql = "SELECT FECHA, CUENTA, GRUPO_CONSUMO, DESCRIPCION, IMPORTE FROM MOVIMIENTO WHERE DESCRIPCION LIKE '%" + txt + "%'";
            ResultSet rs = stmt.executeQuery(sql);
            DataMatrix dw = new DataMatrix("DATE STR STR STR DEC", 5);
            dw.setColumnName(0, "FECHA");
            dw.setColumnName(1, "CUENTA");
            dw.setColumnName(2, "GRUPO_CONSUMO");
            dw.setColumnName(3, "DESCRIPCION");
            dw.setColumnName(4, "IMPORTE");
            while(rs.next()) {
                int f = dw.addRow();
                dw.setItemDate(f, 0, rs.getDate("FECHA"));
                dw.setItemString(f, 1, rs.getString("CUENTA"));
                dw.setItemString(f, 2, rs.getString("GRUPO_CONSUMO"));
                dw.setItemString(f, 3, rs.getString("DESCRIPCION"));
                dw.setItemDecimal(f, 4, rs.getDouble("IMPORTE"));
            }
            result = dw.getHTMLTable();
        } catch (SQLException ex) {
            result = ex.toString();
        }
        return result;
    }
    
    public String cierrePlazoFijo(Connection con, ParamPool params) {
        String result = "";
        
        try {
            int nroPF = params.getIntParamValue("PF_ORDEN");
            String cta = params.getStringParamValue("CTA_CR");
            String ctaCr = Common.getValorPorAbreviatura(con, "CUENTA", "CUENTA", cta);
            
            java.util.Date ahora = new java.util.Date();
            
            Statement qry = con.createStatement();
            ResultSet rs = qry.executeQuery("SELECT * FROM PLAZO_FIJO WHERE NRO_PLAZO_FIJO = " + nroPF);
            if (rs.next()) {
                double capital = rs.getDouble("CAPITAL");
                double interes = rs.getDouble("INTERES");
                
                PreparedStatement psInsCr1 = con.prepareStatement("INSERT INTO MOVIMIENTO ( GRUPO_CONSUMO, FECHA, CUENTA, PAGOS, IMPORTE, IMPUTADO, DESCRIPCION ) VALUES ( ?, ?, ?, 1, ?, 'N', ? )");
                psInsCr1.setString(1, "ININPUTABLE");
                psInsCr1.setDate(2, new Date(ahora.getTime()));
                psInsCr1.setString(3, ctaCr);
                psInsCr1.setDouble(4, capital);
                psInsCr1.setString(5, "CR CAPITAL P/CIERRE PF.");
                psInsCr1.execute();

                PreparedStatement psInsCr2 = con.prepareStatement("INSERT INTO MOVIMIENTO ( GRUPO_CONSUMO, FECHA, CUENTA, PAGOS, IMPORTE, IMPUTADO, DESCRIPCION ) VALUES ( ?, ?, ?, 1, ?, 'N', ? )");
                psInsCr2.setString(1, "INGRESOS");
                psInsCr2.setDate(2, new Date(ahora.getTime()));
                psInsCr2.setString(3, ctaCr);
                psInsCr2.setDouble(4, interes);
                psInsCr2.setString(5, "CR INTERES P/CIERRE PF.");
                psInsCr2.execute();
                
                Statement upd = con.createStatement();
                upd.executeUpdate("UPDATE PLAZO_FIJO SET CERRADO = 'S' WHERE NRO_PLAZO_FIJO = " + nroPF);
                
                PreparedStatement psUpdCta = con.prepareStatement("UPDATE CUENTA SET SALDO = SALDO - ? WHERE TIPO_CUENTA = 'PLAZO FIJO'");
                psUpdCta.setDouble(1, capital + interes);
                psUpdCta.execute();

                Imputador.impurtarMovimientos(con);
                con.commit();
            }
            
            return result;
        } catch (SQLException ex) {
            try {
                con.rollback();
            } catch (SQLException ex1) {
                result = ex1.toString();
            }
            result += ex.toString();
        }
        return result;
    }
    
    public String altaPlazoFijo(Connection con, ParamPool params) {
        String result = "";
        
        java.util.Date ahora = new java.util.Date();
        
        try {
            String cta2 = params.getStringParamValue("CTA_DB");
            String ctaDb = Common.getValorPorAbreviatura(con, "CUENTA", "CUENTA", cta2);
            
            // Insert de plazo_fijo
            PreparedStatement psInsPf = con.prepareStatement("INSERT INTO PLAZO_FIJO ( BANCO, FECHA_VTO, CAPITAL, INTERES, CERRADO ) VALUES ( ?, ?, ?, ?, 'N' )");
            psInsPf.setString(1, params.getStringParamValue("BANCO"));
            psInsPf.setDate(2, params.getDateParamValue("FECHA_VTO"));
            psInsPf.setDouble(3, params.getDoubleParamValue("CAPITAL"));
            psInsPf.setDouble(4, params.getDoubleParamValue("INTERES"));
            psInsPf.execute();
            
            // Insert del debito en cuenta.
            PreparedStatement psInsDb = con.prepareStatement("INSERT INTO MOVIMIENTO ( GRUPO_CONSUMO, FECHA, CUENTA, PAGOS, IMPORTE, IMPUTADO, DESCRIPCION ) VALUES ( ?, ?, ?, 1, ?, 'N', ? )");
            psInsDb.setString(1, "ININPUTABLE");
            psInsDb.setDate(2, new Date(ahora.getTime()));
            psInsDb.setString(3, ctaDb);
            psInsDb.setDouble(4, params.getDoubleParamValue("CAPITAL") * -1);
            psInsDb.setString(5, "CONSTITUCION DE PLAZO FIJO");
            psInsDb.execute();
            
            // Update de la cuenta pfs.
            PreparedStatement psUpdCta = con.prepareStatement("UPDATE CUENTA SET SALDO = SALDO + ? WHERE TIPO_CUENTA = 'PLAZO FIJO'");
            double importe = params.getDoubleParamValue("CAPITAL") + params.getDoubleParamValue("INTERES");
            psUpdCta.setDouble(1, importe);
            psUpdCta.execute();
            
            // Imputacion de cuentas.
            Imputador.impurtarMovimientos(con);
            con.commit();
            
        } catch (SQLException ex) {
            result += ex.toString();
            try {
                con.rollback();
            } catch (SQLException ex1) {
                result += ex1.toString();
            }
        }
        
        return result;
    }
    
    public String resumenConsolidado(Connection con, ParamPool params) throws SQLException {
        DataMatrix tDisp = this.makeMatrix(con, "DISPONIBLE");
        DataMatrix tInvs = this.makeMatrix(con, "INVERSIONES");
        DataMatrix tPags = this.makeMatrix(con, "A_PAGAR");
        DataMatrix tCobs = this.makeMatrix(con, "POR_COBRAR");
        
        tDisp.setTitulo("DISPONIBLE");
        tDisp.showColsTotals(true);
        tInvs.setTitulo("INVERSIONES");
        tInvs.showColsTotals(true);
        tPags.setTitulo("A PAGAR");
        tPags.showColsTotals(true);
        tCobs.setTitulo("POR COBRAR");
        tCobs.showColsTotals(true);
        
        String content = "";
        content += "<tr><td valign=top>" + tDisp.getHTMLTable() + "</td><td valign=top>" + tPags.getHTMLTable() + "</td></tr>";
        content += "<tr><td valign=top>" + tInvs.getHTMLTable() + "</td><td valign=top>" + tCobs.getHTMLTable() + "</td></tr>";
        
        return "<table border=0>" + content + "</table>";
    }
    
    private DataMatrix makeMatrix(Connection con, String grpRes) throws SQLException {
        DataMatrix dw = new DataMatrix("STR DEC DEC", 3);
        dw.setColumnName(0, "CUENTA");
        dw.setColumnName(1, "PESOS");
        dw.setColumnName(2, "DOLARES");
        
        Statement qryCtas = con.createStatement();
        ResultSet rsCtas = qryCtas.executeQuery("SELECT CUENTA, SALDO, MONEDA FROM CUENTA WHERE GRUPO_RESUMEN = '" + grpRes + "'");
        while (rsCtas.next()) {
            int row = dw.addRow();
            dw.setItemString(row, 0, rsCtas.getString("CUENTA"));
            double saldo = rsCtas.getDouble("SALDO");
            String moneda = rsCtas.getString("MONEDA");  
            dw.setItemDecimal(row, (moneda.equals("P")) ? 1 : 2, saldo);
        }
        return dw;
    }
    
    public String pagoResumen(Connection con, ParamPool params) {
        String result = "";
        try {
            String pCta1 = params.getStringParamValue("CUENTA_TJ");
            String ctaTj = Common.getValorPorAbreviatura(con, "CUENTA", "CUENTA", pCta1);
            
            String pCta2 = params.getStringParamValue("CUENTA_DB");
            String ctaDb = Common.getValorPorAbreviatura(con, "CUENTA", "CUENTA", pCta2);            
            
            Statement qry = con.createStatement();
            ResultSet rsQry = qry.executeQuery("SELECT NRO_RESUMEN, IMPORTE_TOTAL FROM RESUMEN_TARJETA WHERE CUENTA = '" + ctaTj + "' AND FECHA_CIERRE IS NOT NULL AND PAGADO = 'N'");
            if (rsQry.next()) {
                // (1) Obtener nro de resumen y saldo del resumen de la cuenta<1>
                double impTotal = rsQry.getDouble("IMPORTE_TOTAL") * -1;
                int nroResumen = rsQry.getInt("NRO_RESUMEN");
                
                if (rsQry.isLast()) {
                    // Grabar debito en la cuenta<2> con el saldo obtenido en el punto (1).
                    String sql = "INSERT INTO MOVIMIENTO ( GRUPO_CONSUMO, FECHA, CUENTA, PAGOS, IMPORTE, IMPUTADO, DESCRIPCION ) ";
                    sql += "VALUES ( 'ININPUTABLE', SYSDATE, '" + ctaDb + "', 1, " + impTotal + ", 'N', 'PAGO RESUMEN TARJETA')";
                    Statement sdeb = con.createStatement();
                    sdeb.executeUpdate(sql);
                    
                    // Solicitar la imputación.
                    Imputador.impurtarMovimientos(con);
                    
                    // Marcar el resumen (1) como pagado.
                    Statement sur = con.createStatement();
                    sur.executeUpdate("UPDATE RESUMEN_TARJETA SET PAGADO = 'S', FECHA_PAGO = SYSDATE WHERE NRO_RESUMEN = " + nroResumen);
                    con.commit();
                }
                else {
                    result = "ERROR, MAS DE UN RESUMEN ELEGILE!";
                }
            }
        } catch (SQLException ex) {
            result += ex.toString();
            try {
                con.rollback();
            } catch (SQLException ex1) {
                result += "FALLO EL ROLLBACK!";
            }
        }
        return result;
    }
    
    public String cierreResumen(Connection con, ParamPool params) {
        String result;
        try {
            String pCta = params.getStringParamValue("CUENTA");
            String cta = Common.getValorPorAbreviatura(con, "CUENTA", "CUENTA", pCta);
            
            String sql = "UPDATE RESUMEN_TARJETA SET FECHA_CIERRE = sysdate WHERE NRO_RESUMEN = ( SELECT MIN(NRO_RESUMEN) FROM RESUMEN_TARJETA WHERE CUENTA = '" + cta + "' AND FECHA_CIERRE IS NULL )";
            
            Statement stmt = con.createStatement();
            stmt.executeUpdate(sql);
            result = "OK";
            con.commit();
        } catch (SQLException ex) {
            result = ex.toString();
            try {
                con.rollback();
            } catch (SQLException ex1) {
                result += ex1.toString();
            }
        }
        return result;
    }
    
    public String reportResumens(Connection con, ParamPool params) {
        String content;
        
        try {
            String pCta = params.getStringParamValue("CUENTA");            
            String cta = Common.getValorPorAbreviatura(con, "CUENTA", "CUENTA", pCta);
            
            String sqlr = "select min(nro_resumen), max(nro_resumen) from resumen_tarjeta where cuenta = '" + cta + "' AND pagado = 'N'";
            Statement stmtr = con.createStatement();
            ResultSet rsr = stmtr.executeQuery(sqlr);
            int fr = 0, lr, ctd = 0;
            if (rsr.next()) {
                fr = rsr.getInt(1);
                lr = rsr.getInt(2);
                ctd = lr - fr + 1;
            }
                                    
            String sql = "SELECT M.NRO_MOVIMIENTO, MR.NRO_RESUMEN, DESCRIPCION, IMPORTE_PAGO\n" +
                    "FROM MOVIMIENTO_RESUMEN MR, MOVIMIENTO M\n" +
                    "WHERE MR.NRO_MOVIMIENTO = M.NRO_MOVIMIENTO \n" +
                    "AND NRO_RESUMEN IN ( SELECT NRO_RESUMEN FROM RESUMEN_TARJETA WHERE CUENTA = '" + cta + "' AND PAGADO = 'N' )\n" +
                    "ORDER BY NRO_MOVIMIENTO;";
            
            ArrayList<String> descs = new ArrayList();
            ArrayList<double[]> valores = new ArrayList();
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(sql);        
            String last = "";
            double[] vals = null;
            String desc = "";
            while (rs.next()) {
                desc = rs.getString("DESCRIPCION") + ";;" + rs.getInt("NRO_MOVIMIENTO");
                double imp = rs.getDouble("IMPORTE_PAGO");
                int res = rs.getInt("NRO_RESUMEN") - fr;
                
                if (rs.isFirst()) {
                    last = desc;
                    vals = new double[ctd];
                }
                
                if (!desc.equals(last)) {
                    descs.add(last);
                    valores.add(vals);
                    
                    last = desc;
                    vals = new double[ctd];
                    vals[res] = imp;
                }
                else {
                    vals[res] = imp;
                }
            }
            descs.add(desc);
            valores.add(vals);

            DataMatrix dw = new DataMatrix(ctd+1);
            dw.showColsTotals(true);
            dw.addColumn("Descripcion", java.sql.Types.VARCHAR);
            for (int m=0; m<ctd; m++) {
                dw.addColumn("Mes " + m, java.sql.Types.DECIMAL);
            }
            for (double val[] : valores) {
                int f = dw.addRow();                
                dw.setItemString(f, 0, descs.get(f).split(";;")[0]);
                for (int c=0; c<val.length; c++) {
                    dw.setItemDecimal(f, c+1, val[c]);
                }
            }
            
            content = dw.getHTMLTable();
        } catch (SQLException ex) {
            content = ex.toString();
        }
        return content;
    }
    
    public String ajusteSaldo(Connection con, ParamPool params) {  
        String content = "";
        try {
            String pCta = params.getStringParamValue("CUENTA");
            double pSdo = params.getDoubleParamValue("SALDO");
            
            String cta = Common.getValorPorAbreviatura(con, "CUENTA", "CUENTA", pCta);
            
            double saldoAct = Common.runQuerySingleDouleValue(con, "SELECT SALDO FROM CUENTA WHERE CUENTA = '" + cta + "'");
            
            double ajuste = pSdo - saldoAct;
            
            Statement stmt = con.createStatement();
            String sql = "INSERT INTO MOVIMIENTO ( GRUPO_CONSUMO, FECHA, CUENTA, PAGOS, IMPORTE, IMPUTADO, DESCRIPCION ) ";
            sql += "VALUES ( 'ININPUTABLE', SYSDATE, '" + cta + "', 1, " + ajuste + ", 'N', 'AJUSTE SALDO')";
            stmt.executeUpdate(sql);
            Imputador.impurtarMovimientos(con);
            con.commit();
        } catch (SQLException ex) {
            content = ex.toString();
            try {
                con.rollback();
            } catch (SQLException ex1) {
                content += ex1.toString();
            }
        }
        return content;
    }
}
