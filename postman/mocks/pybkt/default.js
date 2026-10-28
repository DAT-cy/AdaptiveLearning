const http = require("http");
const PORT = process.env.PORT || 4600;
const JSON_HEADER = { "Content-Type": "application/json" };

const server = http.createServer(async (req, res) => {
  const url = new URL(req.url, "http://mock.local");
  if (req.method === "GET" && url.pathname === "/health") {
    return respond(res, 200, { status: "UP", library: "pyBKT" });
  }
  if (req.method !== "POST" || url.pathname !== "/mastery/estimate") {
    return respond(res, 501, { message: "Unmodelled pyBKT route", path: url.pathname });
  }

  const payload = JSON.parse((await readBody(req)).toString() || "{}");
  const training = Array.isArray(payload.trainingInteractions) ? payload.trainingInteractions : [];
  const learner = Array.isArray(payload.learnerInteractions) ? payload.learnerInteractions : [];
  const students = [...new Set(training.map((item) => String(item.studentId || "").trim()).filter(Boolean))];
  const skills = [...new Set(learner.map((item) => String(item.skill || "").trim()).filter(Boolean))];
  const response = students.length < 2 || learner.length === 0
    ? {
        ready: false,
        masteryBySkill: {},
        trainingStudentCount: students.length,
        trainingEventCount: training.length,
        message: "Need two training learners and learner interactions",
      }
    : {
        ready: true,
        masteryBySkill: Object.fromEntries(skills.map((skill) => [skill, 0.75])),
        trainingStudentCount: students.length,
        trainingEventCount: training.length,
        message: "Calibrated test response",
      };

  if (req.headers["x-test-response"] === "failure") {
    return respond(res, 503, { message: "BKT unavailable" });
  }
  if (req.headers["x-test-response"] === "timeout") {
    return setTimeout(() => respond(res, 200, response), 1000);
  }
  return respond(res, 200, response);
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

server.listen(PORT, () => process.stdout.write(`pyBKT mock listening on ${PORT}\n`));
