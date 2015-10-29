
package personal;

import java.sql.Date;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class DataMatrix {
    ArrayList<String> header;
    ArrayList<Object[]> rows;
    int dataTypes[];
    int colCount;
    boolean sumarCols;
    String hdrColor, titulo;
    
    public DataMatrix(int cols) {
        rows = new ArrayList();
        header = new ArrayList();
        this.colCount = cols;
        dataTypes = new int[cols];        
        this.sumarCols = false;
        this.titulo = null;
    }
    
    public DataMatrix(String colTypesList, int cols) {
        this.sumarCols = false;
        rows = new ArrayList();
        header = new ArrayList();
        this.colCount = cols;
        dataTypes = new int[this.colCount];
        this.titulo = null;
        
        String ts[] = colTypesList.split(" ");
        for (int i=0; i<ts.length; i++) {
            if (ts[i].equalsIgnoreCase("STR")) {
                dataTypes[i] = java.sql.Types.VARCHAR;
            }
            else if (ts[i].equalsIgnoreCase("DEC")) {
                dataTypes[i] = java.sql.Types.DECIMAL;
            }
            else if (ts[i].equalsIgnoreCase("DATE")) {
                dataTypes[i] = java.sql.Types.DATE;
            }
        }
        for (int i=0; i<this.colCount; i++) {
            header.add("<none>");
        }
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    private int getColumnIdx(String colName) {
        int idx = 0;
        for (String c : this.header) {
            if (c.equalsIgnoreCase(colName)) {
                break;
            }
            idx++;
        }
        return idx;
    }
    
    public void setHdrColor(String hdrColor) {
        this.hdrColor = hdrColor;
    }
        
    public void showColsTotals(boolean t) {
        this.sumarCols = t;
    }
    
    public void setColumnName(int idx, String n) {
        header.set(idx, n);
    }
    
    public void addColumn(String n, int tipoDato) {
        header.add(n);
        int idx = header.size() - 1;
        dataTypes[idx] = tipoDato;
    }
    
    public int getRowCount() {
        return rows.size();
    }
    
    public int addRow() {
        Object cels[] = new Object[this.colCount];
        this.rows.add(cels);
        return rows.size() -1;
    }
    
    public void setItemString(int row, int col, String str) {
        Object cels[] = this.rows.get(row);
        cels[col] = str;
    }

    public void setItemDecimal(int row, int col, double val) {
        Object cels[] = this.rows.get(row);
        cels[col] = val;
    }    
    
    public void setItemDecimal(int row, String colName, double val) {
        Object cels[] = this.rows.get(row);
        cels[this.getColumnIdx(colName)] = val;
    } 
    
    public void setItemChar(int row, int col, char val) {
        Object cels[] = this.rows.get(row);
        cels[col] = val;
    }    
    
    public void setItemInt(int row, int col, int val) {
        Object cels[] = this.rows.get(row);
        cels[col] = val;
    }
    
    public void setItemDate(int row, int col, Date val) {
        Object cels[] = this.rows.get(row);
        cels[col] = val;
    }
    
    public String getHTMLTable() {
        String html = "";
        
        DecimalFormat df2 = new DecimalFormat( "#,###,##0.00" );
        
        for (String s : header) {
            html += "<td bgcolor='" + this.hdrColor + "'>" + s + "</td>";
        }
        html = "<tr>" + html + "</tr>";
        double total[] = new double[this.colCount];        
        boolean gris = false;
        for (Object row[] : rows) {
            html += "<tr>";
            String color = (gris) ? "#F2F2F2" : "#FFFFFF";
            gris = !gris;
            for (int i=0; i<this.colCount; i++) {
                if (row[i] != null) {
                    switch(dataTypes[i]) {
                        case java.sql.Types.INTEGER:
                            html += "<td align=right bgcolor='" + color + "'>";
                            html += "" + (int) row[i];
                            total[i] += (int) row[i];
                            break;
                        case java.sql.Types.DECIMAL:
                            html += "<td align=right bgcolor='" + color + "'>";
                            if ((double) row[i] != 0 ) {
                                html += df2.format((double) row[i]);
                                total[i] += (double) row[i];
                            }
                            break;
                        case java.sql.Types.VARCHAR:
                        case java.sql.Types.CHAR:
                            html += "<td bgcolor='" + color + "'>";
                            html += (String) row[i];
                            break;
                        case java.sql.Types.DATE:
                            html += "<td align=center bgcolor='" + color + "'>";
                            html += (Date) row[i];
                            break;
                        default:
                            System.err.println("Tipo no soportado en DataMatrix! (" + dataTypes[i] + ")");
                    }
                }
                else {
                    html += "<td bgcolor='" + color + "'>";
                }
                html += "</td>";
            }
            html += "</tr>";
        }
        if (this.sumarCols) {
            String temp = "";
            for (int i=0; i<this.colCount; i++) {
                if (this.dataTypes[i] == java.sql.Types.DECIMAL || this.dataTypes[i] == java.sql.Types.INTEGER) {
                    temp += "<td>" + df2.format(total[i]) + "</td>";
                }
                else {
                    temp += "<td></td>";
                }
            }
            html += "<tr>" + temp + "</tr>";
        }
        
        if (this.titulo != null) {
            html = "<tr><td colspan=" + this.colCount + "><strong>" + this.titulo + "</strong></td></tr>" + html;
        }
        
        return "<table>" + html + "</table>";
    }
}
