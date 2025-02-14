/*
 * Copyright 2013-2019 the HotswapAgent authors.
 *
 * This file is part of HotswapAgent.
 *
 * HotswapAgent is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 2 of the License, or (at your
 * option) any later version.
 *
 * HotswapAgent is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with HotswapAgent. If not, see http://www.gnu.org/licenses/.
 */
package org.hotswap.agent.plugin.mybatis;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.hotswap.agent.annotation.FileEvent;
import org.hotswap.agent.annotation.Init;
import org.hotswap.agent.annotation.OnResourceFileEvent;
import org.hotswap.agent.annotation.Plugin;
import org.hotswap.agent.command.Command;
import org.hotswap.agent.command.ReflectionCommand;
import org.hotswap.agent.command.Scheduler;
import org.hotswap.agent.config.PluginConfiguration;
import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.plugin.mybatis.transformers.MyBatisTransformers;

/**
 * Reload MyBatis configuration after entity create/change.
 *
 * @author Vladimir Dvorak
 */
@Plugin(name = "MyBatis",
        description = "Reload MyBatis configuration after configuration create/change.",
        testedVersions = {"All between 5.3.2"},
        expectedVersions = {"5.3.2" },
        supportClass = { MyBatisTransformers.class })
public class MyBatisPlugin {
    private static AgentLogger LOGGER = AgentLogger.getLogger(MyBatisPlugin.class);

    @Init
    Scheduler scheduler;

    @Init
    ClassLoader appClassLoader;

    Map<String, Object> configurationMap = new HashMap<>();

    Command reloadConfigurationCommand =
            new ReflectionCommand(this, MyBatisRefreshCommands.class.getName(), "reloadConfiguration");

    @Init
    public void init(PluginConfiguration pluginConfiguration) {
        LOGGER.info("MyBatis plugin initialized.");
    }

    public void registerConfigurationFile(String configFile, Object configObject) {
        if (configFile != null && !configurationMap.containsKey(configFile)) {
            configFile = configFile.replaceAll("\\\\", "/"); // check
            LOGGER.debug("MyBatisPlugin - configuration file registered : {}", configFile);
            configurationMap.put(configFile, configObject);
        }
    }

    @OnResourceFileEvent(path="/", filter = ".*.xml", events = {FileEvent.MODIFY})
    public void registerResourceListeners(URL url) {
        // if (configurationMap.containsKey(url.getPath())) {
        //     refresh(500);
        // }
        String urlPath = url.getPath();
        if (urlPath.startsWith("/")) {
            urlPath = urlPath.substring(1);   
        }
        for (String key : configurationMap.keySet()) {
        	if (key.indexOf(urlPath) != -1) {
        		refresh(500);
                break;
        	}
		}
    }

    // reload the configuration - schedule a command to run in the application classloader and merge
    // duplicate commands.
    private void refresh(int timeout) {
        scheduler.scheduleCommand(reloadConfigurationCommand, timeout);
    }

}
