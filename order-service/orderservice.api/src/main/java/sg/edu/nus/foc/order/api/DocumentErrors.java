package sg.edu.nus.foc.order.api;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import sg.edu.nus.foc.order.contracts.ErrandContracts.ApiError;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ApiResponses({
    @ApiResponse(
            responseCode = "400",
            description = "Invalid request fields",
            content = @Content(schema = @Schema(implementation = ApiError.class))),
    @ApiResponse(
            responseCode = "403",
            description = "Actor does not own this operation",
            content = @Content(schema = @Schema(implementation = ApiError.class))),
    @ApiResponse(
            responseCode = "404",
            description = "Resource not found",
            content = @Content(schema = @Schema(implementation = ApiError.class))),
    @ApiResponse(
            responseCode = "409",
            description = "Stale version, illegal transition or reused command",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
})
public @interface DocumentErrors {}
