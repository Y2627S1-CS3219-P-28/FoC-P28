import { StoreIcon, type LucideIcon } from "lucide-react"

export type NavItem = {
  href: string
  label: string
  description: string
  icon: LucideIcon
}

// Each service owner registers the UI entry point for their feature here. Items appear
// in the header navigation and as cards on the home page, in this order.
// Example:
//   { href: "/suppliers", label: "Suppliers", description: "...", icon: StoreIcon },
export const NAV_ITEMS: NavItem[] = [
  {
    href: "/suppliers",
    label: "Suppliers",
    description: "Browse campus stores, facilities and landmarks for pickups and deliveries.",
    icon: StoreIcon,
  },
]
