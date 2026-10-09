import { act, renderHook } from "@testing-library/react"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

import { useCreditBalance, type CreditBalance } from "@/hooks/use-credit-balance"
import { invalidateCreditBalance } from "@/lib/credit-balance-events"

const mocks = vi.hoisted(() => ({
  api: vi.fn(),
  eventSync: null as null | (() => Promise<unknown>),
  auth: {
    user: { uid: "requester-1" },
    loading: false,
  },
}))

vi.mock("@/hooks/use-api", () => ({
  useApi: () => mocks.api,
}))

vi.mock("@/components/providers/auth-provider", () => ({
  useAuth: () => mocks.auth,
}))

vi.mock("@/hooks/use-credit-balance-events", () => ({
  useCreditBalanceEvents: (sync: () => Promise<unknown>) => {
    mocks.eventSync = sync
  },
}))

function balance(asOf: string, usableBalance: number): CreditBalance {
  return {
    userId: "requester-1",
    totalBalance: 50,
    reservedBalance: 50 - usableBalance,
    usableBalance,
    asOf,
  }
}

describe("useCreditBalance", () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.resetAllMocks()
    mocks.eventSync = null
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it("refreshes immediately after a local mutation and again after the SSE notification", async () => {
    const beforeCancellation = balance("2026-10-09T00:00:00Z", 38)
    const afterCancellation = balance("2026-10-09T00:00:01Z", 50)
    mocks.api
      .mockResolvedValueOnce(beforeCancellation)
      .mockResolvedValueOnce(beforeCancellation)
      .mockResolvedValueOnce(afterCancellation)

    const { result } = renderHook(() => useCreditBalance())

    await act(async () => {
      await vi.advanceTimersByTimeAsync(0)
    })
    expect(result.current.balance).toEqual(beforeCancellation)

    await act(async () => {
      invalidateCreditBalance()
      await Promise.resolve()
    })
    expect(mocks.api).toHaveBeenCalledTimes(2)
    expect(result.current.balance).toEqual(beforeCancellation)

    await act(async () => {
      await mocks.eventSync?.()
    })
    expect(result.current.balance).toEqual(afterCancellation)
    expect(mocks.api).toHaveBeenCalledTimes(3)
  })

  it("does not poll while the page remains open", async () => {
    const beforeCompletion = balance("2026-10-09T00:00:00Z", 38)
    mocks.api.mockResolvedValue(beforeCompletion)

    const { result } = renderHook(() => useCreditBalance())

    await act(async () => {
      await vi.advanceTimersByTimeAsync(0)
    })
    expect(result.current.balance).toEqual(beforeCompletion)

    await act(async () => {
      await vi.advanceTimersByTimeAsync(60_000)
    })
    expect(result.current.balance).toEqual(beforeCompletion)
    expect(result.current.loading).toBe(false)
    expect(mocks.api).toHaveBeenCalledOnce()
  })

  it("keeps the last usable balance when a background synchronization fails", async () => {
    const currentBalance = balance("2026-10-09T00:00:00Z", 44)
    mocks.api
      .mockResolvedValueOnce(currentBalance)
      .mockRejectedValueOnce(new Error("Credit Service unavailable"))

    const { result } = renderHook(() => useCreditBalance())

    await act(async () => {
      await vi.advanceTimersByTimeAsync(0)
      window.dispatchEvent(new Event("focus"))
      await Promise.resolve()
    })

    expect(result.current.balance).toEqual(currentBalance)
    expect(result.current.error).toBe(false)
    expect(result.current.loading).toBe(false)
  })

  it("shares overlapping foreground and background requests without leaving loading stuck", async () => {
    const currentBalance = balance("2026-10-09T00:00:00Z", 44)
    let rejectRequest!: (reason?: unknown) => void
    const failedRequest = new Promise<CreditBalance>((_resolve, reject) => {
      rejectRequest = reject
    })
    mocks.api
      .mockResolvedValueOnce(currentBalance)
      .mockReturnValueOnce(failedRequest)

    const { result } = renderHook(() => useCreditBalance())
    await act(async () => {
      await vi.advanceTimersByTimeAsync(0)
    })

    let refreshRequest!: Promise<CreditBalance | null>
    await act(async () => {
      refreshRequest = result.current.refresh()
      window.dispatchEvent(new Event("focus"))
      await Promise.resolve()
    })
    expect(mocks.api).toHaveBeenCalledTimes(2)

    await act(async () => {
      rejectRequest(new Error("Credit Service unavailable"))
      await refreshRequest
    })
    expect(result.current.error).toBe(true)
    expect(result.current.loading).toBe(false)
  })
})
