import java.sql.*;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

public class DatabaseORM {
   private String databaseName;
   private String username = "root";
   private String password = "";
   
   private Connection con = null;
   private Statement stmt = null;
   private Statement stmtInd = null;
   
   public DatabaseORM() {
      throw new UnsupportedOperationException();
   }
   
   public DatabaseORM(String _databaseName) {
      databaseName = _databaseName;
      
      connect();
   }
   
   public DatabaseORM(String _databaseName, String _username, String _password) {
      databaseName = _databaseName;
      username = _username;
      password = _password;
      
      connect();
   }
   
   private void connect() {
      try {
         Class.forName("com.mysql.jdbc.Driver");
         con = DriverManager.getConnection("jdbc:mysql://localhost:3306/" + databaseName + "?autoReconnect=true&useSSL=false", username, password);
         stmt = con.createStatement();
         stmtInd = con.createStatement();
      } catch(Exception e) {
         System.out.println("An error occured while conecting to database: " + e.getMessage());
      }
   }
   
   public void build() {
      try {
         ResultSet rs = stmt.executeQuery("SHOW TABLES");
         
         File file = new File(databaseName);
         
         file.mkdir();
         
         while(rs.next()) {
            buildTable(rs.getString(1));
         }
      } catch(Exception e) {
         System.out.println("Something went wrong! " + e.getMessage());
      }
   }
   
   private void buildTable(String tableName) throws Exception {
      ResultSet rs = stmtInd.executeQuery("DESCRIBE " + tableName);
      
      tableName = tableName.substring(0, 1).toUpperCase() + tableName.substring(1);
      
      String attributeName = "";
      String type = "";
      String defaultValue = "";
      
      String defaultValues = "";
      String constructorAttributes = "";
      String constructorBody = "";
      
      boolean canBeNull;
      
      Timestamp today = new Timestamp(System.currentTimeMillis());
      
      String temp = "/**\n * Name: Josip Peter Lozancic\n * Date: " + today.toString().substring(0, 19) + "\n *\n * Description: Java class representation of the \"" + tableName + "\" table from the \"" + databaseName + "\" database\n */\n\n";
      
      temp += "public class " + tableName + " {\n";
      
      while(rs.next()) {
         attributeName = rs.getString(1);
         
         attributeName = attributeName.substring(0, 1).toLowerCase() + attributeName.substring(1);
         
         defaultValue = rs.getString(5);
         
         if(rs.getString(2).contains("int")) {
            type = "int";
         } else if(rs.getString(2).contains("decimal")) {
            type = "double";
         } else {
            type = "String";
         }
      
         temp += "\tprivate ";
         
         temp += type + " " + attributeName + ";\n";
         
         constructorAttributes += type + " _" + attributeName + ", ";
         
         constructorBody += "\n\t\t" + attributeName + " = _" + attributeName + ";";
          
         if(type.equals("String")) {
            if(rs.getString(3).equals("NO")) {
               canBeNull = false;
            } else {
               canBeNull = true;
            }
            
            defaultValues += "\n\t\t" + attributeName + " = " + (defaultValue == null ? (canBeNull ? "null" : "\"\"") : "\"" + defaultValue + "\"") + ";";
         } else {
            defaultValues += "\n\t\t" + attributeName + " = " + (defaultValue == null || defaultValue.isEmpty() ? "0" : defaultValue) + ";";
         }
      }
      
      temp += "\n\tpublic " + tableName + "() {" + defaultValues + (defaultValues.isEmpty() ? "}\n" : "\n\t}\n");
      temp += "\n\tpublic " + tableName + "(" + constructorAttributes.substring(0, constructorAttributes.length() - 2) + ") {" + constructorBody + "\n\t}\n";
      
      rs.beforeFirst();
      
      String upperAttributeName = "";
      String toStringAttributes = "\n\t\tString temp = \"" + String.format("%10.10s | %20.20s |", "Type", "Name") + "\";\n";
      
      toStringAttributes += "\n\t\ttemp += \"\\n-----------------------------------\";";
      
      while(rs.next()) {
         attributeName = rs.getString(1);
         
         attributeName = attributeName.substring(0, 1).toLowerCase() + attributeName.substring(1);
         
         upperAttributeName = attributeName.substring(0, 1).toUpperCase() + attributeName.substring(1);
         
         if(rs.getString(2).contains("int")) {
            type = "int";
         } else if(rs.getString(2).contains("decimal")) {
            type = "double";
         } else {
            type = "String";
         }
         
         toStringAttributes += "\n\t\ttemp += \"\\n" + String.format("%10.10s | %20.20s |", type, attributeName) + "\";";
         
         temp += "\n\tpublic ";
         
         temp += type + " get" + upperAttributeName + "() { return " + attributeName + "; }\n";
         
         temp += "\n\tpublic void set" + upperAttributeName + "(";
         
         temp += type + " _" + attributeName + ") { " + attributeName + " = _" + attributeName + "; }\n";
      }
      
      toStringAttributes += "\n\n\t\treturn temp;";
      
      temp += "\n\tpublic String toString() {" + toStringAttributes + "\n\t}\n";
      
      temp += "}";
      
      FileWriter fileWriter = new FileWriter(databaseName + "/" + tableName + ".java");
      PrintWriter printWriter = new PrintWriter(fileWriter);
      
      printWriter.print(temp);
      
      printWriter.close();
   }
}