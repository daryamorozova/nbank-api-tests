package iteration2_restassured_jun;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.Matchers.equalTo;

@ExtendWith(RestAssuredSetup.class)
public class ChangeNameTest {

    static AccountService accountService;

    @BeforeAll
    static void setUp() {
        GenerateUserTokens.setUpUser1Token();
        accountService = new AccountService();
        RestAssured.baseURI = "http://localhost:4111/api/v1/customer";
    }

    @ParameterizedTest
    @ValueSource(strings = {"John Doe", "Иван Иванов"})
    public void testPositiveChangeName(String newName) {
        // Update the name
        RestAssured
                .given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", GenerateUserTokens.authTokenUser1)
                .body("{\"name\": \"" + newName + "\"}")
                .put("/profile")
                .then()
                .statusCode(HttpStatus.SC_OK);

        // Verify the name is updated
        RestAssured
                .given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", GenerateUserTokens.authTokenUser1)
                .get("/profile")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("name", equalTo(newName));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "John Michael Doe", "John", "", "John  Doe",
            "John1 Doe2", "John! @Doe", "1John Doe", "John-Doe",
            "John Иванов",
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa " +
                    "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
    })
    public void testNegativeChangeName(String invalidName) {
        // Try to update the name with an invalid value
        RestAssured
                .given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", GenerateUserTokens.authTokenUser1)
                .body("{\"name\": \"" + invalidName + "\"}")
                .put("/profile")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST);

        RestAssured
                .given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", GenerateUserTokens.authTokenUser1)
                .get("/profile")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("name", equalTo(null));  // Assuming the default name was null
    }
}