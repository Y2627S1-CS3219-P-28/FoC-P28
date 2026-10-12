import { AdminGate } from "@/components/admin/admin-mode-provider"
import { RequireAuth } from "@/components/require-auth"

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  return <RequireAuth><AdminGate>{children}</AdminGate></RequireAuth>
}
