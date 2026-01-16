# Clinware Agent

This project runs a Javalin web server backed by a small AI agent and a Node MCP script (`tavily-server.js`) for searching news.

## Run locally
1. Build:
   ```
   mvn clean package -DskipTests
   ```
2. Set env vars (PowerShell):
   ```powershell
   $env:OPENAI_API_KEY='sk-...'
   $env:TAVILY_API_KEY='tvly-...'
   $env:PORT='7000'  # optional
   mvn exec:java
   ```
3. Open http://localhost:7000

## Docker
Build and run (recommended):

```bash
docker build -t clinware-agent .
docker run -p 7000:7000 --env OPENAI_API_KEY=sk-... --env TAVILY_API_KEY=tvly-... clinware-agent
```

Notes:
- **Do not** commit `.env` to git. Use environment variables in your deployment platform (Render, AWS, etc.).
- Rotate keys if they are exposed.
