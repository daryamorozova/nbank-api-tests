package api.requests.steps;

import api.models.CreateAccountResponse;
import api.models.GetProfileResponse;
import api.requests.skelethon.Endpoint;
import api.requests.skelethon.requesters.ValidatedCrudRequester;
import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;

import java.util.List;

public class UserSteps {
    private String username;
    private String password;

    public UserSteps(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public List<CreateAccountResponse> getAllAccounts() {
        return new ValidatedCrudRequester<CreateAccountResponse>(
                RequestSpecs.authAsUser(username, password),
                Endpoint.GET_ACCOUNTS,
                ResponseSpecs.requestReturnsOK()).getAll(CreateAccountResponse[].class);
    }

    public GetProfileResponse getProfile() {
        return new ValidatedCrudRequester<GetProfileResponse>(
                RequestSpecs.authAsUser(username, password),
                Endpoint.GET_PROFILE,
                ResponseSpecs.requestReturnsOK()
        ).getOne(GetProfileResponse.class); // 👈 одиночный объект, не массив
    }

    public CreateAccountResponse createAccount() {
        return new ValidatedCrudRequester<CreateAccountResponse>(
                RequestSpecs.authAsUser(username, password),
                Endpoint.ACCOUNTS,                    // должен быть POST
                ResponseSpecs.entityWasCreated()
        ).post(null);
    }
}