package iteration2;

import models.CreateUserRequest;
import models.GetProfileResponse;
import models.UpdateProfileRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import requests.skelethon.Endpoint;
import requests.skelethon.requesters.CrudRequester;
import requests.skelethon.requesters.ValidatedCrudRequester;
import requests.steps.AdminSteps;
import specs.RequestSpecs;
import specs.ResponseSpecs;


public class ChangeNameTest {
    private CrudRequester crudRequester;
    private ValidatedCrudRequester validatedCrudRequester;
    private CreateUserRequest userRequest;

    public String getCurrentUserName(String username, String password) {
        GetProfileResponse response = crudRequester
                .get();
        return response.getUsername();
    }

    @BeforeEach
    void setUp() {
        // Создание пользователя
        userRequest = AdminSteps.createUser();
    }

    @ParameterizedTest
    @ValueSource(strings = {"John Doe", "Иван Иванов"})
    public void testPositiveChangeName(String newName) {

        UpdateProfileRequest updateProfileRequest = UpdateProfileRequest.builder().name(newName).build();

        new ValidatedCrudRequester(RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.UPDATE_PROFILE,
                ResponseSpecs.requestReturnsOK())
                .put(updateProfileRequest);


    }


}
