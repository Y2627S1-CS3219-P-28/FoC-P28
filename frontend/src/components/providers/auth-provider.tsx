"use client"

import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react"
import {
  createUserWithEmailAndPassword,
  onIdTokenChanged,
  signInWithEmailAndPassword,
  signOut as firebaseSignOut,
  type User,
} from "firebase/auth"

import { useConfig } from "@/components/providers/config-provider"
import { getFirebaseAuth } from "@/lib/firebase"

type AuthContextValue = {
  user: User | null
  loading: boolean
  signIn: (email: string, password: string) => Promise<void>
  // Temporary: creates the Firebase account only. The User Service owns registration
  // (profile, roles, NUS-domain checks) and should replace this once its API exists.
  signUp: (email: string, password: string) => Promise<void>
  signOut: () => Promise<void>
  getIdToken: () => Promise<string | null>
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const config = useConfig()
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const auth = getFirebaseAuth(config)
    return onIdTokenChanged(auth, (next) => {
      setUser(next)
      setLoading(false)
    })
  }, [config])

  const signIn = useCallback(
    async (email: string, password: string) => {
      await signInWithEmailAndPassword(getFirebaseAuth(config), email, password)
    },
    [config],
  )

  const signUp = useCallback(
    async (email: string, password: string) => {
      await createUserWithEmailAndPassword(getFirebaseAuth(config), email, password)
    },
    [config],
  )

  const signOut = useCallback(() => firebaseSignOut(getFirebaseAuth(config)), [config])

  // Firebase refreshes the ID token automatically when it is close to expiry.
  const getIdToken = useCallback(async () => {
    const current = getFirebaseAuth(config).currentUser
    return current ? current.getIdToken() : null
  }, [config])

  const value = useMemo(
    () => ({ user, loading, signIn, signUp, signOut, getIdToken }),
    [user, loading, signIn, signUp, signOut, getIdToken],
  )
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const value = useContext(AuthContext)
  if (!value) throw new Error("useAuth must be used inside <AuthProvider>")
  return value
}
