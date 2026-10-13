"use client"

import { useCallback, useState } from "react"
import { toast } from "sonner"

import { TransactionHistoryView } from "@/components/credits/transaction-history"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { useAdminApi } from "@/hooks/use-admin-api"
import { useAdminPage } from "@/hooks/use-admin-page"
import type { AdminSearch, AdminUser } from "@/lib/admin-api"
import { AdminComingSoon, AdminConfirmation, AdminDetails, AdminFilterActions, AdminFilterForm, AdminHeading, AdminLoadState, AdminPagination, AdminSearchForm, CopyId, adminDate } from "./admin-ui"

function AdminUserTransactions({ userId }: { userId: string }) {
  const api = useAdminApi()
  const [page, setPage] = useState(1)
  const load = useCallback((signal: AbortSignal) => api.userTransactions(userId, page, signal), [api, userId, page])
  const { data, loading, error, refresh } = useAdminPage(load)
  return <TransactionHistoryView items={data?.items ?? []} totalItems={data?.totalItems ?? 0}
    totalPages={data?.totalPages ?? 0} page={page} onPage={setPage} error={!!error}
    loading={loading} refreshing={loading} refresh={refresh} />
}

export function AdminUserProfile({ userId, onBack }: { userId: string; onBack: () => void }) {
  const api = useAdminApi()
  const load = useCallback((signal: AbortSignal) => api.user(userId, signal), [api, userId])
  const { data, loading, error, refresh } = useAdminPage(load)
  const [updated, setUpdated] = useState<AdminUser | null>(null)
  const selected = updated ?? data
  const [showSuspend, setShowSuspend] = useState(false)
  const [suspensionEnd, setSuspensionEnd] = useState("")
  const [confirmDeactivate, setConfirmDeactivate] = useState(false)
  const [busy, setBusy] = useState(false)
  const [actionError, setActionError] = useState<string | null>(null)
  const missing = api.mode === "live" ? "Coming soon" : null

  async function mutate(action: () => Promise<AdminUser>, message: string) {
    setBusy(true)
    setActionError(null)
    try {
      setUpdated(await action())
      setConfirmDeactivate(false)
      setShowSuspend(false)
      toast.success(message)
    } catch (error) {
      setActionError(error instanceof Error ? error.message : "Could not update the user.")
    } finally { setBusy(false) }
  }

  function suspend() {
    const end = new Date(suspensionEnd)
    if (!suspensionEnd || !Number.isFinite(end.getTime()) || end.getTime() <= Date.now()) {
      setActionError("Choose a suspension end date and time in the future.")
      return
    }
    if (selected) void mutate(() => api.suspendUser(selected.userId, end.toISOString()), "User suspended.")
  }

  return <section className="mx-auto w-full max-w-6xl space-y-6 px-4 py-8 sm:px-6 lg:py-10">
    <Button variant="outline" disabled={busy} onClick={onBack}>Back to users</Button>
    <AdminHeading title={selected?.username ?? "User profile"} description="Review this user's account, credit activity and access." />
    <AdminLoadState loading={loading} error={error} empty={false} retry={refresh} />
    {selected && <>
      <Card>
        <CardHeader><CardTitle className="text-xl">Admin actions</CardTitle><CardDescription>Manage access for the selected user.</CardDescription></CardHeader>
        <CardContent className="space-y-4">
          {!api.capabilities.userActions && <p className="text-sm text-muted-foreground">User management is coming soon.</p>}
          <div className="flex flex-wrap gap-2">
            <Button variant="outline" disabled={!api.capabilities.userActions || busy || !selected.active} onClick={() => { setShowSuspend(!showSuspend); setActionError(null) }}>Suspend User</Button>
            <Button variant="outline" disabled={!api.capabilities.userActions || busy || !selected.active || selected.roles.includes("admin")}
              onClick={() => void mutate(() => api.promoteUser(selected.userId), "User promoted to admin.")}>
              {selected.roles.includes("admin") ? "Already an admin" : "Promote to Admin"}</Button>
            <Button variant="destructive" disabled={!api.capabilities.userActions || busy || !selected.active}
              onClick={() => { setActionError(null); setConfirmDeactivate(true) }}>Deactivate User</Button>
          </div>
          {showSuspend && <div className="space-y-3 rounded-lg border bg-muted/30 p-4">
            <Label htmlFor="suspension-end">Suspension end (your local time)</Label>
            <Input id="suspension-end" type="datetime-local" value={suspensionEnd} onChange={(event) => setSuspensionEnd(event.target.value)} disabled={busy} />
            <Button disabled={busy} onClick={suspend}>{busy ? "Applying…" : "Confirm suspension"}</Button>
          </div>}
          {actionError && !confirmDeactivate && <p role="alert" className="text-sm text-destructive">{actionError}</p>}
        </CardContent>
      </Card>
      <Card>
        <CardHeader><CardTitle className="text-xl">Profile summary</CardTitle><CardDescription>Account details and courier standing.</CardDescription></CardHeader>
        <CardContent><AdminDetails fields={[
          ["Name", selected.username], ["User ID", <CopyId key="uid" id={selected.userId} />],
          ["Profile ID", selected.id ? <CopyId key="profile" id={selected.id} /> : missing], ["Email", selected.email],
          ["Phone number", selected.phoneNumber ?? missing], ["Address", selected.address ?? missing],
          ["Roles", <div key="roles" className="flex flex-wrap gap-1">{selected.roles.map((role) => <Badge key={role} variant="secondary">{role}</Badge>)}</div>],
          ["Penalty score", selected.penalty],
          ["Account status", selected.active === null ? missing : <Badge key="status" variant={selected.active ? "outline" : "destructive"}>{selected.active ? "Active" : "Inactive"}</Badge>],
          ["Courier status", selected.isCourierSuspended ? "Suspended" : "Active"],
          ["Suspension ends", adminDate(selected.suspensionEndDate)],
        ]} /></CardContent>
      </Card>
      <Card>
        <CardHeader><CardTitle className="text-xl">Credit balance</CardTitle><CardDescription>{api.mode === "mock" ? "Sample credit account projection." : "Selected user's credit balance."}</CardDescription></CardHeader>
        <CardContent><dl className="grid gap-5 sm:grid-cols-3">
          {[["Available credits", selected.usableBalance], ["Reserved credits", selected.reservedBalance], ["Total credits", selected.totalBalance]].map(([label, value]) =>
            <div key={String(label)}><dt className="text-sm text-muted-foreground">{label}</dt>
              <dd className={value == null ? "mt-1 text-sm text-muted-foreground" : "mt-1 text-2xl font-semibold tabular-nums"}>{value ?? missing}</dd></div>)}
        </dl></CardContent>
      </Card>
      {api.capabilities.creditTransactions ? <AdminUserTransactions key={selected.userId} userId={selected.userId} />
        : <AdminComingSoon title="Credit transactions" />}
    </>}
    <AdminConfirmation open={confirmDeactivate} title="Deactivate this user?"
      description="This will deactivate the selected account. Please check that you have selected the correct user before continuing."
      busy={busy} error={actionError} onClose={() => { setConfirmDeactivate(false); setActionError(null) }}
      onConfirm={() => { if (selected) void mutate(() => api.deactivateUser(selected.userId), "User deactivated.") }} />
  </section>
}

