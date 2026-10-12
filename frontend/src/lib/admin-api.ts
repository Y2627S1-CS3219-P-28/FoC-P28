import type { RequestOptions } from "@/lib/api"
import type { CreditTransactionPage } from "@/lib/credit-transactions"
import type { Order, OrderStatus } from "@/lib/orders"
import type { Page, Supplier, SupplierInput } from "@/lib/suppliers"

export type AdminOrder = Order & {
  requesterName: string | null
  courierName: string | null
  pickupSupplierName: string | null
  deliverySupplierName: string | null
}

export type AdminUser = {
  id: string
  userId: string
  username: string
  email: string
  roles: string[]
  penalty: number
  isCourierSuspended: boolean
  suspensionEndDate: string | null
  phoneNumber: string | null
  address: string | null
  active: boolean
  usableBalance: number
  reservedBalance: number
  totalBalance: number
}

export type AdminRoleContext = { userId: string; roles: string[] }
export type AdminSearch = { page: number; searchBy: "name" | "id"; q: string }
export type AdminOrderQuery = { page: number; status: OrderStatus | null; orderId: string }
export type AdminRequest = <T>(path: string, options?: Omit<RequestOptions, "token">) => Promise<T>

export const ADMIN_ORDER_STATUSES: OrderStatus[] = [
  "OPEN", "ACCEPTED", "IN_PROGRESS", "PICKED_UP", "DELIVERED",
  "COMPLETED", "CANCELLED", "ABORTED", "EXPIRED",
]

const MOCK_ROOT = "/api/orders/admin-mock"
const resource = (kind: string, id: string) => MOCK_ROOT + "/" + kind + "/" + encodeURIComponent(id)

/** All prototype transport lives here. Replace this adapter with real providers later. */
export function createAdminApi(request: AdminRequest) {
  const search = (query: AdminSearch) => new URLSearchParams({
    page: String(query.page), size: "10", searchBy: query.searchBy, q: query.q.trim(),
  }).toString()

  return {
    roleContext: (userId: string, signal?: AbortSignal) =>
      request<AdminRoleContext>(MOCK_ROOT + "/role-context?" + new URLSearchParams({ userId }), { signal }),
    orders: (query: AdminOrderQuery, signal?: AbortSignal) => {
      const params = new URLSearchParams({ page: String(query.page), size: "10" })
      if (query.status) params.set("status", query.status)
      if (query.orderId.trim()) params.set("orderId", query.orderId.trim())
      return request<Page<AdminOrder>>(MOCK_ROOT + "/orders?" + params, { signal })
    },
    order: async (id: string, signal?: AbortSignal) => {
      const params = new URLSearchParams({ page: "1", size: "1", orderId: id })
      const result = await request<Page<AdminOrder>>(MOCK_ROOT + "/orders?" + params, { signal })
      const order = result.items[0]
      if (!order || order.id !== id) throw new Error("Order not found.")
      return order
    },
    users: (query: AdminSearch, signal?: AbortSignal) =>
      request<Page<AdminUser>>(MOCK_ROOT + "/users?" + search(query), { signal }),
    user: (id: string, signal?: AbortSignal) => request<AdminUser>(resource("users", id), { signal }),
    userTransactions: (id: string, page: number, signal?: AbortSignal) =>
      request<CreditTransactionPage>(resource("users", id) + "/transactions?" + new URLSearchParams({ page: String(page), size: "20" }), { signal }),
    suspendUser: (id: string, suspensionEndDate: string) =>
      request<AdminUser>(resource("users", id) + "/suspend", { method: "POST", body: { suspensionEndDate } }),
    promoteUser: (id: string) =>
      request<AdminUser>(resource("users", id) + "/promote", { method: "POST" }),
    deactivateUser: (id: string) =>
      request<AdminUser>(resource("users", id) + "/deactivate", { method: "POST" }),
    suppliers: (query: AdminSearch, signal?: AbortSignal) =>
      request<Page<Supplier>>(MOCK_ROOT + "/suppliers?" + search(query), { signal }),
    supplier: (id: string, signal?: AbortSignal) => request<Supplier>(resource("suppliers", id), { signal }),
    createSupplier: (input: SupplierInput) =>
      request<Supplier>(MOCK_ROOT + "/suppliers", { method: "POST", body: input }),
    deactivateSupplier: (id: string) =>
      request<Supplier>(resource("suppliers", id) + "/deactivate", { method: "POST" }),
  }
}
