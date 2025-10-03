package iteration2;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import iteration1.BaseTest;
import models.TransferRequest;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import requests.AccountRequester;
import requests.TransferRequester;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TransferTest extends BaseTest {

    static final double EPSILON = 0.001;

    static AccountRequester accountRequester1;
    static AccountRequester accountRequester2;
    static TransferRequester transferRequester1;
    static TransferRequester transferRequester2;


    @BeforeEach
    void setUp() {
        RequestSpecification requestSpec1 = RequestSpecs.authAsUser("kate1999", "verysTRongPassword44$");
        ResponseSpecification responseSpec1 = ResponseSpecs.requestReturnsOK();
        ResponseSpecification responseSpec2 = ResponseSpecs.requestReturnsBadRequestWithoutKeyWithOutValue();

        accountRequester1 = new AccountRequester(requestSpec1, responseSpec1);
        transferRequester1 = new TransferRequester(requestSpec1, responseSpec1);


        RequestSpecification requestSpec2 = RequestSpecs.authAsUser("kate1991", "verysTRongPassword44$");

        accountRequester2 = new AccountRequester(requestSpec2, responseSpec1);
        transferRequester2 = new TransferRequester(requestSpec1, responseSpec2);
    }

    private void checkTransferAndBalance(int senderAccountId, int receiverAccountId, double depositAmount, String expectedErrorValue) {
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

        ValidatableResponse response = transferRequester1.post(transferRequest);

        // Получаем статус код и проверяем его
        int statusCode = response.extract().statusCode();

        if (statusCode == HttpStatus.SC_OK) {
            // Ожидаем успешный ответ
            response.assertThat().spec(ResponseSpecs.requestReturnsOK());

            // Получаем обновлённые балансы после перевода
            double senderUpdatedBalance = accountRequester1.getAccountBalanceById(senderAccountId);
            double receiverUpdatedBalance = accountRequester2.getAccountBalanceById(receiverAccountId);

            // Проверяем корректность балансов после перевода
            assertThat(Math.abs(senderUpdatedBalance - (senderInitialBalance - depositAmount)) < EPSILON, is(true));
            assertThat(Math.abs(receiverUpdatedBalance - (receiverInitialBalance + depositAmount)) < EPSILON, is(true));
        } else if (statusCode == HttpStatus.SC_BAD_REQUEST) {
            // Ожидаем ошибку
            if (expectedErrorValue != null) {
                response.assertThat().spec(ResponseSpecs.requestReturnsBadRequestWithoutKey(expectedErrorValue));
            } else {
                response.assertThat().spec(ResponseSpecs.requestReturnsBadRequestWithoutKeyWithOutValue());
            }
        } else {
            throw new AssertionError("Unexpected status code: " + statusCode);
        }

    }

    @ParameterizedTest
    @CsvSource({
            "1, 3, 1",
            "1, 3, 0.01",
            "1, 3, 9999.99",
            "1, 3, 10000",
            "1, 3, 1000",
    })
    public void testPositiveTransferCases(int senderAccountId, int receiverAccountId, Double amount) {
        checkTransferAndBalance(senderAccountId, receiverAccountId, amount, null);
    }

    public static Stream<Arguments> transferInvalidData() {
        return Stream.of(
                Arguments.of("1", "3", "0.00", "Transfer amount must be at least 0.01"),
                Arguments.of("1", "3", "-500.00", "Transfer amount must be at least 0.01"),
                Arguments.of("1", "3", "10001.00", "Transfer amount cannot exceed 10000"),
                Arguments.of("1", "3", "10000.00", "Invalid transfer: insufficient funds or invalid accounts"),
                Arguments.of("1", "10", "500", "Invalid transfer: insufficient funds or invalid accounts")
        );
    }

    @MethodSource("transferInvalidData")
    @ParameterizedTest
    public void testNegativeTransferCases(int senderAccountId, int receiverAccountId, Double amount, String expectedErrorValue) {
        checkTransferAndBalance(senderAccountId, receiverAccountId, amount, expectedErrorValue);
    }

    private static Stream<Arguments> transferWithNullAmountData() {
        return Stream.of(
                Arguments.of(1, 3, null, "Transfer amount must not be null")
        );
    }

    @MethodSource("transferWithNullAmountData")
    @ParameterizedTest
    public void testTransferWithNullAmount(Integer senderAccountId, Integer receiverAccountId, Double amount, String expectedMessage) {
        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                () -> checkTransferAndBalance(senderAccountId, receiverAccountId, amount, null),
                "Expected checkTransferAndBalance() to throw, but it didn't"
        );

        // Проверяем, что возникает ожидаемое исключение
        assertThat(thrown.getMessage(), is(expectedMessage));
    }
}