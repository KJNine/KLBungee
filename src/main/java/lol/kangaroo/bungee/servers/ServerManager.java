package lol.kangaroo.bungee.servers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lol.kangaroo.bungee.KLBungeePlugin;
import lol.kangaroo.bungee.config.ConfigManager;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.config.Configuration;

public class ServerManager {
	
	private ConfigManager configManager;
	private ProxyServer proxy;
	
	public static final Pattern SERVER_NAME_PATTERN = Pattern.compile("^([a-z]{3,5})([0-9]{2})");

	public Map<ServerInfo, Boolean> serverStatus = new HashMap<>();
	public Map<Integer, Integer> serverPlayerCap = new HashMap<>();
	
	public ServerManager(ConfigManager configManager, ProxyServer proxy) {
		this.configManager = configManager;
		this.proxy = proxy;
		proxy.getScheduler().schedule(KLBungeePlugin.instance, () -> updateServerStatus(), 1L, 30L, TimeUnit.SECONDS);
	}
	
	public void updateServerStatus() {
		// Removes non-existent servers (Dynamic servers would cause this)
		for(ServerInfo serv : serverStatus.keySet()) {
			if(!proxy.getServers().containsValue(serv))
				serverStatus.remove(serv);
		}
		for(ServerInfo serv : proxy.getServers().values()) {
			serv.ping((ping, err) -> {
				if(err != null) {
					// Error means ping failed, offline.
					serverStatus.put(serv, false);
					serverPlayerCap.remove(getServerID(serv.getName()));
				} else {
					// No Error, online.
					serverStatus.put(serv, true);
					serverPlayerCap.put(getServerID(serv.getName()), ping.getPlayers().getMax());
				}
			});
		}
	}
	
	/**
	 * @throws IllegalArgumentException if name does not resolve to id.
	 */
	public int getServerID(String name) {
		Configuration servers = configManager.getConfig("settings").getSection("server-index");
		for(String s : servers.getKeys()) {
			if(servers.getString(s).equalsIgnoreCase(name)) {
				return Integer.valueOf(s);
			}
		}
		if(name.equalsIgnoreCase("network")) return 0;
		if(name.toLowerCase().startsWith("gs@")) return Integer.valueOf(name.substring(3));
		throw new IllegalArgumentException("invalid server name, server cannot proceed, so how about you don't fuck with server names");
	}

	/**
	 * if id does not resolve to a name, the name will show "gs@[id]"
	 */
	public String getServerName(int id) {
		Configuration servers = configManager.getConfig("settings").getSection("server-index");
		for(String s : servers.getKeys()) {
			if(s.equals(id + "")) {
				return servers.getString(s);
			}
		}
		if(id == 0) return "network";
		return "gs@" + id;
	}
	
	public ServerInfo getServerInfo(int id) {
		return proxy.getServerInfo(getServerName(id));
	}
	
	public ServerType getServerType(int id) {
		return ServerType.getFromID(id);
	}
	
	public boolean isGameServer(int id) {
		switch(getServerType(id)) {
		case HUB:
		case UNKNOWN:
			return false;
		default:
			return true;
		}
	}
	
	public boolean isHub(int id) {
		return getServerType(id).equals(ServerType.HUB);
	}
	
	/**
	 * excludes servers that are full or almost full.
	 * @return the ID of a hub which has an open slot and is next in balancing hubs.
	 */
	public int findAvailableHub() {
		// Prefer Hub 1 (or lowest available) up to 20 players, then start balancing.
		List<Integer> hubs = new ArrayList<>(getAllOnlineHubs());
		Iterator<Integer> hubIt = hubs.iterator();
		Collections.sort(hubs);
		Map<Integer, Integer> pcounts = new HashMap<>();
		while(hubIt.hasNext()) {
			int id = hubIt.next();
			int pcap = serverPlayerCap.containsKey(id) ? serverPlayerCap.get(id) : 0;
			ServerInfo serv = getServerInfo(id);
			if(serv.getPlayers().size() > pcap) {
				hubIt.remove();
			} else {
				pcounts.put(id, serv.getPlayers().size());
			}
		}
		int hub1id = hubs.get(0);
		int hub1pc = pcounts.get(hub1id);
		if(hub1pc < 20) return hubs.get(0); 
		else {
			// Hub 1 has 20 or more, find the lowest player count hub.
			int lowestId = hub1id;
			int lowestCount = hub1pc;
			for(int i : hubs) {
				if(pcounts.get(i) < lowestCount) {
					lowestId = i;
					lowestCount = pcounts.get(i);
				}
			}
			return lowestId;
		}
	}
	
	public boolean isServerFull(int id) {
		int pcap = serverPlayerCap.containsKey(id) ? serverPlayerCap.get(id) : 0;
		ServerInfo serv = getServerInfo(id);
		if(serv.getPlayers().size() > pcap) {
			return true;
		} else {
			return false;
		}
	}
	
	public boolean isServerOnline(int id) {
		ServerInfo serv = getServerInfo(id);
		if(serverStatus.containsKey(serv) && serverStatus.get(serv))
			return true;
		return false;
	}
	
	public Set<Integer> getAllOnlineServers() {
		Set<Integer> srv = new HashSet<>();
		for(ServerInfo serv : proxy.getServers().values()) {
			if(serverStatus.containsKey(serv) && serverStatus.get(serv))
				srv.add(getServerID(serv.getName()));
		}
		return srv;
	}
	
	public Set<Integer> getAllOnlineHubs() {
		Set<Integer> srv = new HashSet<>();
		for(ServerInfo serv : proxy.getServers().values()) {
			if(serverStatus.containsKey(serv) && serverStatus.get(serv)) {
				int servId = getServerID(serv.getName());
				if(ServerType.HUB.containsId(servId)) srv.add(servId);
			}
		}
		return srv;
	}
	
	public String formatServerName(int id) {
		if(id == 0) return "Network";
		String srvName = getServerName(id);
		Matcher m = SERVER_NAME_PATTERN.matcher(srvName);
		if(!m.matches()) throw new RuntimeException("Server name does not match pattern: " + srvName + " (id: " + id + ")");
		String fm = m.group(1).toUpperCase() + "-" + m.group(2);
		return fm;
	}
	
}
