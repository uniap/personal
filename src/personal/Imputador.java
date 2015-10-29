package personal;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Imputador {
    
    public static void impurtarMovimientos(Connection con) throws SQLException {
        Statement stmt = con.createStatement();
        String sql = "select M.NRO_MOVIMIENTO, M.GRUPO_CONSUMO, M.FECHA, M.CUENTA, M.PAGOS, M.IMPORTE, M.DESCRIPCION, C.TIPO_CUENTA from MOVIMIENTO M, CUENTA C where M.CUENTA = C.CUENTA and M.IMPUTADO = 'N'";
        try (ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String tipoCta = rs.getString("TIPO_CUENTA");
                double importe = rs.getDouble("IMPORTE");
                String cuenta = rs.getString("CUENTA");
                int nroMov = rs.getInt("NRO_MOVIMIENTO");
                Date fecha = rs.getDate("FECHA");
                int pagos = rs.getInt("PAGOS");
                String descr = rs.getString("DESCRIPCION");

                switch (tipoCta) {
                    case "MOVIMIENTOS":
                        Statement stu = con.createStatement();
                        stu.executeUpdate("UPDATE CUENTA SET SALDO = SALDO + " + importe + " WHERE CUENTA = '" + cuenta + "'");
                        break;

                    case "TARJETA CREDITO":
                        int res = Imputador.getPrimerResumen(con, cuenta, fecha, pagos);
                        PreparedStatement pstmt = con.prepareStatement("INSERT INTO MOVIMIENTO_RESUMEN ( IMPORTE_PAGO, NRO_MOVIMIENTO, NRO_RESUMEN, TEXTO_RESUMEN ) VALUES ( ?, ?, ?, ? )");

                        for (int p=0; p<pagos; p++) {
                            pstmt.setDouble(1, importe/pagos);
                            pstmt.setInt(2, nroMov);
                            pstmt.setInt(3, res);
                            pstmt.setString(4, descr + " " + (p+1) + "/" + pagos);
                            pstmt.execute();

                            String su = "UPDATE RESUMEN_TARJETA SET IMPORTE_TOTAL = IMPORTE_TOTAL + " + importe/pagos + " WHERE NRO_RESUMEN = " + res;
                            Statement stmtu = con.createStatement();
                            stmtu.executeUpdate(su);
                            res++;
                        }
                        double saldo = Common.runQuerySingleDouleValue(con, "SELECT IMPORTE_TOTAL FROM RESUMEN_TARJETA WHERE CUENTA = '" + cuenta + "' AND NRO_RESUMEN = ( SELECT MIN(NRO_RESUMEN) FROM RESUMEN_TARJETA WHERE CUENTA = '" + cuenta + "' AND FECHA_CIERRE IS NULL )");

                        Statement stmt1 = con.createStatement();
                        stmt1.executeUpdate("UPDATE CUENTA SET SALDO = " + saldo + " WHERE CUENTA = '" + cuenta + "'");                        
                        break;
                }
                Statement stu1 = con.createStatement();
                stu1.executeUpdate("UPDATE MOVIMIENTO SET IMPUTADO = 'S' WHERE NRO_MOVIMIENTO = " + nroMov);
            }
        }
    }
    
    private static int getPrimerResumen(Connection con, String cta, Date fecha, int pagos) throws SQLException {
        Statement stmt = con.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT MAX(FECHA_CIERRE) FROM RESUMEN_TARJETA WHERE CUENTA = '" + cta + "'");
        rs.next();
        Date ultCierre = rs.getDate(1);
        if (ultCierre != null) {
            if (fecha.compareTo(ultCierre) < 0) {
                // LA FECHA ES MENOR AL ULTIMO CIERRE, NO ADMISIBLE.
                return -1;
            }
        }
        int ctdResAbiertos = Common.runQuerySingleIntValue(con, "SELECT COUNT(*) FROM RESUMEN_TARJETA WHERE CUENTA = '" + cta + "' AND FECHA_CIERRE IS NULL");
        if (ctdResAbiertos < pagos) {
            String sql = "INSERT INTO RESUMEN_TARJETA ( CUENTA, IMPORTE_TOTAL, PAGADO ) VALUES ( ?, 0, 'N' )";
            PreparedStatement pstmt = con.prepareStatement(sql);
            for (int r=0; r<(pagos - ctdResAbiertos); r++) {
                pstmt.setString(1, cta);
                pstmt.executeUpdate();
            }
        }
        int first = Common.runQuerySingleIntValue(con, "SELECT MIN(NRO_RESUMEN) FROM RESUMEN_TARJETA WHERE CUENTA = '" + cta + "' AND FECHA_CIERRE IS NULL");
        
        return first;
    }
}