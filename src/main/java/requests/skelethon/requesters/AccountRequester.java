package requests.skelethon.requesters;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;

public class AccountRequester {
    private RequestSpecification requestSpecification;
    private ResponseSpecification responseSpecification;

    public AccountRequester(RequestSpecification requestSpecification, ResponseSpecification responseSpecification) {
        this.requestSpecification = requestSpecification;
        this.responseSpecification = responseSpecification;
    }

    public double getAccountBalanceById(int accountId) {
        ValidatableResponse response = given()
                .spec(requestSpecification)
                .get("/api/v1/customer/accounts")
                .then()
                .assertThat()
                .spec(responseSpecification);

        // Извлечение баланса из ответа
        List<Map<String, Object>> accounts = response.extract().jsonPath().getList("");
        for (Map<String, Object> account : accounts) {
            if (accountId == ((Number) account.get("id")).intValue()) {
                return ((Number) account.get("balance")).doubleValue();
            }
        }
        throw new IllegalStateException("Account with ID " + accountId + " not found.");
    }
}
