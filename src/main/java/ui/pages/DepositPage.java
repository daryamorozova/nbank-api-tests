package ui.pages;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class DepositPage extends BasePage<DepositPage>{
    private SelenideElement buttonDeposit = $("button.btn.btn-primary.shadow-custom.mt-4");

    @Override
    public String url() {
        return "/deposit";
    }

    public DepositPage deposit(String accountNumber, Double sumOfDeposit) {
        choseAccount.shouldBe(Condition.visible)
                .selectOptionContainingText(accountNumber);
        amountInput.setValue(String.valueOf(sumOfDeposit));;
        buttonDeposit.click();
        return this;
    }
}