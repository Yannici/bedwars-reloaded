package io.github.bedwarsrel.BedwarsRel;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import de.inventivegames.hologram.Hologram;
import de.inventivegames.hologram.HologramAPI;
import de.inventivegames.hologram.touch.TouchAction;
import de.inventivegames.hologram.touch.TouchHandler;
import de.inventivegames.hologram.view.ViewHandler;
import io.github.bedwarsrel.BedwarsRel.Statistics.PlayerStatistic;
import io.github.bedwarsrel.BedwarsRel.Statistics.StatField;
import lombok.Getter;

public class HologramAPIInteraction implements IHologramInteraction {

  @Getter
  private ArrayList<Location> hologramLocations = null;

  @Getter
  private HashMap<Location, List<Hologram>> hologramSets = null;

  public void unloadHolograms() {
    if (Main.getInstance().isHologramsEnabled()) {
      for (Hologram hologram : HologramAPI.getHolograms()) {
        if (hologram.isSpawned()) {
          hologram.despawn();
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  public void loadHolograms() {
    if (!Main.getInstance().isHologramsEnabled()) {
      return;
    }

    if (this.hologramLocations != null) {
      this.unloadHolograms();
    }

    this.hologramLocations = new ArrayList<Location>();
    this.hologramSets = new HashMap<Location, List<Hologram>>();

    File file = new File(Main.getInstance().getDataFolder(), "holodb.yml");
    if (file.exists()) {
      YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
      List<Object> locations = (List<Object>) config.get("locations");
      for (Object location : locations) {
        Location loc = Utils.locationDeserialize(location);
        if (loc == null) {
          continue;
        }

        this.hologramLocations.add(loc);
      }
    }

    if (this.hologramLocations.size() == 0) {
      return;
    }

    for (Location holoLocation : this.hologramLocations) {
      this.createStatisticHologram(holoLocation);
    }
  }

  private void updateHologramDatabase() {
    try {
      // update hologram-database file
      File file = new File(Main.getInstance().getDataFolder(), "holodb.yml");
      YamlConfiguration config = new YamlConfiguration();
      List<Map<String, Object>> serializedLocations = new ArrayList<Map<String, Object>>();

      for (Location holoLocation : this.hologramLocations) {
        serializedLocations.add(Utils.locationSerialize(holoLocation));
      }

      if (!file.exists()) {
        file.createNewFile();
      }

      config.set("locations", serializedLocations);
      config.save(file);
    } catch (Exception ex) {
      Main.getInstance().getBugsnag().notify(ex);
      ex.printStackTrace();
    }
  }

  public void addHologramLocation(Location eyeLocation) {
    this.hologramLocations.add(eyeLocation);
    this.updateHologramDatabase();
  }

  private void onHologramTouch(final Player player, final Hologram touchedHologram) {
    if (!player.hasMetadata("bw-remove-holo")
        || (!player.isOp() && !player.hasPermission("bw.setup"))) {
      return;
    }

    player.removeMetadata("bw-remove-holo", Main.getInstance());
    Main.getInstance().getServer().getScheduler().runTask(Main.getInstance(), new Runnable() {

      @Override
      public void run() {
        // remove all player holograms on this location
        Location touchedSetLocation = null;
        for (Entry<Location, List<Hologram>> hologramSet : HologramAPIInteraction.this
            .getHologramSets().entrySet()) {
          for (Hologram hologram : hologramSet.getValue()) {
            if (hologram.equals(touchedHologram)) {
              touchedSetLocation = hologramSet.getKey();
              break;
            }
          }

          if (touchedSetLocation != null) {
            break;
          }
        }
        if (touchedSetLocation != null) {
          List<Hologram> touchedSetHolograms =
              HologramAPIInteraction.this.getHologramSets().get(touchedSetLocation);
          for (Hologram hologram : touchedSetHolograms) {
            hologram.despawn();
          }
          HologramAPIInteraction.this.getHologramSets().remove(touchedSetLocation);

          HologramAPIInteraction.this.hologramLocations.remove(touchedSetLocation);
          HologramAPIInteraction.this.updateHologramDatabase();
        }
        player.sendMessage(
            ChatWriter.pluginMessage(ChatColor.GREEN + Main._l("success.holoremoved")));
      }

    });

  }

  private void createStatisticHologram(Location holoLocation) {

    List<String> lines = new ArrayList<String>();
    List<Hologram> holograms = new ArrayList<Hologram>();
    final PlayerStatistic statistic = new PlayerStatistic();

    lines.add(ChatColor.translateAlternateColorCodes('&', Main.getInstance()
        .getStringConfig("holographic-stats.head-line", "Your &eBEDWARS&f stats")));

    for (StatField statField : statistic.getStatFields()) {
      lines.add(Main._l("stats." + statField.name()) + ": " + "%%" + statField.name() + "%%");
    }

    int currentLine = 0;
    while (currentLine < lines.size()) {
      Hologram holo = HologramAPI.createHologram(
          new Location(holoLocation.getWorld(), holoLocation.getX(),
              holoLocation.getY() - (currentLine * 0.3), holoLocation.getZ()),
          lines.get(currentLine));
      holo.addViewHandler(new ViewHandler() {

        @Override
        public String onView(Hologram hologram, Player player, String string) {
          PlayerStatistic playerStatistic =
              Main.getInstance().getPlayerStatisticManager().getStatistic(player);
          for (StatField statField : statistic.getStatFields()) {
            string = string.replace("%%" + statField.name() + "%%",
                playerStatistic.getValue(statField.name()).toString());
          }
          return string;
        }
      });
      holo.setTouchable(true);
      holo.addTouchHandler(new TouchHandler() {

        @Override
        public void onTouch(Hologram hologram, Player player, TouchAction action) {
          HologramAPIInteraction.this.onHologramTouch(player, hologram);
        }

      });
      holo.spawn();
      holograms.add(holo);
      currentLine++;
    }
    this.hologramSets.put(holoLocation, holograms);
  }

  @Override
  public String getType() {
    return "HologramAPI";
  }

  @Override
  public void updateHolograms(Player p) {}

  @Override
  public void updateHolograms(Player player, long l) {}

  @Override
  public void unloadAllHolograms(Player player) {}

  @Override
  public void updateHolograms() {}

}
