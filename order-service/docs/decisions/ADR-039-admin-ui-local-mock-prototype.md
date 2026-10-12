# ADR-039: Local Admin UI mock prototype

Status: user-approved prototype scope, Yao Xiang, 2026-10-11.
Change: [CHANGE-111](../../changes/CHANGE-111-admin-ui-mock-prototype.md).

The user's detailed request approves Orders/Users/Suppliers admin tabs, separate
normal/admin navigation, sample role verification and all sample provider APIs
inside Order Service. Other backend services are read-only. This is a prototype
milestone and does not approve real administrative writes or peer integration.

## Decision

Reuse AppShell and its desktop sidebar/mobile sheet, add a top-right mode switch,
guard admin routes with account-bound verified role-context, and use a dedicated
semantic API adapter. Use existing shadcn dialogs, table, supplier form and style.
Sample endpoint responses reuse current model fields and page conventions;
explicitly document new User profile fields as fixture-only extensions.

The isolated Order sample store is process-local, synchronized and copied via
MapStruct. Its mutations never call a peer or touch Order persistence.
All sample HTTP calls are synchronous; no new broker, job, table or migration.

Local Compose already uses prod security with emulator JWTs. Therefore condition
the controller and store on ALL of: explicit demo enabled flag, local environment,
and Firebase emulator configuration. Do not change the existing security chain.
Cloud defaults omit the flag and emulator and cannot register these endpoints
under staging/production environment, even if the demo flag is accidentally true.
The mock role claim authorizes only sample UI/data, not existing real admin APIs.

## Alternatives

### Effective internal-field correction: CHANGE-116 (2026-10-12)

The user rejects CHANGE-115's wider Order card/custom grid interpretation. Restore
the original card widths and two-column internal grid. Only swap Version with
Item Description, placing description last; requester name/ID stays above courier
name/ID. Supplier map/status refinements remain. Existing components/sequences/
contracts unchanged; no new architecture decision or diagram change needed.

### Effective presentation adjustment: CHANGE-115 (2026-10-12)

User requests a wider final description row, requester above courier, removal
of only the Supplier list overview map and status beside Location details.
Metadata occupies Order Information's first row, then credits left/description
right (two-column span) in its final row; this card spans the detail-page width.
People uses explicit requester name/ID then courier name/ID, not JSON key order.
Supplier detail/creation maps remain. Badge is descriptive, outside actions.
Implementation detail only: existing component roles/contracts/ownership unchanged.
Remove the obsolete list-to-map edge from the class diagram; detail/create map
dependencies remain.

### Effective map/layout refinement: CHANGE-114 (2026-10-12)

The user's detailed request approves supplier maps and address/pin selection,
detail-only deactivation, separate User table fields and grouped Order details.
Choose Leaflet1.9.4 plus attributed OSM tiles and debounced Photon suggestions
for this existing prototype; no API key, GCP project or backend proxy needed.
These public map reads are the authorized mapping exception to service gateway
calls; no app token or account data is forwarded. All FoC administrative calls
retain the existing mock adapter/gateway/guard. Provider URLs are isolated in
map-provider.ts for later replacement; public service availability is not an SLA.

Optional SupplierForm map mode is enabled only for Admin mock creation. Search
selection sets coordinates; drag/click/manual edits keep them synchronized.
Preserve normal SupplierForm behavior and validation. Address is a visible
reference after adjustment; optional prefill uses existing locationDescription.
No new persisted address field. Overview pins represent only the current page.
Maps render client-only, clean up Leaflet/listeners on unmount and retain manual/
Google Maps fallback when public services fail. Popup text uses safe DOM text.

Order detail cards separate information, people, locations, status/timing and
repost facts. Related names link to Admin selected-record routes; IDs copy
independently. Unknown future returned fields stay in an Other information card.
Remove only card-level supplier deactivation; keep its detail confirmation.
No backend/schema/event/auth/configuration change or real peer integration.

~~~mermaid
sequenceDiagram
    actor Admin
    participant Form as Optional Admin supplier picker
    participant Search as Public geocoder adapter
    participant Photon as Photon HTTPS API
    participant Map as Leaflet / OSM map
    participant API as Existing Order mock API
    Admin->>Form: Type an address
    Form->>Search: Debounced query with abort/timeout
    Search->>Photon: Anonymous bounded address query
    Photon-->>Form: Validated label and coordinates
    Admin->>Form: Select suggestion
    Form->>Map: Place selected pin
    Admin->>Map: Drag pin / click position
    Map-->>Form: New latitude/longitude
    Admin->>Form: Edit coordinates or submit
    Form->>API: Existing SupplierInput with numeric coordinates
    API-->>Form: Sample supplier / existing validation error
~~~

### Effective dedicated-page refinement: CHANGE-113

User-requested 2026-10-11, continued 2026-10-12: Order and Supplier details now
use full dedicated Admin pages, replacing their detail modals from CHANGE-112.
Order View navigates to /admin/orders/{id}; its loader sends an exact-ID query
to the existing mock list endpoint with page1/size1 and rejects empty/mismatched
results. Supplier View uses /admin/suppliers/{id} and existing sample GET by ID.
Details, copy IDs, badges and back/retry controls reuse existing components.
User actions move above summary/history; Supplier deactivation is near the top
and retains confirmation/error/inactive guards. Order remains read-only.
No backend endpoint, authorization, persistence or provider contract changes.

