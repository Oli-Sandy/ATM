import java.util.HashMap;
import java.util.Scanner;

class User {
    private String password;
    private String email;

    public User(String password, String email) {
        this.password = password;
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public String getEmail() {
        return email;
    }
}

public class Main {

    public static void main(String[] args) {

        Scanner input = new Scanner(System.in);
        HashMap<String, User> users = new HashMap<>();

        int choice;

        do {
            System.out.println("\n===== USER SYSTEM =====");
            System.out.println("1. Sign Up");
            System.out.println("2. Log In");
            System.out.println("3. View Users");
            System.out.println("4. Exit");
            System.out.print("Choose an option: ");

            choice = input.nextInt();
            input.nextLine(); // Clear buffer

            switch (choice) {

                case 1:
                    System.out.print("Enter username: ");
                    String username = input.nextLine();

                    if (users.containsKey(username)) {
                        System.out.println("Username already exists!");
                        break;
                    }

                    System.out.print("Enter password: ");
                    String password = input.nextLine();

                    System.out.print("Enter email: ");
                    String email = input.nextLine();

                    users.put(username, new User(password, email));

                    System.out.println("Account created successfully!");
                    break;

                case 2:
                    System.out.print("Username: ");
                    String loginUsername = input.nextLine();

                    System.out.print("Password: ");
                    String loginPassword = input.nextLine();

                    if (users.containsKey(loginUsername)
                            && users.get(loginUsername).getPassword().equals(loginPassword)) {

                        System.out.println("Login successful!");
                        System.out.println("Welcome, " + loginUsername + "!");

                    } else {
                        System.out.println("Invalid username or password.");
                    }
                    break;

                case 3:
                    if (users.isEmpty()) {
                        System.out.println("No users registered.");
                    } else {
                        System.out.println("\n=== REGISTERED USERS ===");

                        for (String user : users.keySet()) {
                            User details = users.get(user);

                            System.out.println("Username: " + user);
                            System.out.println("Email: " + details.getEmail());
                            System.out.println();
                        }
                    }
                    break;

                case 4:
                    System.out.println("Exiting program...");
                    break;

                default:
                    System.out.println("Invalid option.");
            }

        } while (choice != 4);

        input.close();
    }
}