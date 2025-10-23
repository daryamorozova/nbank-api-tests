package ui.pages;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class TransferPage extends BasePage<TransferPage> {

    private SelenideElement buttonMakeTransfer = $(Selectors.byText("🚀 Send Transfer"));
    private SelenideElement inputRecipientName = $("[placeholder='Enter recipient name']");
    private SelenideElement accountNumberRecipient = $("[placeholder='Enter recipient account number']");
    private SelenideElement confirmTransfer = $("#confirmCheck");

    @Override
    public String url() {
        return "/transfer";
    }

    public TransferPage transfer(String accountNumberSender, String recipientName, String accountNumberRecip, Double sumOfTransfer) {
        choseAccount.shouldBe(Condition.visible)
                .selectOptionContainingText(accountNumberSender);
        inputRecipientName.setValue(recipientName);
        accountNumberRecipient.setValue(accountNumberRecip);
        amountInput.setValue(String.valueOf(sumOfTransfer));
        confirmTransfer.shouldBe(Condition.visible, Condition.enabled)
                .setSelected(true);
        buttonMakeTransfer.click();
        return this;
    }
}