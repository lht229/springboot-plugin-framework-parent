package com.github.codingdebugallday.plugin.framework.factory.process.pipe.bean;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.github.codingdebugallday.plugin.framework.annotation.ConfigDefinition;
import com.github.codingdebugallday.plugin.framework.factory.PluginInfoContainer;
import com.github.codingdebugallday.plugin.framework.factory.PluginRegistryInfo;
import com.github.codingdebugallday.plugin.framework.factory.process.pipe.PluginPipeProcessor;
import com.github.codingdebugallday.plugin.framework.factory.process.pipe.bean.configuration.ConfigurationParser;
import com.github.codingdebugallday.plugin.framework.factory.process.pipe.bean.configuration.PluginConfigDefinition;
import com.github.codingdebugallday.plugin.framework.factory.process.pipe.bean.configuration.YamlConfigurationParser;
import com.github.codingdebugallday.plugin.framework.factory.process.pipe.classs.group.ConfigDefinitionGroup;
import com.github.codingdebugallday.plugin.framework.integration.IntegrationConfiguration;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.util.StringUtils;

/**
 * <p>
 * 插件中配置文件bean的处理者 包括配置文件
 * </p>
 *
 * @author isaac 2020/6/16 14:05
 * @since 1.0
 */
public class ConfigFileBeanProcessor implements PluginPipeProcessor {

    private static final String KEY = "ConfigFileBeanProcessor";

    private final ConfigurationParser configurationParser;
    private final DefaultListableBeanFactory defaultListableBeanFactory;

    public ConfigFileBeanProcessor(ApplicationContext mainApplicationContext) {
        IntegrationConfiguration integrationConfiguration =
                mainApplicationContext.getBean(IntegrationConfiguration.class);
        this.configurationParser = new YamlConfigurationParser(integrationConfiguration);
        this.defaultListableBeanFactory = (DefaultListableBeanFactory)
                mainApplicationContext.getAutowireCapableBeanFactory();
    }


    @Override
    public void initialize() {
        // ignore
    }

    @Override
    public void register(PluginRegistryInfo pluginRegistryInfo) {
        List<Class<?>> configDefinitions =
                pluginRegistryInfo.getGroupClasses(ConfigDefinitionGroup.GROUP_ID);
        if (configDefinitions == null || configDefinitions.isEmpty()) {
            return;
        }
        String pluginId = pluginRegistryInfo.getPluginWrapper().getPluginId();
        Set<String> beanNames = new HashSet<>();
        for (Class<?> aClass : configDefinitions) {
            String beanName = registry(pluginRegistryInfo, aClass);
            if (!StringUtils.isEmpty(beanName)) {
                beanNames.add(beanName);
                PluginInfoContainer.addRegisterBeanName(pluginId, beanName);
            }
        }
        pluginRegistryInfo.addProcessorInfo(KEY, beanNames);
    }

    @Override
    public void unregister(PluginRegistryInfo pluginRegistryInfo) {
        Set<String> beanNames = pluginRegistryInfo.getProcessorInfo(KEY);
        if (beanNames == null) {
            return;
        }
        String pluginId = pluginRegistryInfo.getPluginWrapper().getPluginId();
        for (String beanName : beanNames) {
            if (defaultListableBeanFactory.containsSingleton(beanName)) {
                defaultListableBeanFactory.destroySingleton(beanName);
                PluginInfoContainer.removeRegisterBeanName(pluginId, beanName);
            }
        }
    }

    /**
     * 注册配置文件
     *
     * @param pluginRegistryInfo 插件注册的信息
     * @param aClass             配置文件类
     * @return 注册的bean名称
     */
    private String registry(PluginRegistryInfo pluginRegistryInfo, Class<?> aClass) {
        ConfigDefinition configDefinition = aClass.getAnnotation(ConfigDefinition.class);
        if (configDefinition == null) {
            return null;
        }
        String fileName = configDefinition.value();
        if (StringUtils.isEmpty(fileName)) {
            throw new IllegalArgumentException(aClass.getName() + " configDefinition value is null");
        }
        PluginConfigDefinition pluginConfigDefinition =
                new PluginConfigDefinition(fileName, aClass);
        Object parseObject = configurationParser.parse(pluginRegistryInfo.getBasePlugin(),
                pluginConfigDefinition);
        String name = configDefinition.name();
        if (StringUtils.isEmpty(name)) {
            name = aClass.getName();
        }
        if (!defaultListableBeanFactory.containsSingleton(name)) {
            defaultListableBeanFactory.registerSingleton(name, parseObject);
        }
        return name;
    }

}
