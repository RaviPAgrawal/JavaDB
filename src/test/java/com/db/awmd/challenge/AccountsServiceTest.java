package com.db.awmd.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.db.awmd.challenge.beans.Transfer;
import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.exception.AccountNotFoundException;
import com.db.awmd.challenge.repository.AccountsRepository;
import com.db.awmd.challenge.service.AccountsService;
import com.db.awmd.challenge.service.NotificationService;

@RunWith(MockitoJUnitRunner.class)
public class AccountsServiceTest {

    @InjectMocks
    private AccountsService accountsService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AccountsRepository accountsRepository;

    @Test
    public void createAccount() {
        doNothing().when(accountsRepository).createAccount(any(Account.class));
        Account account = mock(Account.class);
        accountsService.createAccount(account);
    }

    @Test
    public void getAccount() {
        Account account = new Account("Id-1", new BigDecimal(12));
        when(accountsRepository.getAccount(account.getAccountId())).thenReturn(account);
        Account resultAccount = accountsService.getAccount(account.getAccountId());
        assertThat(resultAccount.getAccountId()).isEqualTo(account.getAccountId());
        assertThat(resultAccount.getBalance()).isEqualTo(account.getBalance());
    }

    @Test
    public void processTransfer() throws AccountNotFoundException {
        Account fromAccount = new Account("Id-1", new BigDecimal(1000));
        Account toAccount = new Account("Id-2", new BigDecimal(500));
        BigDecimal amountToTransfer = new BigDecimal(300);
        Transfer transfer = new Transfer(fromAccount.getAccountId(), toAccount.getAccountId(), amountToTransfer);

        when(accountsRepository.getAccount(transfer.getFromAccountId())).thenReturn(fromAccount);
        when(accountsRepository.getAccount(transfer.getToAccountId())).thenReturn(toAccount);

        accountsService.processTransfer(transfer);

        assertThat(fromAccount.getBalance()).isEqualTo(new BigDecimal(700));
        assertThat(toAccount.getBalance()).isEqualTo(new BigDecimal(800));

        assertTransferNotifications(fromAccount, toAccount);
        assertAccountUpdates(fromAccount, toAccount);
    }

    private void assertAccountUpdates(Account fromAccount, Account toAccount) {
        ArgumentCaptor<Account> accountCaptorToUpdate = ArgumentCaptor.forClass(Account.class);

        verify(accountsRepository, times(2)).updateAccount(accountCaptorToUpdate.capture());

        List<Account> accountsToUpdate = accountCaptorToUpdate.getAllValues();

        assertThat(accountsToUpdate.get(0).getAccountId()).isEqualTo(fromAccount.getAccountId());
        assertThat(accountsToUpdate.get(1).getAccountId()).isEqualTo(toAccount.getAccountId());
    }

    private void assertTransferNotifications(Account fromAccount, Account toAccount) {
        ArgumentCaptor<Account> accountCaptorForNotifications = ArgumentCaptor.forClass(Account.class);
        ArgumentCaptor<String> stringCaptor = ArgumentCaptor.forClass(String.class);

        verify(notificationService, times(2)).notifyAboutTransfer(accountCaptorForNotifications.capture(), stringCaptor.capture());

        List<Account> accountsForNotifications = accountCaptorForNotifications.getAllValues();
        List<String> messages = stringCaptor.getAllValues();

        assertThat(accountsForNotifications.get(0).getAccountId()).isEqualTo(fromAccount.getAccountId());
        assertThat(accountsForNotifications.get(1).getAccountId()).isEqualTo(toAccount.getAccountId());
        assertThat(messages.get(0)).isEqualTo("Amount 300 is transferred to account Id-2");
        assertThat(messages.get(1)).isEqualTo("Amount 300 is deposited from account Id-1");
    }

    @Test
    public void processTransfer_InsufficientBalance() throws AccountNotFoundException {
        Account fromAccount = new Account("Id-1", new BigDecimal(100));
        Account toAccount = new Account("Id-2", new BigDecimal(500));
        BigDecimal amountToTransfer = new BigDecimal(300);
        Transfer transfer = new Transfer(fromAccount.getAccountId(), toAccount.getAccountId(), amountToTransfer);

        when(accountsRepository.getAccount(transfer.getFromAccountId())).thenReturn(fromAccount);
        when(accountsRepository.getAccount(transfer.getToAccountId())).thenReturn(toAccount);

        accountsService.processTransfer(transfer);
        assertThat(fromAccount.getBalance()).isEqualTo(new BigDecimal(100));
        assertThat(toAccount.getBalance()).isEqualTo(new BigDecimal(500));

        verify(notificationService, times(0)).notifyAboutTransfer(any(Account.class), any(String.class));
        verify(accountsRepository, times(0)).updateAccount(any(Account.class));
    }

    @Test(expected = AccountNotFoundException.class)
    public void processTransfer_TransferAccountDoesNotExist() throws AccountNotFoundException {
        Account fromAccount = new Account("Id-1", new BigDecimal(500));
        Account toAccount = new Account("Id-2", new BigDecimal(500));

        BigDecimal amountToTransfer = new BigDecimal(300);
        Transfer transfer = new Transfer(fromAccount.getAccountId(), toAccount.getAccountId(), amountToTransfer);

        when(accountsRepository.getAccount(transfer.getFromAccountId())).thenReturn(null);
        when(accountsRepository.getAccount(transfer.getToAccountId())).thenReturn(toAccount);

        accountsService.processTransfer(transfer);
    }
}
