// Importing the tools that are going to be used
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class Solution {
    private static final Scanner INPUT = new Scanner(System.in);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Map<String, User> USERS = new LinkedHashMap<>();
    private static final int PASSWORD_ITERATIONS = 120_000;
    private static final int PASSWORD_KEY_LENGTH = 256;
    private static long nextAccountNumber = 100001;

    // Dashboard
    public static void main(String[] args) throws Exception {
        while (true) {
            System.out.println("\n===== BUSINESS BANKING =====");
            System.out.println("1. Sign up");
            System.out.println("2. Log in");
            System.out.println("3. Exit");
            int choice = readInt("Choose an option: ", 1, 3);

            if (choice == 1) {
                signUp();
            } else if (choice == 2) {
                User user = logIn();
                if (user != null) {
                    manageAccounts(user);
                }
            } else {
                System.out.println("Goodbye.");
                return;
            }
        }
    }

    // Error Handling
    private static void signUp() throws Exception {
        System.out.print("Choose a username: ");
        String username = INPUT.nextLine().trim();
        if (username.isEmpty()) {
            System.out.println("Username cannot be empty.");
            return;
        }
        if (USERS.containsKey(username)) {
            System.out.println("That username is already registered.");
            return;
        }

        char[] password = readPassword("Choose a password (at least 8 characters): ");
        if (password.length < 8) {
            Arrays.fill(password, '\0');
            System.out.println("Password must contain at least 8 characters.");
            return;
        }

        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        byte[] passwordHash = hashPassword(password, salt);
        Arrays.fill(password, '\0');

        USERS.put(username, new User(username, salt, passwordHash));
        System.out.println("Account created. You can now log in.");
    }

    // Log in function
    private static User logIn() throws Exception {
        System.out.print("Username: ");
        String username = INPUT.nextLine().trim();
        User user = USERS.get(username);
        char[] password = readPassword("Password: ");

        // Password Hash
        boolean authenticated = user != null
                && MessageDigest.isEqual(user.passwordHash, hashPassword(password, user.salt));
        Arrays.fill(password, '\0');

        // Authentication for username and password
        if (!authenticated) {
            System.out.println("Invalid username or password.");
            return null;
        }

        System.out.println("Login successful. Welcome, " + user.username + "!");
        return user;
    }

    private static void manageAccounts(User user) {
        if (user.accounts.isEmpty()) {
            System.out.println("You do not have any accounts yet.");
            if (readYesNo("Would you like to create one now? (y/n): ")) {
                createAccount(user);
            }
        }

        //Account Menu
        while (true) {
            System.out.println("\n===== ACCOUNT MENU =====");
            System.out.println("1. View accounts and balances");
            System.out.println("2. Create an account");
            System.out.println("3. Deposit");
            System.out.println("4. Withdraw");
            System.out.println("5. Transfer between your accounts");
            System.out.println("6. Log out");
            int choice = readInt("Choose an option: ", 1, 6);

            switch (choice) {
                case 1:
                    showAccounts(user);
                    break;
                case 2:
                    createAccount(user);
                    break;
                case 3:
                    deposit(user);
                    break;
                case 4:
                    withdraw(user);
                    break;
                case 5:
                    transfer(user);
                    break;
                case 6:
                    System.out.println("You have been logged out.");
                    return;
                default:
                    throw new IllegalStateException("Unexpected menu choice.");
            }
        }
    }

    private static void createAccount(User user) {
        System.out.println("\nAccount types:");
        AccountType[] types = AccountType.values();
        for (int i = 0; i < types.length; i++) {
            System.out.printf("%d. %s (overdraft up to %s)%n",
                    i + 1, types[i].displayName, formatMoney(types[i].overdraftLimit));
        }

        AccountType type = types[readInt("Choose an account type: ", 1, types.length) - 1];
        if (user.accounts.containsKey(type)) {
            System.out.println("You already have a " + type.displayName + " account.");
            return;
        }

        boolean twoSignatories = readYesNo("Does this account require two signatories? (y/n): ");
        String accountNumber = String.valueOf(nextAccountNumber++);
        Account account = new Account(accountNumber, type, twoSignatories);
        user.accounts.put(type, account);

        System.out.println(type.displayName + " account " + accountNumber + " created.");
        if (twoSignatories) {
            System.out.println("Withdrawals and transfers are disabled until a dual-approval process is available.");
        }
    }

    private static void showAccounts(User user) {
        if (user.accounts.isEmpty()) {
            System.out.println("No accounts to display.");
            return;
        }

        System.out.println("\n=== YOUR ACCOUNTS ===");
        for (Account account : user.accounts.values()) {
            System.out.printf("%s account | Number: %s | Balance: %s | Overdraft: %s | Two signatories: %s%n",
                    account.type.displayName,
                    account.number,
                    formatMoney(account.balance),
                    formatMoney(account.type.overdraftLimit),
                    account.requiresTwoSignatories ? "Yes" : "No");
        }
    }

    private static void deposit(User user) {
        Account account = selectAccount(user);
        if (account == null) {
            return;
        }

        BigDecimal amount = readPositiveAmount("Deposit amount: ");
        account.balance = account.balance.add(amount);
        System.out.println("Deposit complete. New balance: " + formatMoney(account.balance));
    }

    private static void withdraw(User user) {
        Account account = selectAccount(user);
        if (account == null) {
            return;
        }
        if (account.requiresTwoSignatories) {
            System.out.println("Withdrawal denied: this account requires approval from two signatories.");
            return;
        }

        BigDecimal amount = readPositiveAmount("Withdrawal amount: ");
        BigDecimal newBalance = account.balance.subtract(amount);
        if (newBalance.compareTo(account.type.overdraftLimit.negate()) < 0) {
            System.out.println("Withdrawal denied: it would exceed this account's overdraft limit.");
            return;
        }

        account.balance = newBalance;
        System.out.println("Withdrawal complete. New balance: " + formatMoney(account.balance));
    }

    private static void transfer(User user) {
        Account source = selectAccount(user);
        if (source == null) {
            return;
        }
        if (source.requiresTwoSignatories) {
            System.out.println("Transfer denied: this account requires approval from two signatories.");
            return;
        }

        Account destination = selectAccount(user);
        if (destination == null) {
            return;
        }
        if (source == destination) {
            System.out.println("Choose two different accounts for a transfer.");
            return;
        }

        BigDecimal amount = readPositiveAmount("Transfer amount: ");
        BigDecimal newSourceBalance = source.balance.subtract(amount);
        if (newSourceBalance.compareTo(source.type.overdraftLimit.negate()) < 0) {
            System.out.println("Transfer denied: it would exceed the source account's overdraft limit.");
            return;
        }

        // Update both balances only after validating the complete transfer.
        source.balance = newSourceBalance;
        destination.balance = destination.balance.add(amount);
        System.out.println("Transfer complete.");
    }

    private static Account selectAccount(User user) {
        if (user.accounts.isEmpty()) {
            System.out.println("You do not have any accounts. Create one first.");
            return null;
        }

        Account[] accounts = user.accounts.values().toArray(new Account[0]);
        System.out.println("Select an account:");
        for (int i = 0; i < accounts.length; i++) {
            System.out.printf("%d. %s (%s)%n",
                    i + 1, accounts[i].type.displayName, accounts[i].number);
        }
        return accounts[readInt("Account: ", 1, accounts.length) - 1];
    }

    private static boolean readYesNo(String prompt) {
        while (true) {
            System.out.print(prompt);
            String answer = INPUT.nextLine().trim();
            if (answer.equalsIgnoreCase("y") || answer.equalsIgnoreCase("yes")) {
                return true;
            }
            if (answer.equalsIgnoreCase("n") || answer.equalsIgnoreCase("no")) {
                return false;
            }
            System.out.println("Please enter y or n.");
        }
    }

    private static int readInt(String prompt, int minimum, int maximum) {
        while (true) {
            System.out.print(prompt);
            try {
                int value = Integer.parseInt(INPUT.nextLine().trim());
                if (value >= minimum && value <= maximum) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
                // Reprompt below for non-numeric input.
            }
            System.out.printf("Enter a number from %d to %d.%n", minimum, maximum);
        }
    }

    private static BigDecimal readPositiveAmount(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                BigDecimal amount = new BigDecimal(INPUT.nextLine().trim())
                        .setScale(2, RoundingMode.UNNECESSARY);
                if (amount.compareTo(BigDecimal.ZERO) > 0) {
                    return amount;
                }
            } catch (NumberFormatException | ArithmeticException ignored) {
                // Reprompt if the amount is malformed or has fractions of a penny.
            }
            System.out.println("Enter a positive amount with no more than two decimal places.");
        }
    }

    private static char[] readPassword(String prompt) {
        java.io.Console console = System.console();
        if (console != null) {
            char[] password = console.readPassword("%s", prompt);
            return password == null ? new char[0] : password;
        }

        // IDE consoles often have no java.io.Console; Scanner input may be visible.
        System.out.print(prompt);
        return INPUT.nextLine().toCharArray();
    }

    private static byte[] hashPassword(char[] password, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password, salt, PASSWORD_ITERATIONS, PASSWORD_KEY_LENGTH);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec)
                    .getEncoded();
        } finally {
            spec.clearPassword();
        }
    }

    private static String formatMoney(BigDecimal amount) {
        return "£" + amount.setScale(2, RoundingMode.UNNECESSARY).toPlainString();
    }

    private enum AccountType {
        SMALL_BUSINESS("Small Business", new BigDecimal("1000.00")),
        COMMUNITY("Community", new BigDecimal("2500.00")),
        CLIENT("Client", new BigDecimal("1500.00"));

        private final String displayName;
        private final BigDecimal overdraftLimit;

        AccountType(String displayName, BigDecimal overdraftLimit) {
            this.displayName = displayName;
            this.overdraftLimit = overdraftLimit;
        }
    }

    private static class User {
        private final String username;
        private final byte[] salt;
        private final byte[] passwordHash;
        private final Map<AccountType, Account> accounts = new LinkedHashMap<>();

        private User(String username, byte[] salt, byte[] passwordHash) {
            this.username = username;
            this.salt = salt;
            this.passwordHash = passwordHash;
        }
    }

    private static class Account {
        private final String number;
        private final AccountType type;
        private final boolean requiresTwoSignatories;
        private BigDecimal balance = BigDecimal.ZERO.setScale(2);

        private Account(String number, AccountType type, boolean requiresTwoSignatories) {
            this.number = number;
            this.type = type;
            this.requiresTwoSignatories = requiresTwoSignatories;
        }
    }
}
