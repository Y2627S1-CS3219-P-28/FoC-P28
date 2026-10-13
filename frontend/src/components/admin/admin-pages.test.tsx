import { cleanup, fireEvent, render, screen, waitFor, within } from "@testing-library/react"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

import { AdminOrders } from "./admin-orders"
import { AdminUsers } from "./admin-users"
import { AdminSuppliers } from "./admin-suppliers"
import { CopyId } from "./admin-ui"
import { AdminSelectedUser } from "./admin-selected-user"

const mocks = vi.hoisted(() => ({
  mode: "mock" as "mock" | "live",
  capabilities: { userDirectory: true, userActions: true, creditTransactions: true },
  orders: vi.fn(), users: vi.fn(), user: vi.fn(), suppliers: vi.fn(),
  order: vi.fn(), push: vi.fn(),
  recordName: vi.fn(),
  supplier: vi.fn(), userTransactions: vi.fn(),
  suspendUser: vi.fn(), promoteUser: vi.fn(), deactivateUser: vi.fn(),
  createSupplier: vi.fn(), deactivateSupplier: vi.fn(),
}))
vi.mock("@/hooks/use-admin-api", () => ({ useAdminApi: () => mocks }))
vi.mock("sonner", () => ({ toast: { success: vi.fn(), error: vi.fn() } }))
vi.mock("next/navigation", () => ({ useRouter: () => ({ push: mocks.push }) }))

const user = {
  id: "profile-1", userId: "user-1", username: "Alex", email: "alex@u.nus.edu",
  roles: ["requester", "courier"], active: true, penalty: 1, isCourierSuspended: false,
  suspensionEndDate: null, phoneNumber: "80000000", address: "NUS",
  usableBalance: 100, reservedBalance: 5, totalBalance: 105,
}
const supplier = {
  id: "supplier-1", name: "The Deck", type: "Food", building: "Arts", floor: "1",
  active: true, openingTime: "09:00", closingTime: "18:00", latitude: 1, longitude: 103,
  locationDescription: "Lobby", imageUrl: null, openNow: false,
  createdAt: "2026-10-01T00:00:00Z", updatedAt: "2026-10-01T00:00:00Z",
}
const order = {
  id: "order-1", itemDescription: "Lunch delivery", status: "OPEN", offeredCredits: 5,
  requesterId: "user-1", requesterName: "Alex", courierId: null, courierName: null,
  pickupSupplierId: "supplier-1", pickupSupplierName: "The Deck",
  deliverySupplierId: "supplier-2", deliverySupplierName: "Library",
  createdAt: "2026-10-01T00:00:00Z", expiresAt: "2026-10-01T02:00:00Z",
}
const page = (items: unknown[], current = 1) => ({ items, page: current, size: 10, totalItems: 12, totalPages: 2 })
beforeEach(() => {
  vi.clearAllMocks()
  mocks.mode = "mock"
  mocks.capabilities = { userDirectory: true, userActions: true, creditTransactions: true }
  mocks.orders.mockImplementation((query) => Promise.resolve(page([order], query.page)))
  mocks.order.mockResolvedValue(order)
  mocks.users.mockResolvedValue(page([user]))
  mocks.user.mockResolvedValue(user)
  mocks.supplier.mockResolvedValue(supplier)
  mocks.userTransactions.mockResolvedValue(page([]))
  mocks.suppliers.mockResolvedValue(page([supplier]))
  mocks.promoteUser.mockResolvedValue({ ...user, roles: [...user.roles, "admin"] })
  mocks.suspendUser.mockResolvedValue({ ...user, isCourierSuspended: true })
  mocks.deactivateUser.mockResolvedValue({ ...user, active: false })
  mocks.deactivateSupplier.mockResolvedValue({ ...supplier, active: false })
})
afterEach(cleanup)

