import { describe, expect, it } from "vitest"

import { buildRegistrationFactPayload } from "@/lib/registration"

describe("credit registration contract helpers", () => {
  it("builds the peer Credit Service registration fact contract", () => {
    expect(buildRegistrationFactPayload("uid-1", "event-1", "2026-09-30T04:00:00.000Z")).toEqual({
      eventId: "event-1",
      userId: "uid-1",
      occurredAt: "2026-09-30T04:00:00.000Z",
    })
  })
})
