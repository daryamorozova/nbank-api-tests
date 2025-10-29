package iteration2.ui;

import api.models.CreateAccountResponse;
import common.annotations.UserSession;
import common.storage.SessionStorage;
import iteration1.ui.BaseUiTest;
import org.junit.jupiter.api.Test;
import ui.pages.BankAlert;
import ui.pages.DepositPage;
import ui.pages.UserDashboard;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class DepositTest extends BaseUiTest {

    @Test
    @UserSession
    public void userCanDepositTest() {
        new UserDashboard().open().createNewAccount();

        var steps = SessionStorage.getSteps();

        List<CreateAccountResponse> createdAccounts = steps.getAllAccounts();
        assertThat(createdAccounts).hasSize(1);

        CreateAccountResponse firstCreatedAccount = createdAccounts.getFirst();
        String accountNumber = firstCreatedAccount.getAccountNumber();

        new UserDashboard().checkAlertMessageAndAccept
                (BankAlert.NEW_ACCOUNT_CREATED.getMessage() + accountNumber);

        assertThat(firstCreatedAccount.getBalance()).isZero();

        Double sumOfDeposit = 500.0;

        new DepositPage().open().deposit(accountNumber, sumOfDeposit)
                .checkAlertMessageAndAccept(
                        String.format("%s $%s to account %s",
                                BankAlert.DEPOSIT_SUCCESS.getMessage(),
                                sumOfDeposit.toString(),
                                accountNumber)
                );

        List<CreateAccountResponse> updatedAccounts = steps.getAllAccounts();
        CreateAccountResponse updatedAccount = updatedAccounts.stream()
                .filter(a -> a.getAccountNumber().equals(accountNumber))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Account not found after deposit"));

        assertThat(updatedAccount.getBalance()).isEqualByComparingTo(sumOfDeposit);
    }

    @Test
    @UserSession
    public void userCanNotDepositTest() {
        new UserDashboard().open().createNewAccount();

        var steps = SessionStorage.getSteps();

        List<CreateAccountResponse> createdAccounts = steps.getAllAccounts();
        assertThat(createdAccounts).hasSize(1);

        CreateAccountResponse firstCreatedAccount = createdAccounts.getFirst();
        String accountNumber = firstCreatedAccount.getAccountNumber();

        new UserDashboard().checkAlertMessageAndAccept
                (BankAlert.NEW_ACCOUNT_CREATED.getMessage() + accountNumber);

        assertThat(firstCreatedAccount.getBalance()).isZero();

        Double sumOfDeposit = 10000.0;

        new DepositPage().open().deposit(accountNumber, sumOfDeposit)
                .checkAlertMessageAndAccept(BankAlert.DEPOSIT_ERROR.getMessage());

        List<CreateAccountResponse> updatedAccounts = steps.getAllAccounts();
        CreateAccountResponse updatedAccount = updatedAccounts.stream()
                .filter(a -> a.getAccountNumber().equals(accountNumber))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Account not found after deposit"));

        assertThat(updatedAccount.getBalance()).isZero();
    }
}