~~~mermaid
sequenceDiagram
    actor Admin
    participant List as Admin Orders / Suppliers
    participant Detail as Dedicated Admin detail route
    participant Adapter as Existing mock adapter
    participant API as Order sample API
    Admin->>List: Click View
    List->>Detail: Navigate with selected ID
    Detail->>Adapter: order(id) / supplier(id)
    Adapter->>API: Exact-ID orders query / GET supplier by ID
    API-->>Detail: Copied record or error
    Detail-->>Admin: Full details with back / retry controls
    opt Supplier deactivation
        Admin->>Detail: Confirm existing action near heading
        Detail->>API: Existing sample deactivation
        API-->>Detail: Updated inactive supplier or error
    end
~~~

### Approved refinement: CHANGE-112 (2026-10-11)

The user's follow-up replaces the profile popup with a full Users-tab profile,
using normal Profile Card layouts and shared TransactionHistoryView presentation.
Data loaders remain separate; only the current sample user's history is fetched.
Orders names navigate to /admin/users/{id} or /admin/suppliers/{id}; detail reads
are independent of list pagination. Unknown records show retry/back controls.
Search/filter/input/button heights align; normal SupplierBadges show open/closed/
inactive status. The header retains only mode switching for signed-in users.
Order fields use individual columns; description appears only in View details.
This is a prototype design refinement, not a new production provider integration.
ADMIN-API-006 records the missing Credit admin history contract.

~~~mermaid
sequenceDiagram
    actor Admin
    participant Orders as Admin Orders
    participant Route as Admin selected record route
    participant Adapter as Admin API adapter
    participant Mock as Order-owned sample API
    Admin->>Orders: Click requester/courier/location name
    Orders->>Route: Navigate with record ID
    Route->>Adapter: user(id) or supplier(id)
    Adapter->>Mock: GET selected sample record
    Mock-->>Route: Copied profile or location
    opt User profile loaded
        Route->>Adapter: userTransactions(id, page)
        Adapter->>Mock: GET users/id/transactions
        Mock-->>Route: Transaction page or independent error
    end
    Route-->>Admin: Selected record with copy ID / applicable actions
~~~

Real provider integration now cannot demonstrate missing user management APIs
without real admin roles; browser-only fixtures do not exercise HTTP contracts.
The user chooses Order-owned mocks. A profile-only non-prod condition was rejected
after inspecting local prod-auth Compose because it would disable the local demo.

## Consequences and evidence

Sample data resets on restart; multiple developers in one local process share
sample changes. Permission cache belongs to the current UID and late/stale reads
are ignored. Real authorization remains provider-owned; UI route membership is
not a security boundary. Peer gaps and exact contracts are in
[admin-peer-api-request](../admin-peer-api-request.md).
Verification must include production exclusion, failed role lookup, identity
switches, search/paging, user/supplier confirmations and regression checks.
This approval does not claim real admin integration or completed Sprint scope.

~~~mermaid
sequenceDiagram
    actor Admin as Signed-in local tester
    participant UI as AppShell / AdminModeProvider
    participant Adapter as Admin API adapter
    participant API as Order local mock controller
    participant Store as Sample store
    Admin->>UI: Click Admin Mode
    UI->>Adapter: roleContext(current UID)
    Adapter->>API: GET /role-context?userId=UID
    API-->>UI: userId / admin role (mock)
    UI->>UI: Compare UID and require admin role
    UI-->>Admin: Only Orders / Users / Suppliers tabs
    Admin->>UI: Search or confirm sample action
    UI->>Adapter: Typed semantic operation
    Adapter->>API: Local sample HTTP request
    API->>Store: Query / update fixture atomically
    Store-->>API: Copied response / page
    API-->>Adapter: HTTP JSON response
    Adapter-->>UI: Typed result
    UI-->>Admin: Updated view
~~~

~~~mermaid
classDiagram
    AppShell --> AdminModeProvider
    AdminModeProvider --> AdminApiAdapter
    AdminOrders --> AdminApiAdapter
    AdminOrders --> AdminOrderDetails
    AdminOrderDetails --> AdminApiAdapter
    AdminUsers --> AdminApiAdapter
    AdminUsers --> AdminUserProfile
    AdminSelectedUser --> AdminUserProfile
    AdminUserProfile --> TransactionHistoryView
    AdminUserProfile --> AdminApiAdapter
    AdminSuppliers --> AdminApiAdapter
    AdminSuppliers --> AdminSupplierDetails
    AdminSupplierDetails --> AdminApiAdapter
    AdminSupplierDetails --> LocationMap
    SupplierForm --> AddressSearch : optional admin picker
    SupplierForm --> LocationMap : optional editable pin
    LocationMap --> LeafletMap : client only
    AddressSearch --> PublicGeocoderAdapter
    AdminApiAdapter --> AdminMockController : local HTTP
    AdminMockController --> AdminMockService
    AdminMockService --> AdminMockMapper : response copies
    AdminMockService --> AdminMockOrderResponse
    AdminMockService --> AdminMockUserResponse
    AdminMockService --> AdminMockSupplierResponse
    AdminMockService --> AdminMockTransactionResponse
    AdminMockOrderResponse --|> OrderResponse
~~~
