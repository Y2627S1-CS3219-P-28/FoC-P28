"use client"

import { useEffect, useState } from "react"
import { useRouter } from "next/navigation"

import { useAuth } from "@/components/providers/auth-provider"
import { useConfig } from "@/components/providers/config-provider"


import { Button } from "@/components/ui/button"
import { Card, CardHeader, CardTitle } from "@/components/ui/card"

export default function ProfilePage() {
  const { user, loading, profile, profileLoading, refreshProfile, getIdToken } = useAuth()
  const config = useConfig()
  const router = useRouter()

  const [error, setError] = useState<string | null>(null)
  const [editing, setEditing] = useState(false)

  useEffect(() => {
    if (!loading && !user) {
      router.replace("/login")
    }
  }, [loading, user, getIdToken, config, router])

  if (loading || profileLoading) {
    return (
      <div className="p-6">
        <p>Loading profile...</p>
        <pre className="mt-4 text-sm">
          {JSON.stringify(
            {
              loading,
              profileLoading,
              hasUser: !!user,
              hasProfile: !!profile,
            },
            null,
            2
          )}
        </pre>
      </div>
    )
  }

  //if (loading || profileLoading) {
    //return <div className="flex min-h-screen items-center justify-center">
              //<Card className="w-full max-w-sm">
                //<CardHeader>
                  //<CardTitle className="text-xl">{"Loading..."}</CardTitle>
                //</CardHeader>
              //</Card>
            //</div>
  //}

  if (!user) { 
    return null 
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
    <div className="mx-auto flex w-full max-w-6xl flex-col gap-5 px-4 py-6 sm:py-8">
        <div className="flex flex-col gap-1">
          <h1 className="text-2xl font-semibold tracking-tight">My Profile</h1>
        </div>
        <div className="flex flex-col gap-1">
          <div className="w-full border-t pt-4">
            <p>Username: {profile.username}</p>
            <p>Email: {profile.email}</p>
            <p>Roles: {profile.roles.join(", ")}</p>
            <p>Penalty: {profile.penalty}</p>

            <p>
              Courier status:{" "}
              {profile.isCourierSuspended ? "Suspended" : "Active"}
            </p>
          </div>
        </div>
        <div className="flex flex-col gap-1">
          <div className="w-full border-t pt-4">
            <Button
              type="button"
              className="w-fit"
              disabled={editing}
              onClick={() => router.push("/profile/edit")}
            >
              Edit profile
            </Button>
          </div>
        </div>
    </div>
  )
}