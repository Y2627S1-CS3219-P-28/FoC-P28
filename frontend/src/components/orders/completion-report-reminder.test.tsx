/*
 * AI Assistance Disclosure:
 * Tool: OpenAI Codex (GPT-5), date: 2026-10-13
 * Mode: Implementation.
 * Scope: Generated or updated frontend code for the team-specified report form, order association, navigation, and completion reminder.
 * Author review: I reviewed for correctness
 */
import { render, screen } from "@testing-library/react"
import { expect, it } from "vitest"
import { CompletionReportReminder } from "@/components/orders/completion-report-reminder"

it("keeps a completed order linked to reporting", () => {
  render(<CompletionReportReminder orderId="order / 1" />)
  expect(screen.getByRole("link", { name: "report an issue" })).toHaveAttribute("href", "/reporting?category=ORDER&orderId=order%20%2F%201")
})
