package iteration2;

import generators.RandomData;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import iteration1.BaseTest;
import models.*;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import requests.AccountRequester;
import requests.AdminCreateUserRequester;
import requests.CreateAccountRequester;
import requests.DepositRequester;
import specs.RequestSpecs;
import specs.ResponseSpecs;
import utils.ParseDepositAmount;

import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class DepositTest extends BaseTest {

    static final double EPSILON = 0.001;
    private RequestSpecification requestSpec;
    AccountRequester accountRequester;
    DepositRequester depositRequester;
    private String username;
    private String password;
    private long accountId;

    @BeforeEach
    void setUp() {
        CreateUserRequest createUserRequest = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        CreateUserResponse createUserResponse = new AdminCreateUserRequester(RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(createUserRequest)
                .extract()
                .as(CreateUserResponse.class);

        username = createUserRequest.getUsername();
        password = createUserRequest.getPassword();
        requestSpec = RequestSpecs.authAsUser(username, password);

        ResponseSpecification okSpec = ResponseSpecs.requestReturnsOK();
        accountRequester = new AccountRequester(requestSpec, okSpec);
        // Deposits create a new deposit transaction => expect 201
        depositRequester = new DepositRequester(requestSpec, ResponseSpecs.entityWasCreated());

        CreateAccountResponse createAccountResponse = new CreateAccountRequester(requestSpec, ResponseSpecs.entityWasCreated())
                .post(null)
                .extract()
                .as(CreateAccountResponse.class);
        accountId = createAccountResponse.getId(); // Сохраняем ID созданного аккаунта
    }

    @ParameterizedTest
    @CsvSource({
            "0.01, true"
//            "1, true",
//            "4999.99, true",
//            "5000, true"
    })
    public void testPositiveDepositCases(double depositAmount, boolean expectedSuccess) {
        // Получаем начальный баланс аккаунта
        double initialBalance = accountRequester.getAccountBalanceById(accountId);

        // Создание запроса на депозит
        DepositRequest depositRequest = new DepositRequest(accountId, depositAmount);

        // Выполнение запроса и проверка результата
        ValidatableResponse response = depositRequester.post(depositRequest);

        // Валидация ответа
        response.assertThat().statusCode(HttpStatus.SC_CREATED);

        // Получаем обновленный баланс аккаунта
        double updatedBalance = accountRequester.getAccountBalanceById(accountId);

        // Рассчитываем ожидаемый баланс
        double expectedBalance = initialBalance + depositAmount;

        // Проверяем, что обновленный баланс соответствует ожидаемому
        assertThat(Math.abs(updatedBalance - expectedBalance) < EPSILON, is(true));
    }

    private void checkDepositAndBalance(long accountId, double depositAmount, String errorValue, int expectedStatusCode) {
        double initialBalance;
        try {
            initialBalance = accountRequester.getAccountBalanceById(accountId);
        } catch (IllegalStateException ex) {
            // Аккаунт недоступен/не существует для текущего пользователя — проверяем только код ответа
            initialBalance = Double.NaN;
        }

        DepositRequest depositRequest = DepositRequest.builder()
                .id(accountId)
                .balance(depositAmount)
                .build();

        ResponseSpecification errorSpec = ResponseSpecs.requestReturnsUnauthorized(errorValue);
        // Для негативных сценариев используем спецификацию BAD_REQUEST
        ValidatableResponse response = new DepositRequester(requestSpec, errorSpec).post(depositRequest);
        response.assertThat().statusCode(expectedStatusCode);

        double updatedBalance = accountRequester.getAccountBalanceById(accountId);
        assertThat(updatedBalance, is(initialBalance));
    }

    public static Stream<Arguments> depositInvalidData() {
        return Stream.of(
                Arguments.of("0.00", "Deposit amount must be at least 0.01"),
                Arguments.of("-500.00", "Deposit amount must be at least 0.01"),
                Arguments.of("5001.00", "Deposit amount cannot exceed 5000")
        );
    }

    @MethodSource("depositInvalidData")
    @ParameterizedTest
    public void testNegativeDepositCases(double depositAmount, String errorValue) {
        checkDepositAndBalance(accountId, depositAmount, errorValue, HttpStatus.SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void testDepositWithInvalidValues(String depositAmount) {
        double initialBalance = accountRequester.getAccountBalanceById(accountId);
        Double parsedAmount = new ParseDepositAmount().parseDepositAmount(depositAmount);

        DepositRequest depositRequest = new DepositRequest(accountId, parsedAmount);
        ValidatableResponse response = new DepositRequester(requestSpec, ResponseSpecs.requestReturnsBadRequestWithoutKeyWithOutValue())
                .post(depositRequest);
        response.assertThat().statusCode(HttpStatus.SC_BAD_REQUEST);

        double updatedBalance = accountRequester.getAccountBalanceById(accountId);
        assertThat(updatedBalance, is(initialBalance));
    }

    public static Stream<Arguments> depositUnAuthData() {
        return Stream.of(
                Arguments.of("3", "500.00", "Unauthorized access to account"),
                Arguments.of("10", "500.00", "Unauthorized access to account")
        );
    }

    @MethodSource("depositUnAuthData")
    @ParameterizedTest
    public void testDepositToNonExistentOrUnauthorizedAccount(long targetAccountId, double depositAmount, String errorValue) {
        DepositRequest depositRequest = new DepositRequest(targetAccountId, depositAmount);
        ValidatableResponse response = new DepositRequester(requestSpec, ResponseSpecs.requestReturnsUnauthorized(errorValue))
                .post(depositRequest);
        response.assertThat().statusCode(HttpStatus.SC_BAD_REQUEST);
    }
}