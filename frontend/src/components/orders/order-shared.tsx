"use client"
import Link from "next/link"
import { useEffect, useState } from "react"
import { useApi } from "@/hooks/use-api"
import { ApiError } from "@/lib/api"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import { type Errand, singaporeTime } from "@/lib/orders"

export function OrderLayout({ title, description, children }: { title: string; description: string; children: React.ReactNode }) {
  return <section className="mx-auto w-full max-w-5xl space-y-6 p-4 sm:p-8">
    <nav aria-label="Errands" className="flex flex-wrap gap-4 text-sm">
      <Link className="underline underline-offset-4" href="/errands">Browse errands</Link>
      <Link className="underline underline-offset-4" href="/requests/new">Post a request</Link>
    </nav>
    <div><Badge variant="secondary">Development preview</Badge>
      <h1 className="mt-3 text-2xl font-semibold tracking-tight sm:text-3xl">{title}</h1>
      <p className="mt-2 text-muted-foreground">{description}</p>
      <p className="mt-2 text-xs text-muted-foreground">Sample suppliers and simulated credits. No credits are charged. Demo identities are not authenticated.</p>
    </div>{children}
  </section>
}
export function ErrorMessage({ error }: { error: unknown }) {
  if (!error) return null
  return <div role="alert" className="rounded-lg border border-destructive/40 bg-destructive/5 p-4 text-sm">
    <p>{error instanceof Error ? error.message : "Something went wrong. Please retry."}</p>
    {error instanceof ApiError && !!error.details?.length && <ul className="mt-2 list-inside list-disc">
      {error.details.map((detail, i) => <li key={i}>{detail.field}: {detail.message}</li>)}
    </ul>}
  </div>
}
export function useOrderQuery<T>(path: string) {
  const api = useApi()
  const [data, setData] = useState<T | null>(null)
  const [error, setError] = useState<unknown>(null)
  const [loading, setLoading] = useState(true)
  const [loadedPath, setLoadedPath] = useState("")
  const [revision, setRevision] = useState(0)
  useEffect(() => {
    const controller = new AbortController()
    api<T>(path, { signal: controller.signal }).then(value => {
      setData(value); setError(null); setLoading(false); setLoadedPath(path)
    }).catch(cause => {
      if (!controller.signal.aborted) { setError(cause); setData(null); setLoading(false); setLoadedPath(path) }
    })
    return () => controller.abort()
  }, [api, path, revision])
  function refresh() { setLoading(true); setRevision(value => value + 1) }
  return { data: loadedPath === path ? data : null, error: loadedPath === path ? error : null,
    loading: loading || loadedPath !== path, refresh, setData }
}
export function QueryState({ loading, error, refresh }: { loading: boolean; error: unknown; refresh: () => void }) {
  return <div aria-live="polite">
    {loading && <p className="py-3 text-sm text-muted-foreground">Loading errands…</p>}
    <ErrorMessage error={error} />
    {!!error && <Button className="mt-3" variant="outline" onClick={refresh}>Try again</Button>}
  </div>
}
export function ErrandSummary({ errand }: { errand: Errand }) {
  return <Card><CardContent className="space-y-4 pt-4">
    <div className="flex flex-wrap items-start justify-between gap-3">
      <h2 className="min-w-0 break-words text-lg font-medium">{errand.description}</h2>
      <Badge>{errand.status}</Badge>
    </div>
    <dl className="grid gap-4 text-sm sm:grid-cols-2">
      <div><dt className="text-muted-foreground">Pickup</dt><dd className="font-medium">{errand.pickup.name}</dd><dd>{errand.pickup.location}</dd></div>
      <div><dt className="text-muted-foreground">Delivery</dt><dd className="font-medium">{errand.delivery.name}</dd><dd>{errand.delivery.location}</dd></div>
      <div><dt className="text-muted-foreground">Reward</dt><dd>{errand.creditAmount} credits (simulated)</dd></div>
      <div><dt className="text-muted-foreground">Delivery duration</dt><dd>{errand.deliveryDurationMinutes} minutes from task start</dd></div>
      <div><dt className="text-muted-foreground">Accept before · Singapore time</dt><dd>{singaporeTime(errand.expiresAt)}</dd></div>
      <div><dt className="text-muted-foreground">Requester</dt><dd className="break-all">{errand.requesterId}</dd></div>
    </dl>
  </CardContent></Card>
}
