package com.tmobile.cso.vault.api.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tmobile.cso.vault.api.config.ApiConfig;
import com.tmobile.cso.vault.api.config.Param;
import com.tmobile.cso.vault.api.model.UserLogin;
import com.tmobile.cso.vault.api.model.UserpassUser;
import org.apache.catalina.User;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.context.annotation.ComponentScan;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@ComponentScan(basePackages={"com.tmobile.cso.vault.api"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@PowerMockIgnore({"javax.management.*"})
public class JSONUtilTest {

    @InjectMocks
    JSONUtil jsonUtil;

    @Test
    public void test_getJSON() {
        String str = "sample";
        assertEquals("\"sample\"", JSONUtil.getJSON(str));
    }

    @Test
    public void test_getJSONasDefaultPrettyPrint() {
        String str = "sample";
        assertEquals("\"sample\"", JSONUtil.getJSONasDefaultPrettyPrint(str));
    }

    @Test
    public void test_getJSONasDefaultPrettyPrintFromString() {
        String str = "true";
        assertEquals("true", JSONUtil.getJSONasDefaultPrettyPrintFromString(str));
    }

    @Test
    public void test_getObj() throws IOException {
        UserLogin user = new UserLogin();
        user.setUsername("testuser");
        String jsonStr = "{\"username\":\"testuser\",\"password\":null}";
        UserLogin userResponse = (UserLogin)JSONUtil.getObj(jsonStr, UserLogin.class);
        assertEquals(user.getUsername(), userResponse.getUsername());
    }

}
