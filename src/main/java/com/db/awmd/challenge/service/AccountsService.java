package com.db.awmd.challenge.service;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.db.awmd.challenge.beans.Transfer;
import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.exception.AccountNotFoundException;
import com.db.awmd.challenge.repository.AccountsRepository;

@Service
public class AccountsService {

  @Autowired
  private AccountsRepository accountsRepository;

  @Autowired
  private NotificationService notificationService;

  public void createAccount(Account account) {
    this.accountsRepository.createAccount(account);
  }

  public Account getAccount(String accountId) {
    return this.accountsRepository.getAccount(accountId);
  }

  public void clearAccounts() {
    accountsRepository.clearAccounts();
  }

  /**
   * Transfers amount from one account to another and notifies the account holders about the same.
   * This operation is done in a thread safe manner and without having any deadlock.
   * To achieve this, locks on the account objects are obtained in a specific order.
   * This order is determined by comparing the account numbers.
   * @param transfer
   * @throws AccountNotFoundException
   */
  public void processTransfer(Transfer transfer) throws AccountNotFoundException {
    Account fromAccount = getAccount(transfer.getFromAccountId());
    Account toAccount = getAccount(transfer.getToAccountId());

    checkIfTransferAccountsExist(fromAccount, toAccount, transfer);

    BigDecimal amount = transfer.getAmount();

    Account account1 = fromAccount.getAccountId().compareTo(toAccount.getAccountId()) > 0 ? fromAccount : toAccount;
    Account account2 = (account1 == fromAccount) ? toAccount : fromAccount;

    synchronized (account1) {
      synchronized (account2) {
        if (fromAccount.hasSufficientBalance(amount)) {
          fromAccount.withdraw(amount);
          toAccount.deposit(amount);
          updateAccountsInTransfer(fromAccount, toAccount);
          notificationService.notifyAboutTransfer(fromAccount, "Amount " + amount + " is transferred to account " + toAccount.getAccountId());
          notificationService.notifyAboutTransfer(toAccount, "Amount " + amount + " is deposited from account " + fromAccount.getAccountId());
        }
      }
    }
  }

  private void checkIfTransferAccountsExist(Account fromAccount, Account toAccount, Transfer transfer) throws AccountNotFoundException {
    if (fromAccount == null) {
      throw new AccountNotFoundException("Account id " + transfer.getFromAccountId() + " does not exists!");
    }
    if (toAccount == null) {
      throw new AccountNotFoundException("Account id " + transfer.getToAccountId() + " does not exists!");
    }
  }

  //@Transactional
  public void updateAccountsInTransfer(Account fromAccount, Account toAccount) {
    accountsRepository.updateAccount(fromAccount);
    accountsRepository.updateAccount(toAccount);
  }
}
