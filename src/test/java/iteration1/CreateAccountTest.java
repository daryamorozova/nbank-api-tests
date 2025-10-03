package iteration1;

import generators.RandomData;
import io.restassured.response.ValidatableResponse;
import models.CreateAccountResponse;
import models.CreateUserRequest;
import models.GetUserAccounts;
import models.UserRole;
import org.junit.jupiter.api.Test;
import requests.AdminCreateUserRequester;
import requests.CreateAccountRequester;
import requests.UserGetAccountsRequester;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class CreateAccountTest extends BaseTest {

    @Test
    public void userCanCreateAccountTest() {
        // Создаем пользователя
        CreateUserRequest userRequest = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(userRequest);

        // Создаем аккаунт и извлекаем ответ
        ValidatableResponse response = new CreateAccountRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.entityWasCreated())
                .post(null);

        CreateAccountResponse createAccountResponse = response.extract().as(CreateAccountResponse.class);
        String createdAccountNumber = createAccountResponse.getAccountNumber();

        // Получаем список аккаунтов пользователя
        UserGetAccountsRequester userGetAccountsRequester = new UserGetAccountsRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.requestReturnsOK());
        List<CreateAccountResponse> userAccounts = userGetAccountsRequester.getAccounts();

        // Проверяем, что созданный аккаунт присутствует в списке
        boolean accountExists = userAccounts.stream()
                .anyMatch(account -> account.getAccountNumber().equals(createdAccountNumber));

        assertThat("Созданный аккаунт не найден в списке аккаунтов пользователя", accountExists, is(true));
    }
}