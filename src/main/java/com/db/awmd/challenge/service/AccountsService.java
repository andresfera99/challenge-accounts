package com.db.awmd.challenge.service;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.repository.AccountsRepository;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class AccountsService {

    @Getter
    private final AccountsRepository accountsRepository;

    @Autowired
    public AccountsService(AccountsRepository accountsRepository) {
        this.accountsRepository = accountsRepository;
    }

    public void createAccount(Account account) {
        this.accountsRepository.createAccount(account);
    }

    public Account getAccount(String accountId) {
        return this.accountsRepository.getAccount(accountId);
    }

    public synchronized boolean transferMoney(String accountFrom, String accountTo, BigDecimal money) {
        Account originAccount = this.accountsRepository.getAccount(accountFrom);
        Account destinationAccount = this.accountsRepository.getAccount(accountTo);

        if (originAccount == null || destinationAccount == null || money == null || money.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        //store the result of the operation
        BigDecimal balance = originAccount.getBalance();
        //cant subtract more
        if (balance.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        BigDecimal operation = balance.subtract(money);
        //check if its safe to proceed
        if (operation.compareTo(BigDecimal.ZERO) >= 0) {
            //proceed with the operation
            destinationAccount.setBalance(destinationAccount.getBalance().add(money));
            originAccount.setBalance(originAccount.getBalance().subtract(money));

            //notify account owners
            EmailNotificationService emailNotificationService = new EmailNotificationService();
            emailNotificationService.notifyAboutTransfer(originAccount, money + " sent to " + destinationAccount.getAccountId());
            emailNotificationService.notifyAboutTransfer(destinationAccount, money + " received from " + originAccount.getAccountId());

            return true;
        }

        return false;


    }
}
