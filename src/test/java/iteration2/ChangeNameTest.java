package iteration2;

import io.restassured.specification.ResponseSpecification;
import models.CreateUserRequest;
import models.UpdateProfileRequest;
import models.UpdateProfileResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
    @ValueSource(strings = {
            "John Michael Doe"
//            , "John", "", "John  Doe",
//            "John1 Doe2", "John! @Doe", "1John Doe", "John-Doe",
//            "John Иванов",
//            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa " +
//                    "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
    })
    public void testNegativeChangeName(String invalidName) {
        UpdateProfileRequest updateProfileRequest = UpdateProfileRequest.builder().name(invalidName).build();

        // Используем спецификацию ответа для ошибки Bad Request с текстовым сообщением
        ResponseSpecification badRequestSpec = ResponseSpecs.requestReturnsBadRequestWithoutKey("Name must contain two words with letters only");

        UpdateProfileResponse response = new ValidatedCrudRequester<UpdateProfileResponse>(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.UPDATE_PROFILE,
                badRequestSpec)
                .put(updateProfileRequest);
    }
}