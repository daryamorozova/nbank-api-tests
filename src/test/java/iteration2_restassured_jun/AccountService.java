package iteration2_restassured_jun;

import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;

public class AccountService {
    public Map<String, Object> getAccountById(int id, String authToken) {
        Response response = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", authToken) // Используем переданный токен
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .extract().response();

        JsonPath jsonPath = response.jsonPath();
        List<Map<String, Object>> accounts = jsonPath.getList("$.");

        return accounts.stream()
                .filter(acc -> (Integer) acc.get("id") == id)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Аккаунт не найден"));
    }
}
