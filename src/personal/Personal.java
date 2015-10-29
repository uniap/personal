package personal;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Scanner;

public class Personal {
    
    public static final String HDR_COLOR = "#BCA9F5";
    
    static Esquema esquema;
    
    public static void main(String[] args) throws IOException, ClassNotFoundException, SQLException {
        
        Class.forName("org.h2.Driver");
        Connection conn;
        try {            
            conn = DriverManager.getConnection("jdbc:h2:./testDB;IFEXISTS=TRUE", "sa", "");
            System.out.println("Conectado a la base de datos.");
        }
        catch(SQLException e) {
            // LA BASE NO EXISTE, ES LA PRIMERA EJECUCION. DEBE CREARSE Y EJECUTAR EL DDL.
            System.out.println("Primera ejecucion, se genera el modelo de datos...");
        }
        
        conn = DriverManager.getConnection("jdbc:h2:./testDB", "sa", "");
        ScriptRunner sr = new ScriptRunner(conn, true, true);
        try (FileReader fr = new FileReader("modelo/modelo_personal.sql")) {
            sr.runScript(fr);
        }
        /* try (FileReader fr = new FileReader("modelo/post_modelo_personal.sql")) {
            sr.runScript(fr);
        }   */         
        
        conn.setAutoCommit(false);
        esquema = new Esquema("modelo/comandos.txt", conn);                
        
        gui jf = new gui(esquema);     
        jf.setVisible(true);
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        jf.setLocation(dim.width/2-jf.getSize().width/2, dim.height/2-jf.getSize().height/2);
    }
        
    public static String readCommandLine() {
        System.out.print("< ");
        Scanner in = new Scanner(System.in);
        return in.nextLine();
    }

}

