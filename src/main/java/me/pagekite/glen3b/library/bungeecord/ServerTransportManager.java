package me.pagekite.glen3b.library.bungeecord;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import me.pagekite.glen3b.library.bukkit.Constants;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitRunnable;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

/**
 * This allows the sender to easily transport players between various servers on a BungeeCord network.
 * @author Glen Husman
 */
public final class ServerTransportManager implements PluginMessageListener {

	private Plugin _plugin;

	/**
	 * Represents a handler of a received result. This interface is intended to be used for results which may not be returned immediately. Default usages of this interface will remove the reference to the object after calling it once. This means you must re-register it to receive another notification of receiving data.
	 * @author Glen Husman
	 * @param <T> The type of the result.
	 */
	public static interface ResultReceived<T>{

		/**
		 * Called upon receiving the result of an operation.
		 * @param result The result.
		 */
		public void onReceive(T result);
	}

	private Map<String, List<ResultReceived<String[]>>> _playerListReceivers = new HashMap<String, List<ResultReceived<String[]>>>();

	/**
	 * Internal constructor. <b>Should not be called except by the GBukkitLib plugin instance.</b>
	 * @param plugin The GBukkitLib plugin instance.
	 */
	public ServerTransportManager(Plugin plugin){
		_plugin = plugin;

		plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, "BungeeCord");
		plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, "BungeeCord", this);
	}

	/**
	 * Send the specified player to the specified other server.
	 * @param player The player to send to another server.
	 * @param serverName The name of the target server on the BungeeCord network.
	 */
	public void sendPlayer(Player player, String serverName){
		Validate.notNull(player, "Player must not be null.");
		Validate.notEmpty(serverName, "The server name must not be empty.");

		ByteArrayDataOutput out = ByteStreams.newDataOutput();
		out.writeUTF("Connect");
		out.writeUTF(serverName);
		player.sendPluginMessage(_plugin, "BungeeCord", out.toByteArray());
	}

	/**
	 * Gets the array of players currently online on a server.
	 * @param serverName The name of the server.
	 * @param resultHandler The function to invoke upon receiving the result.
	 * @see ResultReceived
	 */
	public void getPlayers(String serverName, ResultReceived<String[]> resultHandler){
		Validate.notNull(resultHandler, "The result handler must not be null.");
		Validate.notEmpty(serverName, "The server name must not be empty.");

		if(!_playerListReceivers.containsKey(serverName.toLowerCase().trim())){
			_playerListReceivers.put(serverName.toLowerCase().trim(), new ArrayList<ResultReceived<String[]>>());
		}

		_playerListReceivers.get(serverName.toLowerCase().trim()).add(resultHandler);

		final ByteArrayDataOutput out = ByteStreams.newDataOutput();

		out.writeUTF("PlayerList");
		out.writeUTF(serverName);

		schedulePlayerTask(new PlayerTaskHandler(){

			@Override
			public void runTask(Player player) {
				player.sendPluginMessage(_plugin, "BungeeCord", out.toByteArray());
			}

		});
	}

	/**
	 * Interface allowing a task to be run upon a player signing on.
	 * @author Glen Husman
	 *
	 */
	public static interface PlayerTaskHandler{

		/**
		 * Run the task.
		 * @param player The player to use.
		 */
		public void runTask(Player player);
	}

	/**
	 * Used for sending messages to Bungee via random players.
	 */
	private static final Random _randomProvider = new Random();

	/**
	 * Schedule a player task if necessary, or run it immediately if possible.
	 * @param task The task to run.
	 */
	private void schedulePlayerTask(PlayerTaskHandler task){
		Validate.notNull(task);
		
		int onlineCount = Bukkit.getServer().getOnlinePlayers().length;

		if(onlineCount >= 1){
			// We have a player
			task.runTask(Bukkit.getServer().getOnlinePlayers()[_randomProvider.nextInt(onlineCount)]);
		}else{
			// Wait until sign-on
			new PlayerSignonWaiter(task).runTaskTimer(_plugin, Constants.TICKS_PER_SECOND, Constants.TICKS_PER_SECOND / 2);
		}
	}
	
	/**
	 * Used to wait for a player to sign on before sending data. Intended use: Wait for player signon to send/receive BungeeCord task/message.
	 * @author Glen Husman
	 */
	private static final class PlayerSignonWaiter extends BukkitRunnable{

		private PlayerTaskHandler _task;

		public PlayerSignonWaiter(PlayerTaskHandler task){
			_task = task;
		}

		@Override
		public void run() {
			int onlineCount = Bukkit.getServer().getOnlinePlayers().length;

			if(onlineCount >= 1){
				_task.runTask(Bukkit.getServer().getOnlinePlayers()[_randomProvider.nextInt(onlineCount)]);
				cancel();
			}
		}

	}


	// JavaDoc should be copied from interface
	@Override
	public void onPluginMessageReceived(String channel, Player receiver, byte[] message) {
		if (!channel.equals("BungeeCord")) {
			return;
		}

		DataInputStream in = new DataInputStream(new ByteArrayInputStream(message));

		try {
			String subchannel = in.readUTF();
			if (subchannel.equals("PlayerList")) {
				synchronized(_playerListReceivers){
					List<ResultReceived<String[]>> handlers = _playerListReceivers.get(in.readUTF().toLowerCase().trim());
					String[] playerList = in.readUTF().split(", ");
					Iterator<ResultReceived<String[]>> handlerIterator = handlers.iterator();
					while(handlerIterator.hasNext()){
						ResultReceived<String[]> handler = handlerIterator.next();
						handler.onReceive(playerList);
						handlerIterator.remove();
					}
				}
			}
		} catch (IOException e) {
			// There was an issue in creating the subchannel string
			// TODO: Handle it better
			e.printStackTrace();
		}
	}

}