package com.example.mongo.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import org.apache.http.util.EntityUtils;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;


import java.io.DataInput;
import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    @Value("${app.secretKey}")
    private String SECRET_KEY;

    @Value("${spring.email.validation.apikey}")
    private String emailValidationApiKey;

    private final JavaMailSender javaMailSender;



    public String createUser(User user){

        ObjectMapper objectMapper = new ObjectMapper();
        // null because mongodb automatically sets it
        user.setId(null);
        user.setJoinedOn(LocalDate.now());
        user.setFirstTime(true);
        user.setOtp("");


        //Check if the provided role is valid
        if(!UserType.EMPLOYEE.equals(user.getRole())){
            return "invalidRole";
        }



        // isPresent is used with Optional
        if (userRepository.findByPhoneNumber(user.getPhoneNumber()).isPresent())
            return "phone";

        Map<EmailValidationResult, String> resultMap = makeAbstractEmailRequest(this, user.getEmail());

        EmailValidationResult result = resultMap.keySet().iterator().next();
        String status = resultMap.values().iterator().next();

        if(result == null && status.equals("errorRequest")){
            return "errorEmail";
        } else if(status.equals("successful")) {
            if (result.getDeliverability().equals("UNDELIVERABLE") || result.getDeliverability().equals("UNKNOWN") && !result.getIsSmtpValid().isValue() || result.getIsDisposableEmail().isValue() || !result.getIsValidFormat().isValue()) {
                return "invalidEmail";
            }
        } else if(result == null && status.equals("error")){
            if (!isValidEmail(user.getEmail())) {
                System.out.println("invalid Email");
                return "invalidEmail";
            }
        }



        user.setEmail(user.getEmail().toLowerCase());
        user.setRole(user.getRole());
        user.setPassword(user.getFirstName() + "123");
        String password = user.getPassword();



        String hashedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());
        user.setPassword(hashedPassword);

        userRepository.save(user);
        return "added";
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        Pattern pattern = Pattern.compile(emailRegex);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }

    // Validate if the provided role is a valid UserType
