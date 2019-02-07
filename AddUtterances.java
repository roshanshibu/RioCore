import java.io.*;
import java.net.*;
import java.nio.channels.FileChannel;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.*;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.google.gson.*;

public class AddUtterances {

    static final String LUIS_APP_ID      = "15fb095e-dbfc-4b33-b435-e2d9c48f9ac9";
    static final String LUIS_APP_VERSION = "0.1";
    static final String LUIS_AUTHORING_ID  = "58272a2c795749369702922068c382f9";

    static final String LUIS_BASE        = "https://westus.api.cognitive.microsoft.com";

    static final String UTTERANCE_FILE   = "C:\\Users\\Roshan Shibu\\Desktop\\Utterances.json";
    static final String UTF8 = "UTF-8";

    static class LuisClient{

        private final String PATH = "/luis/api/v2.0/apps/{app_id}/versions/{app_version}";
        private final String TRAIN    = "/train";
        private final String EXAMPLES = "/examples";
        private final String APP_INFO = "/";
        private final String GET  = "GET";
        private final String POST = "POST";
        private final String NO_DATA = null;
        private final String key;
        private final String host;
        private final String path;

        LuisClient(String host, String app_id, String app_version, String key) throws Exception {
            this.path = PATH.replace("{app_id}", app_id).replace("{app_version}", app_version);
            this.host = host;
            this.key  = key;
            this.get(APP_INFO).raiseForStatus();
        }

        private LuisResponse call(String endpoint, String method, byte[] data) throws Exception {

        	URL url = new URL(this.host + this.path + endpoint);
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod(method);
            conn.setRequestProperty("Ocp-Apim-Subscription-Key", key);
            if (method.equals(POST)) {
                if (data == null)
                        data = new byte[]{};    
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Content-Length", Integer.toString(data.length));
                try (OutputStream ostream = conn.getOutputStream()) {
                    ostream.write(data, 0, data.length);
                }
            }

            InputStream stream;
            try {
                stream = conn.getInputStream();
            } catch (IOException ex) {
                stream = conn.getErrorStream();
            }
            String body = new Scanner(stream, UTF8).useDelimiter("\\A").next();

            return new LuisResponse(body, conn.getResponseCode(), conn.getResponseMessage());

        }

        private LuisResponse call(String endpoint, String method, String data) throws Exception {
            byte[] bytes = null;
            if (data != null)
                bytes = data.getBytes(UTF8);
            return call(endpoint, method, bytes);
        }

        private LuisResponse call(String endpoint, String method, InputStream stream) throws Exception {
            String data = new Scanner(stream, UTF8).useDelimiter("\\A").next();
            return call(endpoint, method, data);
        }

        private LuisResponse get(String endpoint) throws Exception {
            return call(endpoint, GET, NO_DATA);
        }

        private LuisResponse post(String endpoint, InputStream data) throws Exception {
            return call(endpoint, POST, data);
        }

        private LuisResponse post(String endpoint) throws Exception {
            return call(endpoint, POST, NO_DATA);
        }

        public LuisResponse addUtterances(String filename) throws Exception {
            try (FileInputStream stream = new FileInputStream(filename)) {
                return post(EXAMPLES, stream);
            }
        }

        public LuisResponse train() throws Exception {
            return post(TRAIN);
        }

        public LuisResponse status() throws Exception {
            return get(TRAIN);
        }

    }

    static class LuisResponse {

        private final String    body;
        private final int       status;
        private final String    reason;
        private JsonElement     data;

        LuisResponse(String body, int status, String reason) {
            JsonParser parser = new JsonParser();
            try {
                this.data = parser.parse(body);
            }
            catch (JsonSyntaxException ex) {
                this.data = parser.parse("{ \"message\": \"Invalid JSON response\" }");
            }
            this.body   = new GsonBuilder().setPrettyPrinting().create().toJson(data);
            this.status = status;
            this.reason = reason;
            System.out.println(this.body);
        }

        LuisResponse raiseForStatus() throws StatusException {
            if (this.status < 200 || this.status > 299) {
                throw new StatusException(this);
            }
            return this;
        }
    }

    static class StatusException extends Exception {

        private String details = "";
        private final int status;

        StatusException(LuisResponse response) {
            super(String.format("%d %s", response.status, response.reason));
            JsonObject jsonInfo = (JsonObject)response.data;
            if (jsonInfo.has("error"))
                jsonInfo = (JsonObject)jsonInfo.get("error");
            if (jsonInfo.has("message"))
                this.details = jsonInfo.get("message").getAsString();
            this.status = response.status;
        }

        String getDetails() {
            return this.details;
        }

        int getStatus() {
            return this.status;
        }

    }

