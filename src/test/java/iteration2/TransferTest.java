package iteration2;

import generators.RandomData;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import iteration1.BaseTest;
import models.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import requests.*;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

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

        // Pre-fund sender with multiple deposits (limit is 5000 per deposit)
        new DepositRequester(requestSpec1, ResponseSpecs.requestReturnsOK())
                .post(new DepositRequest(senderAccountId, 5000.00));
        new DepositRequester(requestSpec1, ResponseSpecs.requestReturnsOK())
                .post(new DepositRequest(senderAccountId, 5000.00));

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
        double senderInitialBalance = accountRequester1.getAccountBalanceById(senderAccountId);
        double receiverInitialBalance = accountRequester2.getAccountBalanceById(receiverAccountId);

        TransferRequest transferRequest = TransferRequest.builder()
                .senderAccountId(senderAccountId)
                .receiverAccountId(receiverAccountId)
                .amount(amount)
                .build();

        // Expect 200 OK on successful transfer
        transferRequester1.post(transferRequest);

        double senderUpdatedBalance = accountRequester1.getAccountBalanceById(senderAccountId);
        double receiverUpdatedBalance = accountRequester2.getAccountBalanceById(receiverAccountId);

        assertThat(Math.abs(senderUpdatedBalance - (senderInitialBalance - amount)) < EPSILON, is(true));
        assertThat(Math.abs(receiverUpdatedBalance - (receiverInitialBalance + amount)) < EPSILON, is(true));
    }

    @ParameterizedTest
    @CsvSource({
            "0.00, Transfer amount must be at least 0.01",
            "-500.00, Transfer amount must be at least 0.01",
            "10001.00, Transfer amount cannot exceed 10000"
    })
    public void testNegativeTransferCases(Double amount, String expectedErrorValue) {
        double senderInitialBalance = accountRequester1.getAccountBalanceById(senderAccountId);
        double receiverInitialBalance = accountRequester2.getAccountBalanceById(receiverAccountId);

        TransferRequest transferRequest = TransferRequest.builder()
                .senderAccountId(senderAccountId)
                .receiverAccountId(receiverAccountId)
                .amount(amount)
                .build();

        // Expect 400 with specific error message
        new TransferRequester(requestSpec1, ResponseSpecs.requestReturnsBadRequestWithoutKey(expectedErrorValue))
                .post(transferRequest);

        double senderUpdatedBalance = accountRequester1.getAccountBalanceById(senderAccountId);
        double receiverUpdatedBalance = accountRequester2.getAccountBalanceById(receiverAccountId);

        assertThat(Math.abs(senderUpdatedBalance - senderInitialBalance) < EPSILON, is(true));
        assertThat(Math.abs(receiverUpdatedBalance - receiverInitialBalance) < EPSILON, is(true));
    }
}