import java.io.*;
import java.net.*;
import java.util.*;
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

    public static void main(String[] args) throws IOException {

    	createUtterancesJSON();
        LuisClient luis = null;

        try {
            luis = new LuisClient(LUIS_BASE, LUIS_APP_ID,
                    LUIS_APP_VERSION,LUIS_AUTHORING_ID);
        } catch (Exception ex) {
            printExceptionMsg(ex);
            System.exit(0);
        }

        try {

            System.out.println("Adding utterance(s).");
            luis.addUtterances(UTTERANCE_FILE)
                    .raiseForStatus();

            System.out.println("Requesting training.");
            luis.train()
                    .raiseForStatus();

            System.out.println("Requested training. Requesting training status.");
            luis.status()
                    .raiseForStatus();

        } catch (Exception ex) {
            printExceptionMsg(ex);
        }
    }
    
    public static void createUtterancesJSON() throws IOException{
    	File file = new File("C:\\Users\\Roshan Shibu\\Desktop\\RIO_log.txt"); 
    	BufferedReader br = new BufferedReader(new FileReader(file)); 
    	String st;
    	String[] intents = {"None","change_address","leave_application","password_change_request"};
    	Scanner s = new Scanner(System.in);
    	int choice =-1;
    	String jsonfile= "";
    	int flag=1;
    	String focus ="";
    	while ((st = br.readLine()) != null) {
    		focus = st.substring(st.indexOf(">")+1);
    		System.out.println(focus);
    		System.out.println("Input appropriate intent\n");
    		for(int i=0;i<intents.length;i++)
    			System.out.print(i+") "+intents[i]+" ");
    		choice = s.nextInt();
    		if(flag==1)
    			jsonfile+="[";
    		else
    			jsonfile+=",";
    		jsonfile+="{ \"text\": \""+focus+"\", \"intentName\": \""+intents[choice]+"\",\"entityLabels\": []}";
    		flag++;
    	}
    	jsonfile+="]";
    	
    	File filex = new File("C:\\Users\\Roshan Shibu\\Desktop\\Utterances.json");
    	FileWriter fr = new FileWriter(filex, true);
    	fr.write(jsonfile);
    	fr.close();
    	s.close();
    	br.close();
    }

}
