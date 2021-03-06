/*
   This file is part of GBukkitCore.

    GBukkitCore is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GBukkitCore is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with GBukkitCore.  If not, see <http://www.gnu.org/licenses/>.
 */

package me.pagekite.glen3b.library.bukkit.datastore;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

/**
 * A service allowing plugins to register configuration files to automatically save.
 * @author Glen Husman
 */
public final class AutoSaverScheduler {

	private final class AutosavedConfig implements Runnable{
		public File _file;
		
		public FileConfiguration _config;
		public AutosavedConfig(File file, FileConfiguration cfg){
			_file = file;
			_config = cfg;
		}
		@Override
		public void run() {
			try {
				_config.save(_file);
				_plugin.getLogger().log(Level.FINE, "Autosaving configuration to " + _file.getName() + " in folder " + _file.getParent());
			} catch (IOException e) {
				_plugin.getLogger().log(Level.WARNING, "Autosaving configuration to " + _file.getName() + " in folder " + _file.getParent() + " FAILED!", e);
			}
		}
	}
	
	private Plugin _plugin;
	
	/**
	 * Internal constructor for service registration.
	 * <br/><br/>
	 * <b>This constructor should only be called by GBukkitCore to register this service.</b>
	 * 
	 * @param plugin The plugin to use to register autosave tasks.
	 */
	public AutoSaverScheduler(Plugin plugin){
		_plugin = plugin;
	}
	
	/**
	 * Gets the configuration path for the file name "config.yml" within a plugin's data directory.
	 * @param plugin The plugin who's configuration file's path should be retrieved.
	 * @return A new File instance which represents config.yml in the plugin. The name config.yml is hardcoded.
	 * @see #getConfigurationPath(Plugin, String)
	 */
	public File getConfigurationPath(Plugin plugin){
		return getConfigurationPath(plugin, "config.yml");
	}
	
	/**
	 * Gets the configuration path for the file name fileName within a plugin's data directory.
	 * @param plugin The plugin who's configuration file's path should be retrieved.
	 * @return A new File instance which represents fileName in the plugin.
	 * @see File#File(File, String)
	 * @see Plugin#getDataFolder()
	 */
	public File getConfigurationPath(Plugin plugin, String fileName){
		Validate.notNull(plugin, "The plugin must not be null.");
		Validate.notEmpty(fileName, "The file name must not be empty.");
		
		return new File(plugin.getDataFolder(), fileName);
	}
	
	/**
	 * Register a configuration file to automatically save.
	 * @param path The path of the configuration to save.
	 * @param config The configuration to save to the specified file.
	 * @param saveInterval The interval, in server ticks, between autosaves.
	 * @see #getConfigurationPath(Plugin)
	 */
	public void registerAutosave(File path, FileConfiguration config, long saveInterval){
		Validate.notNull(path, "The configuration file path must not be null.");
		Validate.notNull(config, "The configuration file object reference must not be null.");
		Validate.isTrue(saveInterval > 0, "The save interval must be at least one tick. Value: ", saveInterval);
		
		//Schedule the task
		AutosavedConfig exec = new AutosavedConfig(path, config);
		// TODO: Use async task?
		_taskIdsToExecutors.put(_plugin.getServer().getScheduler().runTaskTimer(_plugin, exec, saveInterval, saveInterval).getTaskId(), exec);
	}

	private Map<Integer, AutosavedConfig> _taskIdsToExecutors = new HashMap<Integer, AutosavedConfig>();
	
	/**
	 * Internally used method to save everything upon disabling. Should not be called except by the GBukkitCore plugin instance.
	 */
	public void onDisable() {
		for(Map.Entry<Integer, AutosavedConfig> task : _taskIdsToExecutors.entrySet()){
			Bukkit.getScheduler().cancelTask(task.getKey());
			task.getValue().run();
		}
	}
	
}
