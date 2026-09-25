import type { Metadata } from "next"

import { RequireAuth } from "@/components/require-auth"
import { SupplierEditor } from "@/components/suppliers/supplier-editor"

export const metadata: Metadata = { title: "New supplier" }

export default function NewSupplierPage() {
  return (
    <RequireAuth>
      <SupplierEditor />
    </RequireAuth>
  )
}
