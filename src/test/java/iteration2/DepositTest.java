package iteration2;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import iteration1.BaseTest;
import models.DepositRequest;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import requests.AccountRequester;
import requests.DepositRequester;
import specs.RequestSpecs;
import specs.ResponseSpecs;
import utils.ParseDepositAmount;

import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class DepositTest extends BaseTest {

    static final double EPSILON = 0.001;

    static AccountRequester accountRequester;
    static DepositRequester depositRequester;

    @BeforeEach
    void setUp() {
        RequestSpecification requestSpec = RequestSpecs.authAsUser("kate1999", "verysTRongPassword44$");
        ResponseSpecification responseSpec = ResponseSpecs.requestReturnsOK();
        accountRequester = new AccountRequester(requestSpec, responseSpec);
        depositRequester = new DepositRequester(requestSpec, responseSpec);
    }

    @ParameterizedTest
    @CsvSource({
            "1, 0.01, true",
            "1, 1, true",
            "1, 4999.99, true",
            "1, 5000, true",
            "2, 5000, true"
    })
    public void testPositiveDepositCases(int accountId, double depositAmount, boolean expectedSuccess) {
        // Получаем начальный баланс аккаунта
        double initialBalance = accountRequester.getAccountBalanceById(accountId);

        // Создание запроса на депозит
        DepositRequest depositRequest = new DepositRequest(accountId, depositAmount);

        // Выполнение запроса и проверка результата
        ValidatableResponse response = depositRequester.post(depositRequest);

        // Валидация ответа
        response.assertThat().statusCode(HttpStatus.SC_OK);

        // Получаем обновленный баланс аккаунта
        double updatedBalance = accountRequester.getAccountBalanceById(accountId);

        // Рассчитываем ожидаемый баланс
        double expectedBalance = initialBalance + depositAmount;

        // Проверяем, что обновленный баланс соответствует ожидаемому
        assertThat(Math.abs(updatedBalance - expectedBalance) < EPSILON, is(true));

    }

    private void checkDepositAndBalance(int accountId, double depositAmount, String errorValue, int expectedStatusCode) {
        double initialBalance = accountRequester.getAccountBalanceById(accountId);
        DepositRequest depositRequest = DepositRequest.builder()
                .id(accountId)
                .balance(depositAmount)
                .build();

        ResponseSpecification responseSpec;
        if (expectedStatusCode == HttpStatus.SC_BAD_REQUEST) {
            responseSpec = ResponseSpecs.requestReturnsBadRequestWithoutKey(errorValue);
        } else {
            responseSpec = ResponseSpecs.requestReturnsUnauthorized(errorValue);
        }

        new DepositRequester(RequestSpecs.authAsUser("kate1999", "verysTRongPassword44$"), responseSpec)
                .post(depositRequest);

        double updatedBalance = accountRequester.getAccountBalanceById(accountId);
        assertThat(updatedBalance, is(initialBalance));
    }

    public static Stream<Arguments> depositInvalidData() {
        return Stream.of(
                Arguments.of("1", "0.00", "Deposit amount must be at least 0.01"),
                Arguments.of("1", "-500.00", "Deposit amount must be at least 0.01"),
                Arguments.of("1", "5001.00", "Deposit amount cannot exceed 5000")
        );
    }

    @MethodSource("depositInvalidData")
    @ParameterizedTest
    public void testNegativeDepositCases(int accountId, double depositAmount, String errorValue) {
        checkDepositAndBalance(accountId, depositAmount, errorValue, HttpStatus.SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void testDepositWithInvalidValues(String depositAmount) {
        double initialBalance = accountRequester.getAccountBalanceById(1);
        Double parsedAmount = new ParseDepositAmount().parseDepositAmount(depositAmount);

        DepositRequest depositRequest = new DepositRequest(1, parsedAmount);
        ValidatableResponse response = depositRequester.post(depositRequest);
        response.assertThat().statusCode(HttpStatus.SC_BAD_REQUEST);

        double updatedBalance = accountRequester.getAccountBalanceById(1);
        assertThat(updatedBalance, is(initialBalance));
    }

    public static Stream<Arguments> depositUnAuthData() {
        return Stream.of(
                Arguments.of("3", "500.00", "Unauthorized access to account"),
                Arguments.of("10", "500.00", "Unauthorized access to account")
        );
    }

    @MethodSource("depositInvalidData")
    @ParameterizedTest
    public void testDepositToNonExistentOrUnauthorizedAccount(int accountId, double depositAmount, String errorValue) {
        checkDepositAndBalance(accountId, depositAmount, errorValue, HttpStatus.SC_BAD_REQUEST);
    }
}