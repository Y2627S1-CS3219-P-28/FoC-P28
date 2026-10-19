import { describe, it, expect, vi } from "vitest"
import { createReportApi } from "./reports"
describe("report transport", () => {
  it("exposes admin review only, without submission or response helpers", () => {
    const api = createReportApi(vi.fn(), "mock")
    for (const name of ["create", "eligible", "respond"]) expect(api).not.toHaveProperty(name)
  })
  it("never calls prototype endpoints in live mode", async () => {
    const request = vi.fn()
    await expect(createReportApi(request, "live").list("u", "admin", 1)).rejects.toThrow("Coming soon")
    expect(request).not.toHaveBeenCalled()
  })
  it("encodes admin queries and passes cancellation", async () => {
    const request = vi.fn().mockResolvedValue({ items: [] })
    const signal = new AbortController().signal
    await createReportApi(request, "mock").list("user/a", "admin", 2, "READY_FOR_REVIEW", signal)
    expect(request).toHaveBeenCalledWith(expect.stringContaining("userId=user%2Fa&mode=admin&page=2&status=READY_FOR_REVIEW"), { signal })
  })
  it("encodes exact Order ID and report status alongside group/pagination", async () => {
    const request = vi.fn().mockResolvedValue({ items: [] })
    const signal = new AbortController().signal
    await createReportApi(request, "mock").list("u", "admin", 2, "OPEN", signal, { orderId: "order/a", status: "READY_FOR_REVIEW" })
    const url = new URL(request.mock.calls[0][0], "http://localhost")
    expect(url.searchParams.get("orderId")).toBe("order/a")
    expect(url.searchParams.get("reportStatus")).toBe("READY_FOR_REVIEW")
    expect(url.searchParams.get("status")).toBe("OPEN")
  })
  it("requests admin case details without a participant mode option", async () => {
    const request = vi.fn().mockResolvedValue({})
    const signal = new AbortController().signal
    await createReportApi(request, "mock").detail("report/a", "admin", signal)
    expect(request).toHaveBeenCalledExactlyOnceWith("/api/orders/admin-mock/reports/report%2Fa?userId=admin&admin=true", { signal })
  })
  it("resolves reports separately from financial actions", async () => {
    const request = vi.fn().mockResolvedValue({ status: "RESOLVED" })
    await createReportApi(request, "mock").resolve("report/a", "admin", "Reviewed evidence")
    expect(request).toHaveBeenCalledExactlyOnceWith("/api/orders/admin-mock/reports/report%2Fa/resolve", { method: "PUT", body: { userId: "admin", explanation: "Reviewed evidence" } })
  })
})
