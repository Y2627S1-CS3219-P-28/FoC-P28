import { cleanup,fireEvent,render,screen,waitFor } from "@testing-library/react"
import { beforeEach,afterEach,it,expect,vi } from "vitest"
import * as React from "react"
import { ApplyPenaltyDialog } from "./apply-penalty-dialog"
const api=vi.hoisted(()=>({mode:"live",order:vi.fn(),penaltyPolicies:vi.fn(),applyPenalty:vi.fn()}))
vi.mock("@/hooks/use-admin-api",()=>({useAdminApi:()=>api}))
vi.mock("@/hooks/use-admin-page",()=>({useAdminPage:(load:()=>Promise<unknown>)=>{const [data,setData]=React.useState<unknown>(null);React.useEffect(()=>{void load().then(setData)},[load]);return {data,loading:!data,error:null,refresh:vi.fn()}}}))
beforeEach(()=>{vi.clearAllMocks();api.order.mockResolvedValue({id:"order",courierId:"courier",status:"DELIVERED",version:3,appliedPenalty:"ITEM_MISHANDLING_OR_DAMAGE",appliedPenaltyAmount:4});api.penaltyPolicies.mockResolvedValue([{policy:"ITEM_MISHANDLING_OR_DAMAGE",penaltyAmount:4},{policy:"WRONG_LOCATION_DELIVERY",penaltyAmount:4}]);api.applyPenalty.mockResolvedValue({id:"order"})})
afterEach(cleanup)
it("has no waiver or completion action and explains highest-only deferred scoring",async()=>{
 render(<ApplyPenaltyDialog orderId="order" onClose={()=>{}} onSaved={()=>{}} />)
 await screen.findByText(/Stored penalty/)
 expect(screen.queryByRole("radio",{name:"No penalty"})).not.toBeInTheDocument()
 expect(screen.getByRole("button",{name:"Confirm penalty"})).toBeDisabled()
 expect(screen.getByText(/cannot be cleared/i)).toBeInTheDocument()
})
it("submits to Order only and retries with the same command ID",async()=>{
 api.applyPenalty.mockRejectedValueOnce(new Error("Retry"))
 render(<ApplyPenaltyDialog orderId="order" onClose={()=>{}} onSaved={()=>{}} />)
 fireEvent.click(await screen.findByRole("radio",{name:/Wrong location delivery/}))
 fireEvent.change(screen.getByLabelText("Penalty reason"),{target:{value:"Confirmed by investigation"}})
 fireEvent.click(screen.getByRole("button",{name:"Confirm penalty"}))
 await screen.findByRole("alert")
 fireEvent.click(screen.getByRole("button",{name:"Retry penalty"}))
 await waitFor(()=>expect(api.applyPenalty).toHaveBeenCalledTimes(2))
 expect(api.applyPenalty.mock.calls[0]).toEqual(api.applyPenalty.mock.calls[1])
 expect(api.applyPenalty.mock.calls[0][1]).toEqual(expect.objectContaining({courierId:"courier",expectedVersion:3,policy:"WRONG_LOCATION_DELIVERY",reason:"Confirmed by investigation"}))
})
