"use client"

import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react"
import {
  createUserWithEmailAndPassword,
  sendEmailVerification,
  onIdTokenChanged,
  signInWithEmailAndPassword,
  signOut as firebaseSignOut,
  type User,
} from "firebase/auth"

import { useConfig } from "@/components/providers/config-provider"
import { getFirebaseAuth } from "@/lib/firebase"
import { buildRegistrationFactPayload } from "@/lib/registration"

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
      const auth = getFirebaseAuth(config);
      const userCredential = await signInWithEmailAndPassword(auth, email, password);

      const user = userCredential.user;

      console.log(user)

      // User can only sign in after verifying the email through link
      if (user && user.emailVerified) {
        console.log(`${user.email} is verified`)
        console.log(`${user.email} redirected to home page.`)
      } else {
        await firebaseSignOut(auth);

        console.log(`${user.email} is not verified`)
        throw new Error("EMAIL_NOT_VERIFIED");
      }
    },
    [config],
  )

  const signUp = useCallback(
    async (email: string, password: string) => {
        let user = null;

        try {
            const auth = getFirebaseAuth(config);
            const userCredential = await createUserWithEmailAndPassword(
                auth,
                email, 
                password
            );
            
            // Signed up 
            user = userCredential.user;
            const idToken = await user.getIdToken();
                    
            // User Firebase provided uid as our user ID
            const response = await fetch(`${config.apiBaseUrl}/api/users`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    Authorization: `Bearer ${idToken}`,
                },
                body: JSON.stringify({
                    userId: user.uid,
                    email: user.email
                })
            });

            // Checks if API succeeded
            if (!response.ok) {
                const data = await response.json();

                console.log("API Error: ", data.message || "Unknown error")
                throw new Error(data.message || "Failed to create user");
            }

            const creditResponse = await fetch(`${config.apiBaseUrl}/api/credits/registration-facts`, {
                method: 'POST',
                headers: {
                    'Accept': 'application/json',
                    'Content-Type': 'application/json',
                    Authorization: `Bearer ${idToken}`,
                },
                body: JSON.stringify(buildRegistrationFactPayload(user.uid, crypto.randomUUID(), new Date().toISOString())),
            });

            if (!creditResponse.ok) {
                const data = await creditResponse.json().catch(() => null) as { message?: string } | null;
                throw new Error(data?.message || "Failed to initialize the credit account");
            }

            await sendEmailVerification(user);
        
            console.log("Verification email sent");

            await firebaseSignOut(auth);
        } catch (error) {
            // Error happened
            // Did not go through, delete firebase acc
            if (user) {
                console.log(`Deleting firebase user ${user.email}`);
                await user.delete();
            }

            // Log it in console
            console.error(error);

            // Continue throwing the error up
            if (error instanceof Error) {
              throw error;
            }

            throw new Error("Something went wrong during sign up.");
        }
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
