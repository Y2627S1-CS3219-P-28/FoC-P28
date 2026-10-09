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
import { useCreditBalanceEvents } from "@/hooks/use-credit-balance-events"
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

  useCreditBalanceEvents(sync)

  useEffect(() => {
    if (authLoading || !user) return

    const timer = window.setTimeout(() => void refresh(), 0)
    return () => window.clearTimeout(timer)
  }, [authLoading, refresh, user])

  useEffect(() => {
    if (!user) return

    const syncOnFocus = () => {
      if (document.visibilityState === "visible") void sync()
    }
    window.addEventListener("focus", syncOnFocus)
    return () => window.removeEventListener("focus", syncOnFocus)
  }, [sync, user])

  useEffect(() => {
    if (!user) return

    const refreshAfterMutation = () => void refresh()

    window.addEventListener(CREDIT_BALANCE_INVALIDATED_EVENT, refreshAfterMutation)
    return () => window.removeEventListener(CREDIT_BALANCE_INVALIDATED_EVENT, refreshAfterMutation)
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
