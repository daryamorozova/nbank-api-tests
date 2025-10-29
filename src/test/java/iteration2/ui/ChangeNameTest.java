package iteration2.ui;

import api.models.GetProfileResponse;
import common.annotations.UserSession;
import common.storage.SessionStorage;
import iteration1.ui.BaseUiTest;
import org.junit.jupiter.api.Test;
import ui.pages.BankAlert;
import ui.pages.ProfilePage;
import ui.pages.UserDashboard;

import static org.assertj.core.api.Assertions.assertThat;

public class ChangeNameTest extends BaseUiTest {

    @Test
    @UserSession
    public void userCanChangeNameTest() {
        var steps = SessionStorage.getSteps();

        GetProfileResponse profileBefore = steps.getProfile();
        String nameBefore = profileBefore.getName();

        new UserDashboard().open();
        String newName = "John Doe";
        new ProfilePage().changeName(newName)
                .checkAlertMessageAndAccept(BankAlert.CHANGE_NAME_SUCCESS.getMessage());

        new UserDashboard()
                .open()
                .shouldShowUserName(newName);

        GetProfileResponse profileAfter = steps.getProfile();
        String nameAfter = profileAfter.getName();

        assertThat(nameAfter).isEqualTo(newName);
        assertThat(nameAfter).isNotEqualTo(nameBefore);
    }

    @Test
    public void userCanNotChangeNameTest() {
        var steps = SessionStorage.getSteps();

        GetProfileResponse profileBefore = steps.getProfile();

        new UserDashboard().open();
        String newName = "John Doe Mark";
        new ProfilePage().changeName(newName)
                .checkAlertMessageAndAccept(BankAlert.CHANGE_NAME_ERROR.getMessage());

        new UserDashboard()
                .open()
                .shouldShowUserNameFromApi(profileBefore);

        GetProfileResponse profileAfter = steps.getProfile();
        assertThat(profileAfter.getName()).isNotEqualTo(newName);
        assertThat(profileAfter.getName()).isEqualTo(profileBefore.getName());
    }
}