    static void printExceptionMsg(Exception ex) {
        System.out.println(String.format("%s: %s",
                ex.getClass().getSimpleName(), ex.getMessage()));

        StackTraceElement caller = ex.getStackTrace()[1];
        System.out.println(String.format("    in %s (line %d?)",
                caller.getFileName(), caller.getLineNumber()));
        if (ex instanceof StatusException)
           System.out.println(((StatusException)ex).getDetails());
    }

    public static void main(String[] args) throws IOException, InterruptedException {
    	while (true){
    		Thread.sleep(2000);
	    	int changes = createUtterancesJSON();
	    	if(changes==0)
	    		continue;
	        LuisClient luis = null;
	        try {
		    	luis = new LuisClient(LUIS_BASE, LUIS_APP_ID, LUIS_APP_VERSION, LUIS_AUTHORING_ID);
	        }
	        catch (Exception ex) {
		        printExceptionMsg(ex);
		        System.exit(0);
		    }
	        try {
	            System.out.println("Adding utterance(s).");
	            luis.addUtterances(UTTERANCE_FILE).raiseForStatus();
	            System.out.println("Requesting training.");
	            luis.train().raiseForStatus();
	            System.out.println("Requested training. Requesting training status.");
	            luis.status().raiseForStatus();
	        } catch (Exception ex) {
	            printExceptionMsg(ex);
	        }
	        //clear RIO_log.txt and Utterances.json
	        clearfiles("C:\\Users\\Roshan Shibu\\Desktop\\RIO_log.txt");
	        clearfiles("C:\\Users\\Roshan Shibu\\Desktop\\Utterances.json");
	        publish();
    	}
    }
    
    public static int createUtterancesJSON() throws IOException{
    	int changelog=0;
    	try{
			Class.forName(com.mysql.jdbc.Driver.class.getName());
			Connection conn = null;
			conn = DriverManager.getConnection("jdbc:mysql://localhost/rio","root", "");
			//System.out.print("Database is connected !");
			java.sql.Statement stmt = null;
			//System.out.println("Inserting records into the table...");
		    stmt = conn.createStatement();
		    String sql = "SELECT * FROM train";
		    java.sql.ResultSet rs = stmt.executeQuery(sql);
		    String jsonfile= "";
		    int flag=1;
		    while (rs.next()) {
		    		String train_utterance = rs.getString("train_utterance");
		    		String train_intent = rs.getString("train_intent");
		    		int Done = rs.getInt("Done");
		    		if(Done==1)
		    			continue;
		    		else{
		    			changelog++;
		    			java.sql.PreparedStatement updateEXP = conn.prepareStatement("update`train` set `Done` = 1  where `train_utterance` = '"+train_utterance+"'");
		    			int updateEXP_done = updateEXP.executeUpdate();
		    		}
		    			
		    		System.out.println(train_utterance+" "+train_intent+" "+Done);
		    		if(flag==1)
		    			jsonfile+="[";
		    		else
		    			jsonfile+=",";
		    		jsonfile+="{ \"text\": \""+train_utterance+"\", \"intentName\": \""+train_intent+"\",\"entityLabels\": []}";
		    		flag++;
		    }
		    if(changelog>0)
		    	jsonfile+="]";
		    conn.close();
		    File filex = new File("C:\\Users\\Roshan Shibu\\Desktop\\Utterances.json");
			FileWriter fr = new FileWriter(filex, true);
			fr.write(jsonfile);
			fr.close();
			if(changelog>0)
				System.out.println("Utterances.json has been prepared");
			else
				System.out.println("No Changes detected. Utterances.json NOT prepared");
			
		}
		catch(Exception ex){
			System.out.print("Do not connect to DB - Error:"+ex);
		} 
    	return changelog;
    }
    
    public static void clearfiles(String fileloc) throws IOException{
    	FileWriter fwOb = new FileWriter(fileloc, false); 
        PrintWriter pwOb = new PrintWriter(fwOb, false);
        pwOb.flush();
        pwOb.close();
        fwOb.close();
    }
    
    public static void publish(){
    	HttpClient httpclient = HttpClients.createDefault();

        try
        {
            URIBuilder builder = new URIBuilder("https://westus.api.cognitive.microsoft.com/luis/api/v2.0/apps/15fb095e-dbfc-4b33-b435-e2d9c48f9ac9/publish");


            URI uri = builder.build();
            HttpPost request = new HttpPost(uri);
            request.setHeader("Content-Type", "application/json");
            request.setHeader("Ocp-Apim-Subscription-Key", "58272a2c795749369702922068c382f9");


            // Request body
            StringEntity reqEntity = new StringEntity("{\"versionId\": \"0.1\",\"isStaging\": false,\"region\": \"westus\"}");
            request.setEntity(reqEntity);

            HttpResponse response = httpclient.execute(request);
            HttpEntity entity = response.getEntity();

            if (entity != null) 
            {
                System.out.println(EntityUtils.toString(entity));
            }
        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
        }
    	
    }

}
    
