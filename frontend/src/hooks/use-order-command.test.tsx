import { act, renderHook, waitFor } from "@testing-library/react"
import { beforeEach, describe, expect, it, vi } from "vitest"
import { ApiError } from "@/lib/api"
import { useOrderCommand } from "@/hooks/use-order-command"

const m = vi.hoisted(() => ({ api: vi.fn(), read: vi.fn(), write: vi.fn(), remove: vi.fn(), account: "user-a", poll: null as null | (() => Promise<void>) }))
vi.mock("@/hooks/use-api", () => ({ useApi: () => m.api }))
vi.mock("@/components/providers/auth-provider", () => ({ useAuth: () => ({ user: { uid: m.account }, loading: false }) }))
vi.mock("@/lib/order-command-storage", () => ({ readOrderIntent: m.read, writeOrderIntent: m.write, removeOrderIntent: m.remove }))
vi.mock("@/hooks/use-visible-polling", () => ({ ORDER_POLL_INTERVAL_MS: 5000, useVisiblePolling: (cb: () => Promise<void>, enabled: boolean) => { m.poll = enabled ? cb : null } }))

describe("durable Order UI intent", () => {
  beforeEach(() => {
    vi.clearAllMocks(); m.poll = null; m.account = "user-a"
    m.read.mockResolvedValue(null); m.write.mockResolvedValue(undefined); m.remove.mockResolvedValue(undefined)
    m.api.mockImplementation(async (path: string) => path.endsWith("capabilities") ? { enabled: true } : { items: [] })
  })
  it("keeps one frozen key after timeout, restores it on reload and clears only after terminal success", async () => {
    const success = vi.fn(), failure = vi.fn()
    const first = renderHook(() => useOrderCommand("create", success, failure))
    await waitFor(() => expect(first.result.current.ready).toBe(true))
    m.api.mockRejectedValueOnce(new ApiError(0, "NETWORK_ERROR", "timeout"))
    const details = { commandId: "K", requesterId: "user-a", itemDescription: "old details" }
    await act(() => first.result.current.run("CREATE", "/api/orders", details))
    details.itemDescription = "edited after submit"
    expect(first.result.current.pending).toBe(true)
    expect(m.remove).not.toHaveBeenCalled()
    const intent = m.write.mock.calls[0][2]
    expect(intent.body.itemDescription).toBe("old details")
    expect(intent.body.commandId).toBe("K")
    first.unmount()
    m.read.mockResolvedValue(intent)
    m.api.mockResolvedValue({ enabled: true })
    const restored = renderHook(() => useOrderCommand("create", success, failure))
    await waitFor(() => expect(restored.result.current.pending).toBe(true))
    expect(restored.result.current.needsAuthorization).toBe(false)
    m.api.mockResolvedValue({ commandId: "K", status: "COMPLETED", outcome: "SUCCESS", result: { id: "new-order", status: "OPEN" } })
    await act(async () => { await m.poll?.() })
    expect(success).toHaveBeenCalledWith(expect.objectContaining({ id: "new-order" }))
    expect(m.remove).toHaveBeenCalledWith("user-a", "create")
    expect(restored.result.current.pending).toBe(false)
  })
  it("offers Continue only for authorization, retaining the same command", async () => {
    m.read.mockResolvedValue({ key: "K", kind: "ACCEPT", path: "/api/orders/commands/order-a/accept", body: { commandId: "K" } })
    const hook = renderHook(() => useOrderCommand("accept:order-a", vi.fn(), vi.fn()))
    await waitFor(() => expect(hook.result.current.pending).toBe(true))
    m.api.mockResolvedValue({ commandId: "K", status: "PENDING", reason: "AUTHORIZATION_REQUIRED", attemptCount: 2, message: "Continue to authorize recovery." })
    await act(async () => { await m.poll?.() })
    expect(hook.result.current.needsAuthorization).toBe(true)
    m.api.mockResolvedValue({ commandId: "K", status: "PENDING", reason: "PROCESSING", attemptCount: 3 })
    await act(() => hook.result.current.continueRecovery())
    expect(m.api).toHaveBeenLastCalledWith("/api/orders/commands/K/resume?userId=user-a", { method: "POST" })
    expect(hook.result.current.pending).toBe(true)
    expect(hook.result.current.needsAuthorization).toBe(false)
  })
  it("does not enable durable writes for live HTTP mode", async () => {
    m.api.mockResolvedValue({ enabled: false })
    const success = vi.fn()
    const hook = renderHook(() => useOrderCommand("create", success, vi.fn()))
    await waitFor(() => expect(hook.result.current.ready).toBe(true))
    m.api.mockResolvedValue({ id: "legacy", status: "OPEN" })
    await act(() => hook.result.current.run("CREATE", "/api/orders", { commandId: "K" }))
    expect(m.write).not.toHaveBeenCalled()
    expect(m.api).toHaveBeenLastCalledWith("/api/orders", { method: "POST", body: { commandId: "K" } })
  })
  it("retains an old intent but never resumes it when recovery is disabled", async () => {
    m.api.mockResolvedValue({ enabled: false })
    m.read.mockResolvedValue({ key: "K", kind: "CREATE", path: "/api/orders/commands/create", body: { commandId: "K" } })
    const hook = renderHook(() => useOrderCommand("create", vi.fn(), vi.fn()))
    await waitFor(() => expect(hook.result.current.pending).toBe(true))
    m.api.mockResolvedValue({ commandId: "K", status: "PENDING", reason: "AUTHORIZATION_REQUIRED" })
    await act(async () => { await m.poll?.() })
    const calls = m.api.mock.calls.length
    await act(() => hook.result.current.continueRecovery())
    expect(m.api).toHaveBeenCalledTimes(calls)
    expect(m.remove).not.toHaveBeenCalled()
  })
  it("does not expose the previous account's ready state or pending intent during account restoration", async () => {
    const hook = renderHook(() => useOrderCommand("create", vi.fn(), vi.fn()))
    await waitFor(() => expect(hook.result.current.ready).toBe(true))
    m.account = "user-b"
    m.api.mockImplementation(() => new Promise(() => {}))
    hook.rerender()
    expect(hook.result.current.ready).toBe(false)
    await act(() => hook.result.current.run("CREATE", "/api/orders", { commandId: "other" }))
    expect(m.write).not.toHaveBeenCalled()
  })
  it("clears a confirmed rejection but never sends a request when storage fails", async () => {
    const rejection = vi.fn()
    const hook = renderHook(() => useOrderCommand("create", vi.fn(), rejection))
    await waitFor(() => expect(hook.result.current.ready).toBe(true))
    m.write.mockRejectedValueOnce(new Error("storage unavailable"))
    await act(async () => {
      await expect(hook.result.current.run("CREATE", "/api/orders", { commandId: "K" })).rejects.toThrow()
    })
    expect(m.api.mock.calls.some(([, options]) => options?.method === "POST")).toBe(false)
    m.api.mockResolvedValue({ commandId: "K", status: "COMPLETED", outcome: "REJECTED", message: "Insufficient credits." })
    await act(() => hook.result.current.run("CREATE", "/api/orders", { commandId: "K" }))
    expect(rejection).toHaveBeenCalledWith("Insufficient credits.")
    expect(m.remove).toHaveBeenCalledTimes(1)
  })
  it("explains a recovery-readiness failure while keeping submit disabled", async () => {
    m.api.mockRejectedValue(new ApiError(0, "NETWORK_ERROR", "offline"))
    const hook = renderHook(() => useOrderCommand("create", vi.fn(), vi.fn()))
    await waitFor(() => expect(hook.result.current.message).toMatch(/Could not check request recovery/))
    expect(hook.result.current.ready).toBe(false)
    await act(() => hook.result.current.run("CREATE", "/api/orders", { commandId: "K" }))
    expect(m.write).not.toHaveBeenCalled()
  })
})
