package api.requests.skelethon.requesters;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import api.models.BaseModel;
import api.requests.skelethon.Endpoint;
import api.requests.skelethon.HttpRequest;
import api.requests.skelethon.interfaces.CrudEndpointInterface;

import static io.restassured.RestAssured.given;

public class CrudRequester extends HttpRequest implements CrudEndpointInterface  {
    public CrudRequester(RequestSpecification requestSpecification, Endpoint endpoint, ResponseSpecification responseSpecification) {
        super(requestSpecification, endpoint, responseSpecification);
    }

    @Override
    public ValidatableResponse post(BaseModel model) {
        var body = model == null ? "" : model;
        return  given()
                .spec(requestSpecification)
                .body(body)
                .post(endpoint.getEndpoint())
                .then()
                .assertThat()
                .spec(responseSpecification);
    }

    @Override
    public ValidatableResponse get(long id) {
        return given()
                .spec(requestSpecification)
                .get(endpoint.getEndpoint())
                .then()
                .assertThat()
                .spec(responseSpecification);
    }

    @Override
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
    public ValidatableResponse delete(long id) {
        return given()
                .spec(requestSpecification)
                .pathParam("id", id)
                .delete(endpoint.getEndpoint())
                .then()
                .assertThat()
                .spec(responseSpecification);
    }
}