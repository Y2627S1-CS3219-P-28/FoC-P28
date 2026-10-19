"use client"

import { useCallback, useRef, useState } from "react"
import Link from "next/link"
import { ArrowUpRightIcon } from "lucide-react"
import { toast } from "sonner"
import { useAuth } from "@/components/providers/auth-provider"
import { useConfig } from "@/components/providers/config-provider"
import { useOrderDisplayNames } from "@/hooks/use-order-display-names"
import { useReportApi, useReportResource } from "@/hooks/use-reports"
import { REPORT_CATEGORIES, REPORT_STATUSES, type DeliveryReport, type ReportEvidence } from "@/lib/reports"
import type { AdminOrder } from "@/lib/admin-api"
import { StatusSelect } from "@/components/orders/status-select"
import { formatOrderStatus } from "@/lib/orders"
import { Button, buttonVariants } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { AdminComingSoon, AdminFilterActions, AdminFilterForm, AdminHeading, AdminLoadState, AdminPagination, CopyId, adminDate } from "@/components/admin/admin-ui"

export function AdminReportsPage({ reportId }: { reportId?: string }) {
  const { adminApiMode } = useConfig()
  return <section className="mx-auto w-full max-w-6xl space-y-6 px-4 py-8 sm:px-6">
    <AdminHeading title={reportId ? "Report details" : "Reports"} description="Review delivered-item issues, evidence and responses." />
    {adminApiMode !== "mock" ? <AdminComingSoon title="Delivery reports" />
      : reportId ? <ReportCase key={reportId} id={reportId} /> : <ReportList />}
  </section>
}

function ReportList() {
  const api = useReportApi()
  const { user, loading: authLoading } = useAuth()
  const [page, setPage] = useState(1)
  const [orderId, setOrderId] = useState("")
  const [query, setQuery] = useState({ orderId: "", status: "" })
  function changeFilters(next: typeof query) { setPage(1); setQuery(next) }
  const statuses = { all: "All statuses", ...REPORT_STATUSES }
  const load = useCallback((signal: AbortSignal) => api.list(user?.uid ?? "", "admin", page, "", signal, query), [api, user?.uid, page, query])
  const enabled = Boolean(user) && !authLoading
  const result = useReportResource(load, enabled)
  const root = "/admin/reports"
  return <>
    <AdminFilterForm onSubmit={event => { event.preventDefault(); changeFilters({ ...query, orderId: orderId.trim() }) }}>
      <div className="flex min-w-0 flex-col gap-2"><Label htmlFor="report-status">Status</Label>
        <StatusSelect id="report-status" aria-label="Report status" className="w-full" value={query.status || "all"}
          onChange={event => changeFilters({ ...query, status: event.target.value === "all" ? "" : event.target.value })}>
          {Object.entries(statuses).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
        </StatusSelect>
      </div>
      <div className="flex min-w-0 flex-col gap-2"><Label htmlFor="report-order-id">Order ID</Label>
        <Input className="h-10 min-w-0" id="report-order-id" maxLength={128} value={orderId} onChange={event => setOrderId(event.target.value)} placeholder="Enter an exact Order ID" />
      </div>
      <AdminFilterActions>
        <Button className="h-10" type="submit">Search</Button>
        <Button className="h-10" type="button" variant="outline" onClick={() => { setOrderId(""); changeFilters({ orderId: "", status: "" }) }}>Clear</Button>
      </AdminFilterActions>
    </AdminFilterForm>
    <section className="space-y-4" aria-label="Report results">
      <AdminLoadState loading={result.loading} error={result.error} empty={result.data?.items.length === 0} retry={result.refresh} />
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">{result.data?.items.map(report => <Card key={report.id} className={report.status === "RESOLVED" ? "min-w-0 bg-muted/50 text-muted-foreground" : "min-w-0"}>
          <CardHeader>
            <div className="flex items-start justify-between gap-3">
              <CardTitle className="min-w-0 flex-1"><h3>{REPORT_CATEGORIES[report.violationCategory]}</h3></CardTitle>
              <Badge variant="outline" className="h-auto! min-h-5 max-w-1/2 whitespace-normal! text-right leading-tight">{REPORT_STATUSES[report.status]}</Badge>
            </div>
          </CardHeader>
          <CardContent className="flex flex-1 flex-col gap-4 text-sm">
            <p className="line-clamp-3 text-muted-foreground [overflow-wrap:anywhere]">{report.requesterExplanation}</p>
            <p className="text-xs text-muted-foreground">{report.status === "RESOLVED" ? "Resolved " + adminDate(report.resolvedAt!) : "Submitted " + adminDate(report.createdAt)}</p>
            <div className="mt-auto"><Link href={root + "/" + encodeURIComponent(report.id)} className={buttonVariants({ variant: "outline", size: "sm" })}>View report</Link></div>
          </CardContent>
        </Card>)}</div>
      {result.data && result.data.totalPages > 1 && <AdminPagination page={page} totalPages={result.data.totalPages} totalItems={result.data.totalItems} onPage={setPage} />}
    </section>
  </>
}

