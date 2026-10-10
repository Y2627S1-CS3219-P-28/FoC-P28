"use client"

import { useEffect, useState, useCallback } from "react"
import { useRouter } from "next/navigation"
import { verifyBeforeUpdateEmail, reload, EmailAuthProvider, reauthenticateWithCredential, updatePassword, } from "firebase/auth"

import { useAuth } from "@/components/providers/auth-provider"
import { useConfig } from "@/components/providers/config-provider"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardFooter, CardHeader, CardTitle } from "@/components/ui/card"
import { Field, FieldLabel } from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { DeleteAccountButton } from "@/components/users/delete-account-button"

import { PasswordRequirements } from "../../login/password-requirements"


export default function EditProfilePage() {
    // Profile info comes from auth
    const { user, loading, profile, profileLoading, refreshProfile, getIdToken } = useAuth()
    const config = useConfig()
    const router = useRouter()

    const [saving, setSaving] = useState(false)

    // Username field states
    const [username, setUsername] = useState("")
    const [usernameMessage, setUsernameMessage] = useState("")
    const [usernameError, setUsernameError] = useState<string | null>(null)

    // Email verification states
    const [email, setEmail] = useState("")
    const [emailVerificationSent, setEmailVerificationSent] = useState(false)
    const [emailMessage, setEmailMessage] = useState("")
    const [emailVerifiedMessage, setEmailVerifiedMessage] = useState("")
    const [pendingEmail, setPendingEmail] = useState("")
    const [sendingVerification, setSendingVerification] = useState(false)
    const [emailError, setEmailError] = useState<string | null>(null)

    // Password states
    const [currentPassword, setCurrentPassword] = useState("")
    const [newPassword, setNewPassword] = useState("")
    const [confirmPassword, setConfirmPassword] = useState("")
    const [changingPassword, setChangingPassword] = useState(false)
    const [passwordMessage, setPasswordMessage] = useState("")
    const [passwordError, setPasswordError] = useState<string | null>(null)
    const [passwordValid, setPasswordValid] = useState(true)
    const onPasswordValidity = useCallback((valid: boolean) => setPasswordValid(valid), [])

    // Delete states
    const [deleteError, setDeleteError] = useState("");

    useEffect(() => {
        if (loading || profileLoading) return

        if (!user) {
            router.replace("/login")
            return
        }

        if (!profile) return

        setUsername(profile.username ?? "")
        setEmail(profile.email ?? "")
    }, [loading, user, router])

    async function handleSaveUsername(event: React.FormEvent<HTMLFormElement>) {
        event.preventDefault()
        setUsernameError(null)
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
            setUsernameError(err instanceof Error ? err.message : "Failed to update profile.")
        } finally {
            setSaving(false)
        }
    }

    async function handleCancel() {
        setEmailError(null)
        setUsernameError(null)
        setPasswordError(null)

        setEmailMessage("")
        setEmailVerifiedMessage("")
        setPasswordMessage("")
        setUsernameMessage("")

        setSaving(false)

        router.push("/profile")
    }

    async function handleEmailChange() {
        try {
            if (!user) {
                throw new Error("You must be signed in.")
            }

            setEmailVerifiedMessage("")
            setEmailMessage("")
            setEmailError(null)

            const newEmail = email.trim()

            if (!newEmail) {
                throw new Error("Please enter an email address.")
            }

            if (newEmail.toLowerCase() === (user.email ?? "").toLowerCase()) {
                throw new Error("This is already your current email address.")
            }

            setSendingVerification(true)
            
            await verifyBeforeUpdateEmail(user, newEmail)

            setPendingEmail(newEmail)
            setEmailVerificationSent(true)
            setEmailMessage(
                "Verification email sent. Verify the new address before returning."
            )
        } catch (err) {
            setEmailError(
                err instanceof Error ? err.message : "Failed to send verification email."
            )
        } finally {
            setSendingVerification(false)
        }
    }

    async function handleCheckEmailVerification() {
        if (!user) {
            throw new Error("You must be signed in.")
        }

        if (!pendingEmail) {
            throw new Error("Please request an email verification first.")
        }

        setEmailMessage("")
        setEmailVerifiedMessage("Checking verification status...")
        setEmailError(null)

        try {
            // Refresh the Firebase user's state
            await reload(user)

            const currentEmail = user.email ?? ""

            if (currentEmail.toLowerCase() !== pendingEmail.toLowerCase() || !user.emailVerified) {
                setEmailVerifiedMessage("")
                throw new Error("Your email has not been verified yet. Please check your inbox.")
            }

            // Refresh the shared profile.
            await refreshProfile()

            setEmail(currentEmail)
            setPendingEmail("")
            setEmailVerificationSent(false)
            setEmailVerifiedMessage("Your email has been updated and saved successfully.")
        } catch (err) {
            setEmailError(
                err instanceof Error ? err.message : "Failed to check email verification."
            )
        }
    }

    async function handleChangePassword(event: React.FormEvent<HTMLFormElement>) {
        event.preventDefault() 
        setPasswordMessage("") 
        setPasswordError("")

        // Check user's email to not be null here
        // as EmailAuthProvider uses user.email
        if (!user || !user.email) { 
            setPasswordError("You must be signed in to change your password.") 
            return 
        }

        if (!currentPassword) { 
            setPasswordError("Please fill in your current password.") 
            return 
        }

        if (!newPassword) { 
            setPasswordError("Please enter a new password to change to.") 
            return 
        }

        if (!confirmPassword) { 
            setPasswordError("Please confirm your new password.") 
            return 
        }

        if (newPassword !== confirmPassword) { 
            setPasswordError("The new passwords do not match.") 
            return 
        } 
        
        if (currentPassword === newPassword) { 
            setPasswordError("Your new password must be different.") 
            return 
        }

        setChangingPassword(true)

        try { 
            // Verify the current password
            const credential = EmailAuthProvider.credential(user.email, currentPassword) 
            await reauthenticateWithCredential(user, credential) 

            // Update the password in Firebase Authentication
            await updatePassword(user, newPassword)
            setCurrentPassword("") 
            setNewPassword("") 
            setConfirmPassword("") 
            setPasswordMessage("Password changed successfully.") 
        } catch (error: unknown) { 
            // Display the corresponding error message to user
            const code = 
                typeof error === "object" && 
                error !== null && 
                "code" in error ? String(error.code) : "" 
                
            switch (code) { 
                case "auth/invalid-credential": 
                case "auth/wrong-password": 
                    setPasswordError("Your current password is incorrect.") 
                    break 
                case "auth/weak-password": 
                    setPasswordError("Please choose a stronger password.") 
                    break
                case "auth/requires-recent-login": 
                    setPasswordError("Please sign in again before changing your password.") 
                    break 
                case "auth/too-many-requests": 
                    setPasswordError("Too many attempts. Please wait before trying again.") 
                    break 
                default: 
                    console.error("Password change failed:", error) 
                    setPasswordError( "Unable to change your password. Please try again." ) 
            } 
        } finally { 
            setChangingPassword(false)
        }
    }

    function handleDeleteError(message: string) {
        console.log("Error received:", message)
        setDeleteError(message);
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
            <div className="w-full pt-4">
                <form id="edit-email-form" className="space-y-4">
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
                        <Button 
                            type="button" 
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
                    {emailError && (
                        <p className="text-sm text-destructive" role="status">
                            {emailError}
                        </p>
                    )}
                    {emailMessage && (
                        <p className="text-sm text-muted-foreground" role="status">
                            {emailMessage}
                        </p>
                    )}
                    {emailVerifiedMessage && (
                        <p className="text-sm text-green-600" role="status">
                            {emailVerifiedMessage}
                        </p>
                    )}
                    {emailVerificationSent && (
                        <div className="space-y-2">
                            <Button 
                                type="button"
                                variant="outline"
                                onClick={handleCheckEmailVerification}
                            >
                                I've verified my email
                            </Button>
                        </div>
                    )}
                </form>
            </div>
            <div className="w-full pt-4">
                <form id="edit-username-form" onSubmit={handleSaveUsername} className="space-y-4">
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
                    {usernameError && ( 
                        <p className="text-sm text-destructive" role="status"> 
                            {usernameError}
                        </p> 
                    )}
                    {usernameMessage && ( 
                        <p className="text-sm text-green-600" role="status"> 
                            {usernameMessage}
                        </p> 
                    )}
                    <Button
                        type="submit"
                        form="edit-username-form"
                        className="w-fit"
                        disabled={saving}
                    >
                        {saving ? "Saving..." : "Save username"}
                    </Button>
                </form>
            </div>
            <div className="w-full pt-4">
                <form id="edit-password-form" onSubmit={handleChangePassword} className="space-y-4">
                    <Field>
                    <FieldLabel htmlFor="currentPassword">Current password</FieldLabel>
                        <Input
                            id="currentPassword"
                            type="password"
                            autoComplete="currentPassword"
                            value={currentPassword}
                            onChange={(event) => setCurrentPassword(event.target.value)}
                            disabled={changingPassword}
                            required
                        />
                    </Field> 
                    <Field>
                        <FieldLabel htmlFor="newPassword">New password</FieldLabel>
                        <Input
                            id="newPassword"
                            type="password"
                            autoComplete="newPassword"
                            value={newPassword}
                            onChange={(event) => setNewPassword(event.target.value)}
                            disabled={changingPassword}
                            required
                        />
                        {<PasswordRequirements password={newPassword} onValidityChange={onPasswordValidity} />}
                    </Field>
                    <Field>
                        <FieldLabel htmlFor="confirmPassword">Confirm new password</FieldLabel>
                        <Input
                            id="confirmPassword"
                            type="password"
                            autoComplete="confirmPassword"
                            value={confirmPassword}
                            onChange={(event) => setConfirmPassword(event.target.value)}
                            disabled={changingPassword}
                            required
                        />
                        {<PasswordRequirements password={newPassword} onValidityChange={onPasswordValidity} />}
                    </Field>
                    {passwordError && (
                        <p className="text-sm text-destructive" role="alert">
                            {passwordError}
                        </p>
                    )} 
                    {passwordMessage && ( 
                        <p className="text-sm text-green-600" role="status"> 
                            {passwordMessage} 
                        </p> 
                    )} 
                    <Button type="submit" disabled={changingPassword}> 
                        {changingPassword ? "Changing Password..." : "Change Password"} 
                    </Button>
                </form>
            </div>
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
            <div className="w-full pt-4">
                {deleteError && (
                    <p className="text-sm text-destructive" role="alert">
                        {deleteError}
                    </p>
                )}
            </div>
            <div className="w-full pt-4">
                <DeleteAccountButton onError={handleDeleteError}/>
            </div>
          </div>
        </CardFooter>
      </Card>
    </div>
  )
}