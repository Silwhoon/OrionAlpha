package login;

public class PinCodeValidator {

  public static boolean isValidPin(String pin) {
    if (pin == null || pin.length() != 4) {
      return false;
    }

    // Make sure the pin is a number
    try {
      Integer.parseInt(pin);
    } catch (NumberFormatException e) {
      return false;
    }

    return true;
  }
}
