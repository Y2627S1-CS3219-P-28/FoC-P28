"use client"

import { useEffect, useState } from "react"
import { useRouter } from "next/navigation"

import { useAuth } from "@/components/providers/auth-provider"
import { useConfig } from "@/components/providers/config-provider"

import { Alert, AlertDescription } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card"
import { Field, FieldGroup, FieldLabel } from "@/components/ui/field"
import { Input } from "@/components/ui/input"


type UserProfile = {
  email: string
  username: string
  roles: string[]
  penalty: number
  isCourierSuspended: boolean
}

export default function ProfilePage() {
  const { user, loading, getIdToken } = useAuth()
  const config = useConfig()
  const router = useRouter()

  const [profile, setProfile] = useState<UserProfile | null>(null)
  const [profileLoading, setProfileLoading] = useState(true)

  useEffect(() => {
    if (loading) return

    if (!user) {
      router.replace("/login")
      return
    }

    async function loadProfile() {
      try {
        const token = await getIdToken()
        
        // Debug code for testing token-related APIs
        // Remove before actual
        // console.log(token)

        const response = await fetch(
          `${config.apiBaseUrl}/api/users/me`,
          {
            headers: {
              Authorization: `Bearer ${token}`,
            },
          }
        )

        if (!response.ok) {
          throw new Error("Failed to load profile")
        }

        const data = await response.json()
        setProfile(data)
      } catch (error) {
        console.error(error)
      } finally {
        setProfileLoading(false)
      }
    }

    loadProfile()
  }, [loading, user, getIdToken, config, router])

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
          <p>Email: {profile.email}</p>
          <p>Username: {profile.username}</p>
          <p>Roles: {profile.roles.join(", ")}</p>
          <p>Penalty: {profile.penalty}</p>

          <p>
            Courier status:{" "}
            {profile.isCourierSuspended ? "Suspended" : "Active"}
          </p>
        </CardContent>
      </Card>
    </div>
  )
}