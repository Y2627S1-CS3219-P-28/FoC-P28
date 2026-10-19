import { describe, expect, it, vi } from "vitest"

import { createAdminApi } from "@/lib/admin-api"

describe("Admin mock API adapter", () => {
  it("keeps sample resolutions isolated while reading shared policy amounts", async () => {
    const request = vi.fn().mockResolvedValue({})
    const api = createAdminApi(request)
    const input = { requesterId: "owner", courierId: null, appliedPenalties: [], resolutionReason: "Requester requested cancellation" }
    await api.resolveOrder("order/1", "cancel", input)
    expect(request).toHaveBeenLastCalledWith("/api/orders/admin-mock/orders/order%2F1/cancel", { method: "POST", body: input })
    await api.penaltyPolicies()
    expect(request).toHaveBeenLastCalledWith("/api/orders/admin/penalty-policies", { signal: undefined })
  })
  it("uses the existing exact-ID Order query and rejects an empty or mismatched result", async () => {
    const request = vi.fn().mockResolvedValue({ items: [{ id: "order/1" }] })
    const admin = createAdminApi(request)
    const signal = new AbortController().signal
    expect(await admin.order("order/1", signal)).toEqual({ id: "order/1" })
    expect(request).toHaveBeenLastCalledWith("/api/orders/admin-mock/orders?page=1&size=1&orderId=order%2F1", { signal })
    request.mockResolvedValueOnce({ items: [] }).mockResolvedValueOnce({ items: [{ id: "another-order" }] })
    await expect(admin.order("missing")).rejects.toThrow("Order not found.")
    await expect(admin.order("expected")).rejects.toThrow("Order not found.")
  })
  it("loads selected records and history using encoded IDs, abort signals and server pagination", async () => {
    const request = vi.fn().mockResolvedValue({})
    const admin = createAdminApi(request)
    const signal = new AbortController().signal
    await admin.supplier("location/1", signal)
    expect(request).toHaveBeenLastCalledWith("/api/orders/admin-mock/suppliers/location%2F1", { signal })
    await admin.userTransactions("user/1", 2, signal)
    expect(request).toHaveBeenLastCalledWith("/api/orders/admin-mock/users/user%2F1/transactions?page=2&size=20", { signal })
  })
  it("encodes searches and preserves server pagination rather than filtering a fetched page", async () => {
    const request = vi.fn().mockResolvedValue({ items: [] })
    const admin = createAdminApi(request)
    await admin.orders({ page: 2, status: "COMPLETED", orderId: "id/with spaces" })
    expect(request).toHaveBeenCalledWith(
      "/api/orders/admin-mock/orders?page=2&size=20&status=COMPLETED&orderId=id%2Fwith+spaces",
      { signal: undefined },
    )
    await admin.users({ page: 1, searchBy: "id", q: "user-1" })
    expect(request).toHaveBeenLastCalledWith(
      "/api/orders/admin-mock/users?page=1&size=20&searchBy=id&q=user-1", { signal: undefined },
    )
  })

  it("uses typed mock mutation contracts without calling any real peer", async () => {
    const request = vi.fn().mockResolvedValue({})
    const admin = createAdminApi(request)
    await admin.suspendUser("user/1", "2026-11-01T00:00:00Z")
    expect(request).toHaveBeenCalledWith("/api/orders/admin-mock/users/user%2F1/suspend", {
      method: "POST", body: { suspensionEndDate: "2026-11-01T00:00:00Z" },
    })
    await admin.roleContext("current-user")
    expect(request).toHaveBeenLastCalledWith(
      "/api/orders/admin-mock/role-context?userId=current-user", { signal: undefined },
    )
  })
})
