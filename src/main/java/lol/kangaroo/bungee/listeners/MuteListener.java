package lol.kangaroo.bungee.listeners;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

import lol.kangaroo.bungee.KLBungeePlugin;
import lol.kangaroo.bungee.player.PlayerManager;
import lol.kangaroo.bungee.player.punish.PunishManager;
import lol.kangaroo.bungee.util.Message;
import lol.kangaroo.common.player.CachedPlayer;
import lol.kangaroo.common.player.PlayerVariable;
import lol.kangaroo.common.player.punish.Mute;
import lol.kangaroo.common.player.punish.Punishment;
import lol.kangaroo.common.util.MSG;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class MuteListener implements Listener {

	private PlayerManager pm;
	private KLBungeePlugin pl;
	private PunishManager pum;
	
	public MuteListener(KLBungeePlugin pl) {
		this.pl = pl;
		this.pm = pl.getPlayerManager();
		this.pum = pl.getPunishManager();
	}
	
	/**
	 * Blocks any chats on the network.
	 * TODO change to be sync by caching mutes
	 */
	@EventHandler
	public void onChat(ChatEvent e) {
		if(!(e.getSender() instanceof ProxiedPlayer)) return;
		if(e.isCommand()) {
			boolean blocked = false;
			for(String s : pl.getConfigManager().getConfig("settings").getStringList("muted-blocked-commands"))
				if(e.getMessage().substring(1).equalsIgnoreCase(s)) blocked = true;
			if(!blocked) return;
		}
		ProxiedPlayer pp = (ProxiedPlayer) e.getSender();
		pl.getProxy().getScheduler().runAsync(pl, () -> {
			if(pum.isMuted(pp.getUniqueId())) {
				e.setCancelled(true);
				Mute mute = null;
				for(Punishment pun : pum.getActivePunishments(pp.getUniqueId()))
					if(pun instanceof Mute) mute = (Mute) pun;
				if(mute == null) return;
				if(mute.getDuration() != -1 && mute.getTimestamp() + mute.getDuration() < System.currentTimeMillis()) {
					pum.executeUnMute(mute, "Mute Expired", PunishManager.ZERO_UUID);
					e.setCancelled(false);
					return;
				}
				
				CachedPlayer p = pm.getCachedPlayer(mute.getUniqueId());
				CachedPlayer author = pm.getCachedPlayer(mute.getAuthor());
				// No prefixes to reduce chat clutter.
				// NICKNAME should only be used for at-the-moment things, such as chat
				// but not for this because someone could use it to detect the real name of the nicked person by checking it when they arent nicked then again when they are.
				String authorName = author != null ? (pl.getRankManager().getRank(author).getColor() + (String) author.getVariable(PlayerVariable.USERNAME)) : MSG.CONSOLE.getMessage(Locale.getDefault());
				String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("d MMM uuuu"));
				String durStr = MSG.TIMEFORMAT_PERMANENT.getMessage(p);
				String timeLeftStr = MSG.BANNED_TIMEPERMANENT.getMessage(p);
				if(mute.getDuration() != -1) {
					Duration dur = Duration.ofMillis(mute.getDuration());
					long years = dur.get(ChronoUnit.YEARS); dur.minus(years, ChronoUnit.YEARS);
					long days = dur.get(ChronoUnit.DAYS); dur.minusDays(days);
					long hours = dur.get(ChronoUnit.HOURS); dur.minusHours(hours);
					long minutes = dur.get(ChronoUnit.MINUTES);
					durStr = (years > 0 ? years + MSG.TIMEFORMAT_YEARS.getMessage(p) + ", " : "")
							+ (days > 0 ? days + MSG.TIMEFORMAT_DAYS.getMessage(p) + ", " : "")
							+ (hours > 0 ? hours + MSG.TIMEFORMAT_HOURS.getMessage(p) + ", " : "")
							+ minutes + MSG.TIMEFORMAT_MINUTES.getMessage(p);
					Duration tlDur = Duration.ofMillis((mute.getTimestamp() + mute.getDuration()) - System.currentTimeMillis());
					long tlyears = tlDur.get(ChronoUnit.YEARS); tlDur.minus(years, ChronoUnit.YEARS);
					long tldays = tlDur.get(ChronoUnit.DAYS); tlDur.minusDays(days);
					long tlhours = tlDur.get(ChronoUnit.HOURS); tlDur.minusHours(hours);
					long tlminutes = tlDur.get(ChronoUnit.MINUTES);
					timeLeftStr = (tlyears > 0 ? tlyears + MSG.TIMEFORMAT_YEARS.getMessage(p) + ", " : "")
							+ (tldays > 0 ? tldays + MSG.TIMEFORMAT_DAYS.getMessage(p) + ", " : "")
							+ (tlhours > 0 ? tlhours + MSG.TIMEFORMAT_HOURS.getMessage(p) + ", " : "")
							+ tlminutes + MSG.TIMEFORMAT_MINUTES.getMessage(p);
				}
				if(mute.getDuration() != -1)
					Message.sendMessage(p, MSG.MUTEMESSAGE_TEMPORARY, MSG.PUNISHMESSAGE_ARE.getMessage(p), durStr, authorName, mute.getReason(), date, timeLeftStr, MSG.APPEAL_URL.getMessage(p));
				else
					Message.sendMessage(p, MSG.MUTEMESSAGE_PERMANENT, MSG.PUNISHMESSAGE_ARE.getMessage(p), authorName, mute.getReason(), date, MSG.APPEAL_URL.getMessage(p));
			} 
		});
	}
	
}