function Evidence({ files }: { files: ReportEvidence[] }) {
  return files.length ? <ul className="space-y-2">{files.map((file, index) => <li key={index}><a href={file.dataUrl} download={file.name} className="text-sm underline underline-offset-4">{file.name}</a></li>)}</ul>
    : <p className="text-sm text-muted-foreground">No attachments provided.</p>
}

function SummaryFields({ rows }: { rows: [string, React.ReactNode][] }) {
  return <dl className="divide-y">{rows.map(([label, value]) => <div key={label} className="grid min-w-0 gap-1 py-3 text-sm sm:grid-cols-[11rem_minmax(0,1fr)] sm:gap-6">
    <dt className="text-muted-foreground">{label}</dt><dd className="min-w-0 whitespace-pre-wrap [overflow-wrap:anywhere]">{value}</dd>
  </div>)}</dl>
}
function ReportSection({ title, children }: { title: string; children: React.ReactNode }) {
  return <section className="min-w-0 space-y-3 border-t px-5 py-6 sm:px-8" aria-label={title}>
    <h2 className="text-base font-medium">{title}</h2>{children}
  </section>
}
function ReportOrderSummary({ order }: { order: AdminOrder }) {
  const displayName = useOrderDisplayNames([order])
  const content = <>
    <div className="flex items-center justify-between gap-3"><h2 className="text-base font-medium">Order</h2><ArrowUpRightIcon className="size-4 shrink-0 text-muted-foreground" aria-hidden /></div>
    <SummaryFields rows={[
      ["Order ID", order.id], ["Description", order.itemDescription], ["Order status", formatOrderStatus(order.status)],
      ["Requester", displayName("users", order.requesterId, order.requesterName)], ["Courier", displayName("users", order.courierId, order.courierName) || "Unassigned"],
      ["Pickup", displayName("suppliers", order.pickupSupplierId, order.pickupSupplierName)], ["Delivery", displayName("suppliers", order.deliverySupplierId, order.deliverySupplierName)],
      ["Offered credits", order.offeredCredits], ["Delivery time limit", order.deliveryTimeLimitMinutes + " minutes"],
      ["Expiry", adminDate(order.expiresAt)],
      ...(order.deliveryDeadlineAt ? [["Delivery deadline", adminDate(order.deliveryDeadlineAt)] as [string, React.ReactNode]] : []),
      ...(order.deliveredAt ? [["Delivered", adminDate(order.deliveredAt)] as [string, React.ReactNode]] : []),
      ["Completion hold", order.isCompletionHold ? "Yes" : "No"],
      ["Overdue", order.isOverdue == null ? "Not available" : order.isOverdue ? (order.overduePenaltyWaived ? "Yes — penalty waived" : "Yes") : "No"],
    ]} />
  </>
  return <Link href={"/admin/orders/" + encodeURIComponent(order.id)} aria-label={"Open order " + order.id + " details"}
    className="block min-w-0 px-5 py-6 transition-colors hover:bg-muted/40 focus-visible:outline-2 focus-visible:-outline-offset-2 focus-visible:outline-ring sm:px-8">{content}</Link>
}
function ReportCase({ id }: { id: string }) {
  const api = useReportApi(); const { user, loading: authLoading } = useAuth()
  const load = useCallback((signal: AbortSignal) => api.detail(id, user?.uid ?? "", signal), [api, id, user?.uid])
  const { data: report, error, loading, refresh } = useReportResource(load, Boolean(user) && !authLoading)
  const [resolving, setResolving] = useState(false)
  const displayName = useOrderDisplayNames(report ? [report.order] : [])
  if (!report) return <AdminLoadState loading={loading} error={error} empty={false} retry={refresh} />
  const root = "/admin/reports"
  const order = { ...report.order, requesterName: displayName("users", report.requesterId, report.order.requesterName), courierName: displayName("users", report.courierId, report.order.courierName), pickupSupplierName: displayName("suppliers", report.order.pickupSupplierId, report.order.pickupSupplierName), deliverySupplierName: displayName("suppliers", report.order.deliverySupplierId, report.order.deliverySupplierName) }
  return <>
    <Link href={root} className={buttonVariants({ variant: "outline" })}>Back to reports</Link>
    {error && <p role="status" className="text-sm text-muted-foreground">Could not refresh. Showing the last available case.</p>}
    <Card className="gap-0 py-0" aria-label="Report case">
      <ReportOrderSummary order={order} />
      <ReportSection title="Report information">
        <SummaryFields rows={[
          ["Report ID", <CopyId key="id" id={report.id} />],
          ["Status", <Badge key="status" variant="outline">{REPORT_STATUSES[report.status]}</Badge>],
          ["Violation category", REPORT_CATEGORIES[report.violationCategory]],
          ["Submitted", adminDate(report.createdAt)],
          ["Courier response", report.courierResponseSubmittedAt ? "Submitted " + adminDate(report.courierResponseSubmittedAt)
            : report.status === "AWAITING_COURIER_RESPONSE" ? "Awaiting response" : "No response submitted."],
        ]} />
      </ReportSection>
      <ReportSection title="Requester account">
        <SummaryFields rows={[["Requester name", <Participant key="requester" id={report.requesterId} name={order.requesterName} />],
          ["Requester ID", <CopyId key="requester-id" id={report.requesterId} />],
          ["Explanation", report.requesterExplanation], ["Attachments", <Evidence key="requester-evidence" files={report.requesterAttachments} />]]} />
      </ReportSection>
      <ReportSection title="Courier account">
        <SummaryFields rows={[["Courier name", <Participant key="courier" id={report.courierId} name={order.courierName} />],
          ["Courier ID", <CopyId key="courier-id" id={report.courierId} />],
          ["Explanation", report.courierExplanation || "No response submitted."], ["Attachments", <Evidence key="courier-evidence" files={report.courierAttachments} />]]} />
      </ReportSection>
      {report.status === "RESOLVED" && <ReportSection title="Resolution"><SummaryFields rows={[
        ["Admin reply", report.resolutionNote], ["Resolved", adminDate(report.resolvedAt!)], ["Resolved by", report.resolvedBy],
      ]} /></ReportSection>}
      {report.status !== "RESOLVED" && <div className="flex justify-end border-t px-5 py-5 sm:px-8"><Button onClick={() => setResolving(true)}>Mark report resolved</Button></div>}
    </Card>
    {resolving && <ResolveReportDialog report={report} onClose={() => setResolving(false)} onDone={() => { setResolving(false); refresh() }} />}
  </>
}
function Participant({ id, name }: { id: string; name: string | null }) {
  return <Link href={"/admin/users/" + encodeURIComponent(id)} className="text-sm underline underline-offset-4">{name || "Deactivated user"}</Link>
}
function ResolveReportDialog({ report, onClose, onDone }: { report: DeliveryReport; onClose: () => void; onDone: () => void }) {
  const api = useReportApi(); const { user } = useAuth()
  const [note, setNote] = useState(""); const [busy, setBusy] = useState(false); const locked = useRef(false); const [error, setError] = useState<string | null>(null)
  async function submit(event: React.FormEvent) {
    event.preventDefault(); if (!user || !note.trim() || locked.current) return
    locked.current = true; setBusy(true); setError(null)
    try { await api.resolve(report.id, user.uid, note.trim()); toast.success("Sample report resolved; its completion hold is released."); onDone() }
    catch (cause) { setError(cause instanceof Error ? cause.message : "Could not resolve report.") }
    finally { locked.current = false; setBusy(false) }
  }
  return <Dialog open onOpenChange={open => { if (!open && !busy) onClose() }}><DialogContent className="sm:max-w-lg"><form onSubmit={submit} className="space-y-4">
    <DialogHeader><DialogTitle>Resolve report</DialogTitle><DialogDescription>Your reply is only visible to the submitter. Reviewing admins can also view it. Resolving the case releases this report’s completion hold. Order and penalty actions remain separate.</DialogDescription></DialogHeader>
    <Label htmlFor="resolution-note">Admin reply</Label><Textarea id="resolution-note" maxLength={2000} required value={note} onChange={event => setNote(event.target.value)} disabled={busy} />
    {error && <p role="alert" className="text-sm text-destructive">{error}</p>}<DialogFooter><Button type="button" variant="outline" disabled={busy} onClick={onClose}>Back</Button><Button type="submit" disabled={busy || !note.trim()}>{busy ? "Saving…" : "Confirm resolved"}</Button></DialogFooter>
  </form></DialogContent></Dialog>
}
