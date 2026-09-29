import type { Metadata } from "next"
import { BrowseErrands } from "@/components/orders/browse-errands"
export const metadata: Metadata = { title: "Browse Errands" }
export default function Page() { return <BrowseErrands /> }
