/*
 * AI Assistance Disclosure:
 * Tool: OpenAI Codex (GPT-5), date: 2026-09-26
 * Mode: Code generation.
 * Scope: Implemented authenticated self-balance loading, focus refresh, and manual refresh behavior.
 * Author review: I have reviewed for correctness.
 */
"use client"

import { useCallback, useEffect, useRef, useState } from "react"

import { useAuth } from "@/components/providers/auth-provider"
import { useApi } from "@/hooks/use-api"

export type CreditBalance = {
  userId: string
  totalBalance: number
  reservedBalance: number
  usableBalance: number
  version: number
  asOf: string
}

type BalanceState = {
  ownerId: string | null
  balance: CreditBalance | null
  error: boolean
  loading: boolean
}

const INITIAL_STATE: BalanceState = {
  ownerId: null,
  balance: null,
  error: false,
  loading: false,
}

export function useCreditBalance() {
  const { user, loading: authLoading } = useAuth()
  const api = useApi()
  const requestVersion = useRef(0)
  const [state, setState] = useState<BalanceState>(INITIAL_STATE)

  const refresh = useCallback(async () => {
    const ownerId = user?.uid
    if (!ownerId) return

    const version = ++requestVersion.current
    setState((current) => ({
      ownerId,
      balance: current.ownerId === ownerId ? current.balance : null,
      error: false,
      loading: true,
    }))

    try {
      const balance = await api<CreditBalance>("/api/credits/me")
      if (requestVersion.current === version) {
        setState({ ownerId, balance, error: false, loading: false })
      }
    } catch {
      if (requestVersion.current === version) {
        setState({ ownerId, balance: null, error: true, loading: false })
      }
    }
  }, [api, user?.uid])

  useEffect(() => {
    if (authLoading || !user) return

    const timer = window.setTimeout(() => void refresh(), 0)
    return () => {
      window.clearTimeout(timer)
      requestVersion.current += 1
    }
  }, [authLoading, refresh, user])

  useEffect(() => {
    if (!user) return

    const refreshOnFocus = () => {
      if (document.visibilityState === "visible") void refresh()
    }
    window.addEventListener("focus", refreshOnFocus)
    return () => window.removeEventListener("focus", refreshOnFocus)
  }, [refresh, user])

  const ownerId = user?.uid ?? null
  const belongsToCurrentUser = state.ownerId === ownerId

  return {
    balance: belongsToCurrentUser ? state.balance : null,
    error: belongsToCurrentUser ? state.error : false,
    loading: authLoading || (ownerId !== null && (!belongsToCurrentUser || state.loading)),
    refresh,
  }
}
