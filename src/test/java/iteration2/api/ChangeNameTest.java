package iteration2.api;

import api.models.*;
import iteration1.api.BaseTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import api.requests.skelethon.Endpoint;
import api.requests.skelethon.requesters.CrudRequester;
import api.requests.skelethon.requesters.ValidatedCrudRequester;
import api.requests.steps.AdminSteps;
import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ChangeNameTest extends BaseTest {

    private CreateUserRequest userRequest;
    private static final List<String> createdUsernames = new ArrayList<>();

    @BeforeEach
    void setUp() {
        // Создание пользователя
        userRequest = AdminSteps.createUser();
        // Копим username, чтобы потом удалить именно тех, кого создали в тестах
        createdUsernames.add(userRequest.getUsername());
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
            , "John", "", "John  Doe",
            "John1 Doe2", "John! @Doe", "1John Doe", "John-Doe",
            "John Иванов",
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa " +
                    "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
    })
    public void testNegativeChangeName(String invalidName) {
        var authSpec = RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword());

        GetProfileResponse before = new ValidatedCrudRequester<GetProfileResponse>(
                authSpec,
                Endpoint.GET_PROFILE,
                ResponseSpecs.requestReturnsOK()
        ).get(0L);
        String beforeName = before.getName();

        String expected = "Name must contain two words with letters only";

        String error = new CrudRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.UPDATE_PROFILE,
                ResponseSpecs.requestReturnsBadRequestWithoutKey(expected)
        ).put(UpdateProfileRequest.builder().name(invalidName).build())
                .extract()
                .asString();

        assertEquals(expected, error);

        GetProfileResponse after = new ValidatedCrudRequester<GetProfileResponse>(
                authSpec,
                Endpoint.GET_PROFILE,
                ResponseSpecs.requestReturnsOK()
        ).get(0L);

        assertEquals(beforeName, after.getName(),
                "Имя пользователя должно остаться без изменений при некорректном запросе");
    }

    @AfterAll
    static void cleanUpUsers() {
        // 1) Получаем всех пользователей
        List<CreateUserResponse> allUsers = AdminSteps.getAllUsers();

        // 2) Удаляем только тех, кто был создан в ходе тестов (по username)
        allUsers.stream()
                .filter(u -> createdUsernames.contains(u.getUsername()))
                .forEach(u -> AdminSteps.deleteUserById(u.getId()));

// 3) Проверяем, что тестовых не осталось
        List<CreateUserResponse> afterCleanup = AdminSteps.getAllUsers();
        boolean anyTestUsersLeft = afterCleanup.stream()
                .anyMatch(u -> createdUsernames.contains(u.getUsername()));

        org.junit.jupiter.api.Assertions.assertFalse(
                anyTestUsersLeft,
                "После очистки не должно остаться ни одного тестового пользователя");
    }
}