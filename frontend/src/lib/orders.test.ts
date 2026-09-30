import { describe, expect, it } from "vitest"

import { buildCreateOrderPayload, buildOrderActionPayload, orderMinePath, type CreateOrderForm, validateCreateOrderForm } from "@/lib/orders"

describe("Order Service frontend contract helpers", () => {
  it("builds a requester create payload with an automatic repost plan", () => {
    const form: CreateOrderForm = {
      itemDescription: "Pick up a parcel",
      pickupSupplierId: "store-a",
      deliverySupplierId: "hall-b",
      offeredCredits: 12,
      deliveryTimeLimitMinutes: 30,
      expiresAt: "2026-10-01T10:00:00.000Z",
      automaticRepost: true,
      repostDueAt: "2026-10-01T11:00:00.000Z",
      repostCreditAmount: 14,
      repostDeliveryDurationMinutes: 35,
    }

    expect(buildCreateOrderPayload(form, "uid-1")).toEqual({
      commandId: expect.any(String),
      requesterId: "uid-1",
      itemDescription: "Pick up a parcel",
      pickupSupplierId: "store-a",
      deliverySupplierId: "hall-b",
      offeredCredits: 12,
      deliveryTimeLimitMinutes: 30,
      expiresAt: "2026-10-01T10:00:00.000Z",
      automaticRepost: true,
      repostDueAt: "2026-10-01T11:00:00.000Z",
      repostCreditAmount: 14,
      repostDeliveryDurationMinutes: 35,
    })
  })

  it("creates an authenticated mine query for each approved mode", () => {
    expect(orderMinePath("requester", "uid-1")).toBe("/api/orders/mine?mode=requester&userId=uid-1&size=20")
    expect(orderMinePath("courier", "uid-2", 1)).toBe("/api/orders/mine?mode=courier&userId=uid-2&page=1&size=20")
  })

  it("uses the current order version and Firebase UID for actions", () => {
    expect(buildOrderActionPayload("uid-2", 4)).toEqual({
      commandId: expect.any(String),
      actorId: "uid-2",
      expectedVersion: 4,
    })
  })

  it("requires order expiry to be at least 30 minutes after creation", () => {
    const now = new Date("2026-09-30T07:00:00.000Z")

    expect(validateCreateOrderForm({
      itemDescription: "Pick up a parcel",
      pickupSupplierId: "store-a",
      deliverySupplierId: "hall-b",
      offeredCredits: 1,
      deliveryTimeLimitMinutes: 15,
      expiresAt: "2026-09-30T07:29:59.000Z",
      automaticRepost: false,
      repostDueAt: "2026-09-30T08:00:00.000Z",
      repostCreditAmount: 1,
      repostDeliveryDurationMinutes: 15,
    }, now)).toBe("Order expiry must be at least 30 minutes from now. Choose a later time.")

    expect(validateCreateOrderForm({
      itemDescription: "Pick up a parcel",
      pickupSupplierId: "store-a",
      deliverySupplierId: "hall-b",
      offeredCredits: 1,
      deliveryTimeLimitMinutes: 15,
      expiresAt: "2026-09-30T07:30:00.000Z",
      automaticRepost: false,
      repostDueAt: "2026-09-30T08:00:00.000Z",
      repostCreditAmount: 1,
      repostDeliveryDurationMinutes: 15,
    }, now)).toBeNull()
  })

  it("validates automatic repost details only when enabled at creation", () => {
    expect(validateCreateOrderForm({
      itemDescription: "Pick up a parcel",
      pickupSupplierId: "store-a",
      deliverySupplierId: "hall-b",
      offeredCredits: 1,
      deliveryTimeLimitMinutes: 15,
      expiresAt: "2026-09-30T08:00:00.000Z",
      automaticRepost: true,
      repostDueAt: "",
      repostCreditAmount: 1,
      repostDeliveryDurationMinutes: 15,
    }, new Date("2026-09-30T07:00:00.000Z"))).toBe("Choose a repost time when automatic repost is enabled.")

    expect(validateCreateOrderForm({
      itemDescription: "Pick up a parcel",
      pickupSupplierId: "store-a",
      deliverySupplierId: "hall-b",
      offeredCredits: 1,
      deliveryTimeLimitMinutes: 15,
      expiresAt: "2026-09-30T08:00:00.000Z",
      automaticRepost: false,
      repostDueAt: "",
      repostCreditAmount: 0,
      repostDeliveryDurationMinutes: 0,
    }, new Date("2026-09-30T07:00:00.000Z"))).toBeNull()
  })
})
