/*
 * AI Assistance Disclosure:
 * Tool: OpenAI Codex (GPT-5), date: 2026-10-09
 * Mode: Test generation.
 * Scope: Generated tests for credit event to test the provided requirements.
 * Author review: I reviewed for correctness.
 */


"use client"

import { useEffect } from "react"
import { EventStreamContentType, fetchEventSource } from "@microsoft/fetch-event-source"

import { useAuth } from "@/components/providers/auth-provider"
import { useConfig } from "@/components/providers/config-provider"
import { requireBearerToken } from "@/lib/api"

const RETRY_DELAYS_MS = [1_000, 2_000, 4_000, 8_000, 16_000, 30_000] as const

export function useCreditBalanceEvents(syncBalance: () => Promise<unknown>) {
  const { getIdToken, loading: authLoading, user } = useAuth()
  const { apiBaseUrl } = useConfig()
  const userId = user?.uid

  useEffect(() => {
    if (authLoading || !userId) return

    const controller = new AbortController()
    let retryAttempt = 0

    void fetchEventSource(`${apiBaseUrl}/api/credits/events`, {
      signal: controller.signal,
      openWhenHidden: false,
      fetch: async (input, init) => {
        const token = requireBearerToken(await getIdToken())
        const headers = new Headers(init?.headers)
        headers.set("Authorization", `Bearer ${token}`)
        return window.fetch(input, { ...init, headers })
      },
      async onopen(response) {
        const contentType = response.headers.get("content-type")
        if (!response.ok || !contentType?.startsWith(EventStreamContentType)) {
          throw new Error(`Credit event stream returned HTTP ${response.status}.`)
        }
        retryAttempt = 0
      },
      onmessage(event) {
        if (event.event === "connected" || event.event === "balance-changed") {
          void syncBalance()
        }
      },
      onclose() {
        throw new Error("Credit event stream closed.")
      },
      onerror() {
        const delay = RETRY_DELAYS_MS[Math.min(retryAttempt, RETRY_DELAYS_MS.length - 1)]
        retryAttempt += 1
        return delay + Math.floor(Math.random() * 251)
      },
    }).catch(() => {
      // Aborting on sign-out or unmount is the expected terminal state.
    })

    return () => controller.abort()
  }, [apiBaseUrl, authLoading, getIdToken, syncBalance, userId])
}
