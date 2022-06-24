package com.example.legacy.ecomm;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import com.example.legacy.ecomm.util.DBUtil;
import com.example.legacy.ecomm.util.UsersSvc;


@Path("/ecomm")
public class ECommAppEndpoint {

	private static Properties props = new Properties();
	private static Logger logger = Logger.getLogger(ECommAppEndpoint.class.getName());
	private static String ingressSubDomain = "ecomm-portal-chatbot.{{ingress-sub-domain}}";

	static {
		try {
			ClassLoader classLoader = ECommAppEndpoint.class.getClassLoader();
			InputStream input = classLoader.getResourceAsStream("verify.config");
			props.load(input);
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Error loading Security Verify configuration.");
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		}
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response listResources(@Context UriInfo uriInfo) {
		String healthURL = (uriInfo.getAbsolutePath() + "/health").replaceAll("(?<!http:)\\/\\/", "/");
		String exampleURL = (uriInfo.getAbsolutePath() + "/v1/example").replaceAll("(?<!http:)\\/\\/", "/");
		return Response.ok("{\"health\":\"" + healthURL + "\",\"example\":\"" + exampleURL + "\"}").build();
	}

	@GET
	@Path("/home")
	@Produces({ MediaType.TEXT_HTML })
	public Response getIndex() {
		String homeHTML = null;
		try {
			homeHTML = FileUtils.readFileToString(
					new File(ECommAppEndpoint.class.getClassLoader().getResource("home.html").getFile()),
					Charset.defaultCharset());

		} catch (IOException e) {
			e.printStackTrace();
			return Response.serverError().build();
		}
		homeHTML = homeHTML.replace("{{message}}",
				"Login with the email and password specified at the time of registration.");
		return Response.ok(homeHTML).build();
	}
	
	private String buildErrorHTML() throws IOException {
		String errorHTML = "Error occurred!";
		File errorFile = new File(ECommAppEndpoint.class.getClassLoader().getResource("error.html").getPath());
		errorHTML = FileUtils.readFileToString(errorFile, StandardCharsets.UTF_8);
		return errorHTML;
	}
	
	
	@GET
	@Path("/setupdb")
	@Produces({ MediaType.TEXT_HTML })
	public Response setUpDB() {
		try {
			DBUtil.setupDB();
			return Response.ok("DB setup successfully!!").build();
		} catch (Exception e) {
			return Response.ok("DB setup failed -  "+e.getMessage()).build();
		}
	}

	@GET
	@Path("/cleandb")
	@Produces({ MediaType.TEXT_HTML })
	public Response cleanDB() {
		try {
			DBUtil.cleanDB();
			return Response.ok("DB tables deleted successfully!!").build();
		} catch (Exception e) {
			return Response.ok("DB clean failed -  "+e.getMessage()).build();
		}
	}
	
	@GET
	@Path("/oidcclient/redirect/home")
	@Produces({ MediaType.TEXT_HTML })
	public Response loginredirect(@javax.ws.rs.core.Context javax.servlet.http.HttpServletRequest request) {
		String errorHTML = "Error occurred!";

		String accessToken = " ";
		String username = " ";
		String fname = " ";
		String hostname = " ";
		try {
			String code = request.getParameter("code");
			errorHTML = buildErrorHTML();

			System.out.println("Code:" + code);

			System.out.println("Props:" + props.toString());
			
			HttpPost post = new HttpPost(props.getProperty("tokenUrl"));
			List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
			urlParameters.add(new BasicNameValuePair("client_id", props.getProperty("clientId")));
			urlParameters.add(new BasicNameValuePair("client_secret", props.getProperty("clientSecret")));
			urlParameters.add(new BasicNameValuePair("code", code));
			urlParameters.add(new BasicNameValuePair("grant_type", "authorization_code"));
			urlParameters.add(new BasicNameValuePair("redirect_uri",
					"http://"+ingressSubDomain+"/portal/ecomm/oidcclient/redirect/home"));
			post.setEntity(new UrlEncodedFormEntity(urlParameters));
			String result = "";
			try (CloseableHttpClient httpClient = HttpClients.createDefault();
					CloseableHttpResponse res = httpClient.execute(post)) {
				result = EntityUtils.toString(res.getEntity());
				JSONObject tokenObj = new JSONObject(result);
				logger.log(Level.INFO, result);

				accessToken = tokenObj.getString("access_token");
				logger.log(Level.INFO, "Tokens:" + accessToken);
			}

			HttpPost postUserInfo = new HttpPost(props.getProperty("userInfoUrl"));

			String authHeader = "Bearer " + accessToken;
			postUserInfo.setHeader(HttpHeaders.AUTHORIZATION, authHeader);

			try (CloseableHttpClient httpClient1 = HttpClients.createDefault();
					CloseableHttpResponse res = httpClient1.execute(postUserInfo)) {
				result = EntityUtils.toString(res.getEntity());
				logger.log(Level.INFO, "User info results:" + result);
				JSONObject userInfoObj = new JSONObject(result);
				username = userInfoObj.getString("preferred_username");
			}

			NewCookie cookie = new NewCookie("verify_token", accessToken, "/", ingressSubDomain,
					"Security Verify Access Token", 10000, false, true);
			String eshopHTML = FileUtils.readFileToString(
					new File(ECommAppEndpoint.class.getClassLoader().getResource("eshop.html").getPath()),
					StandardCharsets.UTF_8);
			JSONObject login = new JSONObject();
			login.put("email", username);
			JSONObject resp = DBUtil.validateLogin(login);

			if (resp == null) {
				Response.ok(buildErrorHTML()).build();
			}
			fname = resp.getString("fname");
			String custId = String.valueOf(resp.getInt("cust_id"));

			eshopHTML = eshopHTML.replace("{{message}}", "Greetings " + fname + " ! Select the items and click to buy.")
					.replace("{{custId}}", custId).replace("{{fname}}", fname);
			return Response.ok(eshopHTML).cookie(cookie).build();

		} catch (Exception e) {
			e.printStackTrace();
			logger.log(Level.SEVERE, e.getMessage());
			return Response.ok(errorHTML).status(401).build();
		}

	}

	@GET
	@Path("/register")
	@Produces({ MediaType.TEXT_HTML })
	public InputStream getRegistrationPage() {
		try {
			return this.getClass().getResourceAsStream("/register.html");
		} catch (Exception e) {
			throw new RuntimeException("Exception returning register.html", e);
		}
	}

	@POST
	@Path("registersvc")
	@Produces({ MediaType.TEXT_HTML })
	public Response registerCustomer(@javax.ws.rs.core.Context javax.servlet.http.HttpServletRequest request) {
		String homeHTML = null;
		try {
			JSONObject cust = new JSONObject();
			cust.put("fname", request.getParameter("fname"));
			cust.put("mname", request.getParameter("mname"));
			cust.put("lname", request.getParameter("lname"));
			cust.put("mobile", request.getParameter("mobile"));
			cust.put("address", request.getParameter("address"));
			cust.put("email", request.getParameter("email"));
			cust.put("password", "demo123");
			DBUtil.insertCustomerData(cust);
			UsersSvc.createUser(cust.getString("email"), cust.getString("lname"), cust.getString("fname"),
					cust.getString("email"), cust.getString("mobile"));
			homeHTML = FileUtils.readFileToString(
					new File(ECommAppEndpoint.class.getClassLoader().getResource("home.html").getFile()),
					Charset.defaultCharset());

		} catch (IOException e) {
			e.printStackTrace();
			return Response.serverError().build();
		}
		homeHTML = homeHTML.replace("{{message}}", "Registration was successful, please login");
		return Response.ok(homeHTML).build();
	}

	@POST
	@Path("/auth/addtocartsvc")
	@Produces({ MediaType.TEXT_HTML })
	public Response addToCart(@javax.ws.rs.core.Context javax.servlet.http.HttpServletRequest request,
			@CookieParam("verify_token") String token) {
		String ccHTML = null;
		try {
			logger.log(Level.INFO, "Token is :" + token);
			if (token == null)
				logger.log(Level.SEVERE, "Token not found");
			boolean isValid = checkTokenIsValid(token);
			if (!isValid) {
				return Response.ok(buildErrorHTML()).build();
			}
			String custId = request.getParameter("custId");
			String fname = request.getParameter("fname");
			String[] items = request.getParameterValues("item");
			StringBuffer orderDt = new StringBuffer();
			String address = DBUtil.getCustomerAddress(custId);
			int totalAmt = 0;
			for (int i = 0; i < items.length; i++) {
				System.out.println(items[i]);
				String[] itemArr = items[i].split("@");
				String item = itemArr[0];
				String amount = itemArr[1];
				totalAmt = totalAmt + Integer.valueOf(amount);
				orderDt.append(item + "  $" + amount);
				orderDt.append("<br/>");
			}
			orderDt.append("Total Amount:" + " $" + totalAmt);
			ccHTML = FileUtils.readFileToString(
					new File(ECommAppEndpoint.class.getClassLoader().getResource("checkout.html").getFile()),
					Charset.defaultCharset());
			ccHTML = ccHTML.replace("{{custId}}", custId).replace("{{order}}", orderDt.toString())
					.replace("{{orderdet}}", orderDt.toString()).replace("{{address}}", address)
					.replace("{{firstname}}", fname);
		} catch (IOException e) {
			e.printStackTrace();
			return Response.serverError().build();
		}
		return Response.ok(ccHTML).build();
	}

	@POST
	@Path("/auth/ordercreatesvc")
	@Produces({ MediaType.TEXT_HTML })
	public Response orderCreateSvc(@javax.ws.rs.core.Context javax.servlet.http.HttpServletRequest request,
			@CookieParam("verify_token") String token) {
		
		logger.log(Level.INFO, "Token is :" + token);
		if (token == null)
			logger.log(Level.SEVERE, "Token not found");
		boolean isValid = checkTokenIsValid(token);
		if (!isValid) {
			try {
				return Response.ok(buildErrorHTML()).build();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		String custId = request.getParameter("custId");
		String orderDt = request.getParameter("orderDt");
		
		String[] orderDtArr = orderDt.split("<br/>");
		StringBuffer itemsBuffer = new StringBuffer();
		for (int i=0; i<orderDtArr.length-1;i++) {
			itemsBuffer.append(orderDtArr[i]);
			if (i < orderDtArr.length-2)
				itemsBuffer.append(",");
		}
		String amount = orderDtArr[orderDtArr.length-1];
		amount = amount.substring(amount.indexOf("$")+1);
		String fname = request.getParameter("fname");
		String ccnum = request.getParameter("ccnum");
		ccnum = ccnum.replaceAll(" ", "-");
		String ccnumfirst12 = ccnum.substring(0,14);
		String ccnumlast4 = ccnum.substring(14);
		String ccmonth = request.getParameter("ccmonth");
		String ccyear = request.getParameter("ccyear");
		String expiry = ccmonth+"/"+ccyear;
	
		
		JSONObject orderJson =  new JSONObject();
		orderJson.put("custid", custId);
		String pattern = "yyyy-MM-dd";
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
		String orderdt = simpleDateFormat.format(new Date());
		orderJson.put("orderdt", orderdt);
		orderJson.put("expiry", expiry);
		orderJson.put("status", "Active");
		orderJson.put("fname", fname);
		orderJson.put("ccnumfirst12", ccnumfirst12);
		orderJson.put("ccnumlast4", ccnumlast4);
		orderJson.put("item", itemsBuffer.toString());
		orderJson.put("amount", amount);
		
		DBUtil.insertOrdersData(orderJson);
		String compHTML = null;
		try {
			compHTML = FileUtils.readFileToString(
					new File(ECommAppEndpoint.class.getClassLoader().getResource("completion.html").getFile()),
					Charset.defaultCharset());
			compHTML = compHTML.replace("{{custId}}", custId).replace("{{fname}}", fname);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return Response.ok(compHTML).build();
	}
	
	private boolean checkTokenIsValid(String token) {
		try {
			boolean isValidRequest = true;

			if (token == null) {
				isValidRequest = false;
			}
			if (token != null) {
				HttpPost post = new HttpPost(props.getProperty("introspectionUrl"));
				List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
				urlParameters.add(new BasicNameValuePair("client_id", props.getProperty("clientId")));
				urlParameters.add(new BasicNameValuePair("client_secret", props.getProperty("clientSecret")));
				urlParameters.add(new BasicNameValuePair("token", token));

				post.setEntity(new UrlEncodedFormEntity(urlParameters));
				String result = "";
				try (CloseableHttpClient httpClient = HttpClients.createDefault();
						CloseableHttpResponse res = httpClient.execute(post)) {
					result = EntityUtils.toString(res.getEntity());
					logger.log(Level.INFO, "Token introspection results:" + result);
					JSONObject tokenIntro = new JSONObject(result);
					if (tokenIntro.getBoolean("active") == false) {
						isValidRequest = false;
					}
				}
			}
			return isValidRequest;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	@GET
	@Path("/auth/eshop")
	@Produces({ MediaType.TEXT_HTML })
	public Response eshop(@javax.ws.rs.core.Context javax.servlet.http.HttpServletRequest request,
			@CookieParam("verify_token") String token) {
		try {
			boolean isValid = checkTokenIsValid(token);
			if (!isValid) {
				return Response.ok(buildErrorHTML()).build();
			}
			String eshopHTML = null;
			String fname = request.getParameter("fname");
			String custId = request.getParameter("custId");
			eshopHTML = FileUtils.readFileToString(
					new File(ECommAppEndpoint.class.getClassLoader().getResource("eshop.html").getFile()),
					Charset.defaultCharset());
			eshopHTML = eshopHTML.replace("{{message}}", "Greetings " + fname + " ! Select the items and click to buy.")
					.replace("{{custId}}", custId).replace("{{fname}}", fname);
			return Response.ok(eshopHTML).build();
		} catch (Exception e) {
			e.printStackTrace();
			return Response.serverError().build();
		}
	}
	
	@GET
	@Path("/auth/cancelorder")
	@Produces({MediaType.APPLICATION_JSON})
	public Response cancel(@javax.ws.rs.core.Context javax.servlet.http.HttpServletRequest request) {
		try {
			final String authorizationHeaderValue = request.getHeader("verify-token");
			String token = null;
			token = authorizationHeaderValue;
			boolean isValid = checkTokenIsValid(token);
			if (!isValid) {
				return Response.ok("Authorization Failed").build();
			}
			String orderId = request.getParameter("orderid");
		    DBUtil.cancelOrder(orderId);
		    JSONObject resp = new JSONObject();
		    resp.put("message", "Order cancelled");
			return Response.ok(resp.toString()).build();
		} catch (Exception e) {
			e.printStackTrace();
			return Response.serverError().build();
		}
	}
	
	@GET
	@Path("/auth/getorders")
	@Produces({MediaType.APPLICATION_JSON})
	public Response getAllOrders(@javax.ws.rs.core.Context javax.servlet.http.HttpServletRequest request) {
		try {
			final String authorizationHeaderValue = request.getHeader("verify-token");
			String token = null;
			token = authorizationHeaderValue;
			boolean isValid = checkTokenIsValid(token);
			if (!isValid) {
				return Response.ok("Authorization Failed").build();
			}
			String emailid = request.getParameter("emailid");
			String custId = DBUtil.getCustID(emailid);
		    JSONObject orders = DBUtil.getAllOrders(custId);
			return Response.ok(orders.toString()).build();
		} catch (Exception e) {
			e.printStackTrace();
			return Response.serverError().build();
		}
	}
	
	
	@GET
	@Path("/auth/getactiveorders")
	@Produces({MediaType.APPLICATION_JSON})
	public Response getActiveOrders(@javax.ws.rs.core.Context javax.servlet.http.HttpServletRequest request) {
		try {
			System.out.println("Get active orders start...");
			final String authorizationHeaderValue = request.getHeader("verify-token");
			String token = null;
			token = authorizationHeaderValue;
			boolean isValid = checkTokenIsValid(token);
			if (!isValid) {
				System.out.println("Token is invalid or null...");
				return Response.ok("Authorization Failed").build();
			}
			System.out.println("Token valid...");
			String emailid = request.getParameter("emailid");
			String custId = DBUtil.getCustID(emailid);
		    JSONObject orders = DBUtil.getOrders(custId,"Active");
		    System.out.println(orders.toString());
			return Response.ok(orders.toString()).build();
		} catch (Exception e) {
			e.printStackTrace();
			return Response.serverError().build();
		}
	}
}
