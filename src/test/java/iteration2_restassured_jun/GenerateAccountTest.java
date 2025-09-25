package iteration2_restassured_jun;

import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.hamcrest.Matchers.is;


import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;


/**
 * Добавлена аннотация @ExtendWith и указан класс RestAssuredSetup, чтобы использовать настройку RestAssured в тестовом классе
 * Теперь каждый раз, когда будет запускаться тестовый класс с аннотацией @ExtendWith(RestAssuredSetup.class),
 * перед выполнением тестовых методов будет вызываться метод beforeAll из класса RestAssuredSetup,
 * и RestAssured будет настроен с фильтрами логирования запросов и ответов.
 *
 * Предусловия:
 * Админ должен создать двух пользователей с двумя аккаунтами, для которых будут производиться операции депозита и трансфера
 */

@ExtendWith(RestAssuredSetup.class)

public class GenerateAccountTest {

    @BeforeAll
    static void setUp() {
        GenerateUserTokens.setUpUser1Token();
        GenerateUserTokens.setUpUser2Token();
    }

    @Test
    public void user1CanCreateAccount1Test() {
              given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", GenerateUserTokens.authTokenUser1)
                .post("http://localhost:4111/api/v1/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED);
    }

    @Test
    public void user1CanSeeCreatedAccountTest() {
        // Получаем список аккаунтов
        Response response = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", GenerateUserTokens.authTokenUser1)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .extract().response();

        // Парсим JSON-ответ
        JsonPath jsonPath = response.jsonPath();

        // Получаем список аккаунтов
        List<Map<String, Object>> accounts = jsonPath.getList("$.");

        // Проверяем, что среди аккаунтов есть созданный и его баланс равен 0
        boolean accountExistsWithZeroBalance = accounts.stream()
                .anyMatch(account -> {
                    // Проверяем, что баланс аккаунта равен 0
                    return (account.get("balance") != null) && ((Float) account.get("balance") == 0.0);
                });

        // Утверждаем, что аккаунт с нулевым балансом существует
        assertThat(accountExistsWithZeroBalance, is(true));
    }

    @Test
    public void user2CanCreateAccount1Test() {
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", GenerateUserTokens.authTokenUser2)
                .post("http://localhost:4111/api/v1/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED);
    }

    @Test
    public void user2CanSeeCreatedAccountTest() {
        // Получаем список аккаунтов
        Response response = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", GenerateUserTokens.authTokenUser2)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .extract().response();

        // Парсим JSON-ответ
        JsonPath jsonPath = response.jsonPath();

        // Получаем список аккаунтов
        List<Map<String, Object>> accounts = jsonPath.getList("$.");

        // Проверяем, что среди аккаунтов есть созданный и его баланс равен 0
        boolean accountExistsWithZeroBalance = accounts.stream()
                .anyMatch(account -> {
                    // Проверяем, что баланс аккаунта равен 0
                    return (account.get("balance") != null) && ((Float) account.get("balance") == 0.0);
                });

        // Утверждаем, что аккаунт с нулевым балансом существует
        assertThat(accountExistsWithZeroBalance, is(true));
    }
}