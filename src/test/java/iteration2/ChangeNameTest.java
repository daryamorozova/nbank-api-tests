package iteration2;

import models.CreateUserRequest;
import models.GetProfileResponse;
import models.UpdateProfileRequest;
import models.UpdateProfileResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.apache.http.HttpStatus;
import requests.skelethon.Endpoint;
import requests.skelethon.requesters.CrudRequester;
import requests.skelethon.requesters.ValidatedCrudRequester;
import requests.steps.AdminSteps;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class ChangeNameTest {
    private CrudRequester crudRequester;
    private ValidatedCrudRequester validatedCrudRequester;
    private CreateUserRequest userRequest;

    public String getCurrentUserName(long userId) {
        GetProfileResponse response = new ValidatedCrudRequester<GetProfileResponse>(RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()), Endpoint.GET_PROFILE, ResponseSpecs.requestReturnsOK()).get(0);
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

        UpdateProfileResponse response = new ValidatedCrudRequester<UpdateProfileResponse>(RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.UPDATE_PROFILE,
                ResponseSpecs.requestReturnsOK())
                .put(updateProfileRequest);

        // Проверяем, что имя в профиле обновилось
        assertEquals(newName, response.getCustomer().getName(), "Имя пользователя не обновилось корректно");
        assertEquals("Profile updated successfully", response.getMessage(), "Сообщение об обновлении профиля не совпадает");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    public void testNegativeChangeName(String invalidName) {
        UpdateProfileRequest updateProfileRequest = UpdateProfileRequest.builder().name(invalidName).build();

        new CrudRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.UPDATE_PROFILE,
                ResponseSpecs.requestReturnsBadRequestWithoutKeyWithOutValue()
        )
        .put(updateProfileRequest)
        .assertThat()
        .statusCode(HttpStatus.SC_BAD_REQUEST);
    }


}
