package api.requests.skelethon;

import api.models.*;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum Endpoint {
    ADMIN_USER(
            "/admin/users",
            CreateUserRequest.class,
            CreateUserResponse.class
    ),

    LOGIN(
            "/auth/login",
            LoginUserRequest.class,
            LoginUserResponse.class
    ),

    ACCOUNTS(
            "/accounts",
            BaseModel.class,
            CreateAccountResponse.class
    ),

    DEPOSIT(
            "/accounts/deposit",
            DepositRequest.class,
            DepositResponse.class
    ),

    TRANSFER(
            "/accounts/transfer",
            TransferRequest.class,
            TransferResponse.class
    ),

    UPDATE_PROFILE(
            "/customer/profile",
            UpdateProfileRequest.class,
            UpdateProfileResponse.class
    ),

    GET_PROFILE(
            "/customer/profile",
            BaseModel.class,
            GetProfileResponse.class
    ),

    GET_ACCOUNTS(
            "/customer/accounts",
            BaseModel.class,
            GetUserAccounts.class
    ),

    DELETE_USER(
            "/admin/users/{id}",
            DeleteUserRequest.class,
            DeleteUserResponse.class
    );

    private final String endpoint;
    private Class<? extends BaseModel> requestModel;
    private Class<? extends BaseModel> responseModel;
}