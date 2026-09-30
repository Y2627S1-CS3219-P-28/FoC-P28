"use client"

import { createContext, useContext, useEffect, useMemo, useState } from "react"

import type { OrderMode } from "@/lib/orders"

type OrderModeContextValue = {
  mode: OrderMode
  setMode: (mode: OrderMode) => void
}

const STORAGE_KEY = "foc.order-mode"
const OrderModeContext = createContext<OrderModeContextValue | null>(null)

export function OrderModeProvider({ children }: { children: React.ReactNode }) {
  const [mode, setModeState] = useState<OrderMode>("requester")

  useEffect(() => {
    const saved = window.localStorage.getItem(STORAGE_KEY)
    // The persisted mode is external browser state; hydrate it after the server render.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    if (saved === "requester" || saved === "courier") setModeState(saved)
  }, [])

  const value = useMemo<OrderModeContextValue>(
    () => ({
      mode,
      setMode: (next) => {
        setModeState(next)
        window.localStorage.setItem(STORAGE_KEY, next)
      },
    }),
    [mode],
  )

  return <OrderModeContext.Provider value={value}>{children}</OrderModeContext.Provider>
}

export function useOrderMode(): OrderModeContextValue {
  const value = useContext(OrderModeContext)
  if (!value) throw new Error("useOrderMode must be used inside <OrderModeProvider>")
  return value
}
