/*
 * AI Assistance Disclosure:
 * Tool: OpenAI Codex (GPT-5), date: 2026-09-28
 * Mode: Code generation.
 * Scope: Generated the authenticated My Requests coming-soon page.
 * Author review: I reviewed for correctness and edited where needed.
 */
import type { Metadata } from "next"
import { ClipboardListIcon } from "lucide-react"

import { ComingSoonPage } from "@/components/coming-soon-page"

export const metadata: Metadata = { title: "My Requests" }

export default function MyRequestsPage() {
  return (
    <ComingSoonPage
      title="My Requests coming soon"
      description="Soon you will be able to manage and track the requests you have posted."
      icon={ClipboardListIcon}
    />
  )
}
