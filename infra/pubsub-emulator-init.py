#
# AI Assistance Disclosure:
# Tool: OpenAI Codex (GPT-5), date: 2026-10-26
# Mode: Code generation.
# Scope: Generated the pub sub emulator init script for order service and user service event handling
# Author review: I reviewed for correctness and edited where needed.
#

"""Provision only the private Docker emulator; no credentials or cloud API path."""
import json
import os
from urllib.error import HTTPError
from urllib.request import Request, urlopen

host = os.environ.get("PUBSUB_EMULATOR_HOST", "")
if host != "pubsub-emulator:8085":
    raise SystemExit("Refusing setup outside the private Docker emulator")
base = f"http://{host}/v1/projects/demo-foc-local"


def create(path, body):
    request = Request(base + path, data=json.dumps(body).encode(),
                      headers={"Content-Type": "application/json"}, method="PUT")
    try:
        with urlopen(request, timeout=10) as response:
            if response.status != 200:
                raise RuntimeError(f"Emulator setup failed: {response.status}")
    except HTTPError as error:
        if error.code != 409:
            raise


for topic in ("order-completion", "open-order-refund",
              "courier-penalty-addition", "courier-penalty-deduction",
              "user-created"):
    create(f"/topics/{topic}-local-v1", {})

for topic, route in (("order-completion", "completion"), ("open-order-refund", "open-refund")):
    create(f"/subscriptions/credit-{topic}-local-v1", {
        "topic": f"projects/demo-foc-local/topics/{topic}-local-v1",
        "ackDeadlineSeconds": 30,
        "pushConfig": {"pushEndpoint": f"http://credit-service:8080/api/credits/internal/order-events/{route}"},
    })

create("/subscriptions/credit-user-created-local-v1", {
    "topic": "projects/demo-foc-local/topics/user-created-local-v1",
    "ackDeadlineSeconds": 30,
    "pushConfig": {"pushEndpoint": "http://credit-service:8080/api/credits/internal/user-events/created"},
})

print("Local emulator initialized: refund/completion/user-created -> real Credit; penalty topics ready; no cloud API used.")
