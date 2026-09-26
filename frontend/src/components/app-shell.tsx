/*
 * AI Assistance Disclosure:
 * Tool: OpenAI Codex (GPT-5), date: 2026-09-26
 * Mode: Code generation.
 * Scope: Implemented the responsive sidebar and authenticated credit summary UI.
 * Author review: I reviewed for correctness and edited where needed.
 */
"use client"

import Link from "next/link"
import { usePathname, useRouter } from "next/navigation"
import { useState } from "react"
import {
  CoinsIcon,
  LogOutIcon,
  MenuIcon,
  PackageIcon,
  RefreshCwIcon,
  UserIcon,
} from "lucide-react"

import { useAuth } from "@/components/providers/auth-provider"
import { useConfig } from "@/components/providers/config-provider"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetTrigger } from "@/components/ui/sheet"
import { Skeleton } from "@/components/ui/skeleton"
import { NAV_ITEMS } from "@/config/navigation"
import { useCreditBalance } from "@/hooks/use-credit-balance"
import { cn } from "@/lib/utils"

type CreditSummaryProps = ReturnType<typeof useCreditBalance>

function CreditSummary({ balance, error, loading, refresh }: CreditSummaryProps) {
  if (loading && !balance) {
    return (
      <div className="rounded-xl border border-sidebar-border bg-sidebar-accent/50 p-3" aria-label="Loading credits">
        <Skeleton className="mb-3 h-4 w-16" />
        <Skeleton className="mb-2 h-7 w-32" />
        <Skeleton className="h-3 w-40" />
      </div>
    )
  }

  if (error || !balance) {
    return (
      <div className="rounded-xl border border-sidebar-border bg-sidebar-accent/50 p-3">
        <div className="flex items-center gap-2 text-sm font-medium">
          <CoinsIcon className="size-4" />
          <span>Credits unavailable</span>
        </div>
        <Button variant="ghost" size="sm" className="mt-2 px-0" onClick={() => void refresh()}>
          <RefreshCwIcon />
          Refresh
        </Button>
      </div>
    )
  }

  return (
    <div className="rounded-xl border border-sidebar-border bg-sidebar-accent/50 p-3">
      <div className="flex items-center justify-between gap-2">
        <div className="flex items-center gap-2 text-xs font-medium uppercase tracking-wide text-muted-foreground">
          <CoinsIcon className="size-4" />
          Credits
        </div>
        <Button
          variant="ghost"
          size="icon-xs"
          onClick={() => void refresh()}
          disabled={loading}
          aria-label="Refresh credit balance"
          title="Refresh credit balance"
        >
          <RefreshCwIcon className={cn(loading && "animate-spin")} />
        </Button>
      </div>
      <p className="mt-2 text-lg font-semibold">Available: {balance.usableBalance}</p>
      <div className="mt-1 flex flex-wrap gap-x-3 gap-y-1 text-xs text-muted-foreground">
        <span>{balance.reservedBalance} reserved</span>
        <span>{balance.totalBalance} total</span>
      </div>
    </div>
  )
}

function SidebarNavigation({
  pathname,
  onNavigate,
}: {
  pathname: string
  onNavigate?: () => void
}) {
  const isActive = (href: string) => pathname === href || pathname.startsWith(href + "/")

  return (
    <nav className="flex flex-col gap-1 px-3 py-4" aria-label="Primary navigation">
      {NAV_ITEMS.map((item) => (
        <Link
          key={item.href}
          href={item.href}
          onClick={onNavigate}
          className={cn(
            "flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium text-sidebar-foreground/70 transition-colors hover:bg-sidebar-accent hover:text-sidebar-accent-foreground",
            isActive(item.href) && "bg-sidebar-accent text-sidebar-accent-foreground",
          )}
        >
          <item.icon className="size-4" />
          {item.label}
        </Link>
      ))}
    </nav>
  )
}

