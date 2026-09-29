"use client"
import { useState, useRef } from "react"
import { useRouter } from "next/navigation"
import { useApi } from "@/hooks/use-api"
import { type Errand, type Delivery, type Page, orderPath } from "@/lib/orders"
import { OrderLayout, ErrandSummary, ErrorMessage, QueryState, useOrderQuery } from "./order-shared"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"

export function BrowseErrands() {
  const [page, setPage] = useState(1)
  const [actorId, setActor] = useState("demo-courier")
  const query = useOrderQuery<Page<Errand>>(orderPath + "/errands?page=" + page + "&size=10")
  const api = useApi()
  const router = useRouter()
  const [busy, setBusy] = useState<string | null>(null)
  const [error, setError] = useState<unknown>(null)
  const commands = useRef(new Map<string, string>())
  async function accept(errand: Errand) {
    if (busy) return
    const key = JSON.stringify([errand.id, actorId.trim(), errand.version])
    if (!commands.current.has(key)) commands.current.set(key, crypto.randomUUID())
    setBusy(errand.id); setError(null)
    try {
      const result = await api<Delivery>(orderPath + "/errands/" + encodeURIComponent(errand.id) + "/accept", {
        method: "POST", body: { actorId: actorId.trim(), commandId: commands.current.get(key), expectedVersion: errand.version },
      })
      router.push("/errands/" + encodeURIComponent(result.id) + "?actor=" + encodeURIComponent(actorId.trim()))
    } catch (cause) { setError(cause); setBusy(null) }
  }
  return <OrderLayout title="Browse errands" description="Available requests, ready for a courier.">
    <div className="flex flex-wrap items-end gap-4">
      <div className="w-full max-w-sm space-y-2"><Label htmlFor="courierId">Demo courier ID</Label>
        <Input id="courierId" value={actorId} onChange={e => setActor(e.target.value)} maxLength={128} disabled={!!busy} /></div>
      <Button variant="outline" onClick={query.refresh} disabled={query.loading || !!busy}>Refresh</Button>
    </div>
    <ErrorMessage error={error} />
    {!!error && <p className="text-sm text-muted-foreground">If the errand changed, refresh the list before trying again.</p>}
    <QueryState {...query} />
    {!query.loading && !query.error && query.data?.items.length === 0 && <p className="rounded-xl border p-8 text-center text-muted-foreground">No available errands right now. Post a request or check again later.</p>}
    {!query.loading && !query.error && query.data?.items.map(errand => <article key={errand.id} className="space-y-3">
      <ErrandSummary errand={errand} />
      <Button onClick={() => void accept(errand)} disabled={!!busy || !actorId.trim() || actorId.trim() === errand.requesterId}>
        {busy === errand.id ? "Accepting…" : actorId.trim() === errand.requesterId ? "Your request" : "Accept errand"}
      </Button>
    </article>)}
    {query.data && <nav aria-label="Errand pages" className="flex flex-wrap items-center gap-4">
      <Button variant="outline" disabled={page === 1 || query.loading || !!busy} onClick={() => setPage(page - 1)}>Previous</Button>
      <span className="text-sm">Page {query.data.page} of {Math.max(1, query.data.totalPages)}</span>
      <Button variant="outline" disabled={page >= query.data.totalPages || query.loading || !!busy} onClick={() => setPage(page + 1)}>Next</Button>
    </nav>}
  </OrderLayout>
}
