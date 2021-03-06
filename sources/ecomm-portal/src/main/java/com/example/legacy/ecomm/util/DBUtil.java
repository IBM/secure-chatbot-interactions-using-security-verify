package com.example.legacy.ecomm.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import com.example.legacy.ecomm.Customer;


public class DBUtil {

	private static String url = "";
	private static String user = "";
	private static String password = "";
	private static String schema = "";
	
	
	private static Properties props = new Properties();
	private static Logger logger = Logger.getLogger(DBUtil.class.getName());

	static {
		try {
			ClassLoader classLoader = DBUtil.class.getClassLoader();
			InputStream input = classLoader.getResourceAsStream("db.config");
			props.load(input);
			url = props.getProperty("jdbcurl");
			user = props.getProperty("userid");
			password = props.getProperty("password");
			schema = props.getProperty("schema");
			
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Error loading DB configuration.");
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		}
	}
	
	public static void main(String args[]) {
		setupDB();
		//cancelOrder("6");
		// getOrders("1", "Active");
	    //	System.out.println(getAllOrders("1").toString());
       // getCustID("balaji.kadambi@gmail.com");
    	//cleanDB();
	}

	public static JSONObject setupDB() {
		JSONObject respJson = new JSONObject();
		Connection con = null;
		Statement stmt;

		try {
			// Load the driver
			Class.forName("com.ibm.db2.jcc.DB2Driver");
			System.out.println("**** Loaded the JDBC driver");

			// Create the connection using the IBM Data Server Driver for JDBC and SQLJ
			con = DriverManager.getConnection(url, user, password);
			// Commit changes manually
			con.setAutoCommit(false);
			System.out.println("**** Created a JDBC connection to the data source");

			// Create the Statement
			stmt = con.createStatement();
			System.out.println("**** Created JDBC Statement object");

			List<String> statements = getStatements("legacy-tables.ddl");
			for (String statement : statements)
				stmt.executeUpdate(statement.replace("{{schema}}", schema));

			con.commit();
		} catch (Exception e) {
			e.printStackTrace();
			respJson.put("error", e.getMessage());
			return respJson;
		} finally {
			try {
				con.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		respJson.put("message", "DB setup successful.");
		return respJson;

	}

	public static JSONObject cleanDB() {
		JSONObject respJson = new JSONObject();
		Connection con = null;
		Statement stmt;

		try {
			// Load the driver
			Class.forName("com.ibm.db2.jcc.DB2Driver");
			System.out.println("**** Loaded the JDBC driver");

			// Create the connection using the IBM Data Server Driver for JDBC and SQLJ
			con = DriverManager.getConnection(url, user, password);
			// Commit changes manually
			con.setAutoCommit(false);
			System.out.println("**** Created a JDBC connection to the data source");

			// Create the Statement
			stmt = con.createStatement();
			System.out.println("**** Created JDBC Statement object");

			List<String> statements = getStatements("legacy-tables-drop.ddl");
			for (String statement : statements)
				stmt.executeUpdate(statement.replace("{{schema}}", schema));

			con.commit();
		} catch (Exception e) {
			e.printStackTrace();
			respJson.put("error", e.getMessage());
			return respJson;
		} finally {
			try {
				con.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		respJson.put("message", "DB tables deleted.");
		return respJson;

	}

	public static JSONObject insertCustomerData(JSONObject cust) {
		JSONObject respJson = new JSONObject();
		Connection con = null;
		Statement stmt;

		try {
			// Load the driver
			Class.forName("com.ibm.db2.jcc.DB2Driver");
			System.out.println("**** Loaded the JDBC driver");

			// Create the connection using the IBM Data Server Driver for JDBC and SQLJ
			con = DriverManager.getConnection(url, user, password);
			// Commit changes manually
			con.setAutoCommit(false);
			System.out.println("**** Created a JDBC connection to the data source");

			// Create the Statement
			stmt = con.createStatement();
			System.out.println("**** Created JDBC Statement object");
			List<String> statements = getStatements("customer.dml");

			for (String statement : statements) {
				statement = statement.replace("{{schema}}",schema).replace("{{fname}}", cust.getString("fname"))
						.replace("{{mname}}", cust.getString("mname")).replace("{{lname}}", cust.getString("lname"))
						.replace("{{address}}", cust.getString("address"))
						.replace("{{mobile}}", cust.getString("mobile")).replace("{{email}}", cust.getString("email"))
						.replace("{{password}}", cust.getString("password"));

				// Execute a query and generate a ResultSet instance
				System.out.println(statement);
				stmt.executeUpdate(statement);
			}

			con.commit();
		} catch (Exception e) {
			e.printStackTrace();
			respJson.put("error", e.getMessage());
			return respJson;
		} finally {
			try {
				con.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		respJson.put("message", "Customer record successfully added.");
		return respJson;
	}

	public static JSONObject insertOrdersData(JSONObject order) {
		JSONObject respJson = new JSONObject();
		Connection con = null;
		Statement stmt;

		try {
			// Load the driver
			Class.forName("com.ibm.db2.jcc.DB2Driver");
			System.out.println("**** Loaded the JDBC driver");

			// Create the connection using the IBM Data Server Driver for JDBC and SQLJ
			con = DriverManager.getConnection(url, user, password);
			// Commit changes manually
			con.setAutoCommit(false);
			System.out.println("**** Created a JDBC connection to the data source");

			// Create the Statement
			stmt = con.createStatement();
			System.out.println("**** Created JDBC Statement object");
			List<String> statements = getStatements("orders.dml");

			for (String statement : statements) {
				statement = statement.replace("{{schema}}",schema).replace("{{custid}}", order.get("custid").toString())
						.replace("{{orderdt}}", order.getString("orderdt"))
						.replace("{{status}}", order.getString("status"))
						.replace("{{item}}", order.getString("item"))
						.replace("{{amount}}", order.getString("amount"))
						.replace("{{ccnumfirst12}}", order.getString("ccnumfirst12"))
						.replace("{{expiry}}", order.getString("expiry"))
						.replace("{{ccnumlast4}}", order.getString("ccnumlast4"));
				// Execute a query and generate a ResultSet instance
				System.out.println(statement);
				stmt.executeUpdate(statement);
			}

			con.commit();
		} catch (Exception e) {
			e.printStackTrace();
			respJson.put("error", e.getMessage());
			return respJson;
		} finally {
			try {
				con.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		respJson.put("message", "Customer record successfully added.");
		return respJson;
	}

	public static List<String> getStatements(String fileName) {
		FileInputStream inputStream = null;
		ArrayList<String> statements = new ArrayList<String>();
		try {
			// Getting ClassLoader obj
			ClassLoader classLoader = DBUtil.class.getClassLoader();
			// Getting resource(File) from class loader
			File configFile = new File(classLoader.getResource(fileName).getFile());
			inputStream = new FileInputStream(configFile);
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
			String line;
			StringBuffer statementBuffer = new StringBuffer();
			while ((line = reader.readLine()) != null) {
				statementBuffer.append(line);
				if (line.contains(";")) {
					String tempStr = statementBuffer.toString();
					tempStr = tempStr.replaceAll(System.getProperty("line.separator"), " ");
					tempStr = tempStr.replaceAll(";", " ");
					statements.add(tempStr);
					statementBuffer = new StringBuffer();
				}
			}
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				inputStream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		return statements;
	}

	public static JSONObject validateLogin(JSONObject login) {
		Connection con = null;
		Statement stmt;

		try {
			// Load the driver
			Class.forName("com.ibm.db2.jcc.DB2Driver");
			System.out.println("**** Loaded the JDBC driver");

			// Create the connection using the IBM Data Server Driver for JDBC and SQLJ
			con = DriverManager.getConnection(url, user, password);
			// Commit changes manually
			con.setAutoCommit(false);
			System.out.println("**** Created a JDBC connection to the data source");

			// Create the Statement
			stmt = con.createStatement();
			System.out.println("**** Created JDBC Statement object");
			// Execute a query and generate a ResultSet instance
			String stmt1 = "SELECT * FROM "+schema+".CUSTOMER WHERE E_MAIL=" +"'"+login.getString("email") +"'";
			System.out.println(stmt1);
			ResultSet rs = stmt.executeQuery(stmt1);
			System.out.println("**** Created JDBC ResultSet object");
            
			// Print all of the employee numbers to standard output device
			while (rs.next()) {
	           String fname = rs.getString("FIRST_NAME");
	           int custId = rs.getInt("CUST_ID");
	           JSONObject resp = new JSONObject();
	           resp.put("fname", fname);
	           resp.put("cust_id", custId);
	           return resp;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return null;
	}
	
	public static JSONObject getOrders(String custId, String status) {
		Connection con = null;
		Statement stmt;
		JSONObject orders = new JSONObject();
		try {
			// Load the driver
			Class.forName("com.ibm.db2.jcc.DB2Driver");
			System.out.println("**** Loaded the JDBC driver");

			// Create the connection using the IBM Data Server Driver for JDBC and SQLJ
			con = DriverManager.getConnection(url, user, password);
			// Commit changes manually
			con.setAutoCommit(false);
			System.out.println("**** Created a JDBC connection to the data source");

			// Create the Statement
			stmt = con.createStatement();
			System.out.println("**** Created JDBC Statement object");
			// Execute a query and generate a ResultSet instance
			String stmt1 = "SELECT * FROM "+schema+".ORDERS WHERE CUST_ID=" +custId + " AND STATUS='"+status+"'";
			System.out.println(stmt1);
			ResultSet rs = stmt.executeQuery(stmt1);
			System.out.println("**** Created JDBC ResultSet object");
           
            JSONArray arr = new JSONArray();
            orders.put("Orders", arr);
			// Print all of the employee numbers to standard output device
			while (rs.next()) {
			   JSONObject resp = new JSONObject();
	           resp.put("orderId", rs.getInt("ORDER_ID"));
	           resp.put("orderDt", rs.getString("ORDER_DT"));
	           resp.put("status", rs.getString("status"));
	           resp.put("item", rs.getString("ITEM"));
	           resp.put("amount", rs.getInt("AMOUNT"));
	           resp.put("creditCard", rs.getString("CCNUM_FIRST12_DIGITS")+ rs.getString("CCNUM_LAST4_DIGITS"));
	           resp.put("ccExpiry", rs.getString("EXPIRY"));
	          // System.out.println(resp.toString());
	           arr.put(resp);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return orders;
		}
		return orders;
	}
	
	public static JSONObject getAllOrders(String custId) {
		Connection con = null;
		Statement stmt;
		JSONObject orders = new JSONObject();
		try {
			// Load the driver
			Class.forName("com.ibm.db2.jcc.DB2Driver");
			System.out.println("**** Loaded the JDBC driver");

			// Create the connection using the IBM Data Server Driver for JDBC and SQLJ
			con = DriverManager.getConnection(url, user, password);
			// Commit changes manually
			con.setAutoCommit(false);
			System.out.println("**** Created a JDBC connection to the data source");

			// Create the Statement
			stmt = con.createStatement();
			System.out.println("**** Created JDBC Statement object");
			// Execute a query and generate a ResultSet instance
			String stmt1 = "SELECT * FROM "+schema+".ORDERS WHERE CUST_ID=" +custId;
			System.out.println(stmt1);
			ResultSet rs = stmt.executeQuery(stmt1);
			System.out.println("**** Created JDBC ResultSet object");
           
            JSONArray arr = new JSONArray();
            orders.put("Orders", arr);
			// Print all of the employee numbers to standard output device
			while (rs.next()) {
			   JSONObject resp = new JSONObject();
	           resp.put("orderId", rs.getInt("ORDER_ID"));
	           resp.put("orderDt", rs.getString("ORDER_DT"));
	           resp.put("status", rs.getString("status"));
	           resp.put("item", rs.getString("ITEM"));
	           resp.put("amount", rs.getInt("AMOUNT"));
	           resp.put("creditCard", rs.getString("CCNUM_FIRST12_DIGITS")+ rs.getString("CCNUM_LAST4_DIGITS"));
	           resp.put("ccExpiry", rs.getString("EXPIRY"));
	         //  System.out.println(resp.toString());
	           arr.put(resp);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return orders;
		}
		return orders;
	}
	

	public static String getCustID(String emailId) {
		Connection con = null;
		Statement stmt;

		try {
			// Load the driver
			Class.forName("com.ibm.db2.jcc.DB2Driver");
			System.out.println("**** Loaded the JDBC driver");

			// Create the connection using the IBM Data Server Driver for JDBC and SQLJ
			con = DriverManager.getConnection(url, user, password);
			// Commit changes manually
			con.setAutoCommit(false);
			System.out.println("**** Created a JDBC connection to the data source");

			// Create the Statement
			stmt = con.createStatement();
			System.out.println("**** Created JDBC Statement object");
			// Execute a query and generate a ResultSet instance
			String stmt1 = "SELECT * FROM "+schema+".CUSTOMER WHERE E_MAIL=" +"'"+emailId +"'";
			System.out.println(stmt1);
			ResultSet rs = stmt.executeQuery(stmt1);
			System.out.println("**** Created JDBC ResultSet object");
            
			// Print all of the employee numbers to standard output device
			while (rs.next()) {
	           String custId = rs.getString("CUST_ID");
	           System.out.println("Cust id : "+custId);
	           return custId;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return null;

	}

	public static String getCustomerAddress(String custId) {
		Connection con = null;
		Statement stmt;

		try {
			// Load the driver
			Class.forName("com.ibm.db2.jcc.DB2Driver");
			System.out.println("**** Loaded the JDBC driver");

			// Create the connection using the IBM Data Server Driver for JDBC and SQLJ
			con = DriverManager.getConnection(url, user, password);
			// Commit changes manually
			con.setAutoCommit(false);
			System.out.println("**** Created a JDBC connection to the data source");

			// Create the Statement
			stmt = con.createStatement();
			System.out.println("**** Created JDBC Statement object");
			// Execute a query and generate a ResultSet instance
			String stmt1 = "SELECT * FROM "+schema+".CUSTOMER WHERE CUST_ID=" +"'"+custId +"'";
			System.out.println(stmt1);
			ResultSet rs = stmt.executeQuery(stmt1);
			System.out.println("**** Created JDBC ResultSet object");
            
			// Print all of the employee numbers to standard output device
			while (rs.next()) {
	           String address = rs.getString("ADDRESS");
	           return address;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return null;

	}
	
	public static int cancelOrder(String orderId) {
		Connection con = null;
		Statement stmt;

		try {
			// Load the driver
			Class.forName("com.ibm.db2.jcc.DB2Driver");
			System.out.println("**** Loaded the JDBC driver");

			// Create the connection using the IBM Data Server Driver for JDBC and SQLJ
			con = DriverManager.getConnection(url, user, password);
			// Commit changes manually
			con.setAutoCommit(false);
			System.out.println("**** Created a JDBC connection to the data source");

			// Create the Statement
			stmt = con.createStatement();
			System.out.println("**** Created JDBC Statement object");
			// Execute a query and generate a ResultSet instance
			String stmt1 = "UPDATE "+schema+".ORDERS SET STATUS="+"'Cancelled'" +" WHERE ORDER_ID=" +orderId;
			System.out.println(stmt1);
			int rs = stmt.executeUpdate(stmt1);
			System.out.println("**** Created JDBC ResultSet object");
			con.commit();
			return rs;
            
		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		}
	}

}