function SidebarPanel({
  credit,
  email,
  onNavigate,
  onSignOut,
  pathname,
}: {
  credit: CreditSummaryProps
  email: string | null
  onNavigate?: () => void
  onSignOut: () => Promise<void>
  pathname: string
}) {
  return (
    <div className="flex min-h-0 flex-1 flex-col bg-sidebar text-sidebar-foreground">
      <div className="flex h-14 shrink-0 items-center gap-2 border-b border-sidebar-border py-0 pl-4 pr-12 md:pr-4">
        <Link href="/" onClick={onNavigate} className="flex items-center gap-2 font-semibold">
          <PackageIcon className="size-5" />
          <span>Friend on Campus</span>
        </Link>
      </div>

      <SidebarNavigation pathname={pathname} onNavigate={onNavigate} />

      <div className="mt-auto p-4">
        <CreditSummary {...credit} />
      </div>
      <div className="flex flex-col gap-3 border-t border-sidebar-border p-4">
        <div className="min-w-0">
          <p className="text-xs font-medium text-muted-foreground">Signed in as</p>
          <p className="truncate text-sm" title={email ?? undefined}>{email}</p>
        </div>
        <Button variant="ghost" className="justify-start px-2" onClick={() => void onSignOut()}>
          <LogOutIcon />
          Sign out
        </Button>
      </div>
    </div>
  )
}

export function AppShell({ children }: { children: React.ReactNode }) {
  const { user, signOut } = useAuth()
  const { environment } = useConfig()
  const pathname = usePathname()
  const router = useRouter()
  const [mobileOpen, setMobileOpen] = useState(false)
  const credit = useCreditBalance()

  async function handleSignOut() {
    setMobileOpen(false)
    await signOut()
    router.replace("/login")
  }

  return (
    <div className="flex min-h-screen">
      {user && (
        <aside className="sticky top-0 hidden h-screen w-64 shrink-0 border-r border-sidebar-border md:flex">
          <SidebarPanel
            credit={credit}
            email={user.email}
            onSignOut={handleSignOut}
            pathname={pathname}
          />
        </aside>
      )}

      <div className="flex min-w-0 flex-1 flex-col">
        <header
          className={cn(
            "sticky top-0 z-40 border-b bg-background/90 backdrop-blur supports-[backdrop-filter]:bg-background/70",
            user && "md:hidden",
          )}
        >
          <div className="mx-auto flex h-14 w-full max-w-6xl items-center gap-3 px-4">
            {user && (
              <Sheet open={mobileOpen} onOpenChange={setMobileOpen}>
                <SheetTrigger render={<Button variant="ghost" size="icon" aria-label="Open menu" />}>
                  <MenuIcon />
                </SheetTrigger>
                <SheetContent side="left" className="w-72 gap-0 bg-sidebar p-0">
                  <SheetHeader className="sr-only">
                    <SheetTitle>Navigation</SheetTitle>
                  </SheetHeader>
                  <SidebarPanel
                    credit={credit}
                    email={user.email}
                    onNavigate={() => setMobileOpen(false)}
                    onSignOut={handleSignOut}
                    pathname={pathname}
                  />
                </SheetContent>
              </Sheet>
            )}

            <Link href="/" className="flex items-center gap-2 font-semibold">
              <PackageIcon className="size-5" />
              <span>FoC</span>
            </Link>
            {environment !== "production" && (
              <Badge variant="outline" className="hidden sm:inline-flex">
                {environment}
              </Badge>
            )}

            <div className="ml-auto">
              {user ? (
                <DropdownMenu>
                  <DropdownMenuTrigger render={<Button variant="ghost" size="icon" aria-label="Account menu" />}>
                    <UserIcon />
                  </DropdownMenuTrigger>
                  <DropdownMenuContent align="end" className="w-56">
                    <DropdownMenuGroup>
                      <DropdownMenuLabel className="truncate">{user.email}</DropdownMenuLabel>
                    </DropdownMenuGroup>
                    <DropdownMenuSeparator />
                    <DropdownMenuItem onClick={() => void handleSignOut()}>
                      <LogOutIcon />
                      Sign out
                    </DropdownMenuItem>
                  </DropdownMenuContent>
                </DropdownMenu>
              ) : (
                pathname !== "/login" && (
                  <Button nativeButton={false} render={<Link href="/login" />} size="sm">
                    Sign in
                  </Button>
                )
              )}
            </div>
          </div>
        </header>

        <main className="flex flex-1 flex-col">{children}</main>
      </div>
    </div>
  )
}
