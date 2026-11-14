package fr.skyframe.lms.web;

import fr.skyframe.lms.security.CurrentUser;
import fr.skyframe.lms.service.AccountService;
import fr.skyframe.lms.web.dto.AccountRoleRequest;
import fr.skyframe.lms.web.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin only: decide who is a teacher (may create courses) or an admin.
 * The class-level @PreAuthorize applies to every endpoint here.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("@access.isAdmin()")
public class AdminController {

    private final AccountService accountService;
    private final CurrentUser currentUser;

    public AdminController(AccountService accountService, CurrentUser currentUser) {
        this.accountService = accountService;
        this.currentUser = currentUser;
    }

    @GetMapping("/users")
    public List<UserResponse> users() {
        return accountService.listUsers().stream().map(UserResponse::from).toList();
    }

    /** Make someone a teacher or admin, or take the role away again. */
    @PutMapping("/users/{id}/role")
    public UserResponse setRole(@PathVariable Long id, @Valid @RequestBody AccountRoleRequest request) {
        return UserResponse.from(accountService.setRole(id, request.role(), currentUser.id()));
    }
}
