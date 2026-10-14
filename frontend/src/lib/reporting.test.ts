/*
 * AI Assistance Disclosure:
 * Tool: OpenAI Codex (GPT-5), date: 2026-10-13
 * Mode: Implementation.
 * Scope: Generated or updated frontend code for the team-specified report form, order association, navigation, and completion reminder.
 * Author review: I reviewed for correctness
 */
import { expect, it } from "vitest"
import { reportCategoryRequiresOrder, validReportCategory } from "@/lib/reporting"

it("requires associations only for order and courier categories", () => {
  expect(reportCategoryRequiresOrder("ORDER")).toBe(true)
  expect(reportCategoryRequiresOrder("COURIER")).toBe(true)
  expect(reportCategoryRequiresOrder("CREDITS")).toBe(false)
  expect(reportCategoryRequiresOrder("GENERAL")).toBe(false)
  expect(validReportCategory("ORDER")).toBe("ORDER")
  expect(validReportCategory("unexpected")).toBe("GENERAL")
})
