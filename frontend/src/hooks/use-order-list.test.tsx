import { act, cleanup, renderHook } from "@testing-library/react"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { useOrderList } from "@/hooks/use-order-list"
import type { Order } from "@/lib/orders"

const mocks = vi.hoisted(() => ({
  api: vi.fn(), auth: { user: { uid: "a" } as { uid: string } | null, loading: false },
}))
vi.mock("@/hooks/use-api", () => ({ useApi: () => mocks.api }))
vi.mock("@/components/providers/auth-provider", () => ({ useAuth: () => mocks.auth }))
const order = { id: "one", status: "OPEN" } as Order
beforeEach(() => {
  vi.useFakeTimers()
  Object.defineProperty(document, "visibilityState", { configurable: true, value: "visible" })
  mocks.auth = { user: { uid: "a" }, loading: false }
  mocks.api.mockReset().mockResolvedValue({ items: [order] })
})
afterEach(() => { cleanup(); vi.useRealTimers() })

describe("authenticated order list polling", () => {
  it("waits for auth and retains cards during background refresh and errors", async () => {
    mocks.auth.loading = true
    const { result, rerender } = renderHook(() => useOrderList("/api/orders/available"))
    await act(() => vi.advanceTimersByTimeAsync(15_000))
    expect(mocks.api).not.toHaveBeenCalled()
    mocks.auth.loading = false
    rerender()
    await act(() => vi.advanceTimersByTimeAsync(0))
    expect(result.current.orders).toEqual([order])
    expect(result.current.loading).toBe(false)
    mocks.api.mockRejectedValue(new Error("offline"))
    await act(() => vi.advanceTimersByTimeAsync(15_000))
    expect(result.current.orders).toEqual([order])
    expect(result.current.loading).toBe(false)
    expect(result.current.error).toBe(true)
  })

  it("isolates account changes and discards an aborted late response", async () => {
    let resolve: (value: { items: Order[] }) => void = () => {}
    mocks.api.mockImplementationOnce(() => new Promise((done) => { resolve = done }))
    const { result, rerender } = renderHook(() => useOrderList("/api/orders/available"))
    await act(() => vi.advanceTimersByTimeAsync(0))
    const signal = mocks.api.mock.calls[0][1].signal
    mocks.auth.user = { uid: "b" }
    rerender()
    expect(signal.aborted).toBe(true)
    expect(result.current.orders).toEqual([])
    await act(() => vi.advanceTimersByTimeAsync(0))
    await act(async () => resolve({ items: [{ id: "old-account" } as Order] }))
    expect(result.current.orders).toEqual([order])
    mocks.auth.user = null
    rerender()
    expect(result.current.orders).toEqual([])
    await act(() => vi.advanceTimersByTimeAsync(30_000))
    expect(mocks.api).toHaveBeenCalledTimes(2)
  })

  it("does not overwrite a successful mutation with an older in-flight list", async () => {
    const { result } = renderHook(() => useOrderList("/api/orders/available"))
    await act(() => vi.advanceTimersByTimeAsync(0))
    let resolve: (value: { items: Order[] }) => void = () => {}
    mocks.api.mockImplementationOnce(() => new Promise((done) => { resolve = done }))
    await act(() => vi.advanceTimersByTimeAsync(15_000))
    act(() => result.current.updateOrders(() => []))
    await act(async () => resolve({ items: [order] }))
    expect(result.current.orders).toEqual([])
  })
})
