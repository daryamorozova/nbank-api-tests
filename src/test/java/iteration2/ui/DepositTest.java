package iteration2.ui;

import api.models.CreateAccountResponse;
import api.models.CreateUserRequest;
import api.requests.steps.AdminSteps;
import api.requests.steps.UserSteps;
import iteration1.ui.BaseUiTest;
import org.junit.jupiter.api.Test;
import ui.pages.BankAlert;
import ui.pages.DepositPage;
import ui.pages.UserDashboard;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class DepositTest extends BaseUiTest {

    @Test
    public void userCanDepositTest() {
        CreateUserRequest user = AdminSteps.createUser();
        authAsUser(user);

        new UserDashboard().open().createNewAccount();

        UserSteps userSteps = new UserSteps(user.getUsername(), user.getPassword());
        List<CreateAccountResponse> createdAccounts = userSteps.getAllAccounts();

        assertThat(createdAccounts).hasSize(1);
        CreateAccountResponse createdAccount = createdAccounts.getFirst();
        assertThat(createdAccount).isNotNull();

        String accountNumber = createdAccount.getAccountNumber();

        new UserDashboard().checkAlertMessageAndAccept(
                BankAlert.NEW_ACCOUNT_CREATED.getMessage() + accountNumber
        );

        assertThat(createdAccount.getBalance()).isZero();

        Double sumOfDeposit = 500.0;

        new DepositPage().open().deposit(accountNumber, sumOfDeposit)
                .checkAlertMessageAndAccept(STR."\{BankAlert.DEPOSIT_SUCCESS.getMessage()} $\{sumOfDeposit} to account \{accountNumber}");

        CreateAccountResponse updatedAccount = userSteps.getAllAccounts().stream()
                .filter(a -> a.getAccountNumber().equals(accountNumber))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Account not found after deposit"));

        assertThat(updatedAccount.getBalance()).isEqualTo(sumOfDeposit);

    }

    @Test
    public void userCanNotDepositTest() {
        CreateUserRequest user = AdminSteps.createUser();
        authAsUser(user);

        new UserDashboard().open().createNewAccount();

        UserSteps userSteps = new UserSteps(user.getUsername(), user.getPassword());
        List<CreateAccountResponse> createdAccounts = userSteps.getAllAccounts();

        assertThat(createdAccounts).hasSize(1);
        CreateAccountResponse createdAccount = createdAccounts.getFirst();
        assertThat(createdAccount).isNotNull();

        String accountNumber = createdAccount.getAccountNumber();

        new UserDashboard().checkAlertMessageAndAccept(
                BankAlert.NEW_ACCOUNT_CREATED.getMessage() + accountNumber
        );

        assertThat(createdAccount.getBalance()).isZero();

        Double sumOfDeposit = 10000.0;

        new DepositPage().open().deposit(accountNumber, sumOfDeposit)
                .checkAlertMessageAndAccept(BankAlert.DEPOSIT_ERROR.getMessage());

        CreateAccountResponse updatedAccount = userSteps.getAllAccounts().stream()
                .filter(a -> a.getAccountNumber().equals(accountNumber))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Account not found after deposit"));

        assertThat(updatedAccount.getBalance()).isZero();
    }
}