package iteration2.ui;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.Selenide;
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

import java.util.Map;

import static com.codeborne.selenide.Selenide.*;
import static org.assertj.core.api.Assertions.assertThat;

public class ChangeNameTest {
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
    public void userCanChangeNameTest() {
        // ШАГИ ПО НАСТРОЙКЕ ОКРУЖЕНИЯ
        // ШАГ 1: админ логинится в банке
        // ШАГ 2: админ создает юзера
        // ШАГ 3: юзер логинится в банке

        CreateUserRequest userRequest = AdminSteps.createUser();

        String userAuthHeader = new CrudRequester(
                RequestSpecs.unauthSpec(),
                Endpoint.LOGIN,
                ResponseSpecs.requestReturnsOK())
                .post(LoginUserRequest.builder().username(userRequest.getUsername()).password(userRequest.getPassword()).build())
                .extract()
                .header("Authorization");

        Selenide.open("/");
        executeJavaScript("localStorage.setItem('authToken', arguments[0]);", userAuthHeader);
        Selenide.open("/dashboard");

        // ШАГИ ТЕСТА
        // ШАГ 4: юзер видит текущее имя
        $(Selectors.byClassName("user-name")).shouldBe(Condition.visible).shouldHave(Condition.text("Noname"));

        // ШАГ 5: юзер переходит в профиль
        $(Selectors.byClassName("user-name")).click();
        $(Selectors.byText("✏\uFE0F Edit Profile")).shouldBe(Condition.visible);

        // ШАГ 6: юзер редактирует имя
        $(Selectors.byAttribute("placeholder", "Enter new name")).shouldBe(Condition.visible);
        String newName = "John Doe";
        $(Selectors.byAttribute("placeholder", "Enter new name")).setValue(newName);

        $(Selectors.byText("\uD83D\uDCBE Save Changes")).click();

        // ШАГ 7: юзер видит алерт
        Alert alert = switchTo().alert();
        String alertText = alert.getText();
        assertThat(alertText).contains("✅ Name updated successfully!");
        alert.accept();

        // ШАГ 8: юзер возвращается на дашборд и проверяет имя
        $(Selectors.byText("\uD83C\uDFE0 Home")).click();
        Selenide.refresh();
        $(Selectors.byClassName("welcome-text")).shouldBe(Condition.visible).shouldHave(Condition.text(STR."Welcome, \{newName}"));
        $(Selectors.byClassName("user-name")).shouldBe(Condition.visible).shouldHave(Condition.text(newName));
    }

    @Test
    public void userCanNotChangeNameTest() {
        // ШАГИ ПО НАСТРОЙКЕ ОКРУЖЕНИЯ
        // ШАГ 1: админ логинится в банке
        // ШАГ 2: админ создает юзера
        // ШАГ 3: юзер логинится в банке

        CreateUserRequest userRequest = AdminSteps.createUser();

        String userAuthHeader = new CrudRequester(
                RequestSpecs.unauthSpec(),
                Endpoint.LOGIN,
                ResponseSpecs.requestReturnsOK())
                .post(LoginUserRequest.builder().username(userRequest.getUsername()).password(userRequest.getPassword()).build())
                .extract()
                .header("Authorization");

        Selenide.open("/");
        executeJavaScript("localStorage.setItem('authToken', arguments[0]);", userAuthHeader);
        Selenide.open("/dashboard");

        // ШАГИ ТЕСТА
        // ШАГ 4: юзер видит текущее имя
        $(Selectors.byClassName("user-name")).shouldBe(Condition.visible).shouldHave(Condition.text("Noname"));

        // ШАГ 5: юзер переходит в профиль
        $(Selectors.byClassName("user-name")).click();
        $(Selectors.byText("✏\uFE0F Edit Profile")).shouldBe(Condition.visible);

        // ШАГ 6: юзер редактирует имя
        $(Selectors.byAttribute("placeholder", "Enter new name")).shouldBe(Condition.visible);
        String newName = "John Doe Mark";
        $(Selectors.byAttribute("placeholder", "Enter new name")).setValue(newName);

        $(Selectors.byText("\uD83D\uDCBE Save Changes")).click();

        // ШАГ 7: юзер видит алерт
        Alert alert = switchTo().alert();
        String alertText = alert.getText();
        assertThat(alertText).contains("Name must contain two words with letters only");
        alert.accept();

        // ШАГ 8: юзер возвращается на дашборд и проверяет имя
        $(Selectors.byText("\uD83C\uDFE0 Home")).click();
        Selenide.refresh();
        $(Selectors.byClassName("welcome-text")).shouldBe(Condition.visible).shouldHave(Condition.text("Welcome, Noname"));
        $(Selectors.byClassName("user-name")).shouldBe(Condition.visible).shouldHave(Condition.text("Noname"));
    }
}