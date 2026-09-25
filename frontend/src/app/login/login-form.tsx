"use client"

import { useRouter, useSearchParams } from "next/navigation"
import { useEffect, useState } from "react"
import { FirebaseError } from "firebase/app"

import { useAuth } from "@/components/providers/auth-provider"
import { Alert, AlertDescription } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card"
import { Field, FieldGroup, FieldLabel } from "@/components/ui/field"
import { Input } from "@/components/ui/input"

type Mode = "sign-in" | "sign-up"

const FIREBASE_MESSAGES: Record<string, string> = {
  "auth/invalid-credential": "Incorrect email or password.",
  "auth/invalid-email": "Enter a valid email address.",
  "auth/email-already-in-use": "An account with this email already exists.",
  "auth/weak-password": "Password must be at least 6 characters.",
  "auth/too-many-requests": "Too many attempts. Please wait a moment and try again.",
  "auth/network-request-failed": "Could not reach the sign-in service. Check your connection.",
}

function describeError(error: unknown): string {
  if (error instanceof FirebaseError) return FIREBASE_MESSAGES[error.code] ?? "Sign-in failed. Please try again."
  return "Something went wrong. Please try again."
}

// Only allow same-site relative redirects after sign-in.
function safeNext(next: string | null): string {
  return next && next.startsWith("/") && !next.startsWith("//") ? next : "/"
}

export function LoginForm() {
  const { user, loading, signIn, signUp } = useAuth()
  const router = useRouter()
  const next = safeNext(useSearchParams().get("next"))

  const [mode, setMode] = useState<Mode>("sign-in")
  const [email, setEmail] = useState("")
  const [password, setPassword] = useState("")
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    if (!loading && user) router.replace(next)
  }, [loading, user, router, next])

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      if (mode === "sign-in") await signIn(email.trim(), password)
      else await signUp(email.trim(), password)
    } catch (err) {
      setError(describeError(err))
    } finally {
      setSubmitting(false)
    }
  }

  const isSignIn = mode === "sign-in"

  return (
    <Card className="w-full max-w-sm">
      <CardHeader>
        <CardTitle className="text-xl">{isSignIn ? "Sign in" : "Create an account"}</CardTitle>
        <CardDescription>
          {isSignIn ? "Use your campus email to continue." : "Sign up to request and deliver campus errands."}
        </CardDescription>
      </CardHeader>
      <form onSubmit={handleSubmit} noValidate>
        <CardContent>
          <FieldGroup>
            {error && (
              <Alert variant="destructive">
                <AlertDescription>{error}</AlertDescription>
              </Alert>
            )}
            <Field>
              <FieldLabel htmlFor="email">Email</FieldLabel>
              <Input
                id="email"
                type="email"
                autoComplete="email"
                required
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                placeholder="e0123456@u.nus.edu"
              />
            </Field>
            <Field>
              <FieldLabel htmlFor="password">Password</FieldLabel>
              <Input
                id="password"
                type="password"
                autoComplete={isSignIn ? "current-password" : "new-password"}
                required
                value={password}
                onChange={(event) => setPassword(event.target.value)}
              />
            </Field>
          </FieldGroup>
        </CardContent>
        <CardFooter className="mt-6 flex flex-col gap-3">
          <Button type="submit" className="w-full" disabled={submitting || !email || !password}>
            {submitting ? "Please wait…" : isSignIn ? "Sign in" : "Create account"}
          </Button>
          <Button
            type="button"
            variant="link"
            size="sm"
            onClick={() => {
              setMode(isSignIn ? "sign-up" : "sign-in")
              setError(null)
            }}
          >
            {isSignIn ? "New here? Create an account" : "Already have an account? Sign in"}
          </Button>
        </CardFooter>
      </form>
    </Card>
  )
}
