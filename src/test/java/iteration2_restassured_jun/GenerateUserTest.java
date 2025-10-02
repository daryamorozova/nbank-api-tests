package iteration2_restassured_jun;

import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Добавлена аннотация @ExtendWith и указан класс RestAssuredSetup, чтобы использовать настройку RestAssured в тестовом классе
 * Теперь каждый раз, когда будет запускаться тестовый класс с аннотацией @ExtendWith(RestAssuredSetup.class),
 * перед выполнением тестовых методов будет вызываться метод beforeAll из класса RestAssuredSetup,
 * и RestAssured будет настроен с фильтрами логирования запросов и ответов.
 * <p>
 * Предусловия:
 * Админ должен создать двух пользователей с двумя аккаунтами, для которых будут производиться операции депозита и трансфера
 */

@ExtendWith(RestAssuredSetup.class)

public class GenerateUserTest {
    public static String authTokenUser1; // статическая переменная для хранения токена
    public static String authTokenUser2; // статическая переменная для хранения токена


    @Test
    public void adminCanGenerateAuthTokenTest() {
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                                {
                                    "username": "admin",
                                    "password": "admin"
                                }
                        """)
                .post("http://localhost:4111/api/v1/auth/login")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .header("Authorization", "Basic YWRtaW46YWRtaW4=");
    }

    @Test
    public void adminCanCreateUser1Test() {
        // создание пользователя
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic YWRtaW46YWRtaW4=")
                .body("""
                                {
                                    "username": "kate1999",
                                    "password": "verysTRongPassword44$",
                                    "role": "USER"
                                }
                        """)
                .post("http://localhost:4111/api/v1/admin/users")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED);
        String response = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic YWRtaW46YWRtaW4=")
                .get("http://localhost:4111/api/v1/admin/users")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .asString();

        JsonPath jsonPath = new JsonPath(response);
        boolean userExists = jsonPath.getList("username").contains("kate1999");
        assertThat("User kate1999 should exist", userExists, is(true));
    }


    @Test
    public void adminCanCreateUser2Test() {
        // создание пользователя
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic YWRtaW46YWRtaW4=")
                .body("""
                                {
                                    "username": "kate2000",
                                    "password": "verysTRongPassword55$",
                                    "role": "USER"
                                }
                        """)
                .post("http://localhost:4111/api/v1/admin/users")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED);

        String response = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic YWRtaW46YWRtaW4=")
                .get("http://localhost:4111/api/v1/admin/users")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .asString();

        JsonPath jsonPath = new JsonPath(response);
        boolean userExists = jsonPath.getList("username").contains("kate2000");
        assertThat("User kate1999 should exist", userExists, is(true));
    }
}