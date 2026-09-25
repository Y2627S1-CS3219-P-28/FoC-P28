"use client"

import Link from "next/link"
import { useCallback, useEffect, useState } from "react"
import { ArrowLeftIcon, ClockIcon, ExternalLinkIcon, MapPinIcon, PencilIcon, PowerIcon, TagIcon } from "lucide-react"
import { toast } from "sonner"

import { SupplierBadges } from "@/components/suppliers/supplier-badges"
import { SupplierImage } from "@/components/suppliers/supplier-image"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import { Separator } from "@/components/ui/separator"
import { Skeleton } from "@/components/ui/skeleton"
import { useApi } from "@/hooks/use-api"
import { useSupplierPermissions } from "@/hooks/use-supplier-permissions"
import { ApiError } from "@/lib/api"
import { formatHours, formatLocation, mapsUrl, type Supplier } from "@/lib/suppliers"

function Row({ icon: Icon, label, children }: { icon: React.ElementType; label: string; children: React.ReactNode }) {
  return (
    <div className="flex gap-3">
      <Icon className="mt-0.5 size-4 shrink-0 text-muted-foreground" />
      <div className="flex min-w-0 flex-col">
        <span className="text-xs text-muted-foreground">{label}</span>
        <span className="text-sm">{children}</span>
      </div>
    </div>
  )
}

export function SupplierDetails({ id }: { id: string }) {
  const api = useApi()
  const { canManage } = useSupplierPermissions()
  const [supplier, setSupplier] = useState<Supplier | null>(null)
  const [error, setError] = useState<ApiError | null>(null)
  const [saving, setSaving] = useState(false)

  const load = useCallback(
    (signal?: AbortSignal) =>
      api<Supplier>(`/api/suppliers/${encodeURIComponent(id)}`, { signal })
        .then((data) => {
          setSupplier(data)
          setError(null)
        })
        .catch((err: unknown) => {
          if (signal?.aborted) return
          setError(err instanceof ApiError ? err : new ApiError(0, "UNKNOWN", "Could not load this supplier."))
        }),
    [api, id],
  )

  useEffect(() => {
    const controller = new AbortController()
    load(controller.signal)
    return () => controller.abort()
  }, [load])

  async function setActive(active: boolean) {
    setSaving(true)
    try {
      const updated = active
        ? await api<Supplier>(`/api/suppliers/${encodeURIComponent(id)}`, { method: "PATCH", body: { active: true } })
        : await api<Supplier>(`/api/suppliers/${encodeURIComponent(id)}`, { method: "DELETE" })
      setSupplier(updated)
      toast.success(active ? "Supplier reactivated" : "Supplier deactivated")
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : "Could not update the supplier.")
    } finally {
      setSaving(false)
    }
  }

  const back = (
    <Button variant="ghost" size="sm" nativeButton={false} render={<Link href="/suppliers" />} className="w-fit">
      <ArrowLeftIcon />
      All suppliers
    </Button>
  )

  if (error) {
    return (
      <div className="mx-auto flex w-full max-w-4xl flex-col gap-4 px-4 py-6">
        {back}
        <Alert variant="destructive">
          <AlertTitle>{error.status === 404 ? "Supplier not found" : "Supplier could not be loaded"}</AlertTitle>
          <AlertDescription>{error.message}</AlertDescription>
        </Alert>
      </div>
    )
  }

  if (!supplier) {
    return (
      <div className="mx-auto flex w-full max-w-4xl flex-col gap-4 px-4 py-6" aria-busy="true">
        {back}
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-64 w-full" />
      </div>
    )
  }

  return (
    <div className="mx-auto flex w-full max-w-4xl flex-col gap-5 px-4 py-6 sm:py-8">
      {back}

      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="flex min-w-0 flex-col gap-2">
          <h1 className="text-2xl font-semibold tracking-tight break-words">{supplier.name}</h1>
          <div className="flex flex-wrap items-center gap-2">
            <SupplierBadges supplier={supplier} />
          </div>
        </div>
        {canManage && (
          <div className="flex flex-wrap gap-2">
            <Button variant="outline" nativeButton={false} render={<Link href={`/suppliers/${supplier.id}/edit`} />}>
              <PencilIcon />
              Edit
            </Button>
            {supplier.active ? (
              <AlertDialog>
                <AlertDialogTrigger render={<Button variant="destructive" disabled={saving} />}>
                  <PowerIcon />
                  Deactivate
                </AlertDialogTrigger>
                <AlertDialogContent>
                  <AlertDialogHeader>
                    <AlertDialogTitle>Deactivate {supplier.name}?</AlertDialogTitle>
                    <AlertDialogDescription>
                      It will no longer be selectable for new errands. Existing errands keep their reference, and you
                      can reactivate it at any time.
                    </AlertDialogDescription>
                  </AlertDialogHeader>
                  <AlertDialogFooter>
                    <AlertDialogCancel>Cancel</AlertDialogCancel>
                    <AlertDialogAction variant="destructive" onClick={() => setActive(false)}>
                      Deactivate
                    </AlertDialogAction>
                  </AlertDialogFooter>
                </AlertDialogContent>
              </AlertDialog>
            ) : (
              <Button onClick={() => setActive(true)} disabled={saving}>
                <PowerIcon />
                Reactivate
              </Button>
            )}
          </div>
        )}
      </div>

      <div className="grid gap-5 md:grid-cols-[2fr_3fr]">
        {supplier.imageUrl && <SupplierImage src={supplier.imageUrl} alt={supplier.name} />}
        <Card className={supplier.imageUrl ? undefined : "md:col-span-2"}>
          <CardContent className="flex flex-col gap-4">
            <Row icon={TagIcon} label="Type">
              {supplier.type}
            </Row>
            <Row icon={MapPinIcon} label="Location">
              {formatLocation(supplier)}
              {supplier.locationDescription && (
                <span className="block text-muted-foreground">{supplier.locationDescription}</span>
              )}
            </Row>
            <Row icon={ClockIcon} label="Opening hours">
              {formatHours(supplier)}
            </Row>
            <Separator />
            <Button
              variant="outline"
              nativeButton={false}
              render={<a href={mapsUrl(supplier)} target="_blank" rel="noopener noreferrer" />}
              className="w-fit"
            >
              <ExternalLinkIcon />
              Open in Google Maps
            </Button>
            {canManage && (
              <p className="text-xs text-muted-foreground">
                ID {supplier.id} · Updated {new Date(supplier.updatedAt).toLocaleString()}
              </p>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
