/*
 * AI Assistance Disclosure:
 * Tool: OpenAI Codex (GPT-5), date: 2026-09-28
 * Mode: Code generation.
 * Scope: Generated the authenticated Browse Errands coming-soon page.
 * Author review: I reviewed for correctness and edited where needed.
 */
import type { Metadata } from "next"
import { SearchIcon } from "lucide-react"

import { ComingSoonPage } from "@/components/coming-soon-page"

export const metadata: Metadata = { title: "Browse Errands" }

export default function BrowseErrandsPage() {
  return (
    <ComingSoonPage
      title="Browse Errands coming soon"
      description="Soon you will be able to find errands posted by others on campus."
      icon={SearchIcon}
    />
  )
}
