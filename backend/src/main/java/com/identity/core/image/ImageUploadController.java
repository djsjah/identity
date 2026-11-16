package com.identity.core.image;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.identity.core.image.service.ImageVerificationFacade;
import com.identity.core.image.task.ImageVerificationTaskService;
import com.identity.core.image.dto.ImageUploadResponseDTO;
import com.identity.core.image.task.dto.ImageVerificationTaskStatusDTO;
import com.identity.persistence.user.User;

@RestController
@RequestMapping("/images")
@RequiredArgsConstructor
@Tag(name = "Image", description = "Image verification API")
public class ImageUploadController {
    private final ImageVerificationFacade imageVerificationFacade;
    private final ImageVerificationTaskService taskService;

    @GetMapping("/{imageId}/result")
    @Operation(summary = "Get the current verification status and result")
    public ImageVerificationTaskStatusDTO getResult(
            @PathVariable String imageId,
            @AuthenticationPrincipal User currentUser
    ) {
        return taskService.getStatus(currentUser.getId(), imageId);
    }

    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Upload image and schedule verification")
    public ImageUploadResponseDTO upload(
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal User currentUser
    ) {
        return imageVerificationFacade.upload(file, currentUser);
    }
}