//    private boolean isValidRole(UserType role) {
//        for (UserType validRole : UserType.values()) {
//            if (role == validRole) {
//                return true;
//            }
//        }
//        return false;
//    }

    public List<UserRetrievalDTO> getUsers() {
        List <User> users = userRepository.findAll();

        return users.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<UserRetrievalDTO> getEmployees(){
        List<User> employees = userRepository.findByRole(String.valueOf(UserType.EMPLOYEE));
        return employees.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private UserRetrievalDTO convertToDTO(User user){
        return UserRetrievalDTO.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .phoneNumber(user.getPhoneNumber())
                .email(user.getEmail())
                .joinedOn(user.getJoinedOn())
                .build();
    }


    public User getUserByPhone(String phone){
        return userRepository.findByPhoneNumber(phone).orElse(null);
    }

    public User getUserById(String id){
        return userRepository.findById(id).orElse(null);
    }

    public String editProfile(String id, EditProfileDTO user){
        ObjectMapper objectMapper = new ObjectMapper();

        User userFound = getUserById(id);

        if(userFound == null){
            return "notFound";
        }

        boolean present = userRepository.findByPhoneNumber(user.getPhoneNumber()).isPresent();
        System.out.println(present);

        // isPresent is used with Optional
        if (present && !userFound.getPhoneNumber().equals(user.getPhoneNumber())) {
            return "phone";
        }

//        if(isValidEmail(user.getEmail())){
//            return "invalidEmail";
//        }

        Map<EmailValidationResult, String> resultMap = makeAbstractEmailRequest(this, user.getEmail());

        EmailValidationResult result = resultMap.keySet().iterator().next();
        String status = resultMap.values().iterator().next();

        if(result == null && status.equals("errorRequest")){
            return "errorEmail";
        } else if(status.equals("successful")) {
            if (result.getDeliverability().equals("UNDELIVERABLE") || result.getDeliverability().equals("UNKNOWN") && !result.getIsSmtpValid().isValue() || result.getIsDisposableEmail().isValue() || !result.getIsValidFormat().isValue()) {
                return "invalidEmail";
            }
        } else if(result == null && status.equals("error")){
            if (!isValidEmail(user.getEmail())) {
                System.out.println("invalid Email");
                return "invalidEmail";
            }
        }



        userFound.setFirstName(user.getFirstName()); // removed tolowerCase()
        userFound.setLastName(user.getLastName());
        userFound.setPhoneNumber(user.getPhoneNumber());
        userFound.setEmail(user.getEmail().toLowerCase());
        userRepository.save(userFound);
        return "updated";

    }

    public String changePassword(ChangePassDTO newPassInfo){
        User userFound = getUserById(newPassInfo.getId());
        if(userFound == null){
            return "notFound";
        }

        if(BCrypt.checkpw(newPassInfo.getOldPass(), userFound.getPassword())){
            String newHashPass = BCrypt.hashpw(newPassInfo.getNewPass(), BCrypt.gensalt());
            userFound.setPassword(newHashPass);
            if(userFound.getFirstTime()){
                userFound.setFirstTime(false);
            }
            userRepository.save(userFound);
            return "updated";
        } else{
            return "incorrect";
        }
    }

    public String deleteUser(String id, String toBeDeletedId){
        User userFound = getUserById(id);


        if(userFound == null){
            return "notFound";
        }

        User userToBeDeleted= getUserById(toBeDeletedId);
        String role = String.valueOf(userFound.getRole());

        if(userToBeDeleted == null){
            return "notFoundDelete";
        }



        // Check if the user performing the deletion has the necessary permissions based on their role
        if (String.valueOf(UserType.ADMIN).equals(role)) {
            // ADMIN can delete EMPLOYEE users
            if(String.valueOf(UserType.ADMIN).equals(String.valueOf(userToBeDeleted.getRole()))){
                return "unauthorized";
            }
            userRepository.deleteById(toBeDeletedId);
            return "deleted";
        } else {
            // EMPLOYEE users are not authorized to delete any other users
            return "unauthorized";
        }

    }

    public String forgotPass(String phoneNumber){
        User user = getUserByPhone(phoneNumber);

        if(user == null){
            return "notFound";
        }
//
        String email = user.getEmail();
        // Get the domain part of the email
        String domain = email.substring(email.indexOf("@"));
        // Get the masked part of the email
        String maskedEmail = maskEmail(email.substring(0, email.length() - domain.length()));
        // Concatenate the masked part with the domain
        String maskedAndPartialEmail = maskedEmail + domain;

        // Generate a random 6-digit number with leading zeros
        int randomNumber = generateRandomNumber();
        String otpNum = String.format("%06d", randomNumber);

        SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
        simpleMailMessage.setTo(email);
        simpleMailMessage.setSubject("Password Reset Request");
        simpleMailMessage.setText("Here is your otp for resetting your password: " + otpNum);
        javaMailSender.send(simpleMailMessage);

        String hashedOtp = BCrypt.hashpw(otpNum, BCrypt.gensalt());
        user.setOtp(hashedOtp);

        userRepository.save(user);

        System.out.println("Random 6-digit number: " + otpNum);
//        System.out.println(maskedAndPartialEmail);
        return maskedAndPartialEmail;
    }

    private String maskEmail(String email){
        int atIndex = email.length();
        StringBuilder maskedEmail = new StringBuilder();
        int charsToMask = atIndex - 4;
        if(charsToMask < -1){
            maskedEmail = new StringBuilder();
            maskedEmail.append(email);
        } else if(charsToMask == -1){
            maskedEmail = new StringBuilder("**");
            maskedEmail.append(email.charAt(atIndex - 1));

        } else if(charsToMask == 0){
            maskedEmail = new StringBuilder("**");
            maskedEmail.append(email, atIndex - 2, atIndex);
        } else{

            for (int i = 0; i < charsToMask; i++) {
                maskedEmail.append("*");
            }
            // Append the rest of the characters
            maskedEmail.append(email, charsToMask, atIndex - 4); // Append the last 4 characters before '@'
            for (int i = atIndex - 4; i < atIndex; i++) {
                maskedEmail.append(email.charAt(i));
            }
            for (int i = atIndex; i < email.length(); i++) {
                maskedEmail.append("*");
            }
        }



        return maskedEmail.toString();


    }

    public String verifyOtp(String phoneNumber, String otp){
        User user = getUserByPhone(phoneNumber);

        if(user == null){
            return "notFound";
        }

        String existingOtp = user.getOtp();

        if(existingOtp.isEmpty()){
            return "otpNotFound";
        }

        if(BCrypt.checkpw(otp,existingOtp)){
            return "allowed";
        } else{
            return "notAllowed";
        }

    }

    public String changePasswordOtp(ForgotPassDTO userData){
        User user = getUserByPhone(userData.getPhoneNumber());

        if(user == null){
            return "notFound";
        }

        String password = userData.getNewPass();
        String existingOtp = user.getOtp();

        if(existingOtp.isEmpty()){
            return "otpNotFound";
        }

        if(BCrypt.checkpw(userData.getOtp(),existingOtp)){
            user.setOtp("");
            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

            user.setPassword(hashedPassword);
            userRepository.save(user);
            return "successful";
        } else{
            System.out.println(BCrypt.checkpw(userData.getOtp(),existingOtp));
            return "notAllowed";
        }




    }

    public void removeOtp(User user){
        user.setOtp("");
        userRepository.save(user);
    }

    private static int generateRandomNumber(){
        Random random = new Random();
        return random.nextInt(1000000);
    }

    private static Map<EmailValidationResult,String> makeAbstractEmailRequest(UserService service, String email){
        try{
            ObjectMapper objectMapper = new ObjectMapper();
            Map<EmailValidationResult,String> res = new HashMap<>();
            String apiKey = service.emailValidationApiKey;
            String url = "https://emailvalidation.abstractapi.com/v1/?api_key=" + apiKey + "&email=" + email;
            System.out.println(url);
            HttpResponse response = Request.Get(url).execute().returnResponse();
            int statusCode = response.getStatusLine().getStatusCode();
            System.out.println("Status Code: " + statusCode);
            if(statusCode == 401 || statusCode == 422 || statusCode == 429 || statusCode == 500 || statusCode == 503){
                System.out.println("UNAUTHORIZEDDDD");
                res.put(null, "error");
                return res;
            }
            HttpEntity content = response.getEntity();
            String jsonString = EntityUtils.toString(content);
            System.out.println(jsonString);
            res.put(objectMapper.readValue(jsonString, EmailValidationResult.class), "successful");
            return res;
        } catch (IOException error) {
            Map<EmailValidationResult,String> res = new HashMap<>();
            res.put(null, "errorRequest");
            System.out.println(error);
            return res;
        }
    }
}
