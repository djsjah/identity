package com.identity.core.admin.image;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import com.identity.app.api.dto.SliceResponseDTO;
import com.identity.core.admin.image.dto.ImageDTO;

import static com.identity.core.admin.image.config.ImagePaginationConstants.*;

@RestController
@RequestMapping("/admin/images")
@RequiredArgsConstructor
@Tag(name = "Admin Images", description = "Image administration API")
public class ImageController {
    private final ImageService imageService;

    @GetMapping("/all")
    @Operation(summary = "Get all images")
    public SliceResponseDTO<ImageDTO> getAll(
            @RequestParam(defaultValue = DEFAULT_PAGE) @Min(MIN_PAGE) int page,
            @RequestParam(defaultValue = DEFAULT_SIZE) @Min(MIN_SIZE) @Max(MAX_SIZE) int size
    ) {
        return imageService.getAll(page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get image by ID")
    public ImageDTO getById(@PathVariable String id) { return imageService.getById(id); }

    @GetMapping("/exists/{id}")
    @Operation(summary = "Check if an image exists by ID")
    public boolean exists(@PathVariable String id) { return imageService.existsById(id); }

    @GetMapping("/search")
    @Operation(summary = "Search images by filepath or upload date")
    public SliceResponseDTO<ImageDTO> search(
            @RequestParam(defaultValue = DEFAULT_PAGE) @Min(MIN_PAGE) int page,
            @RequestParam(defaultValue = DEFAULT_SIZE) @Min(MIN_SIZE) @Max(MAX_SIZE) int size,
            @RequestParam(required = false) String filepath,

            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime uploadDate
    ) {
        return imageService.search(page, size, filepath, uploadDate);
    }
}
