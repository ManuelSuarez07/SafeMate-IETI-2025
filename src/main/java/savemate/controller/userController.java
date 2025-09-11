package safemate.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import safemate.model.User;
import safemate.service.UserService;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    public UserController(UserService userService) { this.userService = userService; }

    @PostMapping
    public ResponseEntity<User> create(@RequestBody User user) {
        User saved = userService.create(user);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> get(@PathVariable Long id) {
        return userService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}