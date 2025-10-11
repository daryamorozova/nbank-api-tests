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

    private AccountRequester accountRequester;
    private ValidatedCrudRequester transferRequester;
    private List<CreateAccountResponse> userAccounts;
    private CreateUserRequest userRequest;

    @BeforeEach
    void setUp() {
        // Create user
        userRequest = AdminSteps.createUser();

        // Create two accounts for the user
        CrudRequester crudRequester = new CrudRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.ACCOUNTS,
                ResponseSpecs.entityWasCreated()
        );

        crudRequester.post(null); // First account
        crudRequester.post(null); // Second account

        // Retrieve the list of user accounts
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

        transferRequester = new ValidatedCrudRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.TRANSFER,
                ResponseSpecs.requestReturnsOK()
        );
    }

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

    private void checkTransferAndBalance(long senderAccountId, long receiverAccountId, double amount, String expectedErrorValue, int expectedStatusCode) {
        double senderInitialBalance = accountRequester.getAccountBalanceById(senderAccountId);
        double receiverInitialBalance = accountRequester.getAccountBalanceById(receiverAccountId);

        TransferRequest transferRequest = TransferRequest.builder()
                .senderAccountId(senderAccountId)
                .receiverAccountId(receiverAccountId)
                .amount(amount)
                .build();

        ResponseSpecification responseSpec = expectedStatusCode == HttpStatus.SC_OK
                ? ResponseSpecs.requestReturnsOK()
                : ResponseSpecs.requestReturnsBadRequestWithoutKey(expectedErrorValue);

        ValidatableResponse response = (ValidatableResponse) transferRequester.post(transferRequest);

        response.assertThat().statusCode(expectedStatusCode);

        if (expectedStatusCode == HttpStatus.SC_OK) {
            double senderUpdatedBalance = accountRequester.getAccountBalanceById(senderAccountId);
            double receiverUpdatedBalance = accountRequester.getAccountBalanceById(receiverAccountId);

            assertThat(Math.abs(senderUpdatedBalance - (senderInitialBalance - amount)) < EPSILON, is(true));
            assertThat(Math.abs(receiverUpdatedBalance - (receiverInitialBalance + amount)) < EPSILON, is(true));
        } else {
            double senderUpdatedBalance = accountRequester.getAccountBalanceById(senderAccountId);
            double receiverUpdatedBalance = accountRequester.getAccountBalanceById(receiverAccountId);

            assertThat(senderUpdatedBalance, is(senderInitialBalance));
            assertThat(receiverUpdatedBalance, is(receiverInitialBalance));
        }
    }

    @ParameterizedTest
    @CsvSource({
            "0.01, true",
            "1, true",
            "9999.99, true",
            "10000, true"
    })
    public void testPositiveTransferCases(double amount, boolean expectedSuccess) {
        long senderAccountId = userAccounts.get(0).getId();
        long receiverAccountId = userAccounts.get(1).getId();
        checkTransferAndBalance(senderAccountId, receiverAccountId, amount, null, HttpStatus.SC_OK);
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
        long senderAccountId = userAccounts.get(0).getId();
        long receiverAccountId = userAccounts.get(1).getId();
        checkTransferAndBalance(senderAccountId, receiverAccountId, amount, expectedErrorValue, expectedStatusCode);
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
                () -> checkTransferAndBalance(senderAccountId, receiverAccountId, amount, null, HttpStatus.SC_OK),
                "Expected checkTransferAndBalance() to throw, but it didn't"
        );

        assertThat(thrown.getMessage(), is(expectedMessage));
    }
}
