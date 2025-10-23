package ui.pages;

import lombok.Getter;

@Getter
public enum BankAlert {
    USER_CREATED_SUCCESSFULLY("✅ User created successfully!"),
    USERNAME_MUST_BE_BETWEEN_3_AND_15_CHARACTERS("Username must be between 3 and 15 characters"),
    NEW_ACCOUNT_CREATED("✅ New Account Created! Account Number: "),
    DEPOSIT_SUCCESS("✅ Successfully deposited"), //✅ Successfully deposited $500 to account ACC1!)
    DEPOSIT_ERROR("❌ Please deposit less or equal to 5000$."),
    TRANSFER_SUCCESS("✅ Successfully transferred"), //✅ Successfully transferred $100 to account ACC31!
    TRANSFER_ERROR("❌ Error: Transfer amount cannot exceed 10000");

    private final String message;

    BankAlert(String message) {
        this.message = message;
    }
}