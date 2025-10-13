package iteration2;

import io.restassured.common.mapper.TypeRef;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import iteration1.BaseTest;
import models.CreateAccountResponse;
import models.CreateUserRequest;
import models.TransferRequest;
import models.TransferResponse;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
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
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TransferTest extends BaseTest {
    static final double EPSILON = 0.001;

    private AccountRequester accountRequester1;
    private AccountRequester accountRequester2;
    private ValidatedCrudRequester transferRequester;
    private CreateAccountResponse account1;
    private CreateAccountResponse account2;

    @BeforeEach
    void setUp() {
        // Создание первого пользователя и его аккаунта
        CreateUserRequest userRequest1 = AdminSteps.createUser();
        accountRequester1 = new AccountRequester(
                RequestSpecs.authAsUser(userRequest1.getUsername(), userRequest1.getPassword()),
                Endpoint.GET_ACCOUNTS,
                ResponseSpecs.requestReturnsOK()
        );
        account1 = createAccountForUser(userRequest1);

        // Создание второго пользователя и его аккаунта
        CreateUserRequest userRequest2 = AdminSteps.createUser();
        accountRequester2 = new AccountRequester(
                RequestSpecs.authAsUser(userRequest2.getUsername(), userRequest2.getPassword()),
                Endpoint.GET_ACCOUNTS,
                ResponseSpecs.requestReturnsOK()
        );
        account2 = createAccountForUser(userRequest2);

        // Настройка transferRequester
        transferRequester = new ValidatedCrudRequester(
                RequestSpecs.authAsUser(userRequest1.getUsername(), userRequest1.getPassword()),
                Endpoint.TRANSFER,
                ResponseSpecs.requestReturnsOK()
        );
    }

    private CreateAccountResponse createAccountForUser(CreateUserRequest userRequest) {
        CrudRequester crudRequester = new CrudRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.ACCOUNTS,
                ResponseSpecs.entityWasCreated()
        );
        crudRequester.post(null);

        return getUserAccounts(userRequest).get(0);
    }

    private List<CreateAccountResponse> getUserAccounts(CreateUserRequest userRequest) {
        RequestSpecification requestSpec = RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword());
        Endpoint endpoint = Endpoint.GET_ACCOUNTS;
        Response response = given().spec(requestSpec).get(endpoint.getEndpoint());
        return response.then().extract().as(new TypeRef<List<CreateAccountResponse>>() {});
    }

    @ParameterizedTest
    @CsvSource({
            "0.01, true",
            "1, true",
            "9999.99, true",
            "10000, true"
    })
    public void testPositiveTransferCases(double amount, boolean expectedSuccess) {
        double senderInitialBalance = accountRequester1.getAccountBalanceById(account1);
        double receiverInitialBalance = accountRequester2.getAccountBalanceById(account2);

        TransferRequest transferRequest = TransferRequest.builder()
                .senderAccountId(account1)
                .receiverAccountId(account2)
                .amount(amount)
                .build();

        ResponseSpecification responseSpec = expectedStatusCode

        ValidatableResponse response = (ValidatableResponse) transferRequester.post(transferRequest);

        response.assertThat().statusCode(expectedStatusCode);


            double senderUpdatedBalance = accountRequester1.getAccountBalanceById(account1);
            double receiverUpdatedBalance = accountRequester2.getAccountBalanceById(account2);

            assertThat(Math.abs(senderUpdatedBalance - (senderInitialBalance - amount)) < EPSILON, is(true));
            assertThat(Math.abs(receiverUpdatedBalance - (receiverInitialBalance + amount)) < EPSILON, is(true));

        }
    }

    public static Stream<Arguments> transferInvalidData() {
        return Stream.of(
                Arguments.of(0.00, "Transfer amount must be at least 0.01", HttpStatus.SC_BAD_REQUEST),
                Arguments.of(-500.00, "Transfer amount must be at least 0.01", HttpStatus.SC_BAD_REQUEST),
                Arguments.of(10001.00, "Transfer amount cannot exceed 10000", HttpStatus.SC_BAD_REQUEST),
                Arguments.of(10000.00, "Invalid transfer: insufficient funds or invalid accounts", HttpStatus.SC_BAD_REQUEST),
                Arguments.of(500.00, "Invalid transfer: insufficient funds or invalid accounts", HttpStatus.SC_BAD_REQUEST)
        );
    }

    @MethodSource("transferInvalidData")
    @ParameterizedTest
    public void testNegativeTransferCases(double amount, String expectedErrorValue, int expectedStatusCode) {

    }

    private static Stream<Arguments> transferWithNullAmountData() {
        return Stream.of(
                Arguments.of(null, "Transfer amount must not be null")
        );
    }

    @MethodSource("transferWithNullAmountData")
    @ParameterizedTest
    public void testTransferWithNullAmount(Double amount, String expectedMessage) {
        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                () -> (account1.getId(), account2.getId(), amount, null, HttpStatus.SC_OK),
                "Expected checkTransferAndBalance() to throw, but it didn't"
        );

        assertThat(thrown.getMessage(), is(expectedMessage));
    }
}