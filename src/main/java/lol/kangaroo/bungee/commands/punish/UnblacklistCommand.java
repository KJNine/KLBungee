package lol.kangaroo.bungee.commands.punish;

import java.util.UUID;

import lol.kangaroo.bungee.KLBungeePlugin;
import lol.kangaroo.bungee.commands.CommandExecutor;
import lol.kangaroo.bungee.player.PlayerManager;
import lol.kangaroo.bungee.util.Message;
import lol.kangaroo.bungee.util.PluginMessage;
import lol.kangaroo.common.permissions.Rank;
import lol.kangaroo.common.player.BasePlayer;
import lol.kangaroo.common.player.CachedPlayer;
import lol.kangaroo.common.util.MSG;
import lol.kangaroo.common.util.MessageWrapper;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class UnblacklistCommand extends CommandExecutor {

	public UnblacklistCommand(PlayerManager pm, ProxyServer proxy) {
		super(pm, proxy, "unblacklist", Rank.SRMOD.getPerm(), "sunblacklist", "unbl", "sunbl");
	}

	@Override
	public void execute(ProxiedPlayer sender, BasePlayer bp, String label, String[] args) {
		if(args.length == 0){
			Message.sendMessage(sender, MSG.PREFIX_ERROR, MSG.COMMAND_UNBLACKLIST_USAGE);
			return;
		}
		proxy.getScheduler().runAsync(KLBungeePlugin.instance, () -> {
			UUID uuid = pm.getFromAny(args[0]);
			CachedPlayer cp = pm.getCachedPlayer(uuid);
			if(args.length == 1) {
				PluginMessage.sendToSpigot(sender, "CommandGUI", new MessageWrapper("unblacklist").writeUuid(uuid));
				return;
			}
			String reason = "No Reason Specified";
			reason = "";
			for(int i = 1; i < args.length; i++) {
				reason += args[i] + " ";
			}
			reason = reason.trim();
			if(!pm.unblacklistPlayer(cp, reason, bp, (label.equalsIgnoreCase("sunblacklist") || label.equalsIgnoreCase("sunbl") ? true : false)))
				Message.sendMessage(sender, MSG.PREFIX_ERROR, MSG.COMMAND_UNBLACKLIST_ALREADY);
		});
	}

	@Override
	public void executeConsole(String label, String[] args) {
		if(args.length <= 1){
			Message.sendConsole(MSG.PREFIX_ERROR, MSG.COMMAND_UNBLACKLIST_USAGE);
			return;
		}
		proxy.getScheduler().runAsync(KLBungeePlugin.instance, () -> {
			UUID uuid = pm.getFromAny(args[0]);
			CachedPlayer cp = pm.getCachedPlayer(uuid);
			String reason = "No Reason Specified";
			reason = "";
			for(int i = 1; i < args.length; i++) {
				reason += args[i] + " ";
			}
			reason = reason.trim();
			if(!pm.unblacklistPlayer(cp, reason, null, (label.equalsIgnoreCase("sunblacklist") || label.equalsIgnoreCase("sunbl") ? true : false)))
				Message.sendConsole(MSG.PREFIX_ERROR, MSG.COMMAND_UNBLACKLIST_ALREADY);
		});
	}
	
}
