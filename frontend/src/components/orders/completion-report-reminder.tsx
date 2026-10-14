/*
 * AI Assistance Disclosure:
 * Tool: OpenAI Codex (GPT-5), date: 2026-10-13
 * Mode: Implementation.
 * Scope: Generated or updated frontend code for the team-specified report form, order association, navigation, and completion reminder.
 * Author review: I reviewed for correctness.
 */
import Link from "next/link"
import { ReceiptTextIcon } from "lucide-react"

import { Alert, AlertDescription } from "@/components/ui/alert"

export function CompletionReportReminder({ orderId }: { orderId: string }) {
  return <Alert className="mt-3"><ReceiptTextIcon />
    <AlertDescription>Review the completed order and <Link href={`/reporting?category=ORDER&orderId=${encodeURIComponent(orderId)}`}>report an issue</Link> if anything is wrong.</AlertDescription>
  </Alert>
}
