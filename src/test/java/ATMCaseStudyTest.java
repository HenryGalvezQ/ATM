import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ATMCaseStudyTest {

    @InjectMocks
    private ATM atm;

    @Mock
    private BankDatabase bankDatabase;

    @Mock
    private Screen screen;

    @Mock
    private Keypad keypad;

    @Mock
    private CashDispenser cashDispenser;

    @Mock
    private DepositSlot depositSlot;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testCorrectLogin() {
        when(keypad.getInput()).thenReturn(12345).thenReturn(54321);
        when(bankDatabase.authenticateUser(12345, 54321)).thenReturn(true);

        callAuthenticateUser(atm);

        boolean isAuthenticated = getUserAuthenticated(atm);
        assertTrue(isAuthenticated);
    }

    @Test
    public void testIncorrectLogin() {
        when(keypad.getInput()).thenReturn(12345).thenReturn(12345);
        when(bankDatabase.authenticateUser(12345, 12345)).thenReturn(false);

        callAuthenticateUser(atm);

        boolean isAuthenticated = getUserAuthenticated(atm);
        assertFalse(isAuthenticated);
    }

    @Test
    public void testAccountValidation() {
        when(bankDatabase.authenticateUser(12345, 54321)).thenReturn(true);
        when(bankDatabase.authenticateUser(12345, 12345)).thenReturn(false);
        when(bankDatabase.authenticateUser(99999, 54321)).thenReturn(false);

        assertTrue(bankDatabase.authenticateUser(12345, 54321));
        assertFalse(bankDatabase.authenticateUser(12345, 12345));
        assertFalse(bankDatabase.authenticateUser(99999, 54321));
    }

    @Test
    public void testBalanceInquiry() {
        BalanceInquiry inquiry = new BalanceInquiry(12345, screen, bankDatabase);
        assertDoesNotThrow(inquiry::execute);
    }

    @Test
    public void testValidWithdrawal() {
        when(bankDatabase.getAvailableBalance(12345)).thenReturn(1000.0);
        when(keypad.getInput()).thenReturn(1);
        when(cashDispenser.isSufficientCashAvailable(20)).thenReturn(true);
        doNothing().when(cashDispenser).dispenseCash(20);
        doAnswer(invocation -> {
            when(bankDatabase.getAvailableBalance(12345)).thenReturn(980.0);
            return null;
        }).when(bankDatabase).debit(12345, 20.0);

        Withdrawal withdrawal = new Withdrawal(12345, screen, bankDatabase, keypad, cashDispenser);

        assertDoesNotThrow(withdrawal::execute);
        assertEquals(980.0, bankDatabase.getAvailableBalance(12345));
    }

    @Test
    public void testDeposit() {
        when(keypad.getInput()).thenReturn(1000);
        when(bankDatabase.getTotalBalance(12345)).thenReturn(950.0);
        doAnswer(invocation -> {
            when(bankDatabase.getTotalBalance(12345)).thenReturn(1000.0);
            return null;
        }).when(bankDatabase).credit(12345, 50.0);

        Deposit deposit = new Deposit(12345, screen, bankDatabase, keypad, depositSlot);

        assertDoesNotThrow(deposit::execute);
        assertEquals(1000.0, bankDatabase.getTotalBalance(12345));
    }

    @Test
    public void testCashDispenser() {
        when(cashDispenser.isSufficientCashAvailable(100)).thenReturn(true);
        doNothing().when(cashDispenser).dispenseCash(100);
        when(cashDispenser.isSufficientCashAvailable(10000)).thenReturn(false);

        assertTrue(cashDispenser.isSufficientCashAvailable(100));
        cashDispenser.dispenseCash(100);
        assertFalse(cashDispenser.isSufficientCashAvailable(10000));
    }

    @Test
    public void testTransactionCreation() throws Exception {
        Method createTransactionMethod = ATM.class.getDeclaredMethod("createTransaction", int.class);
        createTransactionMethod.setAccessible(true);

        Transaction balanceInquiry = (Transaction) createTransactionMethod.invoke(atm, 1);
        assertTrue(balanceInquiry instanceof BalanceInquiry);

        Transaction withdrawal = (Transaction) createTransactionMethod.invoke(atm, 2);
        assertTrue(withdrawal instanceof Withdrawal);

        Transaction deposit = (Transaction) createTransactionMethod.invoke(atm, 3);
        assertTrue(deposit instanceof Deposit);
    }

    @Test
    public void testATMRun() throws Exception {
        Method authenticateUserMethod = ATM.class.getDeclaredMethod("authenticateUser");
        authenticateUserMethod.setAccessible(true);

        Field userAuthenticatedField = ATM.class.getDeclaredField("userAuthenticated");
        userAuthenticatedField.setAccessible(true);
        userAuthenticatedField.set(atm, true);

        Field currentAccountNumberField = ATM.class.getDeclaredField("currentAccountNumber");
        currentAccountNumberField.setAccessible(true);
        currentAccountNumberField.set(atm, 12345);

        assertTrue(userAuthenticatedField.getBoolean(atm));
        assertEquals(12345, currentAccountNumberField.getInt(atm));
    }

    private void callAuthenticateUser(ATM atm) {
        try {
            Method authenticateUserMethod = ATM.class.getDeclaredMethod("authenticateUser");
            authenticateUserMethod.setAccessible(true);
            authenticateUserMethod.invoke(atm);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean getUserAuthenticated(ATM atm) {
        try {
            Field userAuthenticatedField = ATM.class.getDeclaredField("userAuthenticated");
            userAuthenticatedField.setAccessible(true);
            return userAuthenticatedField.getBoolean(atm);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

