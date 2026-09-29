import { DeliveryDetail } from "@/components/orders/delivery-detail"
export default async function Page({ params, searchParams }: {
  params: Promise<{ id: string }>; searchParams: Promise<{ actor?: string | string[] }>
}) {
  const { id } = await params
  const { actor } = await searchParams
  return <DeliveryDetail id={id} initialActor={typeof actor === "string" ? actor : "demo-courier"} />
}
