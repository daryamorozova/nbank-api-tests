package iteration2;

import generators.RandomData;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.CreateUserRequest;
import models.CreateUserResponse;
import models.UpdateProfileRequest;
import models.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import requests.AdminCreateUserRequester;
import requests.GetUserProfileRequester;
import requests.PutUserProfileRequester;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class ChangeNameTest {
    private RequestSpecification requestSpec;
    private ResponseSpecification responseSpecOK;
    private ResponseSpecification responseSpecBadRequest;
    private GetUserProfileRequester getUserProfileRequester;
    private String username;
    private String password;

    @BeforeEach
    void setUp() {
        CreateUserRequest createUserRequest = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        CreateUserResponse createUserResponse = new AdminCreateUserRequester(RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(createUserRequest)
                .extract()
                .as(CreateUserResponse.class);

        username = createUserRequest.getUsername();
        password = createUserRequest.getPassword();
        requestSpec = RequestSpecs.authAsUser(username, password);
        responseSpecOK = ResponseSpecs.requestReturnsOK();
        responseSpecBadRequest = ResponseSpecs.requestReturnsBadRequestWithoutKeyWithOutValue();

        // Инициализируем запросник для получения профиля
        getUserProfileRequester = new GetUserProfileRequester(requestSpec, responseSpecOK);
    }

    @ParameterizedTest
    @ValueSource(strings = {"John Doe", "Иван Иванов"})
    public void testPositiveChangeName(String newName) {
        // Создаем запросник с ожидаемым успешным ответом
        PutUserProfileRequester putUserProfileRequester = new PutUserProfileRequester(requestSpec, responseSpecOK);

        // Обновляем имя
        UpdateProfileRequest updateProfileRequest = UpdateProfileRequest.builder().name(newName).build();
        putUserProfileRequester.updateProfile(updateProfileRequest);

        // Проверяем, что имя обновлено
        String updatedName = getUserProfileRequester.getProfile().extract().body().jsonPath().getString("name");
        assertThat(updatedName, equalTo(newName));
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
        // Создаем запросник с ожидаемым ошибочным ответом
        PutUserProfileRequester putUserProfileRequester = new PutUserProfileRequester(requestSpec, responseSpecBadRequest);

        // Пробуем обновить имя на недопустимое значение
        UpdateProfileRequest updateProfileRequest = UpdateProfileRequest.builder().name(invalidName).build();
        putUserProfileRequester.updateProfile(updateProfileRequest);

        // Проверяем, что имя не изменилось
        String currentName = getUserProfileRequester.getProfile().extract().body().jsonPath().getString("name");
        assertThat(currentName, equalTo(null)); // Убедитесь, что это соответствует вашим ожиданиям
    }
}