import { describe, expect, it, vi } from "vitest"

import { ApiError } from "@/lib/api"
import { createAdminApi } from "@/lib/admin-api"

describe("Live Admin API adapter", () => {
  it("checks the authenticated role context without sending a trusted caller ID", async () => {
    const request = vi.fn().mockResolvedValue({ userId: "signed-in", roles: ["admin"] })
    const api = createAdminApi(request, "live")
    await api.roleContext("signed-in")
    expect(request).toHaveBeenCalledWith("/api/users/role-context", { signal: undefined })
  })

  it("uses real server-filtered Order pages and exact details", async () => {
    const request = vi.fn().mockResolvedValue({ items: [{ id: "order-1" }], page: 2, size: 10, totalItems: 21, totalPages: 3 })
    const api = createAdminApi(request, "live")
    const result = await api.orders({ page: 2, status: "COMPLETED", orderId: "" })
    expect(request).toHaveBeenCalledWith("/api/orders?page=2&size=10&status=COMPLETED", { signal: undefined })
    expect(result.totalItems).toBe(21)
    request.mockResolvedValue({ id: "order/1", status: "OPEN" })
    await api.order("order/1")
    expect(request).toHaveBeenLastCalledWith("/api/orders/order%2F1", { signal: undefined })
    expect((await api.orders({ page: 1, status: "COMPLETED", orderId: "order/1" })).items).toEqual([])
  })

  it("turns only missing exact searches into empty results; real errors never fall back to mocks", async () => {
    const request = vi.fn().mockRejectedValue(new ApiError(404, "NOT_FOUND", "Missing"))
    const api = createAdminApi(request, "live")
    expect((await api.suppliers({ page: 1, searchBy: "id", q: "missing" })).items).toEqual([])
    expect((await api.orders({ page: 1, status: null, orderId: "missing" })).items).toEqual([])
    for (const status of [401, 403, 503]) {
      request.mockRejectedValue(new ApiError(status, "FAILURE", "Real failure"))
      await expect(api.orders({ page: 1, status: null, orderId: "missing" })).rejects.toThrow("Real failure")
      await expect(api.suppliers({ page: 1, searchBy: "id", q: "missing" })).rejects.toThrow("Real failure")
    }
    expect(request.mock.calls.every(([path]) => !path.includes("admin-mock"))).toBe(true)
  })

  it("reuses Supplier search/create/soft-deactivation contracts", async () => {
    const request = vi.fn().mockResolvedValue({ items: [] })
    const api = createAdminApi(request, "live")
    await api.suppliers({ page: 2, searchBy: "name", q: " Deck " })
    expect(request).toHaveBeenLastCalledWith("/api/suppliers?page=2&size=10&status=all&q=Deck", { signal: undefined })
    const input = { name: "Deck", type: "Food", building: "Arts", floor: "", locationDescription: "",
      latitude: 1.3, longitude: 103.8, openingTime: "09:00", closingTime: "18:00", imageUrl: "" }
    await api.createSupplier(input)
    expect(request).toHaveBeenLastCalledWith("/api/suppliers", { method: "POST", body: input })
    await api.deactivateSupplier("supplier/1")
    expect(request).toHaveBeenLastCalledWith("/api/suppliers/supplier%2F1", { method: "DELETE" })
  })

  it("preserves existing summary facts without fabricating missing profile or credit fields", async () => {
    const request = vi.fn().mockResolvedValue({ userId: "user/1", username: "Alex", email: "alex@u.nus.edu",
      roles: ["courier"], penalty: 2, isCourierSuspended: false, suspensionEndDate: null })
    const api = createAdminApi(request, "live")
    const user = await api.user("user/1")
    expect(request).toHaveBeenLastCalledWith("/api/users/summary/user%2F1", { signal: undefined })
    expect(user).toMatchObject({ userId: "user/1", username: "Alex", penalty: 2, active: null,
      id: null, phoneNumber: null, address: null, usableBalance: null, reservedBalance: null, totalBalance: null })
  })

  it("rejects a mismatched selected profile and resolves related labels through existing endpoints", async () => {
    const request = vi.fn().mockResolvedValue({ userId: "another-user", username: "Wrong" })
    const api = createAdminApi(request, "live")
    await expect(api.user("user-1")).rejects.toThrow("does not match")
    request.mockResolvedValue({ userId: "user-1", username: "Alex" })
    expect(await api.recordName("users", "user-1")).toBe("Alex")
    expect(request).toHaveBeenLastCalledWith("/api/users/summary/user-1", { signal: undefined })
    request.mockResolvedValue({ id: "supplier-1", name: "Deck" })
    expect(await api.recordName("suppliers", "supplier-1")).toBe("Deck")
    expect(request).toHaveBeenLastCalledWith("/api/suppliers/supplier-1", { signal: undefined })
    const result = await api.suppliers({ page: 1, searchBy: "id", q: "supplier-1" })
    expect(result).toMatchObject({ items: [{ id: "supplier-1" }], page: 1, totalItems: 1, totalPages: 1 })
  })

  it("refuses every unfinished operation without issuing a peer request", async () => {
    const request = vi.fn()
    const api = createAdminApi(request, "live")
    for (const operation of [() => api.users({ page: 1, searchBy: "name", q: "" }),
      () => api.userTransactions("user-1", 1), () => api.suspendUser("user-1", "2099-01-01T00:00:00Z"),
      () => api.promoteUser("user-1"), () => api.deactivateUser("user-1")]) {
      await expect(operation()).rejects.toThrow("Coming soon")
    }
    expect(request).not.toHaveBeenCalled()
    expect(api.capabilities).toEqual({ userDirectory: false, userActions: false, creditTransactions: false })
  })
})
