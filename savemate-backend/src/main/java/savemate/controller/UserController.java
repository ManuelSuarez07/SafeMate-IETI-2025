package savemate.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import savemate.dto.UserDTO;
import savemate.service.UserService;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserDTO> register(@RequestBody RegisterRequest request) {
        try {
            UserDTO created = userService.createUser(
                    request.getUser(),
                    request.getPassword()
            );
            return new ResponseEntity<>(created, HttpStatus.CREATED);
        } catch (RuntimeException e){
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
    }

    public static class RegisterRequest {
        private UserDTO user;
        private String password;

        public UserDTO getUser() { return user; }
        public void setUser(UserDTO user) { this.user = user; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}
