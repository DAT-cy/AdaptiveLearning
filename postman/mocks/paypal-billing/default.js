const http = require("http");
const PORT = process.env.PORT || 4500;
const JSON_HEADER = { "Content-Type": "application/json" };

const server = http.createServer(async (req, res) => {
  const bodyPromise = readJson(req);
  const url = new URL(req.url, "http://mock.local");

  if (req.method === "POST" && url.pathname === "/v1/oauth2/token") {
    res.writeHead(200, JSON_HEADER);
    return res.end(JSON.stringify({ access_token: "simulation-access-token", token_type: "Bearer", expires_in: 300 }));
  }

  if (req.method === "POST" && url.pathname === "/v2/checkout/orders") {
    const request = await bodyPromise;
    const order = {
      id: `ORDER-${Date.now()}`,
      customId: request.purchase_units?.[0]?.custom_id,
      status: "APPROVAL_PENDING",
    };
    await pm.state.set(`paypal.order.${order.id}`, order);
    res.writeHead(201, JSON_HEADER);
    return res.end(JSON.stringify({
      id: order.id,
      status: order.status,
      links: [{ rel: "approve", href: `https://www.sandbox.paypal.mock/checkoutnow?token=${order.id}` }],
    }));
  }

  if (req.method === "POST" && /^\/v2\/checkout\/orders\/[^/]+\/capture$/.test(url.pathname)) {
    const id = url.pathname.split("/")[4];
    const order = await pm.state.get(`paypal.order.${id}`);
    if (!order) {
      res.writeHead(404, JSON_HEADER);
      return res.end(JSON.stringify({ name: "RESOURCE_NOT_FOUND" }));
    }
    const captureId = `CAPTURE-${id}`;
    await pm.state.set(`paypal.order.${id}`, { ...order, status: "COMPLETED", captureId });
    res.writeHead(201, JSON_HEADER);
    return res.end(JSON.stringify({
      id,
      status: "COMPLETED",
      purchase_units: [{
        custom_id: order.customId,
        payments: { captures: [{ id: captureId, status: "COMPLETED", amount: { value: "19.99", currency_code: "USD" } }] },
      }],
    }));
  }

  if (req.method === "POST" && url.pathname === "/v1/notifications/verify-webhook-signature") {
    await bodyPromise;
    res.writeHead(200, JSON_HEADER);
    return res.end(JSON.stringify({ verification_status: "SUCCESS" }));
  }

  res.writeHead(501, JSON_HEADER);
  res.end(JSON.stringify({ error: "This PayPal mock does not model this endpoint.", path: url.pathname }));
});

function readJson(req) {
  return new Promise((resolve) => {
    let raw = "";
    req.on("data", (chunk) => { raw += chunk; });
    req.on("end", () => {
      try { resolve(raw ? JSON.parse(raw) : {}); }
      catch { resolve({}); }
    });
  });
}

server.listen(PORT, () => console.log(`PayPal billing mock listening on ${PORT}`));
