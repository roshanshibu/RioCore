import java.util.Properties;
import javax.mail.BodyPart;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Date;

public class mail {

	private static String USER_NAME = "riogroup6";
	private static String PASSWORD = "rioabc123";

	public static void main(String[] args) {
		String host = "pop.gmail.com";// change accordingly
		String mailStoreType = "pop3";
		String username = "riogroup6@gmail.com";// change accordingly
		String password = "rioabc123";// change accordingly
		check(host, mailStoreType, username, password);
	}


	public static void check(String host, String storeType, String user, String password) {
		try {
			while (true) {
				Thread.sleep(500);
				System.out.println("Listening for new emails -----------------\n");
				Properties properties = new Properties();
				properties.put("mail.pop3.host", host);
				properties.put("mail.pop3.port", "995");
				properties.put("mail.pop3.starttls.enable", "true");

				Session emailSession = Session.getInstance(properties);
				Store store = emailSession.getStore("pop3s");
				store.connect(host, user, password);

				Folder emailFolder = store.getFolder("Inbox");
				emailFolder.open(Folder.READ_ONLY);
				Message[] messages = emailFolder.getMessages();

				if (messages.length > 0)
					System.out.println("messages.length---" + messages.length);

				for (int i = 0, n = messages.length; i < n; i++) {
					Message message = messages[i];
					Object obj = message.getContent();
					Multipart mp = (Multipart)obj;
					BodyPart bp = mp.getBodyPart(0);

					System.out.println("---------------------------------");
					System.out.println("Email Number " + (i + 1));
					System.out.println("\nSubject: " + message.getSubject());
					System.out.println("From: " + message.getFrom()[0]);
					//System.out.println("To: " + message.getAllRecipients().toString());
					//System.out.println("Received Date:" + message.getReceivedDate());
					System.out.println("Text: " + bp.getContent().toString());
					String MAILTEXT = bp.getContent().toString();
					String TRUEMAILTEXT = MAILTEXT;
					MAILTEXT=MAILTEXT.replace("\n", "").replace("\r", "");
					String SUBJECT = message.getSubject();
					
					String TOSEND=MAILTEXT;
					if(MAILTEXT.equals("") || MAILTEXT.length()<SUBJECT.length())
						TOSEND=SUBJECT+" "+MAILTEXT;
					
					if(message.getSubject().equals(""))
						SUBJECT = "(no subject)"; 
					
					//send the body to NLP engine
					HttpClient httpclient = HttpClients.createDefault();

					try {

						String AppId = "15fb095e-dbfc-4b33-b435-e2d9c48f9ac9";
						String EndpointKey = "58272a2c795749369702922068c382f9";
						URIBuilder endpointURLbuilder = new URIBuilder("https://westus.api.cognitive.microsoft.com/luis/v2.0/apps/" + AppId + "?");
						endpointURLbuilder.setParameter("q", TOSEND);
						URI endpointURL = endpointURLbuilder.build();
						HttpGet request = new HttpGet(endpointURL);
						request.setHeader("Ocp-Apim-Subscription-Key", EndpointKey);
						HttpResponse response = httpclient.execute(request);
						HttpEntity entity = response.getEntity();
						if (entity != null) {
							JSONObject json = new JSONObject(EntityUtils.toString(entity));
							//System.out.println(json);
							
							//String topIntentJSON = json.get("topScoringIntent").toString();
							String intent = json.getJSONObject("topScoringIntent").getString("intent");
							String intentscore = json.getJSONObject("topScoringIntent").getString("score");
							
							//String SentimentAnalysisJSON = json.get("sentimentAnalysis").toString();
							String sentiment = json.getJSONObject("sentimentAnalysis").getString("label");
							String sentimentscore = json.getJSONObject("sentimentAnalysis").getString("score");
							
							String entityx="null";
							JSONArray array = json.getJSONArray("entities");
							if (array.length()!=0){
									entityx = array.getJSONObject(0).getString("entity");
									//System.out.println("The address is changed to "+entityx);
							}
							System.out.println("The mail was matched to intent ->" + intent);
							System.out.println("                with a score of->" + intentscore);
							System.out.println("\n");
							System.out.println("The mail sentiment was found to be ->" + sentiment);
							System.out.println("                    with a score of->" + sentimentscore);
							System.out.println("\n\nReplying to sender now ...");
							
							EntityUtils.consume(entity);
							request.releaseConnection();
							request.abort();

							String from = USER_NAME;
							String pass = PASSWORD;
							String RECIPIENT = message.getFrom()[0].toString().substring(message.getFrom()[0].toString().indexOf("<") + 1, message.getFrom()[0].toString().indexOf(">"));
							//System.out.println("recepient == " + RECIPIENT);
							String[] to = { RECIPIENT };
							String subject = "Your mail has been processed";
							String body = "";
							
							if (intent.equals("None")) {
								body = "Hello, " + message.getFrom()[0].toString().substring(0, message.getFrom()[0].toString().indexOf(" <")) + ",\nUnfortunately, our systems have not been able to interpret the contents of your mail. A customer care executive will contact you soon.\n\nTeam RIO";
								DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
								Date date = new Date();
								String log_entry = "<" + dateFormat.format(date) + ">" + MAILTEXT;
								File file = new File("C:\\Users\\Roshan Shibu\\Desktop\\RIO_log.txt");
								FileWriter fr = new FileWriter(file, true);
								fr.write(log_entry);
								fr.close();
							} 
							else if(intent.equals("change_address")){
								body = "Hello, " + message.getFrom()[0].toString().substring(0, message.getFrom()[0].toString().indexOf(" <")) + ",\nOur automated systems have been able to interpret your request for a change in address.\nYour address has been changed to "+entityx+".\n\nHappy to be of service.\nTeam RIO";	
							}
							else
								body = "Hello, " + message.getFrom()[0].toString().substring(0, message.getFrom()[0].toString().indexOf(" <")) + ",\nOur automated systems have understood your mail regarding " + intent + ".We have initiated appropriate actions.\n\n Team RIO";

							addentry(RECIPIENT,SUBJECT,TRUEMAILTEXT,intent,entityx);
							sendFromGMail(from, pass, to, subject, body);
						}
					}

					catch (Exception e) {
						System.out.println(e.getMessage());
					}

				}
				emailFolder.close(false);
				store.close();
				properties.clear();
			}//end of while true loop

		} catch (NoSuchProviderException e) {
			e.printStackTrace();
		} catch (MessagingException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	private static String readAll(Reader rd) throws IOException {
		StringBuilder sb = new StringBuilder();
		int cp;
		while ((cp = rd.read()) != -1) {
			sb.append((char) cp);
		}
		return sb.toString();
	}

	public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
		InputStream is = new URL(url).openStream();
		try {
			BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
			String jsonText = readAll(rd);
			JSONObject json = new JSONObject(jsonText);
			return json;
		} finally {
			is.close();
		}
	}

	private static void sendFromGMail(String from, String pass, String[] to, String subject, String body) {
		Properties props = System.getProperties();
		String host = "smtp.gmail.com";
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", host);
		props.put("mail.smtp.user", from);
		props.put("mail.smtp.password", pass);
		props.put("mail.smtp.port", "587");
		props.put("mail.smtp.auth", "true");
		Session session = Session.getInstance(props);
		MimeMessage message = new MimeMessage(session);

		try {
			message.setFrom(new InternetAddress(from));
			InternetAddress[] toAddress = new InternetAddress[to.length];

			// To get the array of addresses
			for ( int i = 0; i < to.length; i++ ) {
				toAddress[i] = new InternetAddress(to[i]);
			}

			for ( int i = 0; i < toAddress.length; i++) {
				message.addRecipient(Message.RecipientType.TO, toAddress[i]);
			}

			message.setSubject(subject);
			message.setText(body);
			Transport transport = session.getTransport("smtp");
			transport.connect(host, from, pass);
			transport.sendMessage(message, message.getAllRecipients());
			transport.close();
		} catch (AddressException ae) {
			ae.printStackTrace();
		} catch (MessagingException me) {
			me.printStackTrace();
		}
	}
	
	public static void addentry(String a,String b,String c,String d,String e){
		try{
			Class.forName(com.mysql.jdbc.Driver.class.getName());
			Connection conn = null;
			conn = DriverManager.getConnection("jdbc:mysql://localhost/rio","root", "");
			//System.out.print("Database is connected !");
			
			Statement stmt = null;
			//System.out.println("Inserting records into the table...");
		      stmt = conn.createStatement();
		      
		      Date date = new Date();
		      long timeMilli = date.getTime();
		      
		      String sql = "INSERT INTO data " +
		    		  "VALUES (\""+a+"\", \""+b+"\", \""+c+"\", \""+d+"\", \""+e+"\", \""+timeMilli+"\")";
		      stmt.executeUpdate(sql);
		      conn.close();
			}
			catch(Exception ex)
			{
			System.out.print("Do not connect to DB - Error:"+e);
			}
		
	}

}
