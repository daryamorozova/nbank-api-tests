package requests;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.BaseModel;
import models.CreateAccountResponse;
import models.GetUserAccounts;

import java.util.List;

import static io.restassured.RestAssured.given;

public class UserGetAccountsRequester {

    private RequestSpecification requestSpecification;
    private ResponseSpecification responseSpecification;

    public UserGetAccountsRequester(RequestSpecification requestSpecification, ResponseSpecification responseSpecification) {
        this.requestSpecification = requestSpecification;
        this.responseSpecification = responseSpecification;
    }

    public List<CreateAccountResponse> getAccounts() {
        return given()
                .spec(requestSpecification)
                .get("/api/v1/customer/accounts")
                .then()
                .assertThat()
                .spec(responseSpecification)
                .extract()
                .body()
                .jsonPath().getList(".", CreateAccountResponse.class);
    }
}
