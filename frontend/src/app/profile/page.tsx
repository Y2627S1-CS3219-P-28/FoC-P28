"use client" 

import { useRouter } from "next/navigation" 
import { useAuth } from "@/components/providers/auth-provider" 
import { UserInformation } from "@/components/users/user-information"  
import { Button } from "@/components/ui/button" 
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card" 
import { Skeleton } from "@/components/ui/skeleton" 

export default function ProfilePage() { 
  const { user, loading: authLoading } = useAuth() 
  const router = useRouter() 
  
  if (authLoading || !user) { 
    return ( 
      <div className="mx-auto w-full max-w-6xl space-y-6 px-4 py-8 sm:px-6"> 
        <Skeleton className="h-56 w-full" /> 
        <Skeleton className="h-80 w-full" /> 
      </div> 
    ) 
  } 
  
  return ( 
    <main className="mx-auto w-full max-w-6xl space-y-6 px-4 py-8 sm:px-6 lg:py-10"> 
      <header className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between"> 
        <div> 
          <p className="text-sm text-muted-foreground">Account</p> 
          <h1 className="text-2xl font-semibold tracking-tight sm:text-3xl"> 
            My profile 
          </h1> 
          <p className="mt-1 text-muted-foreground"> 
            Review your account and credit activity. 
          </p> 
        </div> 
      </header> 
      <UserInformation /> 
      
      <Card> 
        <CardHeader> 
          <CardTitle className="text-xl">Account management</CardTitle> 
          <CardDescription> 
            Manage your account settings and deletion. 
          </CardDescription> 
        </CardHeader> 
        <CardContent className="space-y-3"> 
          <Button
              type="button"
              className="w-fit"
              onClick={() => router.push("/profile/edit")}
            >
              Edit profile
            </Button>
        </CardContent> 
      </Card> 
    </main> 
  ) 
}