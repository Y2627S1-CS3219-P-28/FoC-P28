import { ApiError, type RequestOptions } from "@/lib/api"
import type { CreditTransactionPage } from "@/lib/credit-transactions"
import type { Order, OrderStatus } from "@/lib/orders"
import type { Page, Supplier, SupplierInput } from "@/lib/suppliers"
import { adminOrderDateBounds } from "@/lib/admin-order-dates"

export type AdminOrder = Order & {
  requesterName: string | null
  courierName: string | null
  pickupSupplierName: string | null
  deliverySupplierName: string | null
}

export type AdminUser = {
  id: string | null
  userId: string
  username: string
  email: string
  roles: string[]
  penalty: number
  isCourierSuspended: boolean
  suspensionEndDate: string | null
  phoneNumber: string | null
  address: string | null
  active: boolean | null
  usableBalance: number | null
  reservedBalance: number | null
  totalBalance: number | null
}

export type AdminRoleContext = { userId: string; roles: string[] }
export type AdminSearch = { page: number; searchBy: "name" | "id"; q: string }
export type AdminOrderQuery = { page: number; status: OrderStatus | null; orderId: string; fromDate?: string; toDate?: string }
export type AdminRequest = <T>(path: string, options?: Omit<RequestOptions, "token">) => Promise<T>
export type AdminApiMode = "mock" | "live"
export type AdminOrderAction = "cancel"
export type ApplyPenaltyInput = { commandId: string; courierId: string; expectedVersion: number; policy: string; reason: string }
export type PenaltyPolicy = { policy: string; penaltyAmount: number }
export type AdminOrderResolution = {
  requesterId: string
  courierId: string | null
  appliedPenalties: string[]
  resolutionReason: string
}

export const ADMIN_ORDER_STATUSES: OrderStatus[] = [
  "OPEN", "ACCEPTED", "IN_PROGRESS", "PICKED_UP", "DELIVERED",
  "COMPLETED", "CANCELLED", "ABORTED", "EXPIRED",
]

const MOCK_ROOT = "/api/orders/admin-mock"
const ADMIN_TABLE_PAGE_SIZE = 20
const resource = (kind: string, id: string) => MOCK_ROOT + "/" + kind + "/" + encodeURIComponent(id)

