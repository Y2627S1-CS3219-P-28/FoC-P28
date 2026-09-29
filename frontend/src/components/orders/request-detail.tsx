"use client"
import Link from "next/link"
import { type Errand, orderPath } from "@/lib/orders"
import { OrderLayout, ErrandSummary, QueryState, useOrderQuery } from "./order-shared"
import { Button } from "@/components/ui/button"
export function RequestDetail({ id }: { id: string }) {
  const query = useOrderQuery<Errand>(orderPath + "/errands/" + encodeURIComponent(id))
  return <OrderLayout title="Your request" description="Your request has been saved. Share or keep this page to check its status.">
    <QueryState {...query} />
    {query.data && <><ErrandSummary errand={query.data} />
      <p className="break-all text-xs text-muted-foreground">Request ID: {id}</p>
      {query.data.orderId && <Link className="underline" href={"/errands/" + encodeURIComponent(query.data.orderId)}>View delivery progress</Link>}</>}
    <Button variant="outline" onClick={query.refresh} disabled={query.loading}>Refresh status</Button>
  </OrderLayout>
}
