package com.tmobile.cso.vault.api.config;

import com.tmobile.cso.vault.api.utils.JSONUtil;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.InjectMocks;
import org.mockito.cglib.core.CollectionUtils;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.context.annotation.ComponentScan;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNotNull;

@RunWith(PowerMockRunner.class)
@ComponentScan(basePackages={"com.tmobile.cso.vault.api"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@PowerMockIgnore({"javax.management.*"})
public class ParamTest {

    @InjectMocks
    Param param;

    public void setParam() {
        param.setName("serverip");
        param.setAppendToPath(true);
        param.setRequired(true);
        param.setValue("localhost");
    }
    @Test
    public void test_paramSet() {
        setParam();
        assertNotNull(param.toString());
    }

    @Test
    public void test_paramGet() {
        setParam();
        assertNotNull(param.getName());
        assertNotNull(param.getValue());
        assertNotNull(param.isAppendToPath());
        assertNotNull(param.isRequired());
    }

}
