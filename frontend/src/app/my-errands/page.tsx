/*
 * AI Assistance Disclosure:
 * Tool: OpenAI Codex (GPT-5), date: 2026-09-28
 * Mode: Code generation.
 * Scope: Generated the authenticated My Errands coming-soon page.
 * Author review: I reviewed for correctness and edited where needed.
 */
import type { Metadata } from "next"
import { BikeIcon } from "lucide-react"

import { ComingSoonPage } from "@/components/coming-soon-page"

export const metadata: Metadata = { title: "My Errands" }

export default function MyErrandsPage() {
  return (
    <ComingSoonPage
      title="My Errands coming soon"
      description="Soon you will be able to manage and track the errands you are delivering."
      icon={BikeIcon}
    />
  )
}
