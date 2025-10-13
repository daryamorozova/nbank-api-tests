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
import models.TransferRequest;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import requests.skelethon.Endpoint;
import requests.skelethon.requesters.AccountRequester;
import requests.skelethon.requesters.CrudRequester;
import requests.steps.AdminSteps;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.List;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class TransferTest extends BaseTest {
    static final double EPSILON = 0.001;

    private List<CreateAccountResponse> userAccounts1;
    private List<CreateAccountResponse> userAccounts2;
    private AccountRequester accountRequester1;
    private AccountRequester accountRequester2;
    private CreateUserRequest userRequest1;
    private CreateUserRequest userRequest2;

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

    private List<CreateAccountResponse> createUserAndAccounts(CreateUserRequest userRequest) {
        CrudRequester crudRequester = new CrudRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.ACCOUNTS,
                ResponseSpecs.entityWasCreated()
        );
        crudRequester.post(null);

        return getUserAccounts(RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()));
    }

    private void makeDeposit(CreateUserRequest userRequest, long accountId, double amount) {
        CrudRequester depositRequester = new CrudRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.DEPOSIT,
                ResponseSpecs.requestReturnsOK()
        );
        depositRequester.post(DepositRequest.builder().id(accountId).balance(amount).build());
    }

    private CreateUserRequest createUser() {
        return AdminSteps.createUser();
    }

    @BeforeEach
    void setUp() {
        userRequest1 = createUser();
        userRequest2 = createUser();

        userAccounts1 = createUserAndAccounts(userRequest1);
        userAccounts2 = createUserAndAccounts(userRequest2);

        accountRequester1 = new AccountRequester(
                RequestSpecs.authAsUser(userRequest1.getUsername(), userRequest1.getPassword()),
                Endpoint.GET_ACCOUNTS,
                ResponseSpecs.requestReturnsOK()
        );

        accountRequester2 = new AccountRequester(
                RequestSpecs.authAsUser(userRequest2.getUsername(), userRequest2.getPassword()),
                Endpoint.GET_ACCOUNTS,
                ResponseSpecs.requestReturnsOK()
        );

        long accountId1 = userAccounts1.get(0).getId();
        makeDeposit(userRequest1, accountId1, 5000);
        makeDeposit(userRequest1, accountId1, 5000);
        makeDeposit(userRequest1, accountId1, 5000);

    }

    @CsvSource({
            "0.01",
            "1",
            "9999.99",
            "10000"
    })
    @ParameterizedTest
    public void testPositiveTransferCases(double amount) {
        // Используем ID первого аккаунта
        long accountId1 = userAccounts1.get(0).getId();
        // Используем ID второго аккаунта
        long accountId2 = userAccounts2.get(0).getId();

        // Получаем начальный баланс аккаунта1 и 2
        double initialBalance1 = accountRequester1.getAccountBalanceById(accountId1);
        double initialBalance2 = accountRequester2.getAccountBalanceById(accountId2);

        ValidatableResponse response = new CrudRequester(RequestSpecs.authAsUser(userRequest1.getUsername(), userRequest1.getPassword()),
                Endpoint.TRANSFER,
                ResponseSpecs.requestReturnsOK())
                .post(TransferRequest.builder()
                        .senderAccountId(accountId1)
                        .amount(amount)
                        .receiverAccountId(accountId2)
                        .build());


        // Валидация ответа
        response.assertThat().statusCode(HttpStatus.SC_OK);

        // Получаем обновленный баланс аккаунта
        double updatedBalance1 = accountRequester1.getAccountBalanceById(accountId1);
        double updatedBalance2 = accountRequester2.getAccountBalanceById(accountId2);

        // Рассчитываем ожидаемый баланс
        double expectedBalance1 = initialBalance1 - amount;
        double expectedBalance2 = initialBalance2 + amount;

        // Проверяем, что обновленный баланс соответствует ожидаемому
        assertThat(Math.abs(updatedBalance1 - expectedBalance1) < EPSILON, is(true));
        assertThat(Math.abs(updatedBalance2 - expectedBalance2) < EPSILON, is(true));
    }

    public static Stream<Arguments> transferInvalidData() {
        return Stream.of(
                Arguments.of(0.00, "Transfer amount must be at least 0.01", HttpStatus.SC_BAD_REQUEST),
                Arguments.of(-500.00, "Transfer amount must be at least 0.01", HttpStatus.SC_BAD_REQUEST),
                Arguments.of(10001.00, "Transfer amount cannot exceed 10000", HttpStatus.SC_BAD_REQUEST)
        );
    }

    @MethodSource("transferInvalidData")
    @ParameterizedTest
    public void testNegativeTransferCases(double amount, String expectedErrorValue) {
        // Используем ID первого аккаунта
        long accountId1 = userAccounts1.get(0).getId();
        // Используем ID второго аккаунта
        long accountId2 = userAccounts2.get(0).getId();

        ResponseSpecification responseSpec = ResponseSpecs.requestReturnsBadRequestWithoutKey(expectedErrorValue);

        // Получаем начальный баланс аккаунта1 и 2
        double initialBalance1 = accountRequester1.getAccountBalanceById(accountId1);
        double initialBalance2 = accountRequester2.getAccountBalanceById(accountId2);

        ValidatableResponse response = new CrudRequester(RequestSpecs.authAsUser(userRequest1.getUsername(), userRequest1.getPassword()),
                Endpoint.TRANSFER,
                responseSpec)
                .post(TransferRequest.builder()
                        .senderAccountId(accountId1)
                        .amount(amount)
                        .receiverAccountId(accountId2)
                        .build());

        // Валидация ответа
        response.assertThat().statusCode(HttpStatus.SC_BAD_REQUEST);

        // Получаем обновленный баланс аккаунта
        double updatedBalance1 = accountRequester1.getAccountBalanceById(accountId1);
        double updatedBalance2 = accountRequester2.getAccountBalanceById(accountId2);

        // Рассчитываем ожидаемый баланс, он не должен измениться
        double expectedBalance1 = initialBalance1;
        double expectedBalance2 = initialBalance2;

        // Проверяем, что обновленный баланс соответствует ожидаемому
        assertThat(Math.abs(updatedBalance1 - expectedBalance1) < EPSILON, is(true));
        assertThat(Math.abs(updatedBalance2 - expectedBalance2) < EPSILON, is(true));
    }

    private static Stream<Arguments> transferWithNullAmountData() {
        return Stream.of(
                Arguments.of((Double) null, "Transfer amount must not be null")
        );
    }

    @MethodSource("transferWithNullAmountData")
    @ParameterizedTest
    public void testTransferWithNullAmount(Double amount, String expectedMessage) {
        // Используем ID первого аккаунта
        long accountId1 = userAccounts1.get(0).getId();
        // Используем ID второго аккаунта
        long accountId2 = userAccounts2.get(0).getId();

        ResponseSpecification responseSpec = ResponseSpecs.requestReturnsBadRequestWithoutKey(expectedMessage);

        CrudRequester crudRequester = new CrudRequester(
                RequestSpecs.authAsUser(userRequest1.getUsername(), userRequest1.getPassword()),
                Endpoint.TRANSFER,
                responseSpec
        );

        try {
            crudRequester.post(TransferRequest.builder()
                    .senderAccountId(accountId1)
                    .amount(amount)
                    .receiverAccountId(accountId2)
                    .build());
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is(expectedMessage));
            return;
        }

        // Если исключение не было выброшено, то тест должен провалиться
        assertThat(false, is(true));
    }
}