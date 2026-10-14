import json
import os
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

import pandas as pd
from pyBKT.models import Model, Roster


def normalize_interactions(raw_items):
    items = []
    for item in raw_items:
        skill = str(item.get("skill", "")).strip()
        student_id = str(item.get("studentId", "")).strip()
        if not skill or not student_id or item.get("correct") is None:
            continue
        items.append(
            {
                "student_id": student_id,
                "skill_name": skill,
                "correct": int(bool(item["correct"])),
                "order_id": 0,
            }
        )

    counters = {}
    for item in items:
        student_id = item["student_id"]
        counters[student_id] = counters.get(student_id, 0) + 1
        item["order_id"] = counters[student_id]
    return items


def estimate_mastery(payload):
    learner_id = str(payload.get("learnerId", "")).strip()
    learner = normalize_interactions(payload.get("learnerInteractions", []))
    population = normalize_interactions(payload.get("trainingInteractions", []))
    training_students = {item["student_id"] for item in population}
    skills = sorted({item["skill_name"] for item in learner})

    if not learner_id or not learner or len(training_students) < 2:
        return {
            "ready": False,
            "masteryBySkill": {},
            "trainingStudentCount": len(training_students),
            "trainingEventCount": len(population),
            "message": "Cần lịch sử trả lời của ít nhất hai người học khác để hiệu chuẩn BKT.",
        }

    training = pd.DataFrame(population)
    model = Model(seed=42, num_fits=1, parallel=False)
    model.fit(
        data=training,
        defaults={
            "user_id": "student_id",
            "order_id": "order_id",
            "skill_name": "skill_name",
            "correct": "correct",
        },
    )

    model_skills = set(model.fit_model)
    learner_skills = [skill for skill in skills if skill in model_skills]
    if not learner_skills:
        return {
            "ready": False,
            "masteryBySkill": {},
            "trainingStudentCount": len(training_students),
            "trainingEventCount": len(population),
            "message": "Kho dữ liệu huấn luyện chưa có kỹ năng trùng với lịch sử người học.",
        }

    roster = Roster(
        students=[learner_id],
        skills=learner_skills,
        model=model,
    )
    for item in learner:
        if item["skill_name"] in learner_skills:
            roster.update_state(
                item["skill_name"], learner_id, item["correct"]
            )

    mastery = {
        skill: float(roster.get_mastery_prob(skill, learner_id))
        for skill in learner_skills
    }
    return {
        "ready": True,
        "masteryBySkill": mastery,
        "trainingStudentCount": len(training_students),
        "trainingEventCount": len(population),
        "message": "BKT đã hiệu chuẩn từ lịch sử phản hồi của người học khác.",
    }


class Handler(BaseHTTPRequestHandler):
    def respond(self, status, data):
        body = json.dumps(data).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self):
        if self.path == "/health":
            self.respond(200, {"status": "UP", "library": "pyBKT"})
            return
        self.respond(404, {"message": "Not found"})

    def do_POST(self):
        if self.path != "/v1/mastery/estimate":
            self.respond(404, {"message": "Not found"})
            return
        try:
            length = int(self.headers.get("Content-Length", "0"))
            payload = json.loads(self.rfile.read(length))
            self.respond(200, estimate_mastery(payload))
        except (ValueError, TypeError, KeyError) as error:
            self.respond(400, {"message": str(error)})
        except Exception as error:
            self.respond(422, {"message": f"BKT could not fit this data: {error}"})

    def log_message(self, format, *args):
        print(format % args)


if __name__ == "__main__":
    server = ThreadingHTTPServer(
        ("0.0.0.0", int(os.environ.get("PORT", "8001"))), Handler
    )
    server.serve_forever()
