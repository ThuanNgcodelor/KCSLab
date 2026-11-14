package com.example.app.user.controller;

import com.example.app.user.dto.CreateUserRequest;
import com.example.app.user.dto.AvatarUploadResponse;
import com.example.app.user.dto.ChangeOwnPasswordRequest;
import com.example.app.user.dto.UpdateOwnProfileRequest;
import com.example.app.user.dto.UpdateUserRequest;
import com.example.app.user.dto.UserResponse;
import com.example.app.user.service.UserService;
import com.example.app.auth.service.AccountSecurityService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.core.io.Resource;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.time.Duration;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;
    private final AccountSecurityService accountSecurityService;

    public UserController(UserService userService, AccountSecurityService accountSecurityService) {
        this.userService = userService;
        this.accountSecurityService = accountSecurityService;
    }

    @GetMapping
    public List<UserResponse> list() {
        return userService.findAll();
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(request));
    }

    @PatchMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return userService.update(id, request);
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return userService.toResponse(userService.getByEmail(jwt.getSubject()));
    }

    @PatchMapping("/me")
    public UserResponse updateMe(@AuthenticationPrincipal Jwt jwt,
                                 @Valid @RequestBody UpdateOwnProfileRequest request) {
        return userService.updateOwnProfile(jwt.getSubject(), request);
    }

    @PutMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AvatarUploadResponse updateAvatar(@AuthenticationPrincipal Jwt jwt,
                                             @RequestParam("file") MultipartFile file) {
        UserService.AvatarUpdate result = userService.updateAvatar(jwt.getSubject(), file);
        return new AvatarUploadResponse(result.user(), result.storedBytes(), result.width(), result.height());
    }

    @DeleteMapping("/me/avatar")
    public UserResponse deleteAvatar(@AuthenticationPrincipal Jwt jwt) {
        return userService.deleteAvatar(jwt.getSubject());
    }

    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal Jwt jwt,
                               @Valid @RequestBody ChangeOwnPasswordRequest request) {
        accountSecurityService.changePassword(jwt.getSubject(), request);
    }

    @GetMapping(value = "/{id}/avatar", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<Resource> avatar(@PathVariable Long id) {
        var avatar = userService.loadAvatar(id);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .contentLength(avatar.bytes())
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePrivate().immutable())
                .eTag("\"" + avatar.lastModified() + "\"")
                .body(avatar.resource());
    }
}
