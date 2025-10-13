package iteration2;

import generators.RandomData;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import iteration1.BaseTest;
import models.CreateAccountResponse;
import models.CreateUserRequest;
import models.DepositRequest;
import models.TransferRequest;
import models.UserRole;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import requests.AccountRequester;
import requests.AdminCreateUserRequester;
import requests.CreateAccountRequester;
import requests.DepositRequester;
import requests.TransferRequester;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

@TestInstance(Lifecycle.PER_CLASS)
public class TransferTest extends BaseTest {

    static final double EPSILON = 0.001;

    private AccountRequester accountRequester1;
    private AccountRequester accountRequester2;
    private TransferRequester transferRequester1;

    private RequestSpecification requestSpec1;
    private RequestSpecification requestSpec2;

    private long senderAccountId;
    private long receiverAccountId;

    @BeforeEach
    void setUp() {
        // Create two users dynamically
        var user1 = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        var user2 = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        new AdminCreateUserRequester(RequestSpecs.adminSpec(), ResponseSpecs.entityWasCreated())
                .post(user1);
        new AdminCreateUserRequester(RequestSpecs.adminSpec(), ResponseSpecs.entityWasCreated())
                .post(user2);

        requestSpec1 = RequestSpecs.authAsUser(user1.getUsername(), user1.getPassword());
        requestSpec2 = RequestSpecs.authAsUser(user2.getUsername(), user2.getPassword());

        ResponseSpecification okSpec = ResponseSpecs.requestReturnsOK();
        accountRequester1 = new AccountRequester(requestSpec1, okSpec);
        accountRequester2 = new AccountRequester(requestSpec2, okSpec);
        transferRequester1 = new TransferRequester(requestSpec1, okSpec);

        // Create accounts for both users
        CreateAccountResponse senderAccount = new CreateAccountRequester(requestSpec1, ResponseSpecs.entityWasCreated())
                .post(null)
                .extract()
                .as(CreateAccountResponse.class);
        CreateAccountResponse receiverAccount = new CreateAccountRequester(requestSpec2, ResponseSpecs.entityWasCreated())
                .post(null)
                .extract()
                .as(CreateAccountResponse.class);

        senderAccountId = senderAccount.getId();
        receiverAccountId = receiverAccount.getId();

        // Pre-fund sender with 10000 for positive cases
        new DepositRequester(requestSpec1, ResponseSpecs.entityWasCreated())
                .post(new DepositRequest(senderAccountId, 10000.00));
    }

    private void checkTransferAndBalance(long senderAccountId, long receiverAccountId, Double amount, String expectedErrorValue) {
        if (amount == null) {
            throw new IllegalArgumentException("Transfer amount must not be null");
        }

        double senderInitialBalance = accountRequester1.getAccountBalanceById(senderAccountId);
        double receiverInitialBalance = accountRequester2.getAccountBalanceById(receiverAccountId);

        TransferRequest transferRequest = TransferRequest.builder()
                .senderAccountId(senderAccountId)
                .receiverAccountId(receiverAccountId)
                .amount(amount)
                .build();

        ValidatableResponse response = transferRequester1.post(transferRequest);
        int statusCode = response.extract().statusCode();

        if (statusCode == HttpStatus.SC_OK) {
            response.assertThat().spec(ResponseSpecs.requestReturnsOK());
            double senderUpdatedBalance = accountRequester1.getAccountBalanceById(senderAccountId);
            double receiverUpdatedBalance = accountRequester2.getAccountBalanceById(receiverAccountId);
            assertThat(Math.abs(senderUpdatedBalance - (senderInitialBalance - amount)) < EPSILON, is(true));
            assertThat(Math.abs(receiverUpdatedBalance - (receiverInitialBalance + amount)) < EPSILON, is(true));
        } else if (statusCode == HttpStatus.SC_BAD_REQUEST) {
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
            "1",
            "0.01",
            "9999.99",
            "10000",
            "1000"
    })
    public void testPositiveTransferCases(Double amount) {
        checkTransferAndBalance(senderAccountId, receiverAccountId, amount, null);
    }

    @ParameterizedTest
    @CsvSource({
            "0.00, Transfer amount must be at least 0.01",
            "-500.00, Transfer amount must be at least 0.01",
            "10001.00, Transfer amount cannot exceed 10000"
    })
    public void testNegativeTransferCases(Double amount, String expectedErrorValue) {
        checkTransferAndBalance(senderAccountId, receiverAccountId, amount, expectedErrorValue);
    }

    @ParameterizedTest
    @CsvSource({
            ", Transfer amount must not be null"
    })
    public void testTransferWithNullAmount(String ignored, String expectedMessage) {
        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                () -> checkTransferAndBalance(senderAccountId, receiverAccountId, null, null),
                "Expected checkTransferAndBalance() to throw, but it didn't"
        );

        assertThat(thrown.getMessage(), is(expectedMessage));
    }
}