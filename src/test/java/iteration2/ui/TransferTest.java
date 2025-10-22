package iteration2.ui;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.Selenide;
import models.CreateAccountResponse;
import models.CreateUserRequest;
import models.DepositRequest;
import models.LoginUserRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Alert;
import requests.skelethon.Endpoint;
import requests.skelethon.requesters.CrudRequester;
import requests.steps.AdminSteps;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.Arrays;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.codeborne.selenide.Selenide.*;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

public class TransferTest {

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
    public void userCanTransferTest() {
        // ШАГИ ПО НАСТРОЙКЕ ОКРУЖЕНИЯ
        // ШАГ 1: админ логинится в банке
        // ШАГ 2: админ создает юзера1
        // ШАГ 3: админ создает юзера2
        // ШАГ 4: юзер1 логинится в банке
        // ШАГ 5: юзер создает аккаунт
        // ШАГ 6: юзер2 логинится в банке
        // ШАГ 7: юзер2 создает аккаунт

        CreateUserRequest userRequest1 = AdminSteps.createUser();
        CreateUserRequest userRequest2 = AdminSteps.createUser();

        String userAuthHeader1 = new CrudRequester(
                RequestSpecs.unauthSpec(),
                Endpoint.LOGIN,
                ResponseSpecs.requestReturnsOK())
                .post(LoginUserRequest.builder().username(userRequest1.getUsername()).password(userRequest1.getPassword()).build())
                .extract()
                .header("Authorization");

        // Создание аккаунта для пользователя1
        CrudRequester crudRequester1 = new CrudRequester(
                RequestSpecs.authAsUser(userRequest1.getUsername(), userRequest1.getPassword()),
                Endpoint.ACCOUNTS,
                ResponseSpecs.entityWasCreated()
        );
        crudRequester1.post(null);

        // ШАГ 11: получение списка аккаунтов пользователя1

        CreateAccountResponse[] existingUserAccounts1 = given()
                .spec(RequestSpecs.authAsUser(userRequest1.getUsername(), userRequest1.getPassword()))
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then().assertThat()
                .extract().as(CreateAccountResponse[].class);

        CreateAccountResponse createdAccount1 = existingUserAccounts1[0];
        assertThat(createdAccount1).isNotNull();
        String accountNumber1 = createdAccount1.getAccountNumber();


        //

        String userAuthHeader2 = new CrudRequester(
                RequestSpecs.unauthSpec(),
                Endpoint.LOGIN,
                ResponseSpecs.requestReturnsOK())
                .post(LoginUserRequest.builder().username(userRequest2.getUsername()).password(userRequest2.getPassword()).build())
                .extract()
                .header("Authorization");

        // Создание аккаунта для пользователя2
        CrudRequester crudRequester2 = new CrudRequester(
                RequestSpecs.authAsUser(userRequest2.getUsername(), userRequest2.getPassword()),
                Endpoint.ACCOUNTS,
                ResponseSpecs.entityWasCreated()
        );
        crudRequester2.post(null);

        Selenide.open("/");
        executeJavaScript("localStorage.setItem('authToken', arguments[0]);", userAuthHeader2);
        Selenide.open("/dashboard");

        // ШАГ 10: получение списка аккаунтов пользователя2

        CreateAccountResponse[] existingUserAccounts2 = given()
                .spec(RequestSpecs.authAsUser(userRequest2.getUsername(), userRequest2.getPassword()))
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then().assertThat()
                .extract().as(CreateAccountResponse[].class);

        CreateAccountResponse createdAccount2 = existingUserAccounts2[0];
        assertThat(createdAccount2).isNotNull();
        String accountNumber2 = createdAccount2.getAccountNumber();

        Double initialBalance2 = createdAccount2.getBalance();

        // Начальный депозит для пользователя 2

        CrudRequester depositRequester = new CrudRequester(
                RequestSpecs.authAsUser(userRequest2.getUsername(), userRequest2.getPassword()),
                Endpoint.DEPOSIT,
                ResponseSpecs.requestReturnsOK()
        );
        depositRequester.post(DepositRequest.builder().id(createdAccount2.getId()).balance(5000.00).build());

        // перечитать баланс отправителя после депозита
        CreateAccountResponse[] afterDeposit = given()
                .spec(RequestSpecs.authAsUser(userRequest2.getUsername(), userRequest2.getPassword()))
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then().extract().as(CreateAccountResponse[].class);

        double balanceAfterDeposit = Arrays.stream(afterDeposit)
                .filter(a -> a.getAccountNumber().equals(accountNumber2))
                .findFirst().orElseThrow()
                .getBalance();

        // ШАГИ ТЕСТА
        // ШАГ 8: юзер2 переходит на страницу трансфера

        $(Selectors.byText("\uD83D\uDD04 Make a Transfer")).click();

        // ШАГ 9: проверка, произошел переход на страницу депозита
        $(Selectors.byText("\uD83D\uDD04 Make a Transfer")).shouldBe(Condition.visible);

        // ШАГ 12: перевод и проверка успешного перевода
        Double sumOfTransfer = 500.0;

        // юзер 2 нажимает на кнопку 🆕 New Transfer и видит форму перевода
        $(Selectors.byText("\uD83C\uDD95 New Transfer")).click();
        $(Selectors.byText("Recipient Account Number:")).shouldBe(Condition.visible);

        // юзер 2 заполняет форму трансфера и нажимает на кнопку трансфера
        $("select.form-control.account-selector")
                .shouldBe(Condition.visible)
                .selectOptionContainingText(accountNumber2);

        $("[placeholder='Enter recipient name']").setValue("Name");
        $("[placeholder='Enter recipient account number']").setValue(String.valueOf(accountNumber1));
        $("[placeholder='Enter amount']").setValue(String.valueOf(sumOfTransfer));
        $("#confirmCheck")
                .shouldBe(Condition.visible, Condition.enabled)
                .setSelected(true);

        $(Selectors.byText("🚀 Send Transfer")).click();

        Alert alert = switchTo().alert();
        String alertText = alert.getText();

        assertThat(alertText).contains("✅ Successfully transferred"); //✅ Successfully transferred $100 to account ACC31!

        alert.accept();

        Pattern pattern = Pattern.compile(STR."Successfully transferred $\{sumOfTransfer}");
        Matcher matcher = pattern.matcher(alertText);

        matcher.find();

        // ШАГ 9: проверка, что баланс юзера1 был пополнен

        CreateAccountResponse[] accounts1 = given()
                .spec(RequestSpecs.authAsUser(userRequest1.getUsername(), userRequest1.getPassword()))
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then().extract().as(CreateAccountResponse[].class);

        CreateAccountResponse updated1 = Arrays.stream(accounts1)
                .filter(a -> a.getAccountNumber().equals(accountNumber1))
                .findFirst().orElseThrow();

        assertThat(updated1.getBalance()).isEqualTo(sumOfTransfer);


        // ШАГ 9: проверка, что баланс юзера2 стал меньше

        CreateAccountResponse[] accounts2 = given()
                .spec(RequestSpecs.authAsUser(userRequest2.getUsername(), userRequest2.getPassword()))
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then().extract().as(CreateAccountResponse[].class);

        CreateAccountResponse updated2 = Arrays.stream(accounts2)
                .filter(a -> a.getAccountNumber().equals(accountNumber2))
                .findFirst().orElseThrow();

        double expectedBalance2 = balanceAfterDeposit - sumOfTransfer;
        assertThat(updated2.getBalance()).isEqualTo(expectedBalance2);
    }


