import { describe, expect, it, vi } from "vitest"
import { createAdminApi } from "./admin-api"
import { ApiError } from "./api"

const profile = { id: "mongo-1", userId: "uid", username: "Alex", email: "a@u.nus.edu", roles: ["requester"], penalty: 0, isCourierSuspended: false, suspensionEndDate: null, status: "ACTIVE" }
describe("ready admin provider integration", () => {
  it("uses the admin full profile and maps account lifecycle status", async () => {
    const request = vi.fn().mockResolvedValue(profile)
    const api = createAdminApi(request)
    expect(await api.user("uid")).toMatchObject({ id: "mongo-1", active: true })
    expect(request).toHaveBeenCalledWith("/api/users/uid", { signal: undefined })
    request.mockResolvedValue({ ...profile, status: "DEACTIVATION_PENDING" })
    expect((await api.user("uid")).active).toBe(false)
    request.mockResolvedValue({ ...profile, status: undefined })
    expect((await api.user("uid")).active).toBeNull()
  })
  it("loads selected-user balance independently and preserves failures", async () => {
    const balance = { userId: "uid", totalBalance: 20, reservedBalance: 5, usableBalance: 15 }
    const request = vi.fn().mockResolvedValue(balance)
    const api = createAdminApi(request)
    expect(await api.userBalance("uid")).toEqual(balance)
    expect(request).toHaveBeenCalledWith("/api/credits/users/uid/balance", { signal: undefined })
    request.mockResolvedValue({ ...balance, userId: "other" })
    await expect(api.userBalance("uid")).rejects.toThrow("does not match")
    const error = new ApiError(503, "UNAVAILABLE", "Credit unavailable")
    request.mockRejectedValue(error)
    await expect(api.userBalance("uid")).rejects.toBe(error)
  })
  it("promotes with PUT and reloads the preserved profile after deactivation", async () => {
    const request = vi.fn().mockResolvedValue({ ...profile, roles: ["requester", "admin"] })
    const api = createAdminApi(request)
    expect((await api.promoteUser("uid")).roles).toContain("admin")
    expect(request).toHaveBeenCalledWith("/api/users/uid/user-promotion", { method: "PUT" })
    request.mockResolvedValueOnce(null).mockResolvedValueOnce({ ...profile, status: "DEACTIVATED" })
    expect((await api.deactivateUser("uid")).active).toBe(false)
    expect(request).toHaveBeenCalledWith("/api/users/uid", { method: "DELETE" })
    expect(api.capabilities.userDirectory).toBe(true)
    expect(api.capabilities.userActions).toBe(true)
  })
})
