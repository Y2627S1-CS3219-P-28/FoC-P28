"use client"
import { useRef, useState } from "react"
import { type Delivery, orderPath, singaporeTime } from "@/lib/orders"
import { useApi } from "@/hooks/use-api"
import { OrderLayout, ErrandSummary, ErrorMessage, QueryState, useOrderQuery } from "./order-shared"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
const actions = { ACCEPTED: ["start", "Start delivery"], IN_PROGRESS: ["pickup", "Mark picked up"], PICKED_UP: ["deliver", "Mark delivered"] } as const
export function DeliveryDetail({ id, initialActor }: { id: string; initialActor: string }) {
  const path = orderPath + "/executions/" + encodeURIComponent(id)
  const query = useOrderQuery<Delivery>(path)
  const api = useApi()
  const [actorId, setActor] = useState(initialActor)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<unknown>(null)
  const command = useRef<{ key: string; id: string } | null>(null)
  const delivery = query.data
  const next = delivery && delivery.status !== "DELIVERED" ? actions[delivery.status] : null
  async function progress() {
    if (!delivery || !next || busy) return
    const key = JSON.stringify([next[0], delivery.version, actorId.trim()])
    if (command.current?.key !== key) command.current = { key, id: crypto.randomUUID() }
    setBusy(true); setError(null)
    try {
      const result = await api<Delivery>(path + "/" + next[0], {
        method: "POST", body: { actorId: actorId.trim(), expectedVersion: delivery.version, commandId: command.current.id },
      })
      query.setData(result)
    } catch (cause) { setError(cause) }
    finally { setBusy(false) }
  }
  return <OrderLayout title="Your delivery" description="Record each step as you carry out this errand.">
    <QueryState {...query} />
    <ErrorMessage error={error} />
    {delivery && <>
      <div className="flex flex-wrap items-center gap-3"><Badge>{delivery.status.replaceAll("_", " ")}</Badge>
        <Button variant="outline" onClick={query.refresh} disabled={busy || query.loading}>Refresh status</Button></div>
      <ErrandSummary errand={delivery.errand} />
      <div className="max-w-sm space-y-2"><Label htmlFor="actorId">Demo courier ID</Label>
        <Input id="actorId" value={actorId} onChange={e => setActor(e.target.value)} maxLength={128} disabled={busy} />
        <p className="break-all text-xs text-muted-foreground">Assigned to: {delivery.courierId}</p></div>
      {next && <Button size="lg" onClick={() => void progress()} disabled={busy || query.loading || !!query.error || actorId.trim() !== delivery.courierId}>
        {busy ? "Saving…" : next[1]}
      </Button>}
      {delivery.status === "DELIVERED" && <p role="status" className="rounded-lg bg-muted p-4">Delivery recorded. This delivery flow is finished.</p>}
      {delivery.deliveryDeadline && <p className="text-sm">Delivery due: {singaporeTime(delivery.deliveryDeadline)} (Singapore time)</p>}
      <ol aria-label="Delivery progress" className="space-y-3 border-l-2 pl-5">
        {delivery.checkpoints.map(checkpoint => <li key={checkpoint.id} className="text-sm">
          <p className="font-medium">{checkpoint.status.replaceAll("_", " ")}</p>
          <p className="text-muted-foreground">{singaporeTime(checkpoint.occurredAt)} (SGT)</p>
        </li>)}
      </ol>
      <p className="break-all text-xs text-muted-foreground">Delivery ID: {id}. Keep this page URL to resume later.</p>
    </>}
  </OrderLayout>
}