    @Test
    public void userCanNotTransferTest() {
        // ШАГИ ПО НАСТРОЙКЕ ОКРУЖЕНИЯ
        // ШАГ 1: админ логинится в банке
        // ШАГ 2: админ создает юзера1
        // ШАГ 3: админ создает юзера2
        // ШАГ 4: юзер1 логинится в банке
        // ШАГ 5: юзер создает аккаунт
        // ШАГ 6: юзер2 логинится в банке
        // ШАГ 7: юзер2 создает аккаунт

        CreateUserRequest userRequest1 = AdminSteps.createUser();
        CreateUserRequest userRequest2 = AdminSteps.createUser();

        String userAuthHeader1 = new CrudRequester(
                RequestSpecs.unauthSpec(),
                Endpoint.LOGIN,
                ResponseSpecs.requestReturnsOK())
                .post(LoginUserRequest.builder().username(userRequest1.getUsername()).password(userRequest1.getPassword()).build())
                .extract()
                .header("Authorization");

        // Создание аккаунта для пользователя1
        CrudRequester crudRequester1 = new CrudRequester(
                RequestSpecs.authAsUser(userRequest1.getUsername(), userRequest1.getPassword()),
                Endpoint.ACCOUNTS,
                ResponseSpecs.entityWasCreated()
        );
        crudRequester1.post(null);

        // ШАГ 11: получение списка аккаунтов пользователя1

        CreateAccountResponse[] existingUserAccounts1 = given()
                .spec(RequestSpecs.authAsUser(userRequest1.getUsername(), userRequest1.getPassword()))
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then().assertThat()
                .extract().as(CreateAccountResponse[].class);

        CreateAccountResponse createdAccount1 = existingUserAccounts1[0];
        assertThat(createdAccount1).isNotNull();
        String accountNumber1 = createdAccount1.getAccountNumber();


        //

        String userAuthHeader2 = new CrudRequester(
                RequestSpecs.unauthSpec(),
                Endpoint.LOGIN,
                ResponseSpecs.requestReturnsOK())
                .post(LoginUserRequest.builder().username(userRequest2.getUsername()).password(userRequest2.getPassword()).build())
                .extract()
                .header("Authorization");

        // Создание аккаунта для пользователя2
        CrudRequester crudRequester2 = new CrudRequester(
                RequestSpecs.authAsUser(userRequest2.getUsername(), userRequest2.getPassword()),
                Endpoint.ACCOUNTS,
                ResponseSpecs.entityWasCreated()
        );
        crudRequester2.post(null);

        Selenide.open("/");
        executeJavaScript("localStorage.setItem('authToken', arguments[0]);", userAuthHeader2);
        Selenide.open("/dashboard");

        // ШАГ 10: получение списка аккаунтов пользователя2

        CreateAccountResponse[] existingUserAccounts2 = given()
                .spec(RequestSpecs.authAsUser(userRequest2.getUsername(), userRequest2.getPassword()))
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then().assertThat()
                .extract().as(CreateAccountResponse[].class);

        CreateAccountResponse createdAccount2 = existingUserAccounts2[0];
        assertThat(createdAccount2).isNotNull();
        String accountNumber2 = createdAccount2.getAccountNumber();

        Double initialBalance2 = createdAccount2.getBalance();

        // Начальный депозит для пользователя 2

        CrudRequester depositRequester = new CrudRequester(
                RequestSpecs.authAsUser(userRequest2.getUsername(), userRequest2.getPassword()),
                Endpoint.DEPOSIT,
                ResponseSpecs.requestReturnsOK()
        );
        depositRequester.post(DepositRequest.builder().id(createdAccount2.getId()).balance(5000.00).build());

        // перечитать баланс отправителя после депозита
        CreateAccountResponse[] afterDeposit = given()
                .spec(RequestSpecs.authAsUser(userRequest2.getUsername(), userRequest2.getPassword()))
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then().extract().as(CreateAccountResponse[].class);

        double balanceAfterDeposit = Arrays.stream(afterDeposit)
                .filter(a -> a.getAccountNumber().equals(accountNumber2))
                .findFirst().orElseThrow()
                .getBalance();

        // ШАГИ ТЕСТА
        // ШАГ 8: юзер2 переходит на страницу трансфера

        $(Selectors.byText("\uD83D\uDD04 Make a Transfer")).click();

        // ШАГ 9: проверка, произошел переход на страницу депозита
        $(Selectors.byText("\uD83D\uDD04 Make a Transfer")).shouldBe(Condition.visible);

        // ШАГ 12: перевод и проверка успешного перевода
        Double sumOfTransfer = 100000.0;

        // юзер 2 нажимает на кнопку 🆕 New Transfer и видит форму перевода
        $(Selectors.byText("\uD83C\uDD95 New Transfer")).click();
        $(Selectors.byText("Recipient Account Number:")).shouldBe(Condition.visible);

        // юзер 2 заполняет форму трансфера и нажимает на кнопку трансфера
        $("select.form-control.account-selector")
                .shouldBe(Condition.visible)
                .selectOptionContainingText(accountNumber2);

        $("[placeholder='Enter recipient name']").setValue("Name");
        $("[placeholder='Enter recipient account number']").setValue(String.valueOf(accountNumber1));
        $("[placeholder='Enter amount']").setValue(String.valueOf(sumOfTransfer));
        $("#confirmCheck")
                .shouldBe(Condition.visible, Condition.enabled)
                .setSelected(true);

        $(Selectors.byText("🚀 Send Transfer")).click();

        Alert alert = switchTo().alert();
        String alertText = alert.getText();

        assertThat(alertText).contains("❌ Error: Transfer amount cannot exceed 10000");

        alert.accept();

        // ШАГ 9: проверка, что баланс юзера1 был пополнен

        CreateAccountResponse[] accounts1 = given()
                .spec(RequestSpecs.authAsUser(userRequest1.getUsername(), userRequest1.getPassword()))
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then().extract().as(CreateAccountResponse[].class);

        CreateAccountResponse updated1 = Arrays.stream(accounts1)
                .filter(a -> a.getAccountNumber().equals(accountNumber1))
                .findFirst().orElseThrow();

        assertThat(updated1.getBalance()).isZero();


        // ШАГ 9: проверка, что баланс юзера2 стал меньше

        CreateAccountResponse[] accounts2 = given()
                .spec(RequestSpecs.authAsUser(userRequest2.getUsername(), userRequest2.getPassword()))
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then().extract().as(CreateAccountResponse[].class);

        CreateAccountResponse updated2 = Arrays.stream(accounts2)
                .filter(a -> a.getAccountNumber().equals(accountNumber2))
                .findFirst().orElseThrow();

        double expectedBalance2 = balanceAfterDeposit;
        assertThat(updated2.getBalance()).isEqualTo(expectedBalance2);
    }
}
