package iteration2.ui;

import api.models.CreateUserRequest;
import api.models.GetProfileResponse;
import api.requests.steps.AdminSteps;
import api.requests.steps.UserSteps;
import iteration1.ui.BaseUiTest;
import org.junit.jupiter.api.Test;
import ui.pages.BankAlert;
import ui.pages.ProfilePage;
import ui.pages.UserDashboard;

import static org.assertj.core.api.Assertions.assertThat;

public class ChangeNameTest extends BaseUiTest {

    @Test
    public void userCanChangeNameTest() {
        // Запросить имя до изменения через бэкенд
        // изменить через фронт
        // посмотреть что на фронте поменялось имя
        // запросить имя после изменения через бэкенд

        CreateUserRequest user = AdminSteps.createUser();
        authAsUser(user);

        UserSteps userApi = new UserSteps(user.getUsername(), user.getPassword());

        GetProfileResponse profileBefore = userApi.getProfile();
        String nameBefore = profileBefore.getName();

        new UserDashboard().open();
        String newName = "John Doe";
        new ProfilePage().changeName(newName)
                .checkAlertMessageAndAccept(BankAlert.CHANGE_NAME_SUCCESS.getMessage());

        new UserDashboard()
                .open()
                .shouldShowUserName(newName);

        GetProfileResponse profileAfter = userApi.getProfile();
        String nameAfter = profileAfter.getName();

        assertThat(nameAfter).isEqualTo(newName);
        assertThat(nameAfter).isNotEqualTo(nameBefore);
    }

    @Test
    public void userCanNotChangeNameTest() {
        CreateUserRequest user = AdminSteps.createUser();
        authAsUser(user);

        UserSteps userApi = new UserSteps(user.getUsername(), user.getPassword());

        GetProfileResponse profileBefore = userApi.getProfile();

        new UserDashboard().open();
        String newName = "John Doe Mark";
        new ProfilePage().changeName(newName)
                .checkAlertMessageAndAccept(BankAlert.CHANGE_NAME_ERROR.getMessage());

        new UserDashboard()
                .open()
                .shouldShowUserNameFromApi(profileBefore);

        GetProfileResponse profileAfter = userApi.getProfile();
        assertThat(profileAfter.getName()).isNotEqualTo(newName);
        assertThat(profileAfter.getName()).isEqualTo(profileBefore.getName());
    }
}