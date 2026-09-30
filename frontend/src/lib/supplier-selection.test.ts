import { describe, expect, it } from "vitest"

import { supplierOptionLabel } from "@/lib/suppliers"

describe("supplier selection helpers", () => {
  it("shows the supplier name and building while keeping the API value separate", () => {
    expect(supplierOptionLabel({ name: "Campus Store", building: "UTown" })).toBe("Campus Store — UTown")
  })

  it("does not add an empty location to a supplier label", () => {
    expect(supplierOptionLabel({ name: "Campus Store", building: "" })).toBe("Campus Store")
  })
})
