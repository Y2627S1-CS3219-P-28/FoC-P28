/*
 * AI Assistance Disclosure:
 * Tool: OpenAI Codex (GPT-5), date: 2026-10-09
 * Mode: Test generation.
 * Scope: Generated tests for credit event to test the provided requirements.
 * Author review: I reviewed for correctness.
 */

import { renderHook } from "@testing-library/react"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

import { useCreditBalanceEvents } from "@/hooks/use-credit-balance-events"

const mocks = vi.hoisted(() => ({
  fetchEventSource: vi.fn(),
  getIdToken: vi.fn(),
  syncBalance: vi.fn(),
  auth: {
    user: { uid: "requester-1" } as { uid: string } | null,
    loading: false,
  },
}))

vi.mock("@microsoft/fetch-event-source", () => ({
  EventStreamContentType: "text/event-stream",
  fetchEventSource: mocks.fetchEventSource,
}))

vi.mock("@/components/providers/auth-provider", () => ({
  useAuth: () => ({ ...mocks.auth, getIdToken: mocks.getIdToken }),
}))

vi.mock("@/components/providers/config-provider", () => ({
  useConfig: () => ({ apiBaseUrl: "https://gateway.example" }),
}))

describe("useCreditBalanceEvents", () => {
  beforeEach(() => {
    vi.resetAllMocks()
    mocks.auth.user = { uid: "requester-1" }
    mocks.auth.loading = false
    mocks.fetchEventSource.mockResolvedValue(undefined)
    mocks.getIdToken.mockResolvedValue("token-1")
    mocks.syncBalance.mockResolvedValue(undefined)
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it("opens an authenticated stream and synchronizes connected and changed events", async () => {
    const fetchSpy = vi.spyOn(window, "fetch").mockResolvedValue(new Response())
    const { rerender } = renderHook(() => useCreditBalanceEvents(mocks.syncBalance))
    const [url, options] = mocks.fetchEventSource.mock.calls[0]

    expect(url).toBe("https://gateway.example/api/credits/events")
    await options.fetch(url, { headers: { Accept: "text/event-stream" } })
    expect(fetchSpy).toHaveBeenCalledWith(url, expect.objectContaining({
      headers: expect.any(Headers),
    }))
    const headers = fetchSpy.mock.calls[0][1]?.headers as Headers
    expect(headers.get("Authorization")).toBe("Bearer token-1")

    mocks.getIdToken.mockResolvedValue("token-2")
    await options.fetch(url, {})
    const retryHeaders = fetchSpy.mock.calls[1][1]?.headers as Headers
    expect(retryHeaders.get("Authorization")).toBe("Bearer token-2")

    options.onmessage({ event: "connected", data: "", id: "", retry: undefined })
    options.onmessage({ event: "balance-changed", data: "{}", id: "", retry: undefined })
    expect(mocks.syncBalance).toHaveBeenCalledTimes(2)

    const signal = options.signal as AbortSignal
    mocks.auth.user = null
    rerender()
    expect(signal.aborted).toBe(true)
  })

  it("uses bounded retry delays and resets them after a valid connection", async () => {
    vi.spyOn(Math, "random").mockReturnValue(0)
    renderHook(() => useCreditBalanceEvents(mocks.syncBalance))
    const options = mocks.fetchEventSource.mock.calls[0][1]

    expect(Array.from({ length: 7 }, () => options.onerror(new Error("offline"))))
      .toEqual([1_000, 2_000, 4_000, 8_000, 16_000, 30_000, 30_000])
    await options.onopen(new Response(null, {
      status: 200,
      headers: { "Content-Type": "text/event-stream;charset=UTF-8" },
    }))
    expect(options.onerror(new Error("offline"))).toBe(1_000)

    await expect(options.onopen(new Response(null, {
      status: 401,
      headers: { "Content-Type": "application/json" },
    }))).rejects.toThrow("HTTP 401")
  })

  it("does not connect before authentication is ready", () => {
    mocks.auth.user = null
    mocks.auth.loading = true

    renderHook(() => useCreditBalanceEvents(mocks.syncBalance))

    expect(mocks.fetchEventSource).not.toHaveBeenCalled()
  })
})
