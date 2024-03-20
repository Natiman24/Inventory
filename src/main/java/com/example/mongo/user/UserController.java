package com.example.mongo.user;

import com.example.mongo.auth.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(value = "*")
public class UserController {
    private final UserService userService;

    @Qualifier("customUserAuthenticationManager")
    private final AuthenticationManager authenticationManager;

    private final JwtUtil jwtUtil;

    private final JavaMailSender javaMailSender;

    @PostMapping
    public ResponseEntity<Map<String, Object>> createUser(@RequestBody User user){
        Map<String, Object> response = new HashMap<>();

        String status = userService.createUser(user);
        switch (status) {
            case "invalidRole" -> {
                response.put("message", "The role provided is not correct");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            case "invalidEmail" -> {
                response.put("message", "The email you entered is not valid");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            case "phone" -> {
                response.put("message", "There is a user with this phone number");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            case "added" -> {
                response.put("message", "User registered");
                return new ResponseEntity<>(response, HttpStatus.CREATED);
            }
            case "errorEmail" -> {
                response.put("message", "Error occurred while validating email");
                return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
            }

            default -> {
                response.put("message","Unknown error, BUG");
                return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
            }

        }

    }

    @GetMapping
    public ResponseEntity<List<UserRetrievalDTO>> getUsers(){
        return new ResponseEntity<>(userService.getUsers(), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable("id") String id){
        User userFound = userService.getUserById(id);
        if(userFound != null) {
            User user = User.builder().
                    id(userFound.getId()).
                    firstName(userFound.getFirstName()).
                    lastName(userFound.getLastName()).
                    role(userFound.getRole()).
                    phoneNumber(userFound.getPhoneNumber()).
                    email(userFound.getEmail()).
                    joinedOn(userFound.getJoinedOn()).
                    isFirstTime(userFound.getFirstTime()).
                    build();
            return new ResponseEntity<>(user, HttpStatus.OK);
        }

        return new ResponseEntity<>(userFound, HttpStatus.NOT_FOUND);
    }


    @PostMapping("/login")
    public ResponseEntity<Map<String,Object>> login(@RequestBody LoginDTO loginCredentials){
        try{

            Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginCredentials.getPhoneNumber(), loginCredentials.getPassword()));
            String phone = authentication.getName();

            User userFound = userService.getUserByPhone(phone);

            if(!userFound.getRole().equals(loginCredentials.getRole())){
                Map<String,Object> res = new HashMap<>();
                res.put("message", "You are not allowed to access this level");
                return new ResponseEntity<>(res, HttpStatus.UNAUTHORIZED);
            }

            if(userFound.getFirstTime()){
                Map<String,Object> firstTime = new HashMap<>();
                firstTime.put("message", "Correct credentials, change your password");
                firstTime.put("isFirstTime", true);
                return new ResponseEntity<>(firstTime, HttpStatus.OK);
            }

            User user = User.builder().
                    id(userFound.getId()).
                    phoneNumber(phone).
                    email(userFound.getEmail()).
                    firstName(userFound.getFirstName()).
                    lastName(userFound.getLastName()).
                    role(userFound.getRole()).
                    build();

            if(!userFound.getOtp().isEmpty()){
                userService.removeOtp(userFound);
            }

            String token = jwtUtil.createToken(user);
            Map<String,Object> loginRes = new HashMap<>();
            loginRes.put("phone",phone);
            loginRes.put("token", token);
            loginRes.put("message", "Login successful");


            return new ResponseEntity<>(loginRes, HttpStatus.OK);
        }catch (BadCredentialsException e){
            Map<String,Object> errorRes = new HashMap<>();
            errorRes.put("httpResponse",HttpStatus.BAD_REQUEST);
            errorRes.put("message","Invalid username or password");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorRes);
        }catch (Exception e){
            System.out.println(e);
            Map<String,Object> errorRes = new HashMap<>();
            errorRes.put("httpResponse",HttpStatus.BAD_REQUEST);
            errorRes.put("message",e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorRes);
        }
    }

    @DeleteMapping("/{id}/{toBeDeletedId}")
    public ResponseEntity<Map<String,Object>> deleteUser(@PathVariable("id") String id, @PathVariable("toBeDeletedId") String toBeDeletedId){
       try {

           String deleteRes = userService.deleteUser(id, toBeDeletedId);
           Map<String, Object> response = new HashMap<>();

           if (deleteRes.equals("notFound")) {
               response.put("message", "User can't be found");
               return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
           }
           if (deleteRes.equals("notFoundDelete")) {
               response.put("message", "User to be deleted can't be found");
               return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
           }
           if (deleteRes.equals("unauthorized")) {
               response.put("message", "You are not authorized to delete this user");
               return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
           }

           response.put("message", "User deleted successfully");
           return new ResponseEntity<>(response, HttpStatus.OK);
       } catch (Exception e){
           Map<String,Object> errorRes = new HashMap<>();
           errorRes.put("httpResponse",HttpStatus.BAD_REQUEST);
           errorRes.put("message",e.getMessage());
           return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorRes);
       }
    }


    @GetMapping("/employees")
    public ResponseEntity<List<UserRetrievalDTO>> getEmployees(){
        return new ResponseEntity<>(userService.getEmployees(), HttpStatus.OK);
    }

//    @GetMapping("/sales")
//    public ResponseEntity<List<User>> getSalesUsers(){
//        return new ResponseEntity<>(userService.getSalesUsers(), HttpStatus.OK);
//    }

