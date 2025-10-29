package api.requests.steps;

import api.generators.RandomModelGenerator;
import api.models.CreateUserRequest;
import api.models.CreateUserResponse;
import api.requests.skelethon.Endpoint;
import api.requests.skelethon.requesters.CrudRequester;
import api.requests.skelethon.requesters.ValidatedCrudRequester;
import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;

import java.util.List;

public class AdminSteps {
    public static CreateUserRequest createUser() {
        CreateUserRequest userRequest = RandomModelGenerator.generate(CreateUserRequest.class);

        new ValidatedCrudRequester<CreateUserResponse>(
                RequestSpecs.adminSpec(),
                Endpoint.ADMIN_USER,
                ResponseSpecs.entityWasCreated())
                .post(userRequest);

        return userRequest;
    }

    public static void deleteUserById(long id) {
        new CrudRequester(
                RequestSpecs.adminSpec(),
                Endpoint.DELETE_USER,
                ResponseSpecs.requestReturnsOK()
        ).delete(id);
    }

    public static List<CreateUserResponse> getAllUsers() {
        return new ValidatedCrudRequester<CreateUserResponse>(
                RequestSpecs.adminSpec(),
                Endpoint.ADMIN_USER,
                ResponseSpecs.requestReturnsOK()).getAll(CreateUserResponse[].class);
    }

//    public static List<CreateUserResponse> getAllUsers() {
//        return new CrudRequester(
//                RequestSpecs.adminSpec(),
//                Endpoint.ADMIN_USER,
//                ResponseSpecs.requestReturnsOK()
//        )
//                .get(0L) // id игнорируется для /admin/users
//                .extract()
//                .jsonPath()
//                .getList("", CreateUserResponse.class);
//    }
}