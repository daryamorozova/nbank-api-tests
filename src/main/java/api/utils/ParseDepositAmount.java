package api.utils;

public class ParseDepositAmount {
    public Double parseDepositAmount(String depositAmount) {
        if (depositAmount == null || depositAmount.isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(depositAmount);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
