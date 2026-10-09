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

type AuthContextValue = {
  user: User | null
  loading: boolean
  signIn: (email: string, password: string) => Promise<void>
  signUp: (email: string, password: string) => Promise<void>
  resendVerificationEmail: (email: string, password: string) => Promise<void>
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
      setUser(next?.emailVerified ? next : null)
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
            const token = await user.getIdToken();
                    
            // Use Firebase provided uid as our user ID
            const response = await fetch(`${config.apiBaseUrl}/api/users`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                }
            });

            // Checks if API succeeded
            if (!response.ok) {
                const data = await response.json();

                console.log("API Error: ", data.message || "Unknown error")
                throw new Error(data.message || "Failed to create user");
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

  const resendVerificationEmail = useCallback(async (email: string, password: string) => {
      const auth = getFirebaseAuth(config)

      // Sign in temporarily so Firebase can send the verification email.
      const credential = await signInWithEmailAndPassword(
        auth,
        email,
        password,
      )

      try {
        if (credential.user.emailVerified) {
          throw new Error("EMAIL_ALREADY_VERIFIED")
        }

        await sendEmailVerification(credential.user)
      } finally {
        // Sign out unverified user
        await firebaseSignOut(auth)
      }
    },
    [config]
  )

  const value = useMemo(
    () => ({ user, loading, signIn, signUp, signOut, getIdToken, resendVerificationEmail }),
    [user, loading, signIn, signUp, signOut, getIdToken, resendVerificationEmail],
  )
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const value = useContext(AuthContext)
  if (!value) throw new Error("useAuth must be used inside <AuthProvider>")
  return value
}
