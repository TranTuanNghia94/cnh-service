// package com.cnh.ies.controller;

// import com.cnh.ies.dto.common.ApiResponse;
// import com.cnh.ies.dto.common.PaginationRequest;
// import com.cnh.ies.dto.common.PaginationResponse;
// import com.cnh.ies.entity.User;
// import com.cnh.ies.service.UserService;
// import io.swagger.v3.oas.annotations.Operation;
// import io.swagger.v3.oas.annotations.tags.Tag;
// import jakarta.validation.Valid;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.http.ResponseEntity;
// import org.springframework.security.access.prepost.PreAuthorize;
// import org.springframework.web.bind.annotation.*;

// import java.util.List;

// @Slf4j
// @RestController
// @RequestMapping("/users")
// @Tag(name = "Users", description = "User management APIs")
// public class UserController extends BaseController<User, Long> {
    
//     private final UserService userService;
    
//     public UserController(UserService userService) {
//         super(userService);
//         this.userService = userService;
//     }
    
//     @GetMapping
//     @PreAuthorize("hasAuthority('USER_READ')")
//     @Operation(summary = "Get all users", description = "Retrieve all users with pagination and search")
//     public ResponseEntity<ApiResponse<PaginationResponse<User>>> getUsers(PaginationRequest request) {
//         log.info("Fetching users with request: {}", request);
//         return findAllPaginated(request);
//     }
    
//     @GetMapping("/{id}")
//     @PreAuthorize("hasAuthority('USER_READ')")
//     @Operation(summary = "Get user by ID", description = "Retrieve a specific user by their ID")
//     public ResponseEntity<ApiResponse<User>> getUserById(@PathVariable Long id) {
//         log.info("Fetching user with ID: {}", id);
//         return findById(id);
//     }
    
//     @PostMapping
//     @PreAuthorize("hasAuthority('USER_CREATE')")
//     @Operation(summary = "Create user", description = "Create a new user")
//     public ResponseEntity<ApiResponse<User>> createUser(@Valid @RequestBody User user) {
//         log.info("Creating new user: {}", user.getUsername());
//         User createdUser = userService.createUser(user);
//         return ResponseEntity.ok(ApiResponse.success(createdUser, "User created successfully"));
//     }
    
//     @PutMapping("/{id}")
//     @PreAuthorize("hasAuthority('USER_UPDATE')")
//     @Operation(summary = "Update user", description = "Update an existing user")
//     public ResponseEntity<ApiResponse<User>> updateUser(@PathVariable Long id, @Valid @RequestBody User user) {
//         log.info("Updating user with ID: {}", id);
//         User updatedUser = userService.updateUser(id, user);
//         return ResponseEntity.ok(ApiResponse.success(updatedUser, "User updated successfully"));
//     }
    
//     @DeleteMapping("/{id}")
//     @PreAuthorize("hasAuthority('USER_DELETE')")
//     @Operation(summary = "Delete user", description = "Delete a user")
//     public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
//         log.info("Deleting user with ID: {}", id);
//         return delete(id);
//     }
    
//     @GetMapping("/active")
//     @PreAuthorize("hasAuthority('USER_READ')")
//     @Operation(summary = "Get active users", description = "Retrieve all active users")
//     public ResponseEntity<ApiResponse<List<User>>> getActiveUsers() {
//         log.info("Fetching active users");
//         List<User> activeUsers = userService.findActiveUsers();
//         return ResponseEntity.ok(ApiResponse.success(activeUsers));
//     }
// }
