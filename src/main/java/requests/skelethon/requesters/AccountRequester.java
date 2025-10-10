package requests.skelethon.requesters;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import requests.skelethon.Endpoint;
import requests.skelethon.HttpRequest;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;

public class AccountRequester extends HttpRequest {

    public AccountRequester(RequestSpecification requestSpecification, Endpoint endpoint, ResponseSpecification responseSpecification) {
        super(requestSpecification, endpoint, responseSpecification);
    }

    public double getAccountBalanceById(long accountId) {
        ValidatableResponse response = given()
                .spec(requestSpecification)
                .get(Endpoint.GET_ACCOUNTS.getEndpoint())
                .then()
                .assertThat()
                .spec(responseSpecification);

        // Извлечение баланса из ответа
        List<Map<String, Object>> accounts = response.extract().jsonPath().getList("");
        for (Map<String, Object> account : accounts) {
            if (accountId == ((Number) account.get("id")).longValue()) {
                return ((Number) account.get("balance")).doubleValue();
            }
        }
        throw new IllegalStateException("Account with ID " + accountId + " not found.");
    }
}
