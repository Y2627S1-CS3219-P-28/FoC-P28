"use client"

import Link from "next/link"
import { useRouter } from "next/navigation"
import { useEffect, useState } from "react"
import { ArrowLeftIcon } from "lucide-react"
import { toast } from "sonner"

import { SupplierForm } from "@/components/suppliers/supplier-form"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"
import { useApi } from "@/hooks/use-api"
import { useSupplierPermissions } from "@/hooks/use-supplier-permissions"
import { toInput, type Supplier, type SupplierInput } from "@/lib/suppliers"

/** Create (no id) or edit (id) a supplier. Admin-only; the service rejects anyone else anyway. */
export function SupplierEditor({ id }: { id?: string }) {
  const api = useApi()
  const router = useRouter()
  const { permissions, canManage } = useSupplierPermissions()
  const [existing, setExisting] = useState<Supplier | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [types, setTypes] = useState<string[]>([])

  useEffect(() => {
    api<{ items: string[] }>("/api/suppliers/types").then((d) => setTypes(d.items)).catch(() => undefined)
    if (id) {
      api<Supplier>(`/api/suppliers/${encodeURIComponent(id)}`)
        .then(setExisting)
        .catch((e: Error) => setLoadError(e.message))
    }
  }, [api, id])

  async function save(input: SupplierInput) {
    if (id) {
      await api<Supplier>(`/api/suppliers/${encodeURIComponent(id)}`, { method: "PATCH", body: input })
      toast.success("Supplier updated")
      router.push(`/suppliers/${id}`)
    } else {
      // Optional fields are omitted rather than sent empty when creating.
      const body = Object.fromEntries(Object.entries(input).filter(([, v]) => v !== ""))
      const created = await api<Supplier>("/api/suppliers", { method: "POST", body })
      toast.success("Supplier created")
      router.push(`/suppliers/${created.id}`)
    }
  }

  const backHref = id ? `/suppliers/${id}` : "/suppliers"
  const ready = permissions !== null && (!id || existing || loadError)

  return (
    <div className="mx-auto flex w-full max-w-3xl flex-col gap-4 px-4 py-6 sm:py-8">
      <Button variant="ghost" size="sm" nativeButton={false} render={<Link href={backHref} />} className="w-fit">
        <ArrowLeftIcon />
        Back
      </Button>
      {!ready ? (
        <Skeleton className="h-96 w-full" />
      ) : !canManage ? (
        <Alert variant="destructive">
          <AlertTitle>Administrators only</AlertTitle>
          <AlertDescription>Only administrators can create or edit suppliers.</AlertDescription>
        </Alert>
      ) : loadError ? (
        <Alert variant="destructive">
          <AlertTitle>Supplier could not be loaded</AlertTitle>
          <AlertDescription>{loadError}</AlertDescription>
        </Alert>
      ) : (
        <Card>
          <CardHeader>
            <CardTitle className="text-xl">{id ? `Edit ${existing?.name}` : "New supplier"}</CardTitle>
            <CardDescription>
              {id ? "Changes apply immediately to new errands." : "Add a store, facility or landmark to the catalogue."}
            </CardDescription>
          </CardHeader>
          <CardContent>
            <SupplierForm
              initial={existing ? toInput(existing) : undefined}
              types={types}
              submitLabel={id ? "Save changes" : "Create supplier"}
              onSubmit={save}
              onCancel={() => router.push(backHref)}
            />
          </CardContent>
        </Card>
      )}
    </div>
  )
}
