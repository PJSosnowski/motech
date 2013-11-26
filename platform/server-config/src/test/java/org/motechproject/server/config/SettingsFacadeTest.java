package org.motechproject.server.config;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.motechproject.config.service.ConfigurationService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class SettingsFacadeTest {

    private static final String BUNDLE_NAME = "org.motechproject.motech-module-bundle";
    private static final String FILENAME = "settings.properties";
    private static final String LANGUAGE_PROP = "system.language";
    private static final String LANGUAGE_VALUE = "en";
    private static final String TEST_PROP = "test";
    private static final String TEST_VAL = "test-val";
    private static final String MOCK_FILENAME = "testfile";
    private static final String MODULE_NAME = "module";

    SettingsFacade settingsFacade = new SettingsFacade();

    @Mock
    Properties props;

    @Mock
    ConfigurationService configurationService;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void testGetConfigNoService() {
        settingsFacade.setConfigurationService(null);

        setUpConfig();

        String result = settingsFacade.getProperty(LANGUAGE_PROP);

        assertEquals(LANGUAGE_VALUE, result);
    }

    @Test
    public void testGetConfigWithService() throws IOException {
        setUpOsgiEnv();
        when(configurationService.getModuleProperties(eq(BUNDLE_NAME), eq(FILENAME),
                any(Properties.class))).thenReturn(props);
        when(props.getProperty(LANGUAGE_PROP)).thenReturn(LANGUAGE_VALUE);
        when(props.containsKey(LANGUAGE_PROP)).thenReturn(true);
        setUpConfig();

        String result = settingsFacade.getProperty(LANGUAGE_PROP);

        assertEquals(LANGUAGE_VALUE, result);
        verify(configurationService).registersProperties(BUNDLE_NAME, FILENAME);
        verify(configurationService, times(2)).getModuleProperties(eq(BUNDLE_NAME), eq(FILENAME), any(Properties.class));
    }

    @Test
    public void testSetConfig() throws IOException {
        setUpOsgiEnv();
        when(configurationService.getModuleProperties(eq(BUNDLE_NAME), eq(FILENAME), any(Properties.class))).thenReturn(null);
        when(props.getProperty(LANGUAGE_PROP)).thenReturn(LANGUAGE_VALUE);
        when(props.containsKey(LANGUAGE_PROP)).thenReturn(true);
        setUpConfig();

        settingsFacade.afterPropertiesSet();

        ArgumentCaptor<Properties> argument = ArgumentCaptor.forClass(Properties.class);
        verify(configurationService).updateProperties(eq(BUNDLE_NAME), eq(FILENAME),
                any(Properties.class), argument.capture());
        assertEquals(LANGUAGE_VALUE, argument.getValue().getProperty(LANGUAGE_PROP));
    }

    @Test
    public void testAsProperties() {
        setUpConfig();
        Properties otherProps = new Properties();
        otherProps.put(TEST_PROP, TEST_VAL);
        settingsFacade.saveConfigProperties(MOCK_FILENAME, otherProps);
        settingsFacade.afterPropertiesSet();

        Properties result = settingsFacade.asProperties();

        assertEquals(2, result.size());
        assertEquals(TEST_VAL, result.get(TEST_PROP));
        assertEquals(LANGUAGE_VALUE, result.get(LANGUAGE_PROP));
    }

    private void setUpConfig() {
        List<Resource> configFiles = new ArrayList<>();
        configFiles.add(new ClassPathResource("settings.properties"));
        settingsFacade.setConfigFiles(configFiles);
    }

    private void setUpOsgiEnv() {
        settingsFacade.setModuleName(BUNDLE_NAME);
        settingsFacade.setConfigurationService(configurationService);
    }
}
