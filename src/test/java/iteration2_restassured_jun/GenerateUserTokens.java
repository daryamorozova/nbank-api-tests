package iteration2_restassured_jun;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;

import static io.restassured.RestAssured.given;

public class GenerateUserTokens {
    public static String authTokenUser1; // статическая переменная для хранения токена
    public static String authTokenUser2; // статическая переменная для хранения токена

    public static void setUpUser1Token() {
        // Здесь вызывается метод или код для получения токена
         user1CanGenerateAuthTokenTest();
    }

    public static void setUpUser2Token() {
        // Здесь вызывается метод или код для получения токена
        user2CanGenerateAuthTokenTest();
    }

    public static void user1CanGenerateAuthTokenTest() {
        Response response = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                                {
                                    "username": "kate1999",
                                    "password": "verysTRongPassword44$"
                                }
                        """)
                .post("http://localhost:4111/api/v1/auth/login")
                .then()
                .header("Authorization", Matchers.notNullValue())
                .extract().response();

        authTokenUser1 = response.header("Authorization"); // извлечение токена из заголовка
    }

    public static void user2CanGenerateAuthTokenTest() {
        Response response = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                                {
                                    "username": "kate2000",
                                    "password": "verysTRongPassword55$"
                                }
                        """)
                .post("http://localhost:4111/api/v1/auth/login")
                .then()
                .header("Authorization", Matchers.notNullValue())
                .extract().response();

        authTokenUser2 = response.header("Authorization"); // извлечение токена из заголовка
    }
}