    @PutMapping("/edit-profile/{id}")
    public ResponseEntity<Map<String,Object>> editProfile(@PathVariable("id") String id, @RequestBody EditProfileDTO user){
       try {
           String updateRes = userService.editProfile(id, user);
           Map<String, Object> response = new HashMap<>();

           switch (updateRes) {
               case "notFound" -> {
                   response.put("message", "User can't be found");
                   return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
               }
               case "phone" -> {
                   response.put("message", "There is a user with this phone number");
                   return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
               }
               case "updated" -> {
                   response.put("message", "User updated successfully");
                   return new ResponseEntity<>(response, HttpStatus.OK);
               }
               case "invalidEmail" -> {
                   response.put("message", "The email is not valid");
                   return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
               }
               case "errorEmail" -> {
                   response.put("message", "Error occurred while validating email");
                   return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
               }
               default -> {

                   response.put("message", "An error has occurred");
                   return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
               }
           }

       } catch (Exception e){
           Map<String,Object> errorRes = new HashMap<>();
           errorRes.put("httpResponse",HttpStatus.BAD_REQUEST);
           errorRes.put("message",e.getMessage());
           return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorRes);
       }
    }

    @PutMapping("/change-password")
    public ResponseEntity<Map<String, Object>> changePassword(@RequestBody ChangePassDTO newPassInfo){
        try {
            String newPassRes = userService.changePassword(newPassInfo);
            Map<String, Object> response = new HashMap<>();

            if (newPassRes.equals("notFound")) {
                response.put("message", "User can't be found");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }

            if (newPassRes.equals("incorrect")) {
                response.put("message", "Incorrect password");
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }

            if (newPassRes.equals("updated")) {
                response.put("message", "Password updated successfully");
                return new ResponseEntity<>(response, HttpStatus.OK);
            }

            response.put("message", "An error has occurred");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        } catch (Exception e){
            Map<String,Object> errorRes = new HashMap<>();
            errorRes.put("httpResponse",HttpStatus.BAD_REQUEST);
            errorRes.put("message",e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorRes);
        }
    }

    @GetMapping("/phone/{phone}")
    public ResponseEntity<User> getUserByPhone(@PathVariable("phone") String phoneNumber){
        User userFound = userService.getUserByPhone(phoneNumber);
        if(userFound != null) {
            User user = User.builder().
                    id(userFound.getId()).
                    firstName(userFound.getFirstName()).
                    lastName(userFound.getLastName()).
                    role(userFound.getRole()).
                    phoneNumber(userFound.getPhoneNumber()).
                    email(userFound.getEmail()).
                    joinedOn(userFound.getJoinedOn()).
                    isFirstTime(userFound.getFirstTime()).
                    build();
            return new ResponseEntity<>(user, HttpStatus.OK);
        }
        return new ResponseEntity<>(userFound, HttpStatus.OK);
    }

    @PostMapping("/forgot-password/{phone-number}")
    public ResponseEntity<Map<String,Object>> forgotPassword(@PathVariable("phone-number") String phoneNumber){
        try {
            Map<String, Object> response = new HashMap<>();
            String res = userService.forgotPass(phoneNumber);
            if (res.equals("notFound")) {
                response.put("message", "There is no user with this phone number.");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }
            response.put("message", "A one time password has been sent to your email.");
            response.put("email", res);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e){
            Map<String,Object> errorRes = new HashMap<>();
            errorRes.put("httpResponse",HttpStatus.BAD_REQUEST);
            errorRes.put("message",e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorRes);
        }
    }

    @PostMapping("/verify-otp/{phone-number}")
    public ResponseEntity<Map<String,Object>> verifyOtp(@PathVariable("phone-number") String phoneNumber, @RequestParam String otp){
       try {
           Map<String, Object> response = new HashMap<>();
           String res = userService.verifyOtp(phoneNumber, otp);
           switch (res) {
               case "notFound" -> {
                   response.put("message", "There is a problem fetching the user with this phone number");
                   return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
               }
               case "otpNotFound" -> {
                   response.put("message", "There is no otp found");
                   return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
               }
               case "notAllowed" -> {
                   response.put("message", "The phone number or otp is incorrect");
                   return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
               }
           }


           response.put("message", "Correct otp");
           return new ResponseEntity<>(response, HttpStatus.OK);
       } catch (Exception e){
           Map<String,Object> errorRes = new HashMap<>();
           errorRes.put("httpResponse",HttpStatus.BAD_REQUEST);
           errorRes.put("message",e.getMessage());
           return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorRes);
       }
    }

    @PutMapping("/forgot-password/change-password")
    public ResponseEntity<Map<String,Object>> changePasswordOtp(@RequestBody ForgotPassDTO userData){
        try {
            Map<String, Object> response = new HashMap<>();
            String res = userService.changePasswordOtp(userData);
            switch (res) {
                case "notFound" -> {
                    response.put("message", "There is no user with this phone number");
                    return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
                }
                case "otpNotFound" -> {
                    response.put("message", "There is no otp found");
                    return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
                }
                case "notAllowed" -> {
                    response.put("message", "The phone number or otp is incorrect");
                    return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
                }
            }

            response.put("message", "Password updated successfully");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e){
            Map<String,Object> errorRes = new HashMap<>();
            errorRes.put("httpResponse",HttpStatus.BAD_REQUEST);
            errorRes.put("message",e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorRes);
        }
    }


}
