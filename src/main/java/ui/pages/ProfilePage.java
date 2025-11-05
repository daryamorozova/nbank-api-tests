package ui.pages;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class ProfilePage extends BasePage<ProfilePage> {

    private SelenideElement saveChanges =  $(Selectors.byText("\uD83D\uDCBE Save Changes"));
    private SelenideElement inputName =$(Selectors.byAttribute("placeholder", "Enter new name"));
    private SelenideElement name =  $(Selectors.byClassName("user-name"));
    private SelenideElement editProfile = $(Selectors.byText("✏\uFE0F Edit Profile"));

    @Override
    public String url() {
        return "/edit-profile";
    }

    public ProfilePage changeName(String newName) {
        name.click();
        editProfile.shouldBe(Condition.visible);
        inputName.shouldBe(Condition.visible).setValue(newName);
        saveChanges.click();
        return this;
    }
}