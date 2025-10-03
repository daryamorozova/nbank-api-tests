package iteration2;

import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import iteration1.BaseTest;
import models.TransferRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import requests.AccountRequester;
import requests.TransferRequester;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class TransferTest extends BaseTest {

    static final double EPSILON = 0.001;

    static AccountRequester accountRequester1;
    static AccountRequester accountRequester2;
    static TransferRequester transferRequester1;

    @BeforeEach
    void setUp() {
        RequestSpecification requestSpec1 = RequestSpecs.authAsUser("kate1999", "verysTRongPassword44$");
        ResponseSpecification responseSpec1 = ResponseSpecs.requestReturnsOK();
        accountRequester1 = new AccountRequester(requestSpec1, responseSpec1);
        transferRequester1 = new TransferRequester(requestSpec1, responseSpec1);

        RequestSpecification requestSpec2 = RequestSpecs.authAsUser("kate1991", "verysTRongPassword44$");
        ResponseSpecification responseSpec2 = ResponseSpecs.requestReturnsOK();
        accountRequester2 = new AccountRequester(requestSpec2, responseSpec2);
    }

    private void checkTransferAndBalance(int senderAccountId, int receiverAccountId, double depositAmount, String errorValue) {
        // Получаем начальные балансы отправителя и получателя
        double senderInitialBalance = accountRequester1.getAccountBalanceById(senderAccountId);
        double receiverInitialBalance = accountRequester2.getAccountBalanceById(receiverAccountId);

        // Проверяем, что сумма перевода не превышает баланс отправителя
        if (depositAmount > senderInitialBalance) {
            throw new IllegalArgumentException("Сумма перевода превышает баланс отправителя. Текущий баланс: " + senderInitialBalance);
        }

        // Осуществляем перевод
        TransferRequest transferRequest = TransferRequest.builder()
                .senderAccountId(senderAccountId)
                .receiverAccountId(receiverAccountId)
                .amount(depositAmount)
                .build();

        ResponseSpecification responseSpec = ResponseSpecs.requestReturnsOK();

        new TransferRequester(RequestSpecs.authAsUser("kate1999", "verysTRongPassword44$"), responseSpec)
                .post(transferRequest);

        // Получаем обновлённые балансы после перевода
        double senderUpdatedBalance = accountRequester1.getAccountBalanceById(senderAccountId);
        double receiverUpdatedBalance = accountRequester2.getAccountBalanceById(receiverAccountId);

        // Проверяем корректность балансов после перевода
        assertThat(Math.abs(senderUpdatedBalance - (senderInitialBalance - depositAmount)) < EPSILON, is(true));
        assertThat(Math.abs(receiverUpdatedBalance - (receiverInitialBalance + depositAmount)) < EPSILON, is(true));
    }

    @ParameterizedTest
    @CsvSource({
            "1, 3, 1",
            "1, 3, 0.01",
            "1, 3, 9999.99",
            "1, 3, 10000",
            "1, 3, 1000",
    })
    public void testPositiveTransferCases(int senderAccountId, int receiverAccountId, Double amount){
        checkTransferAndBalance(senderAccountId, receiverAccountId, amount, null);
    }
}