package iteration2_restassured_jun;

import com.google.gson.Gson;
import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@ExtendWith(RestAssuredSetup.class)
public class DepositTest {
    static final double EPSILON = 0.001;
    static AccountService accountService;

    @BeforeAll
    static void setUp() {
        GenerateUserTokens.setUpUser1Token();
        accountService = new AccountService();
    }

    @ParameterizedTest
    @CsvSource({
            "1, 0.01, true",
            "1, 1, true",
            "1, 4999.99, true",
            "1, 5000, true",
            "3, 5000, true"
    })
    public void testPositiveDepositCases(int accountId, double depositAmount, boolean expectedSuccess) {
        Map<String, Object> account = accountService.getAccountById(accountId, GenerateUserTokens.authTokenUser1);
        double initialBalance = (Float) account.get("balance");

        Map<String, Object> depositData = new HashMap<>();
        depositData.put("id", accountId);
        depositData.put("balance", depositAmount);

        String jsonBody = new Gson().toJson(depositData);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", GenerateUserTokens.authTokenUser1)
                .body(jsonBody)
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(expectedSuccess ? HttpStatus.SC_OK : HttpStatus.SC_BAD_REQUEST);

        if (expectedSuccess) {
            Map<String, Object> updatedAccount = accountService.getAccountById(accountId, GenerateUserTokens.authTokenUser1);
            Float updatedBalanceFloat = (Float) updatedAccount.get("balance");
            double updatedBalance = updatedBalanceFloat.doubleValue();
            double expectedBalance = initialBalance + depositAmount;
            assertThat(Math.abs(updatedBalance - expectedBalance) < EPSILON, is(true));
        }
    }

    @ParameterizedTest
    @CsvSource({
            "1, 0, false",
            "1, -500, false",
            "1, 5001, false"
    })
    public void testNegativeDepositCases(int accountId, double depositAmount, boolean expectedSuccess) {
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", GenerateUserTokens.authTokenUser1)
                .body(String.format("{ \"id\": %d, \"balance\": %.2f }", accountId, depositAmount))
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(expectedSuccess ? HttpStatus.SC_OK : HttpStatus.SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void testDepositWithInvalidValues(String depositAmount) {
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", GenerateUserTokens.authTokenUser1)
                .body(String.format("{ \"id\": %d, \"balance\": %s }", 1, depositAmount))
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(depositAmount == null ? HttpStatus.SC_INTERNAL_SERVER_ERROR : HttpStatus.SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @CsvSource({
            "2, 500, false",
            "10, 500, false",
    })
    public void testDepositToNonExistentOrUnauthorizedAccount(int accountId, double depositAmount, boolean expectedSuccess) {
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", GenerateUserTokens.authTokenUser1)
                .body(String.format("{ \"id\": %d, \"balance\": %.2f }", accountId, depositAmount))
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }
}