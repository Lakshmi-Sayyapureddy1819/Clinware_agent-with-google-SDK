import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.langchain4j.agent.tool.Tool;
import java.io.*;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.Map;

public class McpClient {
    private static final ObjectMapper mapper = new ObjectMapper();

    @Tool("Searches the internet for news about Clinware, funding, or competitors.")

    public String searchNews(String query) {
        System.out.println("DEBUG: Agent calling Tavily with query: " + query);

        try {
            // 1. Point to the NEW Node script
            File script = new File("tavily-server.js");
            if (!script.exists())
                return "Error: tavily-server.js missing.";

            ProcessBuilder pb = new ProcessBuilder("node", "tavily-server.js");

            // 2. PASS TAVILY KEY TO NODE PROCESS (supports local .env and Docker env vars)
            Map<String, String> env = pb.environment();
            Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
            String tavilyKey = dotenv.get("TAVILY_API_KEY");
            if (tavilyKey == null || tavilyKey.isEmpty()) {
                tavilyKey = System.getenv("TAVILY_API_KEY");
            }
            if (tavilyKey != null && !tavilyKey.isEmpty()) {
                env.put("TAVILY_API_KEY", tavilyKey);
            }

            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
            Process process = pb.start();

            // 3. Send Request
            ObjectNode jsonRpc = mapper.createObjectNode();
            jsonRpc.put("jsonrpc", "2.0");
            jsonRpc.put("id", 1);
            jsonRpc.put("method", "tools/call");

            ObjectNode params = jsonRpc.putObject("params");
            params.put("name", "search_news");
            params.putObject("arguments").put("query", query);

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
            writer.write(jsonRpc.toString());
            writer.newLine();
            writer.flush();

            // 4. Read Response
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            process.destroy();

            if (line == null)
                return "Error: No response from Tavily.";

            JsonNode response = mapper.readTree(line);
            if (response.has("result") && response.get("result").has("content")) {
                return response.get("result").get("content").get(0).get("text").asText();
            }
            return "No news found.";

        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }
}