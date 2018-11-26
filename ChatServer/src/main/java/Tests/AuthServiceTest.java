package Tests;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import server.AuthService;

public class AuthServiceTest {
    
    @Before
    public void init() {
        AuthService.connect();
    }
    
    @Test
    public void test_isUserExist() {
        Assert.assertTrue(AuthService.isUserExist("loginExist"));
    }
    
    @Test
    public void test2_isUserExist() {
        Assert.assertFalse(AuthService.isUserExist("loginNotExist"));
    }
    
    @Test
    public void test_getNickByLoginAndPass() {
        Assert.assertEquals("TestAccount", AuthService.getNickByLoginAndPass("loginExist", "passExist", true));
    }
    
    @Test
    public void test2_getNickByLoginAndPass() {
        Assert.assertNull(AuthService.getNickByLoginAndPass("loginExist", "errorPass", true));
    }
    
    @Test
    public void test_updatePass() {
        Assert.assertTrue(AuthService.updatePass("loginExist", "passExist", "passExist"));
    }
    
    @Test
    public void test_updateNick() {
        Assert.assertTrue(AuthService.updateNick("loginExist", "passExist", "TestAccount"));
    }
    
    @After
    public void deinit() {
        AuthService.disconnect();
    }
}
