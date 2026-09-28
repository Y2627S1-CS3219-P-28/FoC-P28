/*
 * AI Assistance Disclosure:
 * Tool: OpenAI Codex (GPT-5), date: 2026-09-28
 * Mode: Code generation.
 * Scope: Generated the authenticated Dashboard coming-soon page.
 * Author review: I reviewed for correctness and edited where needed.
 */
import type { Metadata } from "next"
import { LayoutDashboardIcon } from "lucide-react"

import { ComingSoonPage } from "@/components/coming-soon-page"

export const metadata: Metadata = { title: "Dashboard" }

export default function HomePage() {
  return (
    <ComingSoonPage
      title="Dashboard coming soon"
      description="Your personalised dashboard is on the way!"
      icon={LayoutDashboardIcon}
    />
  )
}
