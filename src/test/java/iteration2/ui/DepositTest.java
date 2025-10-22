package iteration2.ui;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.Selenide;
import api.models.CreateAccountResponse;
import api.models.CreateUserRequest;
import api.models.LoginUserRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Alert;
import api.requests.skelethon.Endpoint;
import api.requests.skelethon.requesters.CrudRequester;
import api.requests.steps.AdminSteps;
import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;

import java.util.Arrays;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.codeborne.selenide.Selenide.*;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

public class DepositTest {
    @BeforeAll
    public static void setupSelenoid() {
        Configuration.remote = "http://localhost:4444/wd/hub";
        Configuration.baseUrl = "http://192.168.0.107:3000";
        Configuration.browser = "chrome";
        Configuration.browserSize = "1920x1080";

        Configuration.browserCapabilities.setCapability("selenoid:options",
                Map.of("enableVNC", true, "enableLog", true)
        );
    }

    @Test
    public void userCanDepositTest() {
        // ШАГИ ПО НАСТРОЙКЕ ОКРУЖЕНИЯ
        // ШАГ 1: админ логинится в банке
        // ШАГ 2: админ создает юзера
        // ШАГ 3: юзер логинится в банке
        // ШАГ 4: юзер создает аккаунт

        CreateUserRequest userRequest = AdminSteps.createUser();

        String userAuthHeader = new CrudRequester(
                RequestSpecs.unauthSpec(),
                Endpoint.LOGIN,
                ResponseSpecs.requestReturnsOK())
                .post(LoginUserRequest.builder().username(userRequest.getUsername()).password(userRequest.getPassword()).build())
                .extract()
                .header("Authorization");

        // Создание аккаунта для пользователя
        CrudRequester crudRequester = new CrudRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.ACCOUNTS,
                ResponseSpecs.entityWasCreated()
        );

        crudRequester.post(null);

        Selenide.open("/");

        executeJavaScript("localStorage.setItem('authToken', arguments[0]);", userAuthHeader);

        Selenide.open("/dashboard");

        // ШАГИ ТЕСТА
        // ШАГ 5: юзер переходит на страницу перевода

        $(Selectors.byText("\uD83D\uDCB0 Deposit Money")).click();

        // ШАГ 6: проверка, произошел переход на страницу депозита
        $(Selectors.byText("\uD83D\uDCB0 Deposit Money")).shouldBe(Condition.visible);

        // ШАГ 7: получение списка аккаунтов пользователя

        CreateAccountResponse[] existingUserAccounts = given()
                .spec(RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()))
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then().assertThat()
                .extract().as(CreateAccountResponse[].class);


        CreateAccountResponse createdAccount = existingUserAccounts[0];

        assertThat(createdAccount).isNotNull();
        String accountNumber = createdAccount.getAccountNumber();

        // ШАГ 8: перевод и проверка успешного перевода

        Double sumOfDeposit = 500.0;

        $("select.form-control.account-selector")
                .shouldBe(Condition.visible)
                .selectOptionContainingText(accountNumber);


        $(".form-control.deposit-input").setValue(String.valueOf(sumOfDeposit));


        $("button.btn.btn-primary.shadow-custom.mt-4").click();

        Alert alert = switchTo().alert();
        String alertText = alert.getText();

        assertThat(alertText).contains("✅ Successfully deposited"); //✅ Successfully deposited $500 to account ACC1!

        alert.accept();

        Pattern pattern = Pattern.compile(STR."Successfully deposited $\{sumOfDeposit}");
        Matcher matcher = pattern.matcher(alertText);

        matcher.find();

        // ШАГ 9: проверка, что баланс был пополнен

        CreateAccountResponse[] accounts = given()
                .spec(RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()))
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then().extract().as(CreateAccountResponse[].class);

        CreateAccountResponse updated = Arrays.stream(accounts)
                .filter(a -> a.getAccountNumber().equals(accountNumber))
                .findFirst().orElseThrow();

        assertThat(updated.getBalance()).isEqualTo(sumOfDeposit);
    }

    @Test
    public void userCanNotDepositTest() {
        // ШАГИ ПО НАСТРОЙКЕ ОКРУЖЕНИЯ
        // ШАГ 1: админ логинится в банке
        // ШАГ 2: админ создает юзера
        // ШАГ 3: юзер логинится в банке
        // ШАГ 4: юзер создает аккаунт

        CreateUserRequest userRequest = AdminSteps.createUser();

        String userAuthHeader = new CrudRequester(
                RequestSpecs.unauthSpec(),
                Endpoint.LOGIN,
                ResponseSpecs.requestReturnsOK())
                .post(LoginUserRequest.builder().username(userRequest.getUsername()).password(userRequest.getPassword()).build())
                .extract()
                .header("Authorization");

        // Создание аккаунта для пользователя
        CrudRequester crudRequester = new CrudRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                Endpoint.ACCOUNTS,
                ResponseSpecs.entityWasCreated()
        );

        crudRequester.post(null);

        Selenide.open("/");

        executeJavaScript("localStorage.setItem('authToken', arguments[0]);", userAuthHeader);

        Selenide.open("/dashboard");

        // ШАГИ ТЕСТА
        // ШАГ 5: юзер переходит на страницу перевода

        $(Selectors.byText("\uD83D\uDCB0 Deposit Money")).click();

        // ШАГ 6: проверка, произошел переход на страницу депозита
        $(Selectors.byText("\uD83D\uDCB0 Deposit Money")).shouldBe(Condition.visible);

        // ШАГ 7: получение списка аккаунтов пользователя

        CreateAccountResponse[] existingUserAccounts = given()
                .spec(RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()))
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then().assertThat()
                .extract().as(CreateAccountResponse[].class);


        CreateAccountResponse createdAccount = existingUserAccounts[0];

        assertThat(createdAccount).isNotNull();
        String accountNumber = createdAccount.getAccountNumber();

        // ШАГ 8: перевод и проверка ошибки перевода

        Double sumOfDeposit = 10000.0;

        $("select.form-control.account-selector")
                .shouldBe(Condition.visible)
                .selectOptionContainingText(accountNumber);


        $(".form-control.deposit-input").setValue(String.valueOf(sumOfDeposit));


        $("button.btn.btn-primary.shadow-custom.mt-4").click();

        Alert alert = switchTo().alert();
        String alertText = alert.getText();

        assertThat(alertText).contains("❌ Please deposit less or equal to 5000$.");

        alert.accept();


        // ШАГ 9: проверка, что баланс не был пополнен

        CreateAccountResponse[] accounts = given()
                .spec(RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()))
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then().extract().as(CreateAccountResponse[].class);

        CreateAccountResponse updated = Arrays.stream(accounts)
                .filter(a -> a.getAccountNumber().equals(accountNumber))
                .findFirst().orElseThrow();

        assertThat(updated.getBalance()).isZero();
    }
}