/** Transport selection is explicit; live errors never fall back to sample data. */
export function createAdminApi(request: AdminRequest, mode: AdminApiMode = "mock") {
  if (mode === "live") return createLiveAdminApi(request)
  const search = (query: AdminSearch, size = 10) => new URLSearchParams({
    page: String(query.page), size: String(size), searchBy: query.searchBy, q: query.q.trim(),
  }).toString()

  return {
    mode,
    capabilities: { userDirectory: true, userActions: true, creditTransactions: true },
    penaltyPolicies: (signal?: AbortSignal) => request<PenaltyPolicy[]>("/api/orders/admin/penalty-policies", { signal }),
    applyPenalty: (id: string, body: ApplyPenaltyInput) => request<AdminOrder>(resource("orders", id) + "/apply-penalty", { method: "POST", body }),
    resolveOrder: (id: string, action: AdminOrderAction, input: AdminOrderResolution) =>
      request<AdminOrder>(resource("orders", id) + "/" + action, { method: "POST", body: input }),
    recordName: async () => null as string | null,
    roleContext: (userId: string, signal?: AbortSignal) =>
      request<AdminRoleContext>(MOCK_ROOT + "/role-context?" + new URLSearchParams({ userId }), { signal }),
    orders: (query: AdminOrderQuery, signal?: AbortSignal) => {
      const params = new URLSearchParams({ page: String(query.page), size: String(ADMIN_TABLE_PAGE_SIZE) })
      if (query.status) params.set("status", query.status)
      if (query.orderId.trim()) params.set("orderId", query.orderId.trim())
      addDateBounds(params, query)
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
      request<Page<AdminUser>>(MOCK_ROOT + "/users?" + search(query, ADMIN_TABLE_PAGE_SIZE), { signal }),
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

function createLiveAdminApi(request: AdminRequest) {
  const detail = (root: string, id: string) => root + "/" + encodeURIComponent(id)
  const supplier = (id: string, signal?: AbortSignal) => request<Supplier>(detail("/api/suppliers", id), { signal })
  const summary = (id: string, signal?: AbortSignal) =>
    request<Pick<AdminUser, "userId" | "username" | "email" | "roles" | "penalty" | "isCourierSuspended" | "suspensionEndDate">>(
      detail("/api/users/summary", id), { signal })
  const order = async (id: string, signal?: AbortSignal): Promise<AdminOrder> =>
    adminOrder(await request<Order>(detail("/api/orders", id), { signal }))
  const unavailable = async <T>(): Promise<T> => { throw new Error("Coming soon: this administrative API is not available yet.") }

  return {
    mode: "live" as const,
    capabilities: { userDirectory: false, userActions: false, creditTransactions: true },
    penaltyPolicies: (signal?: AbortSignal) => request<PenaltyPolicy[]>("/api/orders/admin/penalty-policies", { signal }),
    applyPenalty: async (id: string, body: ApplyPenaltyInput) => adminOrder(await request<Order>(detail("/api/orders/admin", id) + "/apply-penalty", { method: "POST", body })),
    resolveOrder: async (id: string, action: AdminOrderAction, input: AdminOrderResolution) =>
      adminOrder(await request<Order>(detail("/api/orders/admin", id) + "/" + action, { method: "POST", body: input })),
    roleContext: (_userId: string, signal?: AbortSignal) => request<AdminRoleContext>("/api/users/role-context", { signal }),
    orders: async (query: AdminOrderQuery, signal?: AbortSignal): Promise<Page<AdminOrder>> => {
      if (query.orderId.trim() && !query.fromDate && !query.toDate) {
        return exactPage(() => order(query.orderId.trim(), signal), query.page,
          (result) => !query.status || result.status === query.status, ADMIN_TABLE_PAGE_SIZE)
      }
      const params = new URLSearchParams({ page: String(query.page), size: String(ADMIN_TABLE_PAGE_SIZE) })
      if (query.status) params.set("status", query.status)
      if (query.orderId.trim()) params.set("orderId", query.orderId.trim())
      addDateBounds(params, query)
      const result = await request<Page<Order>>("/api/orders?" + params, { signal })
      return { ...result, items: result.items.map(adminOrder) }
    },
    order,
    // Only the existing public summary is read until the full admin profile is ready.
    user: async (id: string, signal?: AbortSignal): Promise<AdminUser> => {
      const result = await summary(id, signal)
      if (result.userId !== id) throw new Error("User summary does not match the requested user.")
      return { ...result, id: null, phoneNumber: null, address: null, active: null,
        usableBalance: null, reservedBalance: null, totalBalance: null }
    },
    users: unavailable<Page<AdminUser>>,
    userTransactions: (id: string, page: number, signal?: AbortSignal) =>
      request<CreditTransactionPage>(detail("/api/credits/users", id) + "/transactions?" +
        new URLSearchParams({ page: String(page), size: "20" }), { signal }),
    suspendUser: unavailable<AdminUser>,
    promoteUser: unavailable<AdminUser>,
    deactivateUser: unavailable<AdminUser>,
    suppliers: (query: AdminSearch, signal?: AbortSignal): Promise<Page<Supplier>> => {
      if (query.searchBy === "id" && query.q.trim()) return exactPage(() => supplier(query.q.trim(), signal), query.page)
      const params = new URLSearchParams({ page: String(query.page), size: "10", status: "all" })
      if (query.q.trim()) params.set("q", query.q.trim())
      return request<Page<Supplier>>("/api/suppliers?" + params, { signal })
    },
    supplier,
    createSupplier: (input: SupplierInput) => request<Supplier>("/api/suppliers", { method: "POST", body: input }),
    deactivateSupplier: (id: string) => request<Supplier>(detail("/api/suppliers", id), { method: "DELETE" }),
    // Label requests are independent of the primary Order query and its loading state.
    recordName: async (kind: "users" | "suppliers", id: string, signal?: AbortSignal): Promise<string | null> =>
    {
      if (kind === "users") {
        const result = await summary(id, signal)
        if (!result) return null
        if (result.userId !== id) throw new Error("User summary does not match the requested user.")
        return result.username?.trim() || null
      }
      const result = await supplier(id, signal)
      if (!result) return null
      if (result.id !== id) throw new Error("Supplier does not match the requested supplier.")
      return result.name?.trim() || null
    },
  }
}

function addDateBounds(params: URLSearchParams, query: AdminOrderQuery) {
  const bounds = adminOrderDateBounds(query.fromDate, query.toDate)
  if (bounds.createdFrom) params.set("createdFrom", bounds.createdFrom)
  if (bounds.createdBefore) params.set("createdBefore", bounds.createdBefore)
}

function adminOrder(order: Order): AdminOrder {
  return { ...order, requesterName: null, courierName: null, pickupSupplierName: null, deliverySupplierName: null }
}

async function exactPage<T>(load: () => Promise<T>, page: number, matches: (item: T) => boolean = () => true, size = 10): Promise<Page<T>> {
  try {
    const item = await load()
    const totalItems = matches(item) ? 1 : 0
    return { items: page === 1 && totalItems ? [item] : [], page, size, totalItems, totalPages: totalItems }
  } catch (error) {
    if (!(error instanceof ApiError) || error.status !== 404) throw error
    return { items: [], page, size, totalItems: 0, totalPages: 0 }
  }
}
