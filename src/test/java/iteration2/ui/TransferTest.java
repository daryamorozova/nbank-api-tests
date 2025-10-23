package iteration2.ui;

import api.models.CreateAccountResponse;
import api.models.CreateUserRequest;
import api.requests.steps.AdminSteps;
import api.requests.steps.UserSteps;
import iteration1.ui.BaseUiTest;
import org.junit.jupiter.api.Test;
import ui.pages.BankAlert;
import ui.pages.DepositPage;
import ui.pages.TransferPage;
import ui.pages.UserDashboard;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TransferTest extends BaseUiTest {

    @Test
    public void userCanTransferTest() {
        CreateUserRequest user1 = AdminSteps.createUser();
        authAsUser(user1);

        new UserDashboard().open().createNewAccount();

        UserSteps userSteps1 = new UserSteps(user1.getUsername(), user1.getPassword());
        List<CreateAccountResponse> createdAccounts1 = userSteps1.getAllAccounts();

        assertThat(createdAccounts1).hasSize(1);
        CreateAccountResponse createdAccount1 = createdAccounts1.getFirst();
        assertThat(createdAccount1).isNotNull();

        String accountNumber1 = createdAccount1.getAccountNumber();

        new UserDashboard().checkAlertMessageAndAccept(
                BankAlert.NEW_ACCOUNT_CREATED.getMessage() + accountNumber1
        );

        assertThat(createdAccount1.getBalance()).isZero();


        CreateUserRequest user2 = AdminSteps.createUser();
        authAsUser(user2);

        new UserDashboard().open().createNewAccount();

        UserSteps userSteps2 = new UserSteps(user2.getUsername(), user2.getPassword());
        List<CreateAccountResponse> createdAccounts2 = userSteps2.getAllAccounts();

        assertThat(createdAccounts2).hasSize(1);
        CreateAccountResponse createdAccount2 = createdAccounts2.getFirst();
        assertThat(createdAccount2).isNotNull();

        String accountNumber2 = createdAccount2.getAccountNumber();

        new UserDashboard().checkAlertMessageAndAccept(
                BankAlert.NEW_ACCOUNT_CREATED.getMessage() + accountNumber2
        );

        assertThat(createdAccount2.getBalance()).isZero();

        // начальный депозит для пользователя 2
        Double sumOfDeposit = 5000.0;

        new DepositPage().open().deposit(accountNumber2, sumOfDeposit)
                .checkAlertMessageAndAccept(STR."\{BankAlert.DEPOSIT_SUCCESS.getMessage()} $\{sumOfDeposit} to account \{accountNumber2}");

        CreateAccountResponse updatedAccount2 = userSteps2.getAllAccounts().stream()
                .filter(a -> a.getAccountNumber().equals(accountNumber2))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Account not found after deposit"));

        // Баланс отправителя после депозита
        Double initialBalance2 = updatedAccount2.getBalance();
        assertThat(initialBalance2).isEqualTo(sumOfDeposit);

        // осуществление трансфера
        Double sumOfTransfer = 500.0;
        String recipientName = "Name";

        new TransferPage().open().transfer(accountNumber2, recipientName, accountNumber1, sumOfTransfer)
                .checkAlertMessageAndAccept(STR."\{BankAlert.TRANSFER_SUCCESS.getMessage()} $\{sumOfTransfer} to account \{accountNumber1}");

        CreateAccountResponse afterTransferAccount2 = userSteps2.getAllAccounts().stream()
                .filter(a -> a.getAccountNumber().equals(accountNumber2))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Account not found after deposit"));

        Double afterTransferBalance2 = initialBalance2 - sumOfTransfer;
        assertThat(afterTransferAccount2.getBalance()).isEqualTo(afterTransferBalance2);

        CreateAccountResponse afterTransferAccount1 = userSteps1.getAllAccounts().stream()
                .filter(a -> a.getAccountNumber().equals(accountNumber1))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Account not found after deposit"));

        assertThat(afterTransferAccount1.getBalance()).isEqualTo(sumOfTransfer);
    }

    @Test
    public void userCanNotTransferTest() {
        CreateUserRequest user1 = AdminSteps.createUser();
        authAsUser(user1);

        new UserDashboard().open().createNewAccount();

        UserSteps userSteps1 = new UserSteps(user1.getUsername(), user1.getPassword());
        List<CreateAccountResponse> createdAccounts1 = userSteps1.getAllAccounts();

        assertThat(createdAccounts1).hasSize(1);
        CreateAccountResponse createdAccount1 = createdAccounts1.getFirst();
        assertThat(createdAccount1).isNotNull();

        String accountNumber1 = createdAccount1.getAccountNumber();

        new UserDashboard().checkAlertMessageAndAccept(
                BankAlert.NEW_ACCOUNT_CREATED.getMessage() + accountNumber1
        );

        assertThat(createdAccount1.getBalance()).isZero();


        CreateUserRequest user2 = AdminSteps.createUser();
        authAsUser(user2);

        new UserDashboard().open().createNewAccount();

        UserSteps userSteps2 = new UserSteps(user2.getUsername(), user2.getPassword());
        List<CreateAccountResponse> createdAccounts2 = userSteps2.getAllAccounts();

        assertThat(createdAccounts2).hasSize(1);
        CreateAccountResponse createdAccount2 = createdAccounts2.getFirst();
        assertThat(createdAccount2).isNotNull();

        String accountNumber2 = createdAccount2.getAccountNumber();

        new UserDashboard().checkAlertMessageAndAccept(
                BankAlert.NEW_ACCOUNT_CREATED.getMessage() + accountNumber2
        );

        assertThat(createdAccount2.getBalance()).isZero();

        // начальный депозит для пользователя 2
        Double sumOfDeposit = 5000.0;

        new DepositPage().open().deposit(accountNumber2, sumOfDeposit)
                .checkAlertMessageAndAccept(STR."\{BankAlert.DEPOSIT_SUCCESS.getMessage()} $\{sumOfDeposit} to account \{accountNumber2}");

        CreateAccountResponse updatedAccount2 = userSteps2.getAllAccounts().stream()
                .filter(a -> a.getAccountNumber().equals(accountNumber2))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Account not found after deposit"));

        // Баланс отправителя после депозита
        Double initialBalance2 = updatedAccount2.getBalance();
        assertThat(initialBalance2).isEqualTo(sumOfDeposit);

        // осуществление трансфера
        Double sumOfTransfer = 500000.0;
        String recipientName = "Name";

        new TransferPage().open().transfer(accountNumber2, recipientName, accountNumber1, sumOfTransfer)
                .checkAlertMessageAndAccept(BankAlert.TRANSFER_ERROR.getMessage());

        CreateAccountResponse afterTransferAccount2 = userSteps2.getAllAccounts().stream()
                .filter(a -> a.getAccountNumber().equals(accountNumber2))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Account not found after deposit"));

        Double afterTransferBalance2 = initialBalance2;
        assertThat(afterTransferAccount2.getBalance()).isEqualTo(afterTransferBalance2);

        CreateAccountResponse afterTransferAccount1 = userSteps1.getAllAccounts().stream()
                .filter(a -> a.getAccountNumber().equals(accountNumber1))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Account not found after deposit"));

        assertThat(afterTransferAccount1.getBalance()).isZero();
    }
}
