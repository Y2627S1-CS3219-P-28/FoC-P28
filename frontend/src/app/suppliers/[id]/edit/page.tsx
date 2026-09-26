"use client"

import { useParams } from "next/navigation"

import { RequireAuth } from "@/components/require-auth"
import { SupplierEditor } from "@/components/suppliers/supplier-editor"

export default function EditSupplierPage() {
  const { id } = useParams<{ id: string }>()
  return (
    <RequireAuth>
      <SupplierEditor id={id} />
    </RequireAuth>
  )
}
