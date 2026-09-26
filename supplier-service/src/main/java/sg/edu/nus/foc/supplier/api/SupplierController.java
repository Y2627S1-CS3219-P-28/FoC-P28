package sg.edu.nus.foc.supplier.api;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sg.edu.nus.foc.supplier.error.BadRequestException;
import sg.edu.nus.foc.supplier.error.ForbiddenException;
import sg.edu.nus.foc.supplier.security.Role;
import sg.edu.nus.foc.supplier.supplier.CatalogueQuery;
import sg.edu.nus.foc.supplier.supplier.CatalogueQuery.SortDirection;
import sg.edu.nus.foc.supplier.supplier.CatalogueQuery.SortField;
import sg.edu.nus.foc.supplier.supplier.CatalogueQuery.StatusFilter;
import sg.edu.nus.foc.supplier.supplier.PageResult;
import sg.edu.nus.foc.supplier.supplier.PairValidation;
import sg.edu.nus.foc.supplier.supplier.Supplier;
import sg.edu.nus.foc.supplier.supplier.SupplierMatch;
import sg.edu.nus.foc.supplier.supplier.SupplierService;

/**
 * Supplier catalogue API. Every endpoint needs a signed-in user (see SecurityConfig):
 * reads are open to all roles, catalogue management requires {@code admin}.
 */
@RestController
@RequestMapping("/api/suppliers")
@Tag(name = "Suppliers", description = "Campus stores, facilities and landmarks used as errand pickup and delivery points")
public class SupplierController {

    private static final String ADMIN_ONLY = "hasRole('ADMIN')";

    private static final Map<String, SortField> SORT_FIELDS = Map.of(
            "name", SortField.NAME,
            "type", SortField.TYPE,
            "building", SortField.BUILDING,
            "distance", SortField.DISTANCE,
            "updatedAt", SortField.UPDATED_AT);

    private final SupplierService service;

