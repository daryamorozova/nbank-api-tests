package ui.pages;

import api.models.GetProfileResponse;
import api.utils.DisplayNameFormatter;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.SelenideElement;
import lombok.Getter;

import static com.codeborne.selenide.Selenide.$;

@Getter
public class UserDashboard extends BasePage<UserDashboard> {
    private SelenideElement welcomeText = $(Selectors.byClassName("welcome-text"));
    private SelenideElement createNewAccount = $(Selectors.byText("➕ Create New Account"));
    private SelenideElement depositMoney = $(Selectors.byText("\uD83D\uDCB0 Deposit Money"));
    private SelenideElement makeATransfer = $(Selectors.byText("\uD83D\uDD04 Make a Transfer"));
    private SelenideElement userName = $(Selectors.byClassName("user-name"));

    @Override
    public String url() {
        return "/dashboard";
    }

    public UserDashboard createNewAccount() {
        createNewAccount.click();
        return this;
    }

    /** Проверка имени на UI с безопасной нормализацией */
    public UserDashboard shouldShowUserName(String expectedRawName) {
        String expected = DisplayNameFormatter.forUi(expectedRawName);
        userName.shouldBe(Condition.visible)
                .shouldHave(Condition.text(expected));
        welcomeText.shouldBe(Condition.visible)
                .shouldHave(Condition.text("Welcome, " + expected));
        return this;
    }

    public UserDashboard shouldShowUserNameFromApi(GetProfileResponse profile) {
        return shouldShowUserName(profile.getName());
    }
}