describe("Admin pages", () => {
  it("shows live orders and navigation immediately even while names are pending or unavailable", async () => {
    mocks.mode = "live"
    mocks.orders.mockResolvedValue(page([{ ...order, requesterName: null, pickupSupplierName: null, deliverySupplierName: null }]))
    mocks.recordName.mockImplementation(() => new Promise(() => {}))
    render(<AdminOrders />)
    expect(await screen.findByRole("link", { name: "View order order-1" })).toBeInTheDocument()
    expect(screen.getByRole("link", { name: "user-1" })).toHaveAttribute("href", "/admin/users/user-1")
    expect(screen.getByRole("link", { name: "supplier-1" })).toHaveAttribute("href", "/admin/suppliers/supplier-1")
    expect(screen.queryByRole("status")).not.toBeInTheDocument()
  })

  it("opens an existing user summary by exact UID when the live directory is not ready", async () => {
    mocks.mode = "live"
    mocks.capabilities = { userDirectory: false, userActions: false, creditTransactions: false }
    render(<AdminUsers />)
    fireEvent.change(screen.getByLabelText("User ID"), { target: { value: " user-1 " } })
    fireEvent.click(screen.getByRole("button", { name: "View profile" }))
    expect(await screen.findByText("alex@u.nus.edu")).toBeInTheDocument()
    expect(mocks.user).toHaveBeenCalledWith("user-1", expect.any(AbortSignal))
    expect(mocks.users).not.toHaveBeenCalled()
    expect(mocks.userTransactions).not.toHaveBeenCalled()
  })

  it("shows a live directory Coming soon card without loading the missing list endpoint", async () => {
    mocks.mode = "live"
    mocks.capabilities = { userDirectory: false, userActions: false, creditTransactions: false }
    render(<AdminUsers />)
    expect(screen.getByText("User directory")).toBeInTheDocument()
    expect(screen.getByText(/Coming soon/)).toBeInTheDocument()
    expect(mocks.users).not.toHaveBeenCalled()
    expect(screen.queryByText(/Changes reset/)).not.toBeInTheDocument()
  })

  it("keeps live user summary visible with missing-value placeholders and no Credit or mutation calls", async () => {
    mocks.mode = "live"
    mocks.capabilities = { userDirectory: false, userActions: false, creditTransactions: false }
    mocks.user.mockResolvedValue({ ...user, id: null, active: null, phoneNumber: null, address: null,
      usableBalance: null, reservedBalance: null, totalBalance: null })
    render(<AdminSelectedUser userId="user-1" />)
    expect(await screen.findByText("alex@u.nus.edu")).toBeInTheDocument()
    expect(screen.getByText("Credit transactions")).toBeInTheDocument()
    expect(screen.getAllByText("Coming soon").length).toBeGreaterThan(3)
    for (const name of ["Suspend User", "Promote to Admin", "Deactivate User"]) {
      const button = screen.getByRole("button", { name })
      expect(button).toBeDisabled()
      fireEvent.click(button)
    }
    expect(mocks.userTransactions).not.toHaveBeenCalled()
    expect(mocks.suspendUser).not.toHaveBeenCalled()
    expect(mocks.promoteUser).not.toHaveBeenCalled()
    expect(mocks.deactivateUser).not.toHaveBeenCalled()
    expect(screen.queryByText("Inactive")).not.toBeInTheDocument()
    expect(screen.getByRole("button", { name: "Copy user-1 ID" })).toBeInTheDocument()
    expect(screen.queryByText(/Demo data/)).not.toBeInTheDocument()
  })

  it("paginates and searches orders on the server and exposes every status", async () => {
    render(<AdminOrders />)
    await screen.findByRole("link", { name: "View order order-1" })
    fireEvent.click(screen.getByRole("button", { name: "Next" }))
    await waitFor(() => expect(mocks.orders).toHaveBeenLastCalledWith(
      { page: 2, status: null, orderId: "" }, expect.any(AbortSignal),
    ))
    fireEvent.change(screen.getByLabelText("Order ID"), { target: { value: "order-1" } })
    fireEvent.click(screen.getByRole("button", { name: "Search" }))
    await waitFor(() => expect(mocks.orders).toHaveBeenLastCalledWith(
      { page: 1, status: null, orderId: "order-1" }, expect.any(AbortSignal),
    ))
    fireEvent.click(screen.getByRole("combobox", { name: "Order status" }))
    expect(await screen.findByRole("option", { name: "Aborted" })).toBeInTheDocument()
    fireEvent.pointerDown(screen.getByRole("option", { name: "Completed" }), { pointerType: "mouse" })
    fireEvent.click(screen.getByRole("option", { name: "Completed" }))
    await waitFor(() => expect(mocks.orders).toHaveBeenLastCalledWith(
      { page: 1, status: "COMPLETED", orderId: "order-1" }, expect.any(AbortSignal),
    ))
  })

  it("copies the underlying supplier ID when its display name is clicked", async () => {
    const writeText = vi.fn().mockResolvedValue(undefined)
    Object.defineProperty(navigator, "clipboard", { configurable: true, value: { writeText } })
    render(<CopyId id="supplier-1" name="The Deck" />)
    fireEvent.click(screen.getByRole("button", { name: "Copy The Deck ID" }))
    await waitFor(() => expect(writeText).toHaveBeenCalledWith("supplier-1"))
  })

  it("loads a full user profile and applies suspension in UTC", async () => {
    render(<AdminUsers />)
    fireEvent.click(await screen.findByRole("button", { name: "View profile of Alex" }))
    expect(await screen.findByText("80000000")).toBeInTheDocument()
    fireEvent.click(screen.getByRole("button", { name: "Suspend User" }))
    fireEvent.change(screen.getByLabelText("Suspension end (your local time)"), { target: { value: "2099-11-01T12:00" } })
    fireEvent.click(screen.getByRole("button", { name: "Confirm suspension" }))
    await waitFor(() => expect(mocks.suspendUser).toHaveBeenCalledWith("user-1", new Date("2099-11-01T12:00").toISOString()))
  })

  it("rejects a past suspension without calling the API", async () => {
    render(<AdminUsers />)
    fireEvent.click(await screen.findByRole("button", { name: "View profile of Alex" }))
    fireEvent.click(await screen.findByRole("button", { name: "Suspend User" }))
    fireEvent.change(screen.getByLabelText("Suspension end (your local time)"), { target: { value: "2020-01-01T12:00" } })
    fireEvent.click(screen.getByRole("button", { name: "Confirm suspension" }))
    expect(screen.getByRole("alert")).toHaveTextContent("in the future")
    expect(mocks.suspendUser).not.toHaveBeenCalled()
  })

  it("requires confirmation before deactivating a user", async () => {
    render(<AdminUsers />)
    fireEvent.click(await screen.findByRole("button", { name: "View profile of Alex" }))
    fireEvent.click(await screen.findByRole("button", { name: "Deactivate User" }))
    expect(await screen.findByRole("alertdialog")).toBeInTheDocument()
    expect(mocks.deactivateUser).not.toHaveBeenCalled()
    fireEvent.click(screen.getByRole("button", { name: "Confirm deactivation" }))
    await waitFor(() => expect(mocks.deactivateUser).toHaveBeenCalledWith("user-1"))
  })

  it("promotes a user and prevents duplicate promotion controls", async () => {
    render(<AdminUsers />)
    fireEvent.click(await screen.findByRole("button", { name: "View profile of Alex" }))
    fireEvent.click(await screen.findByRole("button", { name: "Promote to Admin" }))
    expect(await screen.findByRole("button", { name: "Already an admin" })).toBeDisabled()
    expect(mocks.promoteUser).toHaveBeenCalledWith("user-1")
  })

  it("requires confirmation before deactivating a supplier", async () => {
    render(<AdminSuppliers initialSupplierId="supplier-1" />)
    fireEvent.click(await screen.findByRole("button", { name: "Deactivate The Deck" }))
    expect(mocks.deactivateSupplier).not.toHaveBeenCalled()
    fireEvent.click(await screen.findByRole("button", { name: "Confirm deactivation" }))
    await waitFor(() => expect(mocks.deactivateSupplier).toHaveBeenCalledWith("supplier-1"))
  })

  it("creates a supplier through the existing form and mock adapter", async () => {
    mocks.createSupplier.mockResolvedValue({ ...supplier, id: "new-supplier" })
    render(<AdminSuppliers />)
    fireEvent.click(screen.getByRole("button", { name: "Create supplier" }))
    const dialog = within(await screen.findByRole("dialog"))
    for (const [label, value] of [["Name", "Printing"], ["Type", "Printing"], ["Building", "COM3"],
      ["Latitude", "1.294"], ["Longitude", "103.773"]]) {
      fireEvent.change(dialog.getByLabelText(label), { target: { value } })
    }
    fireEvent.click(dialog.getByRole("button", { name: "Create supplier" }))
    await waitFor(() => expect(mocks.createSupplier).toHaveBeenCalledWith(expect.objectContaining({
      name: "Printing", type: "Printing", building: "COM3", latitude: 1.294, longitude: 103.773,
      openingTime: "09:00", closingTime: "18:00",
    })))
    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument())
  })

  it("keeps a supplier active when its confirmation is cancelled", async () => {
    render(<AdminSuppliers initialSupplierId="supplier-1" />)
    fireEvent.click(await screen.findByRole("button", { name: "Deactivate The Deck" }))
    fireEvent.click(await screen.findByRole("button", { name: "Keep active" }))
    expect(mocks.deactivateSupplier).not.toHaveBeenCalled()
  })

  it("retains the confirmation and reports a failed supplier mutation", async () => {
    mocks.deactivateSupplier.mockRejectedValue(new Error("Update failed"))
    render(<AdminSuppliers initialSupplierId="supplier-1" />)
    fireEvent.click(await screen.findByRole("button", { name: "Deactivate The Deck" }))
    fireEvent.click(await screen.findByRole("button", { name: "Confirm deactivation" }))
    expect(await screen.findByRole("alert")).toHaveTextContent("Update failed")
    expect(screen.getByRole("alertdialog")).toBeInTheDocument()
    expect(screen.getByRole("button", { name: "Confirm deactivation" })).toBeEnabled()
  })

  it.each([
    ["Users", AdminUsers, mocks.users],
    ["Suppliers", AdminSuppliers, mocks.suppliers],
  ] as const)("searches %s by ID using the dropdown", async (label, Component, request) => {
    render(<Component />)
    await waitFor(() => expect(request).toHaveBeenCalled())
    fireEvent.click(screen.getByRole("combobox", { name: label + " search type" }))
    fireEvent.pointerDown(await screen.findByRole("option", { name: "ID" }), { pointerType: "mouse" })
    fireEvent.click(screen.getByRole("option", { name: "ID" }))
    fireEvent.change(screen.getByLabelText("Search " + label.toLowerCase()), { target: { value: "exact-id" } })
    fireEvent.click(screen.getByRole("button", { name: "Search" }))
    await waitFor(() => expect(request).toHaveBeenLastCalledWith(
      { page: 1, searchBy: "id", q: "exact-id" }, expect.any(AbortSignal),
    ))
  })

  it("shows a failed list request with an actionable retry", async () => {
    mocks.orders.mockRejectedValueOnce(new Error("Service unavailable")).mockResolvedValue(page([order]))
    render(<AdminOrders />)
    expect(await screen.findByRole("alert")).toHaveTextContent("Service unavailable")
    fireEvent.click(screen.getByRole("button", { name: "Try again" }))
    expect(await screen.findByRole("link", { name: "View order order-1" })).toBeInTheDocument()
  })

  it("links each related record to its Admin detail route and keeps descriptions in View only", async () => {
    render(<AdminOrders />)
    expect(await screen.findByRole("link", { name: "Alex" })).toHaveAttribute("href", "/admin/users/user-1")
    expect(screen.getByRole("link", { name: "The Deck" })).toHaveAttribute("href", "/admin/suppliers/supplier-1")
    expect(screen.queryByText("Lunch delivery")).not.toBeInTheDocument()
    for (const name of ["Requester", "Courier", "Pickup", "Delivery", "Created", "Expires"]) {
      expect(screen.getByRole("columnheader", { name })).toBeInTheDocument()
    }
    expect(screen.getByRole("link", { name: "View order order-1" })).toHaveAttribute("href", "/admin/orders/order-1")
  })

  it("renders the profile in the Users tab with its own transaction history and copyable ID", async () => {
    const writeText = vi.fn().mockResolvedValue(undefined)
    Object.defineProperty(navigator, "clipboard", { configurable: true, value: { writeText } })
    render(<AdminUsers />)
    fireEvent.click(await screen.findByRole("button", { name: "View profile of Alex" }))
    await screen.findByText("80000000")
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument()
    expect(screen.getByText("Profile summary")).toBeInTheDocument()
    expect(await screen.findByText("Credit transactions")).toBeInTheDocument()
    expect(mocks.userTransactions).toHaveBeenCalledWith("user-1", 1, expect.any(AbortSignal))
    fireEvent.click(screen.getByRole("button", { name: "Copy user-1 ID" }))
    await waitFor(() => expect(writeText).toHaveBeenCalledWith("user-1"))
    fireEvent.click(screen.getByRole("button", { name: "Back to users" }))
    expect(await screen.findByRole("button", { name: "View profile of Alex" })).toBeInTheDocument()
  })

  it("uses the normal Supplier status badge", async () => {
    render(<AdminSuppliers />)
    expect(await screen.findByText("Closed")).toBeInTheDocument()
    expect(screen.getByRole("link", { name: "View details" })).toHaveAttribute("href", "/admin/suppliers/supplier-1")
  })

  it("opens a deep-linked user without requiring the list and preserves the profile if history fails", async () => {
    mocks.userTransactions.mockRejectedValue(new Error("History unavailable"))
    render(<AdminSelectedUser userId="user-1" />)
    expect(await screen.findByText("80000000")).toBeInTheDocument()
    expect(await screen.findByText("Transactions could not be refreshed")).toBeInTheDocument()
    expect(screen.getByRole("button", { name: "Promote to Admin" })).toBeEnabled()
    expect(mocks.user).toHaveBeenCalledWith("user-1", expect.any(AbortSignal))
    expect(mocks.users).not.toHaveBeenCalled()
  })

  it("opens a deep-linked supplier that is absent from the current list", async () => {
    mocks.suppliers.mockResolvedValue(page([]))
    render(<AdminSuppliers initialSupplierId="supplier-1" />)
    expect(await screen.findByText("Lobby")).toBeInTheDocument()
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument()
    expect(screen.getByRole("button", { name: "Back to suppliers" })).toBeInTheDocument()
    expect(mocks.supplier).toHaveBeenCalledWith("supplier-1", expect.any(AbortSignal))
  })

  it("paginates the selected user's history without switching identity", async () => {
    mocks.userTransactions.mockImplementation((_id, current) => Promise.resolve(page([
      { transactionId: "t-1", type: "RECEIVED", amount: 10, direction: "CREDIT", occurredAt: "2026-10-11T08:00:00Z", orderId: "order-1" },
    ], current)))
    render(<AdminSelectedUser userId="user-1" />)
    await screen.findByText("Credit transactions")
    await screen.findAllByText("+10 credits")
    fireEvent.click(screen.getByRole("button", { name: "Next" }))
    await waitFor(() => expect(mocks.userTransactions).toHaveBeenLastCalledWith("user-1", 2, expect.any(AbortSignal)))
  })

  it("discards a late profile response after selecting another user", async () => {
    let resolveFirst!: (value: typeof user) => void
    mocks.user.mockImplementation((id) => id === "first" ? new Promise((resolve) => { resolveFirst = resolve }) : Promise.resolve({ ...user, userId: "second", username: "Jamie" }))
    const view = render(<AdminSelectedUser userId="first" />)
    view.rerender(<AdminSelectedUser userId="second" />)
    expect(await screen.findByRole("heading", { name: "Jamie" })).toBeInTheDocument()
    resolveFirst({ ...user, userId: "first" })
    await waitFor(() => expect(screen.queryByRole("heading", { name: "Alex" })).not.toBeInTheDocument())
  })

  it("places user admin actions before profile and credit history", async () => {
    render(<AdminSelectedUser userId="user-1" />)
    const actions = await screen.findByText("Admin actions")
    const summary = screen.getByText("Profile summary")
    const history = await screen.findByText("Credit transactions")
    expect(actions.compareDocumentPosition(summary) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
    expect(actions.compareDocumentPosition(history) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
  })

  it("loads a full Order page independently of the list and retains description", async () => {
    render(<AdminOrders initialOrderId="order-1" />)
    expect(await screen.findByText("Lunch delivery")).toBeInTheDocument()
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument()
    expect(mocks.order).toHaveBeenCalledWith("order-1", expect.any(AbortSignal))
    expect(mocks.orders).not.toHaveBeenCalled()
    fireEvent.click(screen.getByRole("button", { name: "Back to orders" }))
    expect(mocks.push).toHaveBeenCalledWith("/admin/orders")
  })

  it("retains a failed Order detail's retry and back controls", async () => {
    mocks.order.mockRejectedValueOnce(new Error("Order not found.")).mockResolvedValue(order)
    render(<AdminOrders initialOrderId="order-1" />)
    expect(await screen.findByRole("alert")).toHaveTextContent("Order not found.")
    expect(screen.getByRole("button", { name: "Back to orders" })).toBeEnabled()
    fireEvent.click(screen.getByRole("button", { name: "Try again" }))
    expect(await screen.findByText("Lunch delivery")).toBeInTheDocument()
  })

  it("places supplier actions above details and confirms deactivation", async () => {
    render(<AdminSuppliers initialSupplierId="supplier-1" />)
    const actions = await screen.findByText("Admin actions")
    expect(actions.compareDocumentPosition(screen.getByText("Location details")) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
    fireEvent.click(screen.getByRole("button", { name: "Deactivate The Deck" }))
    expect(mocks.deactivateSupplier).not.toHaveBeenCalled()
    fireEvent.click(await screen.findByRole("button", { name: "Confirm deactivation" }))
    await waitFor(() => expect(mocks.deactivateSupplier).toHaveBeenCalledWith("supplier-1"))
    await waitFor(() => expect(screen.getByRole("button", { name: "Deactivate The Deck" })).toBeDisabled())
    fireEvent.click(screen.getByRole("button", { name: "Back to suppliers" }))
    expect(mocks.push).toHaveBeenCalledWith("/admin/suppliers")
  })

  it("retains the Supplier profile and confirmation after a failed deactivation", async () => {
    mocks.deactivateSupplier.mockRejectedValue(new Error("Update failed"))
    render(<AdminSuppliers initialSupplierId="supplier-1" />)
    fireEvent.click(await screen.findByRole("button", { name: "Deactivate The Deck" }))
    fireEvent.click(await screen.findByRole("button", { name: "Confirm deactivation" }))
    expect(await screen.findByRole("alert")).toHaveTextContent("Update failed")
    expect(screen.getByRole("alertdialog")).toBeInTheDocument()
    expect(screen.getByText("Location details")).toBeInTheDocument()
    expect(screen.getByRole("button", { name: "Confirm deactivation" })).toBeEnabled()
    expect(mocks.push).not.toHaveBeenCalled()
  })

  it("keeps supplier card browsing but requires opening details before deactivation", async () => {
    render(<AdminSuppliers />)
    expect(await screen.findByRole("link", { name: "View details" })).toBeInTheDocument()
    expect(screen.queryByRole("button", { name: "Deactivate The Deck" })).not.toBeInTheDocument()
    expect(screen.queryByRole("region", { name: "Supplier locations" })).not.toBeInTheDocument()
  })

  it("separates user identity and account values into individual columns", async () => {
    render(<AdminUsers />)
    await screen.findByRole("button", { name: "View profile of Alex" })
    for (const name of ["Name", "Email", "User ID", "Account status", "Courier status", "Available credits", "Reserved credits", "Total credits"]) {
      expect(screen.getByRole("columnheader", { name })).toBeInTheDocument()
    }
    const nameCell = screen.getByRole("button", { name: "Alex" }).closest("td")
    expect(nameCell).not.toHaveTextContent(user.email)
    expect(nameCell).not.toHaveTextContent(user.userId)
  })

  it("groups Order details without losing repost facts and navigates related names separately from copying IDs", async () => {
    mocks.order.mockResolvedValue({ ...order, courierId: "user-2", courierName: "Jamie", originalOrderId: "original-1", automaticRepostEnabled: true, repostCreditAmount: 8, auditNote: "Extra returned information" })
    render(<AdminOrders initialOrderId="order-1" />)
    await screen.findByText("Lunch delivery")
    for (const title of ["Order information", "People", "Locations", "Status and timing", "Repost information", "Other information"]) {
      expect(screen.getByRole("heading", { name: title })).toBeInTheDocument()
    }
    expect(screen.getByRole("link", { name: "The Deck" })).toHaveAttribute("href", "/admin/suppliers/supplier-1")
    expect(screen.getByRole("link", { name: "Alex" })).toHaveAttribute("href", "/admin/users/user-1")
    expect(screen.getByRole("link", { name: "Jamie" })).toHaveAttribute("href", "/admin/users/user-2")
    expect(screen.getByRole("button", { name: "Copy supplier-1 ID" })).toBeInTheDocument()
    expect(screen.getByText("Extra returned information")).toBeInTheDocument()
    expect(screen.getByText(/Original order id/i)).toBeInTheDocument()
  })

  it("swaps version and description inside the original card grid and orders requester above courier", async () => {
    mocks.order.mockResolvedValue({ courierName: "Jamie", courierId: "user-2",
      ...Object.fromEntries(Object.entries(order).filter(([key]) => !key.startsWith("courier"))),
      attemptId: "attempt-1", version: 3,
    })
    render(<AdminOrders initialOrderId="order-1" />)
    const requester = await screen.findByRole("link", { name: "Alex" })
    const courier = screen.getByRole("link", { name: "Jamie" })
    expect(requester.compareDocumentPosition(courier) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
    const description = screen.getByText("Lunch delivery").closest("div")!
    const info = screen.getByRole("heading", { name: "Order information" }).closest("[data-slot=card]")!
    const terms = within(info as HTMLElement).getAllByRole("term").map((term) => term.textContent)
    expect(terms).toEqual(["Id", "Attempt Id", "Version", "Offered Credits", "Item Description"])
    expect(description).not.toHaveClass("sm:col-span-2")
    expect(info).not.toHaveClass("lg:col-span-2")
    expect(within(info as HTMLElement).getAllByRole("term")[0].closest("dl")).toHaveClass("sm:grid-cols-2")
  })

  it("shows supplier status beside location details, outside admin actions, and retains the detail map", async () => {
    render(<AdminSuppliers initialSupplierId="supplier-1" />)
    await screen.findByText("Location details")
    const actions = screen.getByText("Admin actions").closest("[data-slot=card]")!
    const details = screen.getByText("Location details").closest("[data-slot=card]")!
    expect(within(actions as HTMLElement).queryByText("Closed")).not.toBeInTheDocument()
    expect(within(details as HTMLElement).getByText("Closed")).toBeInTheDocument()
    expect(screen.getByRole("region", { name: "Supplier location" })).toBeInTheDocument()
  })
})
