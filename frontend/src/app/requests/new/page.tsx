/*
 * AI Assistance Disclosure:
 * Tool: OpenAI Codex (GPT-5), date: 2026-09-28
 * Mode: Code generation.
 * Scope: Generated the authenticated Post Request coming-soon page.
 * Author review: I reviewed for correctness and edited where needed.
 */
import type { Metadata } from "next"
import { FilePlusIcon } from "lucide-react"

import { ComingSoonPage } from "@/components/coming-soon-page"

export const metadata: Metadata = { title: "Post Request" }

export default function PostRequestPage() {
  return (
    <ComingSoonPage
      title="Post Request coming soon"
      description="Soon you will be able to post a new campus errand request here."
      icon={FilePlusIcon}
    />
  )
}
