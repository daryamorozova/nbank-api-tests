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
        long accountId = userAccounts.get(0).getId();

        // Получаем начальный баланс аккаунта
        double initialBalance = accountRequester.getAccountBalanceById(accountId);

        ValidatableResponse response = new CrudRequester(RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.DEPOSIT,
                ResponseSpecs.requestReturnsOK())
                .post(DepositRequest.builder().id(accountId).balance(depositAmount).build());

        // Валидация ответа
        response.assertThat().statusCode(HttpStatus.SC_OK);

        // Получаем обновленный баланс аккаунта
        double updatedBalance = accountRequester.getAccountBalanceById(accountId);

        // Рассчитываем ожидаемый баланс
        double expectedBalance = initialBalance + depositAmount;

        // Проверяем, что обновленный баланс соответствует ожидаемому
        assertThat(Math.abs(updatedBalance - expectedBalance) < EPSILON, is(true));
    }

    public static Stream<Arguments> depositInvalidData() {
        return Stream.of(
                Arguments.of(0.00, "Deposit amount must be at least 0.01"),
                Arguments.of(-500.00, "Deposit amount must be at least 0.01"),
                Arguments.of(5001.00, "Deposit amount cannot exceed 5000")
        );
    }

    @MethodSource("depositInvalidData")
    @ParameterizedTest
    public void testNegativeDepositCases(double depositAmount, String errorValue) {
        long accountId = userAccounts.get(0).getId();
        ResponseSpecification responseSpec = ResponseSpecs.requestReturnsBadRequestWithoutKey(errorValue);

        DepositRequest depositRequest = DepositRequest.builder()
                .id(accountId)
                .balance(depositAmount)
                .build();

        new CrudRequester(RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.DEPOSIT, responseSpec)
                .post(depositRequest);

        double initialBalance = accountRequester.getAccountBalanceById(accountId);
        double updatedBalance = accountRequester.getAccountBalanceById(accountId);
        assertThat(updatedBalance, is(initialBalance));
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void testDepositWithInvalidValues(String depositAmount) {

        long accountId = userAccounts.get(0).getId();
        // Получаем начальный баланс аккаунта
        double initialBalance = accountRequester.getAccountBalanceById(accountId);

        ValidatableResponse response = new CrudRequester(RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.DEPOSIT,
                ResponseSpecs.requestReturnsBadRequestWithoutKeyWithOutValue())
                .post(DepositRequest.builder().id(accountId).balance(null).build());

        response.assertThat().statusCode(HttpStatus.SC_BAD_REQUEST);

        double updatedBalance = accountRequester.getAccountBalanceById(accountId);
        assertThat(updatedBalance, is(initialBalance));
    }


    public static Stream<Arguments> depositUnAuthData() {
        return Stream.of(
                Arguments.of(500.00, "Unauthorized access to account"));
    }

    @MethodSource("depositUnAuthData")
    @ParameterizedTest
    public void testDepositToNonExistentOrUnauthorizedAccount(double depositAmount, String errorValue) {
        // Получаем список аккаунтов пользователя
        List<CreateAccountResponse> userAccounts = getUserAccounts(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword())
        );

// Получаем ID существующего аккаунта
        long ownId = userAccounts.get(0).getId();

        // Получаем начальный баланс существующего аккаунта
        double initialBalance = accountRequester.getAccountBalanceById(ownId);

        // Находим ID, который точно не принадлежит пользователю
        // Например, берём ID, который больше максимального ID в списке
        long maxAccountId = userAccounts.stream()
                .mapToLong(CreateAccountResponse::getId)
                .max()
                .orElse(0L);

        // Используем ID, который точно не принадлежит пользователю
        long nonExistentAccountId = maxAccountId + 1;

        ResponseSpecification responseSpec = ResponseSpecs.requestReturnsUnauthorized(errorValue);

        DepositRequest depositRequest = DepositRequest.builder()
                .id(nonExistentAccountId)
                .balance(depositAmount)
                .build();

        new CrudRequester(RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.DEPOSIT, responseSpec)
                .post(depositRequest);

        double updatedBalance = accountRequester.getAccountBalanceById(nonExistentAccountId);
        assertThat(updatedBalance, is(initialBalance));
    }
}