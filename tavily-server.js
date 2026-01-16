const readline = require('readline');

// GET KEY FROM ENVIRONMENT VARIABLE
const TAVILY_API_KEY = process.env.TAVILY_API_KEY; 

if (!TAVILY_API_KEY) {
    console.error("ERROR: TAVILY_API_KEY environment variable is missing.");
    process.exit(1);
}

const rl = readline.createInterface({
  input: process.stdin,
  output: process.stdout,
  terminal: false
});

rl.on('line', async (line) => {
  try {
    const request = JSON.parse(line);
    
    // 1. Handshake
    if (request.method === 'initialize') {
      console.log(JSON.stringify({
        jsonrpc: "2.0",
        id: request.id,
        result: {
          protocolVersion: "2024-11-05",
          capabilities: { tools: {} },
          serverInfo: { name: "tavily-news-mcp", version: "1.0.0" }
        }
      }));
    } 
    
    // 2. List Tools
    else if (request.method === 'tools/list') {
      console.log(JSON.stringify({
        jsonrpc: "2.0",
        id: request.id,
        result: {
          tools: [{
            name: "search_news",
            description: "Search the real internet (including LinkedIn and Company Sites) for data.",
            inputSchema: {
              type: "object",
              properties: { query: { type: "string" } },
              required: ["query"]
            }
          }]
        }
      }));
    }
    
    // 3. Execute Search
    else if (request.method === 'tools/call') {
      const query = request.params.arguments.query;
      
      // UPGRADE: Use "advanced" depth and remove "news" topic to find LinkedIn/Website data
      const response = await fetch("https://api.tavily.com/search", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          api_key: TAVILY_API_KEY,
          query: query,
          search_depth: "advanced", // <--- Finds LinkedIn & Websites
          // topic: "news",         // <--- REMOVED! "General" search finds more.
          include_answer: true
        })
      });

      const data = await response.json();
      let finalText = "";

      if (data.answer) {
        finalText = "SUMMARY: " + data.answer;
      } else if (data.results && data.results.length > 0) {
         // Combine top 3 results to ensure we catch the LinkedIn/Website snippet
         finalText = data.results.slice(0, 3).map(r => r.content).join("\n\n");
      } else {
         finalText = "SYSTEM_MSG: No relevant results found in the Tavily search database."; 
      }

      console.log(JSON.stringify({
        jsonrpc: "2.0",
        id: request.id,
        result: { content: [{ type: "text", text: finalText }] }
      }));
    }
  } catch (e) {
    // console.error(e);
  }
});