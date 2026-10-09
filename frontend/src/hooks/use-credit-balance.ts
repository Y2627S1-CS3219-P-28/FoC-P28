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
import { CREDIT_BALANCE_INVALIDATED_EVENT } from "@/lib/credit-balance-events"

export type CreditBalance = {
  userId: string
  totalBalance: number
  reservedBalance: number
  usableBalance: number
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

// Order outcomes reach Credit Service asynchronously. Retry an invalidated balance until
// its returned snapshot changes so a fast, stale first read does not leave the UI outdated.
const INVALIDATION_RETRY_DELAYS_MS = [0, 500, 1_000, 2_000, 4_000, 8_000, 15_000] as const
const BALANCE_SYNC_INTERVAL_MS = 5_000

function balancesMatch(left: CreditBalance | null, right: CreditBalance) {
  return left !== null
    && left.userId === right.userId
    && left.totalBalance === right.totalBalance
    && left.reservedBalance === right.reservedBalance
    && left.usableBalance === right.usableBalance
    && left.asOf === right.asOf
}

export function useCreditBalance() {
  const { user, loading: authLoading } = useAuth()
  const api = useApi()
  const activeOwnerId = useRef<string | null>(null)
  const inFlight = useRef<{ ownerId: string; request: Promise<CreditBalance> } | null>(null)
  const latestBalance = useRef<{ ownerId: string; balance: CreditBalance } | null>(null)
  const [state, setState] = useState<BalanceState>(INITIAL_STATE)

  useEffect(() => {
    const ownerId = user?.uid ?? null
    activeOwnerId.current = ownerId
    return () => {
      if (activeOwnerId.current === ownerId) activeOwnerId.current = null
    }
  }, [user?.uid])

  const loadBalance = useCallback(async (background: boolean) => {
    const ownerId = user?.uid
    if (!ownerId) return null

    if (!background) {
      setState((current) => ({
        ownerId,
        balance: current.ownerId === ownerId ? current.balance : null,
        error: false,
        loading: true,
      }))
    }

    let request = inFlight.current?.ownerId === ownerId ? inFlight.current.request : null
    if (!request) {
      request = api<CreditBalance>("/api/credits/me")
      inFlight.current = { ownerId, request }
    }

    try {
      const balance = await request
      if (activeOwnerId.current !== ownerId) return null

      latestBalance.current = { ownerId, balance }
      setState((current) => {
        const unchanged = balancesMatch(current.balance, balance)
          && current.ownerId === ownerId && !current.error && !current.loading
        return unchanged ? current : { ownerId, balance, error: false, loading: false }
      })
      return balance
    } catch {
      if (!background && activeOwnerId.current === ownerId) {
        setState({ ownerId, balance: null, error: true, loading: false })
      }
      return null
    } finally {
      if (inFlight.current?.request === request) inFlight.current = null
    }
  }, [api, user?.uid])

  const refresh = useCallback(() => loadBalance(false), [loadBalance])
  const sync = useCallback(() => loadBalance(true), [loadBalance])

  useEffect(() => {
    if (authLoading || !user) return

    const timer = window.setTimeout(() => void refresh(), 0)
    return () => window.clearTimeout(timer)
  }, [authLoading, refresh, user])

  useEffect(() => {
    if (!user) return

    const syncWhenVisible = () => {
      if (document.visibilityState === "visible") void sync()
    }
    const timer = window.setInterval(syncWhenVisible, BALANCE_SYNC_INTERVAL_MS)
    window.addEventListener("focus", syncWhenVisible)
    document.addEventListener("visibilitychange", syncWhenVisible)
    return () => {
      window.clearInterval(timer)
      window.removeEventListener("focus", syncWhenVisible)
      document.removeEventListener("visibilitychange", syncWhenVisible)
    }
  }, [sync, user])

  useEffect(() => {
    if (!user) return

    let invalidationVersion = 0
    const retryTimers = new Set<number>()

    const refreshAfterMutation = () => {
      retryTimers.forEach((timer) => window.clearTimeout(timer))
      retryTimers.clear()
      const currentInvalidation = ++invalidationVersion
      const cached = latestBalance.current
      const baseline = cached?.ownerId === user.uid ? cached.balance : null

      const refreshUntilChanged = async (attempt: number) => {
        if (currentInvalidation !== invalidationVersion) return

        const balance = await refresh()
        if (currentInvalidation !== invalidationVersion) return
        if (balance && (baseline === null || !balancesMatch(baseline, balance))) return

        const nextAttempt = attempt + 1
        if (nextAttempt >= INVALIDATION_RETRY_DELAYS_MS.length) return

        const timer = window.setTimeout(() => {
          retryTimers.delete(timer)
          void refreshUntilChanged(nextAttempt)
        }, INVALIDATION_RETRY_DELAYS_MS[nextAttempt])
        retryTimers.add(timer)
      }

      void refreshUntilChanged(0)
    }

    window.addEventListener(CREDIT_BALANCE_INVALIDATED_EVENT, refreshAfterMutation)
    return () => {
      invalidationVersion += 1
      retryTimers.forEach((timer) => window.clearTimeout(timer))
      retryTimers.clear()
      window.removeEventListener(CREDIT_BALANCE_INVALIDATED_EVENT, refreshAfterMutation)
    }
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
