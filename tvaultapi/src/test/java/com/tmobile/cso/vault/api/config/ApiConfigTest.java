package com.tmobile.cso.vault.api.config;

import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.model.AccessPolicy;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import com.tmobile.cso.vault.api.service.AccessService;
import com.tmobile.cso.vault.api.utils.JSONUtil;
import com.tmobile.cso.vault.api.utils.ThreadLocalContext;
import org.apache.logging.log4j.LogManager;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.cglib.core.CollectionUtils;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@ComponentScan(basePackages={"com.tmobile.cso.vault.api"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@PowerMockIgnore({"javax.management.*"})
public class ApiConfigTest {

    @InjectMocks
    ApiConfig apiConfig;

    public void setApiConfig() {
        apiConfig.setApiEndPoint("/health");
        apiConfig.setMethod("GET");

        List<String> params = new ArrayList<>();
        params.add("sealed");
        params.add("progress");

        apiConfig.setOutparams(params);
        apiConfig.setVaultEndPoint("https://localhost:8200/v1/sys/health");


        Param param = new Param();
        param.setName("serverip");
        param.setAppendToPath(true);
        param.setRequired(true);
        List<Param> paramList = new ArrayList<>();
        paramList.add(param);
        apiConfig.setParams(paramList);
    }
    @Test
    public void test_apiConfigSet() {
        setApiConfig();
        assertNotNull(apiConfig.toString());
    }

    @Test
    public void test_apiConfigGet() {
        setApiConfig();
        assertNotNull(apiConfig.getApiEndPoint());
        assertNotNull(apiConfig.getOutparams());
        assertNotNull(apiConfig.getMethod());
        assertNotNull(apiConfig.getParams());
        assertNotNull(apiConfig.getVaultEndPoint());
    }

}
