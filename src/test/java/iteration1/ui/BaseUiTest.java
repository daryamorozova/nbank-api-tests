package iteration1.ui;

import api.models.CreateUserRequest;
import api.specs.RequestSpecs;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.logevents.SelenideLogger;
import common.extensions.AdminSessionExtension;
import common.extensions.BrowserMatchExtension;
import common.extensions.UserSessionExtension;
import io.qameta.allure.selenide.AllureSelenide;
import iteration1.api.BaseTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static com.codeborne.selenide.Selenide.executeJavaScript;

@ExtendWith(AdminSessionExtension.class)
@ExtendWith(UserSessionExtension.class)
@ExtendWith(BrowserMatchExtension.class)
public class BaseUiTest extends BaseTest {
    @BeforeAll
    public static void setupSelenoid() {
        String remote = System.getProperty("selenide.remote",
                api.configs.Config.getProperty("uiRemote"));
        String baseUrl = System.getProperty("selenide.baseUrl",
                api.configs.Config.getProperty("uiBaseUrl"));
        String browser = System.getProperty("selenide.browser",
                api.configs.Config.getProperty("browser"));
        String browserVersion = System.getProperty("selenide.browserVersion",
                api.configs.Config.getProperty("browserVersion")); // добавьте пустой ключ в properties
        String browserSize = System.getProperty("selenide.browserSize",
                api.configs.Config.getProperty("browserSize"));

        Configuration.remote = remote;
        Configuration.baseUrl = baseUrl;
        Configuration.browser = browser;
        if (browserVersion != null && !browserVersion.isBlank()) {
            Configuration.browserVersion = browserVersion;
        }
        Configuration.browserSize = browserSize;

        SelenideLogger.addListener("AllureSelenide", new AllureSelenide());
        Configuration.reportsFolder = "build/reports/tests-ui";
        Configuration.savePageSource = true;
        Configuration.screenshots = true;

        Configuration.browserCapabilities.setCapability("selenoid:options",
                Map.of("enableVNC", true, "enableLog", true));

        System.out.printf("[UI] remote=%s, baseUrl=%s, browser=%s, version=%s, size=%s%n",
                Configuration.remote, Configuration.baseUrl, Configuration.browser,
                Configuration.browserVersion, Configuration.browserSize);
    }

    public void authAsUser(String username, String password) {
        Selenide.open("/");
        String userAuthHeader = RequestSpecs.getUserAuthHeader(username, password);
        executeJavaScript("localStorage.setItem('authToken', arguments[0]);", userAuthHeader);
    }

    public void authAsUser(CreateUserRequest createUserRequest) {
        authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword());
    }
}