export function AdminUsers() {
  const api = useAdminApi()
  return api.capabilities.userDirectory ? <AdminUserDirectory /> : <AdminUserLookup />
}

function AdminUserLookup() {
  const [id, setId] = useState("")
  const [selectedId, setSelectedId] = useState<string | null>(null)
  if (selectedId) return <AdminUserProfile key={selectedId} userId={selectedId} onBack={() => setSelectedId(null)} />
  return <section className="mx-auto w-full max-w-6xl space-y-6 p-4 md:p-8">
    <AdminHeading title="Users" description="View an existing user's summary by their User ID." />
    <AdminFilterForm onSubmit={(event) => {
      event.preventDefault()
      if (id.trim()) setSelectedId(id.trim())
    }}>
      <div className="col-span-2 flex min-w-0 flex-col gap-2"><Label htmlFor="admin-user-id">User ID</Label>
        <Input id="admin-user-id" className="h-10 min-w-0" maxLength={128} value={id} onChange={(event) => setId(event.target.value)} placeholder="Enter an exact User ID" />
      </div>
      <AdminFilterActions><Button type="submit" className="h-10" disabled={!id.trim()}>View profile</Button></AdminFilterActions>
    </AdminFilterForm>
    <AdminComingSoon title="User directory" />
  </section>
}

function AdminUserDirectory() {
  const api = useAdminApi()
  const [query, setQuery] = useState<AdminSearch>({ page: 1, searchBy: "name", q: "" })
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const load = useCallback((signal: AbortSignal) => api.users(query, signal), [api, query])
  const { data, loading, error, refresh } = useAdminPage(load)

  if (selectedId) return <AdminUserProfile key={selectedId} userId={selectedId} onBack={() => { setSelectedId(null); refresh() }} />

  return <section className="mx-auto w-full max-w-6xl space-y-6 p-4 md:p-8">
    <AdminHeading title="Users" description="Find a user, view their profile and manage account access." />
    <AdminSearchForm label="Users" onSearch={(searchBy, q) => setQuery({ page: 1, searchBy, q })} />
    <AdminLoadState loading={loading} error={error} empty={data?.items.length === 0} retry={refresh} />
    {data && data.items.length > 0 && <>
      <div className="overflow-x-auto rounded-xl border">
        <Table className="min-w-[1300px]"><TableHeader><TableRow>{["Name", "Email", "User ID", "Roles", "Account status", "Courier status", "Available credits", "Reserved credits", "Total credits", ""].map((title, index) =>
          <TableHead key={index}>{title || <span className="sr-only">Profile</span>}</TableHead>)}</TableRow></TableHeader>
          <TableBody>{data.items.map((user) => <TableRow key={user.userId}>
            <TableCell><button type="button" className="text-left font-normal hover:underline" onClick={() => setSelectedId(user.userId)}>{user.username}</button></TableCell>
            <TableCell>{user.email}</TableCell>
            <TableCell><CopyId id={user.userId} /></TableCell>
            <TableCell>{user.roles.join(", ")}</TableCell>
            <TableCell><Badge variant="outline">{user.active ? "Active" : "Inactive"}</Badge></TableCell>
            <TableCell><Badge variant="outline">{user.isCourierSuspended ? "Suspended" : "Active"}</Badge></TableCell>
            <TableCell>{user.usableBalance}</TableCell>
            <TableCell>{user.reservedBalance}</TableCell>
            <TableCell>{user.totalBalance}</TableCell>
            <TableCell><Button size="sm" variant="outline" aria-label={"View profile of " + user.username} onClick={() => setSelectedId(user.userId)}>View profile</Button></TableCell>
          </TableRow>)}</TableBody>
        </Table>
      </div>
      <AdminPagination {...data} onPage={(page) => setQuery((current) => ({ ...current, page }))} />
    </>}
  </section>
}
