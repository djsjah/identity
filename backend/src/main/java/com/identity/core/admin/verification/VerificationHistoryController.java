package com.identity.core.admin.verification;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;

import com.identity.app.api.dto.SliceResponseDTO;
import com.identity.core.admin.verification.dto.VerificationHistoryDTO;
import com.identity.core.admin.verification.dto.VerificationHistoryDetailsDTO;
import com.identity.core.admin.verification.dto.VerificationHistorySearchDTO;

import static com.identity.core.admin.verification.config.VerificationHistoryPaginationConstants.*;

@RestController
@RequestMapping("/admin/verification-history")
@RequiredArgsConstructor
@Tag(
        name = "Admin Verification History",
        description = "Verification history administration API"
)
public class VerificationHistoryController {
    private final VerificationHistoryService historyService;

    @GetMapping("/all")
    @Operation(summary = "Get all verification history entries")
    public SliceResponseDTO<VerificationHistoryDTO> getAll(
            @RequestParam(defaultValue = DEFAULT_PAGE) @Min(MIN_PAGE) int page,
            @RequestParam(defaultValue = DEFAULT_SIZE) @Min(MIN_SIZE) @Max(MAX_SIZE) int size
    ) {
        return historyService.getAll(page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get verification history entry by ID")
    public VerificationHistoryDetailsDTO getById(@PathVariable String id) {
        return historyService.getById(id);
    }

    @GetMapping("/search")
    @Operation(summary = "Search verification histories")
    public SliceResponseDTO<VerificationHistoryDTO> search(
            @RequestParam(defaultValue = DEFAULT_PAGE) @Min(MIN_PAGE) int page,
            @RequestParam(defaultValue = DEFAULT_SIZE) @Min(MIN_SIZE) @Max(MAX_SIZE) int size,

            @ParameterObject @ModelAttribute
            VerificationHistorySearchDTO request
    ) {
        return historyService.search(page, size, request);
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "Delete a verification history entry by ID")
    public void deleteById(@PathVariable String id) { historyService.deleteById(id); }
}
