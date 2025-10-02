package iteration2;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import iteration1.BaseTest;
import models.DepositRequest;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import requests.AccountRequester;
import requests.DepositRequester;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class DepositTest extends BaseTest {

    static final double EPSILON = 0.001;

    static AccountRequester accountRequester;
    static DepositRequester depositRequester;

    @BeforeAll
    static void setUp() {
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
            "1, 5000, true"
//            "3, 5000, true"
    })
    public void testPositiveDepositCases(int accountId, double depositAmount, boolean expectedSuccess) {
        // Получаем начальный баланс аккаунта
        double initialBalance = accountRequester.getAccountBalanceById(accountId);

        // Создание запроса на депозит
        DepositRequest depositRequest = new DepositRequest(accountId, depositAmount);

        // Выполнение запроса и проверка результата
        ValidatableResponse response = depositRequester.post(depositRequest);

        // Валидация ответа
        response.assertThat().statusCode(expectedSuccess ? HttpStatus.SC_OK : HttpStatus.SC_BAD_REQUEST);

        if (expectedSuccess) {
            // Получаем обновленный баланс аккаунта
            double updatedBalance = accountRequester.getAccountBalanceById(accountId);

            // Рассчитываем ожидаемый баланс
            double expectedBalance = initialBalance + depositAmount;

            // Проверяем, что обновленный баланс соответствует ожидаемому
            assertThat(Math.abs(updatedBalance - expectedBalance) < EPSILON, is(true));
        }
    }
}