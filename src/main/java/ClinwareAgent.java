import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.*;
import com.google.cloud.vertexai.generativeai.ChatSession;
import com.google.cloud.vertexai.generativeai.ContentMaker;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.ResponseHandler;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import io.github.cdimascio.dotenv.Dotenv;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class ClinwareAgent {

    private static ChatSession chatSession;
    private static VertexAI vertexAi;

    public static void main(String[] args) {
        // 1. Load Config
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        String projectId = dotenv.get("GOOGLE_PROJECT_ID"); // REQUIRED!
        String location = "us-central1"; // Standard Google Cloud region

        if (projectId == null) {
            System.err.println("CRITICAL ERROR: GOOGLE_PROJECT_ID is missing in .env file.");
            return;
        }

        // 2. Initialize Vertex AI & Define Tool
        try {
            vertexAi = new VertexAI(projectId, location);

            // Manual Schema Definition (Required by Official SDK)
            FunctionDeclaration searchFunc = FunctionDeclaration.newBuilder()
                    .setName("search_news")
                    .setDescription("Searches the internet for news about Clinware, funding, or competitors.")
                    .setParameters(
                            Schema.newBuilder()
                                    .setType(Type.OBJECT)
                                    .putProperties("query", Schema.newBuilder().setType(Type.STRING).build())
                                    .addRequired("query")
                                    .build()
                    )
                    .build();

            Tool tool = Tool.newBuilder()
                    .addFunctionDeclarations(searchFunc)
                    .build();

            // 3. Build Model (Gemini Pro)
            GenerativeModel model = new GenerativeModel("gemini-1.5-flash-001", vertexAi);
            model.setTools(Arrays.asList(tool));
            model.setSystemInstruction(ContentMaker.fromMultiModalData(
                    "You are the Clinware Intelligence Agent. Use 'search_news' for questions about Clinware. " +
                    "If the tool returns no data, admit it. Do not hallucinate."
            ));

            // 4. Start Session
            chatSession = model.startChat();

            // 5. Start Web Server
            int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7000"));
            Javalin app = Javalin.create(config -> config.staticFiles.add("/public", Location.CLASSPATH));

            app.post("/chat", ctx -> {
                String userMsg = ctx.body();
                System.out.println("User: " + userMsg);
                try {
                    String response = handleChatTurn(userMsg);
                    ctx.result(response);
                } catch (Exception e) {
                    e.printStackTrace();
                    ctx.result("Error: " + e.getMessage());
                }
            });

            app.start(port);
            System.out.println("--- GOOGLE VERTEX AI AGENT READY ---");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // This loop handles the "Tool Calling" logic manually
    private static String handleChatTurn(String userMsg) throws IOException {
        // 1. Send User Message
        GenerateContentResponse response = chatSession.sendMessage(userMsg);
        
        // 2. Check if Model wants to call a tool
        com.google.cloud.vertexai.api.Content content = response.getCandidates(0).getContent();
        
        for (Part part : content.getPartsList()) {
            if (part.hasFunctionCall()) {
                FunctionCall call = part.getFunctionCall();
                
                if (call.getName().equals("search_news")) {
                    // A. Extract arguments (Query)
                    String query = call.getArgs().getFieldsMap().get("query").getStringValue();
                    System.out.println("DEBUG: Model calling search_news with: " + query);
                    
                    // B. Execute Tool (Using your existing McpClient)
                    String toolResult = new McpClient().searchNews(query);
                    
                    // C. Send Result Back to Model
                    // We must format the result as a Protobuf Struct
                    Struct resultStruct = Struct.newBuilder()
                            .putFields("content", Value.newBuilder().setStringValue(toolResult).build())
                            .build();

                    Content toolResponse = ContentMaker.fromMultiModalData(
                            Part.newBuilder()
                                    .setFunctionResponse(
                                            FunctionResponse.newBuilder()
                                                    .setName("search_news")
                                                    .setResponse(resultStruct)
                                                    .build()
                                    )
                                    .build()
                    );
                    
                    // D. Get Final Answer
                    GenerateContentResponse finalResp = chatSession.sendMessage(toolResponse);
                    return ResponseHandler.getText(finalResp);
                }
            }
        }
        
        // 3. No tool needed, just return text
        return ResponseHandler.getText(response);
    }
}