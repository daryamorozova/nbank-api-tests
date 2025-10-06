package iteration1;

import generators.RandomData;
import io.restassured.response.ValidatableResponse;
import models.CreateAccountResponse;
import models.CreateUserRequest;
import models.UserRole;
import org.junit.jupiter.api.Test;
import requests.AdminCreateUserRequester;
import requests.CreateAccountRequester;
import requests.UserGetAccountsRequester;
import requests.skelethon.Endpoint;
import requests.skelethon.requesters.CrudRequester;
import requests.skelethon.requesters.ValidatedCrudRequester;
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

        new CrudRequester(
                RequestSpecs.adminSpec(),
                Endpoint.ADMIN_USER,
                ResponseSpecs.entityWasCreated())
                .post(userRequest);

        // Создаем аккаунт и извлекаем ответ
        ValidatableResponse response = new CrudRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.ACCOUNTS,
                ResponseSpecs.entityWasCreated())
                .post(null);

        CreateAccountResponse createAccountResponse = response.extract().as(CreateAccountResponse.class);
        String createdAccountNumber = createAccountResponse.getAccountNumber();

        // Получаем список аккаунтов пользователя
        CrudRequester userGetAccountsRequester = new CrudRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.ACCOUNTS,
                ResponseSpecs.requestReturnsOK());
//        List<CreateAccountResponse> userAccounts = ;
//
//        // Проверяем, что созданный аккаунт присутствует в списке
//        boolean accountExists = userAccounts.stream()
//                .anyMatch(account -> account.getAccountNumber().equals(createdAccountNumber));
//
//        assertThat("Созданный аккаунт не найден в списке аккаунтов пользователя", accountExists, is(true));
    }
}