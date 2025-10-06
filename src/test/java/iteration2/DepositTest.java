package iteration2;

import io.restassured.common.mapper.TypeRef;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import iteration1.BaseTest;
import models.CreateAccountResponse;
import models.CreateUserRequest;
import models.DepositRequest;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import requests.skelethon.Endpoint;
import requests.skelethon.requesters.AccountRequester;
import requests.skelethon.requesters.CrudRequester;
import requests.skelethon.requesters.ValidatedCrudRequester;
import requests.steps.AdminSteps;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.List;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;


public class DepositTest extends BaseTest {
    static final double EPSILON = 0.001;

    private AccountRequester accountRequester;
    private CrudRequester crudRequester;
    private ValidatedCrudRequester validatedCrudRequester;
    private CreateUserRequest userRequest;
    private List<CreateAccountResponse> userAccounts;

    private List<CreateAccountResponse> getUserAccounts(RequestSpecification requestSpec) {
        Endpoint endpoint = Endpoint.GET_ACCOUNTS;
        Response response = given()
                .spec(requestSpec)
                .get(endpoint.getEndpoint());

        return response.then()
                .extract()
                .as(new TypeRef<List<CreateAccountResponse>>() {
                });
    }

    @BeforeEach
    void setUp() {
        // Создание пользователя
        userRequest = AdminSteps.createUser();

        // Создание двух аккаунтов для пользователя
        crudRequester = new CrudRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.ACCOUNTS,
                ResponseSpecs.entityWasCreated()
        );

        crudRequester.post(null); // Первый аккаунт
        crudRequester.post(null); // Второй аккаунт

        // Получение списка аккаунтов пользователя
        userAccounts = getUserAccounts(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword())
        );

        if (userAccounts == null || userAccounts.size() < 2) {
            throw new IllegalStateException("Failed to create two accounts for the user.");
        }

        accountRequester = new AccountRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.GET_ACCOUNTS,
                ResponseSpecs.requestReturnsOK()
        );
    }

    @ParameterizedTest
    @CsvSource({
            "0.01, true",
            "1, true",
            "4999.99, true",
            "5000, true"
    })
    public void testPositiveDepositCases(double depositAmount, boolean expectedSuccess) {
        // Используем ID первого аккаунта
        long accountId = userAccounts.getFirst().getId();

        // Получаем начальный баланс аккаунта
        double initialBalance = accountRequester.getAccountBalanceById(accountId);

        ValidatableResponse response = new CrudRequester(RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.DEPOSIT,
                ResponseSpecs.requestReturnsOK())
                .post(DepositRequest.builder().id(accountId).balance(depositAmount).build());

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

    private void checkDepositAndBalance(long accountId, double depositAmount, String errorValue, int expectedStatusCode) {
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

        new CrudRequester(RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.DEPOSIT, responseSpec)
                .post(depositRequest);


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
        long accountId = userAccounts.getFirst().getId();
        checkDepositAndBalance(accountId, depositAmount, errorValue, HttpStatus.SC_BAD_REQUEST);
    }


    @ParameterizedTest
    @NullAndEmptySource
    public void testDepositWithInvalidValues(String depositAmount) {

        long accountId = userAccounts.getFirst().getId();
        // Получаем начальный баланс аккаунта
        double initialBalance = accountRequester.getAccountBalanceById(accountId);
        Double parsedAmount = parseDepositAmount(depositAmount);

        if (parsedAmount == null) {
            throw new IllegalArgumentException("Deposit amount is invalid");
        }

        ValidatableResponse response = new CrudRequester(RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.DEPOSIT,
                ResponseSpecs.requestReturnsOK())
                .post(DepositRequest.builder().id(accountId).balance(parsedAmount).build());

        response.assertThat().statusCode(HttpStatus.SC_BAD_REQUEST);

        double updatedBalance = accountRequester.getAccountBalanceById(accountId);
        assertThat(updatedBalance, is(initialBalance));
    }

    private Double parseDepositAmount(String depositAmount) {
        if (depositAmount == null || depositAmount.isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(depositAmount);
        } catch (NumberFormatException e) {
            return null;
        }
    }


    public static Stream<Arguments> depositUnAuthData() {
        return Stream.of(
                Arguments.of("500.00", "Unauthorized access to account"));
    }

    @MethodSource("depositUnAuthData")
    @ParameterizedTest
    public void testDepositToNonExistentOrUnauthorizedAccount(double depositAmount, String errorValue) {
        // Получаем список аккаунтов пользователя
        List<CreateAccountResponse> userAccounts = getUserAccounts(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword())
        );

        // Находим ID, который точно не принадлежит пользователю
        // Например, берём ID, который больше максимального ID в списке
        long maxAccountId = userAccounts.stream()
                .mapToLong(CreateAccountResponse::getId)
                .max()
                .orElse(0L);

        // Используем ID, который точно не принадлежит пользователю
        long nonExistentAccountId = maxAccountId + 1;

        // Проверяем депозит на несуществующий аккаунт
        checkDepositAndBalance(nonExistentAccountId, depositAmount, errorValue, HttpStatus.SC_BAD_REQUEST);
    }
}
