package com.identity.core.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.identity.core.user.dto.ChangeEmailRequestDTO;
import com.identity.persistence.user.User;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(
        name = "User",
        description = "Current user operations"
)
public class UserController {
    private final UserService userService;

    @PatchMapping("/me/email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Change current user's email")
    public void changeEmail(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody ChangeEmailRequestDTO request
    ) {
        userService.changeEmail(currentUser.getId(), request);
    }
}
