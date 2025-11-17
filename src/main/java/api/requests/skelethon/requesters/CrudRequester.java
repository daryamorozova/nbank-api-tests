package api.requests.skelethon.requesters;

import api.requests.skelethon.interfaces.GetAllEndpointInterface;
import common.helpers.StepLogger;
import io.qameta.allure.Step;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import api.models.BaseModel;
import api.requests.skelethon.Endpoint;
import api.requests.skelethon.HttpRequest;
import api.requests.skelethon.interfaces.CrudEndpointInterface;

import static io.restassured.RestAssured.given;

public class CrudRequester extends HttpRequest implements CrudEndpointInterface, GetAllEndpointInterface {
    public CrudRequester(RequestSpecification requestSpecification, Endpoint endpoint, ResponseSpecification responseSpecification) {
        super(requestSpecification, endpoint, responseSpecification);
    }

    @Override
    @Step("POST запрос на {endpoint} с телом {model}")
    public ValidatableResponse post(BaseModel model) {
        return StepLogger.log("POST request to " + endpoint.getEndpoint(), () -> {
            var body = model == null ? "" : model;
            return given()
                    .spec(requestSpecification)
                    .body(body)
                    .post(endpoint.getEndpoint())
                    .then()
                    .assertThat()
                    .spec(responseSpecification);
        });
    }

    @Override
    @Step("GET запрос на {endpoint} с id {id}")
    public ValidatableResponse get(long id) {
        return given()
                .spec(requestSpecification)
                .get(endpoint.getEndpoint())
                .then()
                .assertThat()
                .spec(responseSpecification);
    }

    @Override
    @Step("PUT запрос на {endpoint} с телом {model}")
    public ValidatableResponse put(BaseModel model) {
        var body = model == null ? "" : model;

        return given()
                .spec(requestSpecification)
                .body(model)
                .put(endpoint.getEndpoint())
                .then()
                .assertThat()
                .spec(responseSpecification);
    }

    @Override
    @Step("DELETE запрос на {endpoint} с id {id}")
    public ValidatableResponse delete(long id) {
        return given()
                .spec(requestSpecification)
                .pathParam("id", id)
                .delete(endpoint.getEndpoint())
                .then()
                .assertThat()
                .spec(responseSpecification);
    }

    @Override
    @Step("GET запрос на {endpoint}")
    public ValidatableResponse getAll(Class<?> clazz) {
        return given()
                .spec(requestSpecification)
                .get(endpoint.getEndpoint())
                .then().assertThat()
                .spec(responseSpecification);
    }
}