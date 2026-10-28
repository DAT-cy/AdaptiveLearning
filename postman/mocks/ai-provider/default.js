const http = require("http");
const PORT = process.env.PORT || 4610;
const JSON_HEADER = { "Content-Type": "application/json" };

const server = http.createServer(async (req, res) => {
  const url = new URL(req.url, "http://mock.local");
  if (req.method !== "POST" || url.pathname !== "/v1/chat/completions") {
    return respond(res, 501, { error: { message: "Unmodelled AI provider route" } });
  }
  const payload = JSON.parse((await readBody(req)).toString() || "{}");
  if (req.headers["x-test-response"] === "provider-error") {
    return respond(res, 400, { error: { message: "Invalid test request" } });
  }
  if (!req.headers.authorization || !req.headers.authorization.startsWith("Bearer ")) {
    return respond(res, 401, { error: { message: "Missing provider authorization" } });
  }
  return respond(res, 200, {
    id: "chatcmpl-test",
    object: "chat.completion",
    created: 1,
    model: payload.model || "gpt-4o-mini",
    choices: [{ index: 0, message: { role: "assistant", content: "{\"result\":\"ok\"}" }, finish_reason: "stop" }],
    usage: { prompt_tokens: 12, completion_tokens: 5, total_tokens: 17 },
  });
});

function readBody(req) {
  return new Promise((resolve) => {
    const chunks = [];
    req.on("data", (chunk) => chunks.push(chunk));
    req.on("end", () => resolve(Buffer.concat(chunks)));
  });
}

function respond(res, status, body) {
  res.writeHead(status, JSON_HEADER);
  res.end(JSON.stringify(body));
}

server.listen(PORT, () => process.stdout.write(`AI provider mock listening on ${PORT}\n`));
