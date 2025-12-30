import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class BcryptTest {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String[] passwords = args.length > 0 ? args : new String[]{"test@123"};
        for (String pwd : passwords) {
            System.out.println("\nPassword: " + pwd);
            System.out.println("BCrypt:   " + encoder.encode(pwd));
        }
    }
    
    @org.junit.jupiter.api.Test
    public void generateHash() {
        main(new String[]{"test@123"});
    }
}
