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


for topic in ("order-completion", "open-order-refund", "accepted-order-cancellation",
              "courier-penalty-addition", "courier-penalty-deduction"):
    create(f"/topics/{topic}-local-v1", {})

for topic, route in (("order-completion", "completion"), ("open-order-refund", "open-refund")):
    create(f"/subscriptions/credit-{topic}-local-v1", {
        "topic": f"projects/demo-foc-local/topics/{topic}-local-v1",
        "ackDeadlineSeconds": 30,
        "pushConfig": {"pushEndpoint": f"http://credit-service:8080/api/credits/internal/order-events/{route}"},
    })
print("Local emulator initialized: refund/completion -> real Credit; penalty topics ready (User consumer pending); no cloud API used.")
