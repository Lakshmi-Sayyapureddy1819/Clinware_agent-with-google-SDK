// mcp-server.js - A mock MCP server for the Clinware Assignment
const readline = require('readline');

const rl = readline.createInterface({
  input: process.stdin,
  output: process.stdout,
  terminal: false
});

rl.on('line', (line) => {
  try {
    const request = JSON.parse(line);
    
    // 1. Handle "Initialize" (Handshake)
    if (request.method === 'initialize') {
      const response = {
        jsonrpc: "2.0",
        id: request.id,
        result: {
          protocolVersion: "2024-11-05",
          capabilities: { tools: {} },
          serverInfo: { name: "mock-news-server", version: "1.0.0" }
        }
      };
      console.log(JSON.stringify(response));
    } 
    
    // 2. Handle "Tools List" (Tell the AI what we can do)
    else if (request.method === 'tools/list') {
      const response = {
        jsonrpc: "2.0",
        id: request.id,
        result: {
          tools: [{
            name: "search_news",
            description: "Search for latest news about Clinware, funding, or products.",
            inputSchema: {
              type: "object",
              properties: {
                query: { type: "string" }
              },
              required: ["query"]
            }
          }]
        }
      };
      console.log(JSON.stringify(response));
    }
    
    // 3. Handle "Tools Call" (Actually do the search)
    else if (request.method === 'tools/call') {
      const query = request.params.arguments.query.toLowerCase();
      let content = "";

      // LOGIC: Fake the search results based on the query
      if (query.includes('clinware')) {
        content = "LATEST NEWS: Clinware (B2B Healthcare AI) raised $4.25 Million in Seed funding in October 2025. The round was led by specialized healthcare investors. They also launched a new AI Admission Agent for Skilled Nursing Facilities.";
      } else if (query.includes('verge')) {
        content = "SEARCH RESULT: No specific articles found for this query on The Verge.";
      } else {
        content = "No relevant news found.";
      }

      const response = {
        jsonrpc: "2.0",
        id: request.id,
        result: {
          content: [{ type: "text", text: content }]
        }
      };
      console.log(JSON.stringify(response));
    }
  } catch (e) {
    // Ignore invalid JSON lines
  }
});