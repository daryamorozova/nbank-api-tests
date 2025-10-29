package iteration2.ui;

import api.models.CreateAccountResponse;
import common.annotations.UserSession;
import common.storage.SessionStorage;
import iteration1.ui.BaseUiTest;
import org.junit.jupiter.api.Test;
import ui.pages.BankAlert;
import ui.pages.DepositPage;
import ui.pages.TransferPage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

public class TransferTest extends BaseUiTest {

    @Test
    @UserSession(2)
    public void userCanTransferTest() {
        var senderSteps = SessionStorage.getSteps(1);
        var receiverSteps = SessionStorage.getSteps(2);

        // 1) Создать аккаунты через API отдельно для каждого пользователя
        CreateAccountResponse senderAcc = senderSteps.createAccount();
        CreateAccountResponse receiverAcc = receiverSteps.createAccount();

        assertThat(senderAcc.getBalance()).isZero();
        assertThat(receiverAcc.getBalance()).isZero();

        // начальный депозит для пользователя
        Double sumOfDeposit = 5000.0;

        new DepositPage().open().deposit(senderAcc.getAccountNumber(), sumOfDeposit)
                .checkAlertMessageAndAccept(STR."\{BankAlert.DEPOSIT_SUCCESS.getMessage()} $\{sumOfDeposit} to account \{senderAcc.getAccountNumber()}");

        var senderAfterDeposit = senderSteps.getAllAccounts().stream()
                .filter(a -> a.getAccountNumber().equals(senderAcc.getAccountNumber()))
                .findFirst()
                .orElseThrow();
        assertThat(senderAfterDeposit.getBalance()).isEqualByComparingTo(sumOfDeposit);

        // осуществление трансфера
        Double sumOfTransfer = 500.0;
        String recipientName = "Name";

        new TransferPage().open().transfer(senderAcc.getAccountNumber(), recipientName, receiverAcc.getAccountNumber(), sumOfTransfer)
                .checkAlertMessageAndAccept(STR."\{BankAlert.TRANSFER_SUCCESS.getMessage()} $\{sumOfTransfer} to account \{receiverAcc.getAccountNumber()}");


        var senderAfterTransfer = senderSteps.getAllAccounts().stream()
                .filter(a -> a.getAccountNumber().equals(senderAcc.getAccountNumber()))
                .findFirst()
                .orElseThrow();

        var receiverAfterTransfer = receiverSteps.getAllAccounts().stream()
                .filter(a -> a.getAccountNumber().equals(receiverAcc.getAccountNumber()))
                .findFirst()
                .orElseThrow();

        assertThat(senderAfterTransfer.getBalance())
                .isCloseTo(
                        sumOfDeposit - sumOfTransfer, // вычисляем ожидаемое значение
                        offset(0.001));
        assertThat(receiverAfterTransfer.getBalance())
                .isEqualByComparingTo(sumOfTransfer);
    }

    @Test
    @UserSession(2)
    public void userCanNotTransferTest() {
        var senderSteps = SessionStorage.getSteps(1);
        var receiverSteps = SessionStorage.getSteps(2);

        // 1) Создать аккаунты через API отдельно для каждого пользователя
        CreateAccountResponse senderAcc = senderSteps.createAccount();
        CreateAccountResponse receiverAcc = receiverSteps.createAccount();

        assertThat(senderAcc.getBalance()).isZero();
        assertThat(receiverAcc.getBalance()).isZero();

        // начальный депозит для пользователя
        Double sumOfDeposit = 5000.0;

        new DepositPage().open().deposit(senderAcc.getAccountNumber(), sumOfDeposit)
                .checkAlertMessageAndAccept(STR."\{BankAlert.DEPOSIT_SUCCESS.getMessage()} $\{sumOfDeposit} to account \{senderAcc.getAccountNumber()}");

        var senderAfterDeposit = senderSteps.getAllAccounts().stream()
                .filter(a -> a.getAccountNumber().equals(senderAcc.getAccountNumber()))
                .findFirst()
                .orElseThrow();
        assertThat(senderAfterDeposit.getBalance()).isEqualByComparingTo(sumOfDeposit);

        // осуществление трансфера
        Double sumOfTransfer = 500000.0;
        String recipientName = "Name";

        new TransferPage().open().transfer(senderAcc.getAccountNumber(), recipientName, receiverAcc.getAccountNumber(), sumOfTransfer)
                .checkAlertMessageAndAccept(STR."\{BankAlert.TRANSFER_ERROR.getMessage()}");


        var senderAfterTransfer = senderSteps.getAllAccounts().stream()
                .filter(a -> a.getAccountNumber().equals(senderAcc.getAccountNumber()))
                .findFirst()
                .orElseThrow();

        var receiverAfterTransfer = receiverSteps.getAllAccounts().stream()
                .filter(a -> a.getAccountNumber().equals(receiverAcc.getAccountNumber()))
                .findFirst()
                .orElseThrow();

        assertThat(senderAfterTransfer.getBalance()).isEqualTo(sumOfDeposit);
        assertThat(receiverAfterTransfer.getBalance()).isZero();
    }
}