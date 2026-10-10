"""Read-only documentation checks; not a substitute for live consumer tests.

From order-service: python scripts/validate-asyncapi-contract.py
Requires PyYAML and jsonschema in the local Python environment.

AI assistance: OpenAI Codex, 2026-10-10/11; documentation validator generation.
Vincent approved the documentation scope; human review remains pending.
"""

import copy
import re
from pathlib import Path

import yaml
from jsonschema import Draft7Validator, FormatChecker


ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/sg/edu/nus/foc/order"
SPEC = ROOT / "docs/asyncapi/asyncapi.yaml"


def require(condition, message):
    if not condition:
        raise AssertionError(message)


def fields(class_name):
    source = (JAVA / f"messagingpublisher/dto/{class_name}.java").read_text(encoding="utf-8")
    # Current DTOs use explicit fields; exclude operational-only JsonIgnore fields.
    ignored = set(re.findall(r"@JsonIgnore\s+private\s+\w+\s+(\w+)\s*;", source))
    return set(re.findall(r"private\s+\w+\s+(\w+)\s*;", source)) - ignored


def json_schema(value):
    if isinstance(value, dict):
        return {key: (item.replace("#/components/schemas/", "#/$defs/")
                      if key == "$ref" else json_schema(item))
                for key, item in value.items()}
    if isinstance(value, list):
        return [json_schema(item) for item in value]
    return value


def main():
    spec = yaml.safe_load(SPEC.read_text(encoding="utf-8"))
    require(spec["asyncapi"] == "3.0.0", "Unexpected AsyncAPI version")
    schemas = spec["components"]["schemas"]
    for name in ("OpenOrderRefundTaskEvent", "OrderCompletionTaskEvent",
                 "AcceptedOrderCancellationTaskEvent", "OrderEventSnapshot", "RepostPlanEventSnapshot"):
        require(set(schemas[name]["properties"]) == fields(name), f"{name}: DTO field drift")

    config = (ROOT / "src/main/resources/application.yaml").read_text(encoding="utf-8")
    mapper = (JAVA / "messagingpublisher/mapper/OrderTaskEventMapper.java").read_text(encoding="utf-8")
    expected = {
        "openRefund": ("OpenOrderRefundTaskEvent", "open-order-refund", "ORDER_OPEN_REFUND_TOPIC", "open-order-refund-dev-v1", "2"),
        "completion": ("OrderCompletionTaskEvent", "order-completion", "ORDER_COMPLETION_TOPIC", "order-completion-dev-v1", "2"),
        "acceptedCancellation": ("AcceptedOrderCancellationTaskEvent", "accepted-order-cancellation", "ORDER_ACCEPTED_CANCELLATION_TOPIC", "accepted-order-cancellation-dev-v1", "1"),
    }
    require(set(spec["channels"]) == set(expected), "Expected exactly three current channels")
    require(len(spec["operations"]) == 3, "Expected exactly three send operations")
    checked_examples = 0
    for channel_id, (event_type, config_key, env_key, topic, version) in expected.items():
        channel = spec["channels"][channel_id]
        require(channel["parameters"]["topicId"]["default"] == topic, "Topic default drift")
        require(channel["x-topic-environment-variable"] == env_key, "Topic variable drift")
        require(f"{config_key}: ${{{env_key}:{topic}}}" in config, "Configuration source drift")
        typed_publisher = (JAVA / f"messagingpublisher/publisher/{event_type[:-5]}Publisher.java").read_text(encoding="utf-8")
        require(f"order.messaging.topics.{config_key}:" in typed_publisher, "Typed publisher routing drift")
        message = spec["components"]["messages"][event_type]
        require(message["payload"]["$ref"] == f"#/components/schemas/{event_type}", "Wrong message schema")
        headers = message["headers"]
        require(set(headers["properties"]) == {"eventId", "eventType", "eventVersion"}, "Attribute drift")
        require(headers["properties"]["eventVersion"]["const"] == version, "Wire version drift")
        require(headers["properties"]["eventType"]["const"] == event_type, "Wrong event type")
        require(re.search(r'target = "eventType", constant = "' + event_type
                          + r'"\)\s+@Mapping\(target = "eventVersion", constant = "'
                          + version + r'"', mapper), "Mapper event version drift")
        body_schema = json_schema(copy.deepcopy(schemas[event_type]))
        body_schema["$defs"] = json_schema(schemas)
        Draft7Validator.check_schema(body_schema)
        validator = Draft7Validator(body_schema, format_checker=FormatChecker())
        header_validator = Draft7Validator(headers, format_checker=FormatChecker())
        for example in message["examples"]:
            validator.validate(example["payload"])
            header_validator.validate(example["headers"])
            require(example["headers"]["eventId"] == example["payload"]["eventId"], "Event ID attribute mismatch")
            if event_type == "AcceptedOrderCancellationTaskEvent":
                payload = example["payload"]
                require(payload["eventVersion"] == int(version), "Body/attribute version mismatch")
                require(payload["orderId"] == payload["order"]["id"], "Snapshot order ID mismatch")
                require(payload["orderVersion"] == payload["order"]["version"], "Snapshot version mismatch")
            checked_examples += 1
        # Negative checks catch extra compact fields and invalid versions/statuses.
        invalid = copy.deepcopy(message["examples"][0]["payload"])
        invalid["undocumentedField"] = True
        require(not validator.is_valid(invalid), "Extra body field was accepted")
        invalid_headers = copy.deepcopy(message["examples"][0]["headers"])
        invalid_headers["eventVersion"] = "999"
        require(not header_validator.is_valid(invalid_headers), "Invalid version was accepted")
        invalid_status = copy.deepcopy(message["examples"][0]["payload"])
        if event_type == "AcceptedOrderCancellationTaskEvent":
            invalid_status["order"]["status"] = "ABORTED"
        else:
            invalid_status["orderStatus"] = "OPEN"
        require(not validator.is_valid(invalid_status), "Invalid outcome status was accepted")

    for operation in spec["operations"].values():
        require(operation["action"] == "send", "Order has no documented inbound Pub/Sub consumer")
        require("reply" not in operation, "Publication acknowledgment is not a business reply")
        require(operation["channel"]["$ref"].split("/")[-1] in expected, "Unknown channel")
    publisher = (JAVA / "messagingpublisher/publisher/GoogleCloudPubSubEventPublisher.java").read_text(encoding="utf-8")
    require(set(re.findall(r'\.putAttributes\("([^"]+)"', publisher)) == {"eventId", "eventType", "eventVersion"}, "Publisher attribute drift")
    require("setOrderingKey" not in publisher, "Ordering behavior changed; review documentation")
    scheduler = (JAVA / "application/OrderOutboxScheduler.java").read_text(encoding="utf-8")
    require("0 */15 * * * *" in scheduler, "Outbox default cadence drift")
    print(f"PASS: 3 send operations/channels, 5 DTO field sets, {checked_examples} examples, negative schema checks, source metadata/topics/cadence parity.")


if __name__ == "__main__":
    main()
