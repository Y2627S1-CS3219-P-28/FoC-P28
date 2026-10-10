"use client"

import { useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import { verifyBeforeUpdateEmail, reload } from "firebase/auth"

import { useAuth } from "@/components/providers/auth-provider"
import { useConfig } from "@/components/providers/config-provider"

import { Alert, AlertDescription } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardFooter, CardHeader, CardTitle } from "@/components/ui/card"
import { Field, FieldLabel } from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { DeleteAccountButton } from "@/components/profile/DeleteAccountButton"


export default function EditProfilePage() {
    // Profile info comes from auth
    const { user, loading, profile, profileLoading, refreshProfile, getIdToken } = useAuth()
    const config = useConfig()
    const router = useRouter()

    const [saving, setSaving] = useState(false)
    const [usernameMessage, setUsernameMessage] = useState("")

    // Regular field states
    const [username, setUsername] = useState("")
    const [email, setEmail] = useState("")
    const [password, setPassword] = useState("")
    const [error, setError] = useState<string | null>(null)

    // Email verification states
    const [emailVerificationSent, setEmailVerificationSent] = useState(false)
    const [emailMessage, setEmailMessage] = useState("")
    const [pendingEmail, setPendingEmail] = useState("")
    const [sendingVerification, setSendingVerification] = useState(false)

    useEffect(() => {
        if (loading) return

        if (!profile) return

        if (!user) {
            router.replace("/login")
            return
        }

        setUsername(profile.username ?? "")
        setEmail(profile.email ?? "")
    }, [loading, user, router])

    async function handleSave(event: React.FormEvent<HTMLFormElement>) {
        event.preventDefault()
        setError(null)
        setSaving(true)
        setUsernameMessage("")

        try {
            if (!user) {
                throw new Error("You must be signed in.")
            }

            // Handles username change
            const token = await getIdToken()

            if (!token) {
                throw new Error("You must be signed in.")
            }

            const response = await fetch(`${config.apiBaseUrl}/api/users/me`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify({
                    username: username.trim(), 
                }),
            });

            if (!response.ok) { 
                const message = await response.text() 
                throw new Error(message || "Failed to update username.") 
            }

            // Reload the shared profile from the backend.
            await refreshProfile()
            
            setUsernameMessage("Username updated successfully.")
        } catch (err) {
            setError(err instanceof Error ? err.message : "Failed to update profile.")
        } finally {
            setSaving(false)
        }
    }

    async function handleCancel() {
        setError(null)
        setSaving(false)
        setUsernameMessage("")

        router.push("/profile")
    }

    async function handleEmailChange() {
        if (!user) {
            setEmailMessage("You must be signed in.")
            return
        }

        setEmailMessage("")
        setError(null)

        const newEmail = email.trim()

        if (!newEmail) {
            setEmailMessage("Please enter an email address.")
            return
        }

        if (
            newEmail.toLowerCase() ===
            (user.email ?? "").toLowerCase()
        ) {
            setEmailMessage("This is already your current email address.")
            return
        }

        setSendingVerification(true)

        try {
            await verifyBeforeUpdateEmail(user, newEmail)

            setPendingEmail(newEmail)
            setEmailVerificationSent(true)
            setEmailMessage(
                "Verification email sent. Verify the new address before returning."
            )
        } catch (err) {
            setEmailMessage(
                err instanceof Error ? err.message : "Failed to send verification email."
            )
        } finally {
            setSendingVerification(false)
        }
    }

    async function handleCheckEmailVerification() {
        if (!user) {
            setEmailMessage("You must be signed in.")
            return
        }

        if (!pendingEmail) {
            setEmailMessage("Please request an email verification first.")
            return
        }

        setEmailMessage("Checking verification status...")
        setError(null)

        try {
            // Refresh the Firebase user's state
            await reload(user)

            const currentEmail = user.email ?? ""

            if (currentEmail.toLowerCase() !== pendingEmail.toLowerCase() || !user.emailVerified) {
                setEmailMessage(
                    "Your email has not been verified yet. Please check your inbox."
                )
                return
            }

            // Refresh the shared profile.
            // loadUserProfile() handles syncing the verified email to MongoDB.
            await refreshProfile()

            setEmail(currentEmail)
            setPendingEmail("")
            setEmailVerificationSent(false)
            setEmailMessage("Your email has been updated and saved successfully.")
        } catch (err) {
            setEmailMessage(
                err instanceof Error ? err.message : "Failed to check email verification."
            )
        }
    }

  if (loading || profileLoading) {
    return <div className="flex min-h-screen items-center justify-center">
              <Card className="w-full max-w-sm">
                <CardHeader>
                  <CardTitle className="text-xl">{"Loading..."}</CardTitle>
                </CardHeader>
              </Card>
            </div>
  }

  if (!profile) {
    return <div className="flex min-h-screen items-center justify-center">
              <Card className="w-full max-w-sm">
                <CardHeader>
                  <CardTitle className="text-xl">{"Unable to load profile."}</CardTitle>
                </CardHeader>
              </Card>
            </div>
  }

  return (
    <div className="flex min-h-screen items-center justify-center">
      <Card className="w-full max-w-sm">
        <CardHeader>
          <CardTitle className="text-xl">{"My Profile"}</CardTitle>
        </CardHeader>

        <CardContent>
            {error && ( 
                <Alert variant="destructive"> 
                    <AlertDescription>{error}</AlertDescription> 
                </Alert> 
            )} 

            <form id="edit-profile-form" onSubmit={handleSave} className="space-y-4">
                <Field>
                    <FieldLabel htmlFor="email">Email</FieldLabel>
                    <Input
                        id="email"
                        type="email"
                        autoComplete="email"
                        value={email}
                        onChange={(event) => setEmail(event.target.value)}
                        placeholder="Enter an email here."
                    />
                </Field>
                <div className="space-y-2">
                    <Button type="button" 
                        disabled={!email.trim() || sendingVerification}
                        onClick={handleEmailChange}
                        >    
                        {sendingVerification 
                            ? "Sending..."
                            : emailVerificationSent 
                                ? "Resend verification email" 
                                : "Send verification email"}
                    </Button>
                </div>
                {emailMessage && (
                    <p className="text-sm text-muted-foreground" role="status">
                        {emailMessage}
                    </p>
                )}
                {emailVerificationSent && (
                    <div className="space-y-2">
                        <Button type="button" onClick={handleCheckEmailVerification}>
                            I've verified my email
                        </Button>
                    </div>
                )}
                <Field>
                <FieldLabel htmlFor="username">Username</FieldLabel>
                    <Input
                        id="username"
                        type="text"
                        autoComplete="username"
                        required
                        minLength={3} 
                        maxLength={30}
                        value={username}
                        onChange={(event) => setUsername(event.target.value)}
                        placeholder="Enter a username here."
                    />
                </Field>
                <Button
                        type="submit"
                        form="edit-profile-form"
                        className="w-fit"
                        disabled={saving}
                    >
                        {saving ? "Saving..." : "Save username"}
                </Button>
                {usernameMessage && ( 
                    <p className="text-sm text-muted-foreground" role="status"> 
                        {usernameMessage}
                    </p> 
                )} 
                <Field>
                <FieldLabel htmlFor="password">Password</FieldLabel>
                    <Input
                        id="password"
                        type="password"
                        autoComplete="password"
                        value={password}
                        onChange={(event) => setPassword(event.target.value)}
                        placeholder="Not implemented yet."
                    />
                </Field>
            </form>
        </CardContent>

        <CardFooter className="flex flex-col items-stretch gap-3">
            <p className="mt-1 mb-3 text-sm text-muted-foreground">
                Unsaved changes will be discarded.
            </p>
            <Button
                  type="button"
                  variant="outline"
                  className="w-fit"
                  disabled={saving}
                  onClick={handleCancel}
                >
                  Exit
            </Button>
          <div className="w-full border-t pt-4">
            <p className="mt-1 mb-3 text-sm text-muted-foreground">
                Permanently delete your account and its associated profile.
                This action cannot be undone.
            </p>
            <DeleteAccountButton />
          </div>
        </CardFooter>
      </Card>
    </div>
  )
}