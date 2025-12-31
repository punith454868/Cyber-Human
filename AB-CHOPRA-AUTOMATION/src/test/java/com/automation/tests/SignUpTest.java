package com.automation.tests;

import com.automation.base.BaseTest;
import com.automation.pages.SignUpPage;
import com.aventstack.extentreports.Status;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class SignUpTest extends BaseTest {

    /**
     * ✅ SMART NAVIGATION TO SIGN UP PAGE
     * Handles different app states:
     * 1. Already on Sign Up page -> Stay there
     * 2. On Sign In page -> Click Sign Up
     * 3. On Home Page (Logged In) -> Logout -> Click Sign Up
     */
    private void navigateToSignUp() throws InterruptedException {
        // 1. Check if already on Sign Up page
        SignUpPage signUpPage = new SignUpPage(driver);
        if (signUpPage.isSignUpPageDisplayed()) {
            test.log(Status.INFO, "Already on Sign Up Page");
            Assert.assertTrue(signUpPage.isSignUpPageDisplayed(), "FATAL: Sign Up Page NOT displayed!");
            return;
        }

        // 2. Check if on Sign In page
        com.automation.pages.SignInPage signInPage = new com.automation.pages.SignInPage(driver);
        if (signInPage.isOnSignInPage()) {
            signInPage.clickSignUp();
            test.log(Status.INFO, "Navigated to Sign Up page from Sign In");
            Thread.sleep(2000);
            Assert.assertTrue(signUpPage.isSignUpPageDisplayed(),
                    "FATAL: Sign Up Page NOT displayed after clicking Sign Up!");
            return;
        }

        // 3. Check if Logged In (Home Page)
        com.automation.pages.HomePage homePage = new com.automation.pages.HomePage(driver);
        if (homePage.isHomePageDisplayed()) {
            test.log(Status.INFO, "Detected Logged In state - Performing Logout");
            homePage.navigateToLogout();

            // Handle Logout Confirmation
            com.automation.pages.ProfilePage profilePage = new com.automation.pages.ProfilePage(driver);
            if (profilePage.isSignInPageDisplayed()) {
                // Already at Sign In
            } else {
                // Might need to confirm logout? navigateToLogout typically handles it?
                // Checking HomePage.java navigateToLogout: clickWellbeing -> clickProfile.
                // It misses the clickLogout -> clickYes part.
                // So we need to do that here.
                profilePage.clickLogout();
                profilePage.clickYes();
            }

            Thread.sleep(2000);
            if (signInPage.isOnSignInPage()) {
                signInPage.clickSignUp();
                test.log(Status.INFO, "Navigated to Sign Up page after Logout");
                Thread.sleep(2000);
                Assert.assertTrue(signUpPage.isSignUpPageDisplayed(),
                        "FATAL: Sign Up Page NOT displayed after Logout!");
                return;
            }
        }

        // 4. Fallback: Try to go back if trapped in some sub-page
        try {
            driver.navigate().back();
            Thread.sleep(1000);
            if (signInPage.isOnSignInPage()) {
                signInPage.clickSignUp();
                return;
            }
        } catch (Exception ignored) {
        }

        test.log(Status.WARNING, "Could not determine app state. Attempting implicit navigation.");
    }

    /**
     * ✅ NEGATIVE TEST DATA (NO HARDCODED EXPECTED MESSAGES)
     * Test scenarios for Sign Up page validation
     */
    @DataProvider(name = "negativeSignUpData")
    public Object[][] getNegativeSignUpData() {
        return new Object[][] {
                // Scenario, Name, Email, Password, Confirm Password
                { "Invalid Email Format", "John Doe", "invalidemail", "Password@123", "Password@123" }, // 1
                { "Empty Name Field", "", "test@example.com", "Password@123", "Password@123" }, // 2
                { "Empty Email Field", "John Doe", "", "Password@123", "Password@123" }, // 3
                { "Empty Password Field", "John Doe", "test@example.com", "", "Password@123" }, // 4
                { "Empty Confirm Password", "John Doe", "test@example.com", "Password@123", "" }, // 5
                { "Password Mismatch", "John Doe", "test@example.com", "Password@123", "DifferentPass@123" }, // 6
                { "Weak Password", "John Doe", "test@example.com", "123", "123" }, // 7
                { "All Fields Empty", "", "", "", "" }, // 8
                { "Email Without Domain", "John Doe", "test@", "Password@123", "Password@123" }, // 9
                { "Short Password", "John Doe", "test@example.com", "12", "12" }, // 10
                { "Only Spaces in Name", "   ", "test@example.com", "Password@123", "Password@123" }, // 11
        };
    }

    /**
     * ✅ RUNTIME-BASED NEGATIVE TEST FOR SIGN UP
     * 
     * Test Flow:
     * 0. Navigate from Sign In page to Sign Up page
     * 1. Enter name, email, password, and confirm password
     * 2. Select country from dropdown
     * 3. Click SIGN UP heading
     * 4. Click Continue button
     * 5. Check if ANY validation appears at runtime
     * 6. PASS if validation detected, FAIL if not
     * 7. Log the actual runtime validation message in Extent Report
     */
    @Test(dataProvider = "negativeSignUpData")
    public void testNegativeSignUp(String testScenario, String name, String email, String password,
            String confirmPassword) throws InterruptedException {

        test = extent.createTest("Negative Test: " + testScenario);
        test.log(Status.INFO, "Name: '" + name + "' | Email: '" + email + "'");
        test.log(Status.INFO, "Password: '" + password + "' | Confirm: '" + confirmPassword + "'");

        // Step 0: Smart Navigation to Sign Up page
        navigateToSignUp();

        SignUpPage signUpPage = new SignUpPage(driver);

        // Step 1: Enter form data
        signUpPage.enterName(name);
        signUpPage.enterEmail(email);
        test.log(Status.INFO, "Entered name and email");

        // Step 2: Select country from dropdown
        signUpPage.selectFirstCountryOption();
        test.log(Status.INFO, "Selected country from dropdown");

        // Step 3: Enter passwords
        signUpPage.enterPassword(password);
        signUpPage.enterConfirmPassword(confirmPassword);
        test.log(Status.INFO, "Entered password and confirm password");

        // Step 4: Click SIGN UP heading (required before Continue)
        signUpPage.clickSignUpHeading();
        test.log(Status.INFO, "Clicked SIGN UP heading");

        // Step 5: Click Continue button
        signUpPage.clickContinue();
        test.log(Status.INFO, "Clicked Continue button");

        // Step 6: Wait for validation to appear (if any)
        Thread.sleep(2000); // Allow time for validation to render

        // Step 7: ✅ RUNTIME VALIDATION CHECK (NO HARDCODED MESSAGES)
        boolean validationDetected = signUpPage.isAnyValidationVisible();

        if (validationDetected) {
            // ✅ PASS: Validation appeared (negative case handled correctly)

            // 🆕 Capture and log the actual runtime validation message
            String validationMessage = signUpPage.getValidationMessage();
            if (validationMessage != null && !validationMessage.trim().isEmpty()) {
                test.log(Status.INFO, "📋 Validation message displayed: \"" + validationMessage + "\"");
            }

            test.log(Status.PASS, "✓ Validation detected at runtime - Negative case handled correctly");
            test.log(Status.PASS, "Test PASSED: Application showed validation for invalid input");
        } else {
            // ❌ FAIL: No validation appeared (security/UX issue)
            test.log(Status.FAIL, "✗ NO validation detected at runtime");
            test.log(Status.FAIL, "Test FAILED: Application did not show any validation for invalid input");
            Assert.fail("Expected validation to appear for negative test case, but NONE was detected");
        }
    }

    /**
     * ✅ POSITIVE SIGN UP TEST (Optional - for comparison)
     * This test expects successful sign up without validation errors
     * 
     * Test Flow:
     * 0. Navigate from Sign In page to Sign Up page
     * 1. Enter valid name, email, password, and confirm password
     * 2. Select country from dropdown
     * 3. Click SIGN UP heading
     * 4. Click Continue button
     * 5. Verify no validation errors appear
     */
    @Test
    public void testPositiveSignUp() throws InterruptedException {
        test = extent.createTest("Positive Test: Valid Sign Up");
        test.log(Status.INFO, "Testing valid sign up credentials");

        // Step 0: Smart Navigation to Sign Up page
        navigateToSignUp();

        SignUpPage signUpPage = new SignUpPage(driver);

        // Use valid credentials
        String validName = "John Doe";
        String validEmail = "john.doe" + System.currentTimeMillis() + "@example.com"; // Unique email
        String validPassword = "SecurePass@123";

        // Step 1: Enter form data
        signUpPage.enterName(validName);
        signUpPage.enterEmail(validEmail);
        test.log(Status.INFO, "Entered valid name and email");

        // Step 2: Select country from dropdown
        signUpPage.selectFirstCountryOption();
        test.log(Status.INFO, "Selected country from dropdown");

        // Step 3: Enter passwords
        signUpPage.enterPassword(validPassword);
        signUpPage.enterConfirmPassword(validPassword);
        test.log(Status.INFO, "Entered valid password and confirm password");

        // Step 4: Click SIGN UP heading (required before Continue)
        signUpPage.clickSignUpHeading();
        test.log(Status.INFO, "Clicked SIGN UP heading");

        // Step 5: Click Continue button
        signUpPage.clickContinue();
        test.log(Status.INFO, "Clicked Continue button");

        Thread.sleep(2000); // Wait for validation/navigation

        // Check for Consent Page (Success State)
        if (signUpPage.isConsentPageDisplayed()) {
            // ✅ PASS: Consent Page displayed - Sign up successful
            test.log(Status.PASS, "✓ Consent Page displayed - Sign up successful");
            test.log(Status.PASS, "Test PASSED: Valid credentials accepted");
            return;
        }

        // For positive case, we expect NO validation errors
        boolean validationDetected = signUpPage.isAnyValidationVisible();

        if (!validationDetected) {
            // ✅ PASS: No validation errors for valid credentials
            test.log(Status.PASS, "✓ No validation errors - Sign up successful");
            test.log(Status.PASS, "Test PASSED: Valid credentials accepted");
        } else {
            // ❌ FAIL: Unexpected validation for valid credentials
            String validationMessage = signUpPage.getValidationMessage();
            if (validationMessage != null) {
                test.log(Status.FAIL, "Validation message: \"" + validationMessage + "\"");
            }
            test.log(Status.FAIL, "✗ Unexpected validation appeared for valid credentials");
            test.log(Status.FAIL, "Test FAILED: Valid credentials were rejected");
            Assert.fail("Valid credentials should not show validation errors");
        }
    }
}