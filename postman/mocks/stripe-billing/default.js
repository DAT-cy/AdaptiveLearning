const http = require("http");
const PORT = process.env.PORT || 4500;
const JSON_HEADER = { "Content-Type": "application/json" };

const server = http.createServer(async (req, res) => {
  const bodyPromise = readBody(req);
  const url = new URL(req.url, "http://mock.local");

  if (req.method === "POST" && url.pathname === "/v1/checkout/sessions") {
    const form = new URLSearchParams((await bodyPromise).toString());
    const id = `cs_test_${Date.now()}`;
    const session = {
      id,
      paymentId: form.get("metadata[paymentId]"),
      currency: form.get("line_items[0][price_data][currency]"),
      amount: form.get("line_items[0][price_data][unit_amount]"),
      status: "open",
    };
    await pm.state.set(`stripe.session.${id}`, session);
    res.writeHead(200, JSON_HEADER);
    return res.end(JSON.stringify({ id, url: `https://checkout.stripe.mock/${id}`, mode: "payment", currency: session.currency, amount: session.amount }));
  }

  res.writeHead(501, JSON_HEADER);
  res.end(JSON.stringify({ error: "This Stripe mock does not model this endpoint.", path: url.pathname }));
});

function readBody(req) {
  return new Promise((resolve) => {
    const chunks = [];
    req.on("data", (chunk) => chunks.push(chunk));
    req.on("end", () => resolve(Buffer.concat(chunks)));
  });
}

server.listen(PORT, () => console.log(`Stripe billing mock listening on ${PORT}`));
