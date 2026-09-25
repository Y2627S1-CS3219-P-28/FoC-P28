"use client"

import Link from "next/link"
import { ArrowRightIcon, LayoutGridIcon } from "lucide-react"

import { useAuth } from "@/components/providers/auth-provider"
import { RequireAuth } from "@/components/require-auth"
import { Card, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Empty, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from "@/components/ui/empty"
import { NAV_ITEMS } from "@/config/navigation"

function Home() {
  const { user } = useAuth()

  return (
    <div className="mx-auto flex w-full max-w-6xl flex-col gap-6 px-4 py-8">
      <div className="flex flex-col gap-1">
        <h1 className="text-2xl font-semibold tracking-tight">Welcome back</h1>
        <p className="text-sm text-muted-foreground">Signed in as {user?.email}</p>
      </div>

      {NAV_ITEMS.length === 0 ? (
        <Empty className="border">
          <EmptyHeader>
            <EmptyMedia variant="icon">
              <LayoutGridIcon />
            </EmptyMedia>
            <EmptyTitle>No features yet</EmptyTitle>
            <EmptyDescription>Features appear here once they are registered in src/config/navigation.ts.</EmptyDescription>
          </EmptyHeader>
        </Empty>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {NAV_ITEMS.map((item) => (
            <Link key={item.href} href={item.href} className="group rounded-xl focus-visible:outline-none">
              <Card className="h-full transition-colors group-hover:bg-muted/50 group-focus-visible:ring-3 group-focus-visible:ring-ring/50">
                <CardHeader>
                  <item.icon className="mb-2 size-5 text-muted-foreground" />
                  <CardTitle className="flex items-center justify-between">
                    {item.label}
                    <ArrowRightIcon className="size-4 opacity-0 transition-opacity group-hover:opacity-100" />
                  </CardTitle>
                  <CardDescription>{item.description}</CardDescription>
                </CardHeader>
              </Card>
            </Link>
          ))}
        </div>
      )}
    </div>
  )
}

export default function HomePage() {
  return (
    <RequireAuth>
      <Home />
    </RequireAuth>
  )
}
