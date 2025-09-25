package iteration2_restassured_jun;

import com.google.gson.Gson;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@ExtendWith(RestAssuredSetup.class)
public class TransferTest {

    static final double EPSILON = 0.01;
    static AccountService accountService;

    @BeforeAll
    static void setUp() {
        GenerateUserTokens.setUpUser1Token();
        GenerateUserTokens.setUpUser2Token();
        accountService = new AccountService();
    }

    @ParameterizedTest
    @CsvSource({
            "1, 2, 1, true",
            "1, 2, 0.01, true",
            "1, 2, 9999.99, true",
            "1, 2, 1, true",
//            "1, 3, 1, true", вынести в отдельный тест
            "1, 2, 1000, true",
    })
    public void testPositiveTransferCases(int senderAccountId, int receiverAccountId, Double amount) {
        Map<String, Object> senderAccount = accountService.getAccountById(senderAccountId, GenerateUserTokens.authTokenUser1);
        double senderInitialBalance = (Float) senderAccount.get("balance");

        // Проверка, что баланс счёта больше суммы перевода
        assertThat(senderInitialBalance > amount, is(true));

        Map<String, Object> receiverAccount = accountService.getAccountById(receiverAccountId, GenerateUserTokens.authTokenUser2);
        double receiverInitialBalance = (Float) receiverAccount.get("balance");

        Map<String, Object> transferData = new HashMap<>();
        transferData.put("senderAccountId", senderAccountId);
        transferData.put("receiverAccountId", receiverAccountId);
        transferData.put("amount", amount);

        String jsonBody = new Gson().toJson(transferData);

        given()
                .contentType(JSON)
                .accept(JSON)
                .header("Authorization", GenerateUserTokens.authTokenUser1)
                .body(jsonBody)
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK);

        Map<String, Object> updatedSenderAccount = accountService.getAccountById(senderAccountId, GenerateUserTokens.authTokenUser1);
        Float updatedSenderBalanceFloat = (Float) updatedSenderAccount.get("balance");
        double updatedSenderBalance = updatedSenderBalanceFloat.doubleValue();
        double expectedSenderBalance = senderInitialBalance - amount;
        assertThat(Math.abs(updatedSenderBalance - expectedSenderBalance) < EPSILON, is(true));

        Map<String, Object> updatedReceiverAccount = accountService.getAccountById(receiverAccountId, GenerateUserTokens.authTokenUser2);
        Float updatedReceiverBalanceFloat = (Float) updatedReceiverAccount.get("balance");
        double updatedReceiverBalance = updatedReceiverBalanceFloat.doubleValue();
        double expectedReceiverBalance = receiverInitialBalance + amount;
        assertThat(Math.abs(updatedReceiverBalance - expectedReceiverBalance) < EPSILON, is(true));
    }

    @ParameterizedTest
    @CsvSource({
            "1, 2, 0, false",
            "1, 2, -500, false",
            "1, 2, 10001, false",
            "1, 2, 10000, false",
            "1, 10, 500, false"
    })
    public void testNegativeTransferCases(int senderAccountId, int receiverAccountId, Double amount) {
        Map<String, Object> transferData = new HashMap<>();
        transferData.put("senderAccountId", senderAccountId);
        transferData.put("receiverAccountId", receiverAccountId);
        transferData.put("amount", amount);

        String jsonBody = new Gson().toJson(transferData);

        given()
                .contentType(JSON)
                .accept(JSON)
                .header("Authorization", GenerateUserTokens.authTokenUser1)
                .body(jsonBody)
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    public void testNullAmountTransferCase() {
        Map<String, Object> transferData = new HashMap<>();
        transferData.put("senderAccountId", 1);
        transferData.put("receiverAccountId", 2);
        transferData.put("amount", null);

        String jsonBody = new Gson().toJson(transferData);

        given()
                .contentType(JSON)
                .accept(JSON)
                .header("Authorization", GenerateUserTokens.authTokenUser1)
                .body(jsonBody)
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
    }

    @ParameterizedTest
    @CsvSource({
            "1, 3, 1, true"
    })
    public void testPositiveTransferCasesFromOneUser(int senderAccountId, int receiverAccountId, Double amount) {
        Map<String, Object> senderAccount = accountService.getAccountById(senderAccountId, GenerateUserTokens.authTokenUser1);
        double senderInitialBalance = (Float) senderAccount.get("balance");

        // Проверка, что баланс счёта больше суммы перевода
        assertThat(senderInitialBalance > amount, is(true));

        Map<String, Object> receiverAccount = accountService.getAccountById(receiverAccountId, GenerateUserTokens.authTokenUser1);
        double receiverInitialBalance = (Float) receiverAccount.get("balance");

        Map<String, Object> transferData = new HashMap<>();
        transferData.put("senderAccountId", senderAccountId);
        transferData.put("receiverAccountId", receiverAccountId);
        transferData.put("amount", amount);

        String jsonBody = new Gson().toJson(transferData);

        given()
                .contentType(JSON)
                .accept(JSON)
                .header("Authorization", GenerateUserTokens.authTokenUser1)
                .body(jsonBody)
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK);

        Map<String, Object> updatedSenderAccount = accountService.getAccountById(senderAccountId, GenerateUserTokens.authTokenUser1);
        Float updatedSenderBalanceFloat = (Float) updatedSenderAccount.get("balance");
        double updatedSenderBalance = updatedSenderBalanceFloat.doubleValue();
        double expectedSenderBalance = senderInitialBalance - amount;
        assertThat(Math.abs(updatedSenderBalance - expectedSenderBalance) < EPSILON, is(true));

        Map<String, Object> updatedReceiverAccount = accountService.getAccountById(receiverAccountId, GenerateUserTokens.authTokenUser1);
        Float updatedReceiverBalanceFloat = (Float) updatedReceiverAccount.get("balance");
        double updatedReceiverBalance = updatedReceiverBalanceFloat.doubleValue();
        double expectedReceiverBalance = receiverInitialBalance + amount;
        assertThat(Math.abs(updatedReceiverBalance - expectedReceiverBalance) < EPSILON, is(true));
    }
}