    public SupplierController(SupplierService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Search, filter, sort and paginate the catalogue (F3.1, F4)")
    public PageResponse<SupplierResponse> list(
            @Parameter(description = "Partial, case-insensitive match on name, building or location")
            @RequestParam(required = false) @Size(max = 100) String q,
            @Parameter(description = "Type filter, repeatable (e.g. type=Food&type=Printing)")
            @RequestParam(name = "type", required = false) List<@Size(max = 40) String> types,
            @Parameter(description = "Partial, case-insensitive building match")
            @RequestParam(required = false) @Size(max = 100) String building,
            @RequestParam(defaultValue = "false") boolean openNow,
            @Parameter(description = "Latitude of the reference point for distance")
            @RequestParam(required = false) @DecimalMin("-90") @DecimalMax("90") Double lat,
            @Parameter(description = "Longitude of the reference point for distance")
            @RequestParam(required = false) @DecimalMin("-180") @DecimalMax("180") Double lng,
            @Parameter(description = "Only suppliers within this many metres of lat/lng")
            @RequestParam(required = false) @DecimalMin("1") @DecimalMax("50000") Double radius,
            @Parameter(description = "active (default), inactive or all; the latter two are admin-only")
            @RequestParam(defaultValue = "active") String status,
            @Parameter(description = "name, type, building, distance or updatedAt (default: name, or distance with lat/lng)")
            @RequestParam(required = false) String sort,
            @Parameter(description = "asc or desc")
            @RequestParam(defaultValue = "asc") String order,
            @RequestParam(defaultValue = "1") @Min(1) @Max(10_000) int page,
            @RequestParam(defaultValue = "" + CatalogueQuery.DEFAULT_PAGE_SIZE) @Min(1)
            @Max(CatalogueQuery.MAX_PAGE_SIZE) int size,
            JwtAuthenticationToken caller) {

        if ((lat == null) != (lng == null)) {
            throw new BadRequestException(lat == null ? "lat" : "lng", "lat and lng must be given together");
        }
        CatalogueQuery.Coordinates near = lat == null ? null : new CatalogueQuery.Coordinates(lat, lng);
        if (radius != null && near == null) {
            throw new BadRequestException("radius", "requires lat and lng");
        }

        StatusFilter statusFilter = parse("status", status, Map.of(
                "active", StatusFilter.ACTIVE, "inactive", StatusFilter.INACTIVE, "all", StatusFilter.ALL));
        if (statusFilter != StatusFilter.ACTIVE && !isAdmin(caller)) {
            throw new ForbiddenException("Only administrators can list inactive suppliers.");
        }
        SortField sortField = sort == null ? null : parse("sort", sort, SORT_FIELDS);
        if (sortField == SortField.DISTANCE && near == null) {
            throw new BadRequestException("sort", "sorting by distance requires lat and lng");
        }
        SortDirection direction = parse("order", order, Map.of("asc", SortDirection.ASC, "desc", SortDirection.DESC));

        CatalogueQuery query = new CatalogueQuery(q, types, building, openNow, near, radius,
                statusFilter, sortField, direction, page, size);
        PageResult<SupplierMatch> result = service.search(query);
        List<SupplierResponse> items = result.items().stream()
                .map(m -> SupplierResponse.from(m.supplier(), m.openNow(), m.distanceMeters()))
                .toList();
        String message = result.totalItems() == 0 ? "No suppliers match your search." : null;
        return new PageResponse<>(items, result.page(), result.size(), result.totalItems(), result.totalPages(), message);
    }

    @GetMapping("/types")
    @Operation(summary = "Distinct supplier types in use, for filters")
    public ItemsResponse<String> types() {
        return new ItemsResponse<>(service.types());
    }

    @GetMapping("/permissions")
    @Operation(summary = "The caller's roles and what they may do in this service")
    public PermissionsResponse permissions(JwtAuthenticationToken caller) {
        List<String> roles = roles(caller).stream().map(Role::id).sorted().toList();
        return new PermissionsResponse(caller.getName(), caller.getToken().getClaimAsString("email"), roles,
                isAdmin(caller));
    }

    @GetMapping("/{id}")
    @Operation(summary = "One supplier by ID, including inactive ones (F3.2, F6.3.3)")
    public SupplierResponse get(@PathVariable String id) {
        Supplier supplier = service.get(id);
        return SupplierResponse.from(supplier, service.isOpenNow(supplier), null);
    }

    @GetMapping("/{id}/status")
    @Operation(summary = "Whether a supplier ID exists and is active, for other services (F5.3)")
    public SupplierStatusResponse status(@PathVariable String id) {
        List<Supplier> found = service.lookup(List.of(id));
        return found.isEmpty()
                ? new SupplierStatusResponse(id, false, false)
                : new SupplierStatusResponse(id, true, found.getFirst().active());
    }

    @PostMapping("/validate")
    @Operation(summary = "Validate an errand's pickup and delivery suppliers (F5.1)")
    public PairValidation validate(@Valid @RequestBody ValidatePairRequest request) {
        return service.validatePair(request.pickupSupplierId().strip(), request.deliverySupplierId().strip());
    }

    @PostMapping("/lookup")
    @Operation(summary = "Resolve many supplier IDs at once, including inactive suppliers (F5.2)")
    public LookupResponse lookup(@Valid @RequestBody LookupRequest request) {
        List<Supplier> found = service.lookup(request.ids());
        Set<String> foundIds = new HashSet<>();
        found.forEach(s -> foundIds.add(s.id()));
        List<SupplierResponse> items = found.stream()
                .map(s -> SupplierResponse.from(s, service.isOpenNow(s), null))
                .toList();
        List<String> missing = request.ids().stream().distinct().filter(id -> !foundIds.contains(id)).toList();
        return new LookupResponse(items, missing);
    }

    @PostMapping
    @PreAuthorize(ADMIN_ONLY)
    @Operation(summary = "Create a supplier (admin, F6.1)")
    public ResponseEntity<SupplierResponse> create(@Valid @RequestBody CreateSupplierRequest request) {
        Supplier created = service.create(request.toDetails());
        return ResponseEntity.created(URI.create("/api/suppliers/" + created.id()))
                .body(SupplierResponse.from(created, service.isOpenNow(created), null));
    }

    @PatchMapping("/{id}")
    @PreAuthorize(ADMIN_ONLY)
    @Operation(summary = "Update any field or the active status of a supplier (admin, F6.2)")
    public SupplierResponse update(@PathVariable String id, @Valid @RequestBody UpdateSupplierRequest request) {
        Supplier current = service.get(id);
        boolean active = request.active() != null ? request.active() : current.active();
        Supplier updated = service.update(id, request.applyTo(current.details()), active);
        return SupplierResponse.from(updated, service.isOpenNow(updated), null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(ADMIN_ONLY)
    @Operation(summary = "Deactivate a supplier (admin, F6.3). Records are never hard-deleted.")
    @ApiResponse(responseCode = "200", description = "The supplier, now inactive")
    public SupplierResponse deactivate(@PathVariable String id) {
        Supplier supplier = service.deactivate(id);
        return SupplierResponse.from(supplier, service.isOpenNow(supplier), null);
    }

    private static <T> T parse(String field, String value, Map<String, T> allowed) {
        String wanted = value.strip().toLowerCase(Locale.ROOT);
        return allowed.entrySet().stream()
                .filter(e -> e.getKey().toLowerCase(Locale.ROOT).equals(wanted))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow(() -> new BadRequestException(field,
                        "must be one of " + allowed.keySet().stream().sorted().toList()));
    }

    private static Set<Role> roles(JwtAuthenticationToken caller) {
        Set<Role> roles = new HashSet<>();
        for (GrantedAuthority authority : caller.getAuthorities()) {
            String name = authority.getAuthority();
            if (name != null && name.startsWith("ROLE_")) {
                Role.fromId(name.substring("ROLE_".length())).ifPresent(roles::add);
            }
        }
        return roles;
    }

    private static boolean isAdmin(JwtAuthenticationToken caller) {
        return roles(caller).contains(Role.ADMIN);
    }
}
