import { RequestDetail } from "@/components/orders/request-detail"
export default async function Page({ params }: { params: Promise<{ id: string }> }) {
  return <RequestDetail id={(await params).id} />
}
