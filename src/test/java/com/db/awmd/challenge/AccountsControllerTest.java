package com.db.awmd.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.service.AccountsService;

import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
public class AccountsControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private AccountsService accountsService;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Before
    public void prepareMockMvc() {
        this.mockMvc = webAppContextSetup(this.webApplicationContext).build();

        // Reset the existing accounts before each test.
        accountsService.getAccountsRepository().clearAccounts();
        this.accountsService.createAccount(new Account("cuenta1", BigDecimal.valueOf(100.0)));
        this.accountsService.createAccount(new Account("cuenta2", BigDecimal.valueOf(10.0)));
    }

    @Test
    public void createAccount() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

        Account account = accountsService.getAccount("Id-123");
        assertThat(account.getAccountId()).isEqualTo("Id-123");
        assertThat(account.getBalance()).isEqualByComparingTo("1000");
    }

    @Test
    public void createDuplicateAccount() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isBadRequest());
    }

    @Test
    public void createAccountNoAccountId() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"balance\":1000}")).andExpect(status().isBadRequest());
    }

    @Test
    public void createAccountNoBalance() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"Id-123\"}")).andExpect(status().isBadRequest());
    }

    @Test
    public void createAccountNoBody() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void createAccountNegativeBalance() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"Id-123\",\"balance\":-1000}")).andExpect(status().isBadRequest());
    }

    @Test
    public void createAccountEmptyAccountId() throws Exception {
        this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"\",\"balance\":1000}")).andExpect(status().isBadRequest());
    }

    @Test
    public void getAccount() throws Exception {
        String uniqueAccountId = "Id-" + System.currentTimeMillis();
        Account account = new Account(uniqueAccountId, new BigDecimal("123.45"));
        this.accountsService.createAccount(account);
        this.mockMvc.perform(get("/v1/accounts/" + uniqueAccountId))
                .andExpect(status().isOk())
                .andExpect(
                        content().string("{\"accountId\":\"" + uniqueAccountId + "\",\"balance\":123.45}"));
    }

    //transfer
    @Test
    public void transferMoney() throws Exception {
        String id1 = "cuenta1";
        String id2 = "cuenta2";


        this.accountsService.transferMoney(id1, id2, 100.0f);
        this.mockMvc.perform(get("/v1/accounts/" + id1))
                .andExpect(status().isOk())
                .andExpect(
                        content().string("{\"accountId\":\"" + id1 + "\",\"balance\":0.0}"));
        this.mockMvc.perform(get("/v1/accounts/" + id2))
                .andExpect(status().isOk())
                .andExpect(
                        content().string("{\"accountId\":\"" + id2 + "\",\"balance\":110.0}"));
    }

    @Test
    public void transferMoneyEmptyAccountId() throws Exception {
        String id1 = "cuenta1";
        String id2 = "cuenta2";
        this.mockMvc.perform(post("/v1/accounts/transfer").contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .param("accountFrom", "")
                        .param("accountTo", id2)
                        .param("money", "100.0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void transferMoneyEmptyAccountIds() throws Exception {
        String id1 = "cuenta1";
        String id2 = "cuenta2";
        this.mockMvc.perform(post("/v1/accounts/transfer").contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .param("accountFrom", "")
                        .param("accountTo", "")
                        .param("money", "100.0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void transferMoneyEmptyAmount() throws Exception {
        String id1 = "cuenta1";
        String id2 = "cuenta2";
        this.mockMvc.perform(post("/v1/accounts/transfer").contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .param("accountFrom", id1)
                        .param("accountTo", id2)
                        .param("money", ""))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void transferMoneyIncorrectAccountIds() throws Exception {
        String id1 = "cuenta1";
        String id2 = "cuenta2";
        this.mockMvc.perform(post("/v1/accounts/transfer").contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .param("accountFrom", "asdasd")
                        .param("accountTo", "asd")
                        .param("money", "100.0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void transferMoneyIvalidAmount() throws Exception {
        String id1 = "cuenta1";
        String id2 = "cuenta2";
        this.mockMvc.perform(post("/v1/accounts/transfer")
                        .param("accountFrom", id1)
                        .param("accountTo", id2)
                        .param("money", "10000000.0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void transferMoneyNegativeAmount() throws Exception {
        String id1 = "cuenta1";
        String id2 = "cuenta2";
        this.mockMvc.perform(post("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountFrom\":\"" + id1 + "\", \"accountTo\":\"" + id2 + "\",\"money\":-1000}")).andExpect(status().isBadRequest());
    }
}
