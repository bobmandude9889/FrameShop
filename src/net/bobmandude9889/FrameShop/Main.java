package net.bobmandude9889.FrameShop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin implements Listener {

	List<FrameShop> shops = new ArrayList<FrameShop>();
	HashMap<Player, FrameShop> creating = new HashMap<Player, FrameShop>();
	List<Player> removing = new ArrayList<Player>();
	Economy econ = null;

	public void onEnable() {
		getServer().getPluginManager().registerEvents(this, this);
		setupEconomy();
		loadConfig();
		for (FrameShop shop : shops) {
			World world = shop.location.getWorld();
			ItemFrame itf = (ItemFrame) world.spawn(shop.location, ItemFrame.class);
	        itf.setFacingDirection(shop.face);
	        HangingPlaceEvent hEvent = new HangingPlaceEvent(itf, getServer().getOnlinePlayers()[0], shop.location.getBlock(), shop.face.getOppositeFace());
	        getServer().getPluginManager().callEvent(hEvent);;
			ItemStack is = new ItemStack(shop.material);
			is.setDurability(shop.durability);
			ItemMeta im = is.getItemMeta();
			String buy = "Buy " + shop.buyAmount + " for $" + shop.buyPrice;
			if (shop.buyPrice == 0)
				buy = "Cannot buy";
			String sell = "Sell " + shop.sellAmount + " $" + shop.sellPrice;
			if (shop.sellPrice == 0)
				sell = "Cannot sell";
			im.setDisplayName(buy + " / " + sell);
			is.setItemMeta(im);
			itf.setItem(is);
		}
	}

	public boolean onCommand(CommandSender sender, Command cmd,
			String commandLabel, String[] args) {
		if (!(sender instanceof Player)) {
			return false;
		}
		Player player = (Player) sender;
		if (cmd.getLabel().equalsIgnoreCase("fs")) {
			if (args.length > 0) {
				if (args[0].equalsIgnoreCase("create")) {
					if (player.hasPermission("fs.use")) {
						if (args.length < 5) {
							player.sendMessage(ChatColor.RED
									+ "/fs create <Buy Amount> <Buy Price> <Sell Amount> <Sell Price>");
							return true;
						}
						double bPrice = 0;
						double sPrice = 0;
						int bAmount = 0;
						int sAmount = 0;
						try {
							bPrice = Double.parseDouble(args[2]);
						} catch (Exception e) {
							player.sendMessage("Please enter a number for you buy price");
							return true;
						}
						try {
							sPrice = Double.parseDouble(args[4]);
						} catch (Exception e) {
							player.sendMessage("Please enter a number for you sell price");
							return true;
						}
						try {
							bAmount = Integer.parseInt(args[1]);
						} catch (Exception e) {
							player.sendMessage("Please enter a number for the buy amount");
							return true;
						}
						try {
							sAmount = Integer.parseInt(args[3]);
						} catch (Exception e) {
							player.sendMessage("Please enter a number for the sell amount");
							return true;
						}
						creating.put(player, new FrameShop(bPrice, sPrice,
								null, bAmount, sAmount, null, (short) 0, null));
						player.sendMessage(ChatColor.GREEN
								+ "Punch the item frame you want to set as a shop!");
					} else {
						player.sendMessage(ChatColor.RED
								+ "You do not have permission to do that!");
					}
					return true;
				} else if (args[0].equalsIgnoreCase("remove")) {
					if (player.hasPermission("fs.remove")) {
						removing.add(player);
						player.sendMessage(ChatColor.GREEN
								+ "Punch the item frame to remove the shop from it!");
					} else {
						player.sendMessage(ChatColor.RED
								+ "You do not have permission to do that!");
					}
					return true;
				} else {
					player.sendMessage(ChatColor.RED + "Unknown arguments!");
				}
			} else {
				return false;
			}
		}
		return false;
	}

	@EventHandler
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getEntityType().equals(EntityType.ITEM_FRAME)) {
			Player player = (Player) e.getDamager();
			if (creating.containsKey(player)) {
				e.setCancelled(true);
				if (isShop(e.getEntity())) {
					player.sendMessage(ChatColor.RED
							+ "That is already a shop!");
				} else {
					FrameShop fs = creating.get(player);
					fs.location = e.getEntity().getLocation();
					ItemFrame itf = (ItemFrame) e.getEntity();
					ItemStack item = ((ItemFrame) e.getEntity()).getItem();
					fs.material = item.getType();
					fs.durability = item.getDurability();
					fs.face = itf.getFacing();
					shops.add(fs);
					ItemFrame itF = (ItemFrame) e.getEntity();
					ItemStack is = itF.getItem();
					ItemMeta im = is.getItemMeta();
					String buy = "Buy " + fs.buyAmount + " for $" + fs.buyPrice;
					if (fs.buyPrice == 0)
						buy = "Cannot buy";
					String sell = "Sell " + fs.sellAmount + " $" + fs.sellPrice;
					if (fs.sellPrice == 0)
						sell = "Cannot sell";
					im.setDisplayName(buy + " / " + sell);
					is.setItemMeta(im);
					itF.setItem(is);
					creating.remove(player);
					player.sendMessage(ChatColor.GREEN + "Created shop!");
				}
				updateConfig();
				reloadConfig();
			} else if (removing.contains(player)) {
				if (isShop(e.getEntity())) {
					shops.remove(getShop(e.getEntity()));
					removing.remove(player);
					ItemFrame itF = (ItemFrame) e.getEntity();
					ItemStack is = itF.getItem();
					is.setItemMeta(null);
					itF.setItem(is);
					player.sendMessage(ChatColor.GREEN + "Removed shop");
					e.setCancelled(true);
					updateConfig();
					reloadConfig();
				}
			} else if (player.hasPermission("fs.use") && isShop(e.getEntity())) {
				e.setCancelled(true);
				if (getShop(e.getEntity()).buyPrice != 0) {
					ItemFrame itF = (ItemFrame) e.getEntity();
					FrameShop fs = getShop(itF);
					if (econ.getBalance(player) >= fs.buyPrice) {
						econ.withdrawPlayer(player, fs.buyPrice);
						ItemStack is = new ItemStack(itF.getItem().getType(),
								fs.buyAmount);
						is.setDurability(itF.getItem().getDurability());
						player.sendMessage(ChatColor.GREEN + "You bought "
								+ fs.buyAmount + " " + is.getType().name()
								+ " for $" + fs.buyPrice);
						is.setItemMeta(null);
						player.getInventory().addItem(is);
					} else {
						player.sendMessage(ChatColor.RED
								+ "You do not have enough money to buy that!");
					}
				} else {
					player.sendMessage(ChatColor.RED
							+ "You cannot buy that item!");
				}
			}
		}
	}

	@EventHandler
	public void onEntityInteractEntity(PlayerInteractEntityEvent e) {
		if (e.getRightClicked().getType().equals(EntityType.ITEM_FRAME)) {
			Player player = e.getPlayer();
			ItemFrame itf = (ItemFrame) e.getRightClicked();
			if (player.hasPermission("fs.use") && isShop(e.getRightClicked())
					&& !itf.getItem().getType().equals(Material.AIR)) {
				e.setCancelled(true);
				ItemFrame itF = (ItemFrame) e.getRightClicked();
				FrameShop fs = getShop(e.getRightClicked());
				if (fs.sellPrice != 0) {
					if (player.getInventory().getItemInHand().getType()
							.equals(itF.getItem().getType())
							&& player.getInventory().getItemInHand()
									.getDurability() == itF.getItem()
									.getDurability()
							&& player.getInventory().getItemInHand()
									.getAmount() >= fs.sellAmount) {
						if (player.getInventory().getItemInHand().getAmount() > fs.sellAmount) {
							ItemStack hand = player.getItemInHand();
							hand.setAmount(player.getItemInHand().getAmount()
									- fs.sellAmount);
						} else {
							player.getInventory().setItemInHand(
									new ItemStack(Material.AIR));
						}
						econ.depositPlayer(player, fs.sellPrice);
						player.sendMessage(ChatColor.GREEN + "You sold "
								+ fs.sellAmount + " "
								+ itF.getItem().getType().name() + " for $"
								+ fs.sellPrice);
					} else {
						player.sendMessage(ChatColor.RED
								+ "Make sure you are holding the item you are selling!");
					}
				} else {
					player.sendMessage(ChatColor.RED
							+ "You can not sell that item!");
				}
			}
		}
	}

	public void updateConfig() {
		List<String> stringShops = new ArrayList<String>();
		for (int i = 0; i < shops.size(); i++) {
			FrameShop shop = shops.get(i);
			stringShops.add(shop.location.getWorld().getName() + ","
					+ shop.location.getBlockX() + ","
					+ shop.location.getBlockY() + ","
					+ shop.location.getBlockZ() + "," + shop.buyAmount + ","
					+ shop.buyPrice + "," + shop.sellAmount + ","
					+ shop.sellPrice + "," + shop.material.toString() + ","
					+ shop.durability + "," + shop.face.toString());
		}
		getConfig().set("shops", null);
		getConfig().set("shops", stringShops);
		saveConfig();
		loadConfig();
	}

	public void loadConfig() {
		shops = new ArrayList<FrameShop>();
		List<String> stringShops = getConfig().getStringList("shops");
		for (int i = 0; i < stringShops.size(); i++) {
			String[] split = stringShops.get(i).split(",");
			World world = getServer().getWorld(split[0]);
			Material material = Material.getMaterial(split[8]);
			BlockFace face = BlockFace.valueOf(split[10]);
			double x = 0;
			double y = 0;
			double z = 0;
			int buyAmount = 0;
			double buyPrice = 0;
			int sellAmount = 0;
			double sellPrice = 0;
			int durability = 0;
			try {
				x = Double.parseDouble(split[1]);
				y = Double.parseDouble(split[2]);
				z = Double.parseDouble(split[3]);
				buyAmount = Integer.parseInt(split[4]);
				buyPrice = Double.parseDouble(split[5]);
				sellAmount = Integer.parseInt(split[6]);
				sellPrice = Double.parseDouble(split[7]);
				durability = Integer.parseInt(split[9]);
			} catch (Exception e) {
				e.printStackTrace();
			}
			FrameShop fs = new FrameShop(buyPrice, sellPrice, new Location(
					world, x, y, z), buyAmount, sellAmount, material,
					(short) durability, face);
			shops.add(fs);
		}
	}

	public boolean isShop(Entity entity) {
		return shops.contains(getShop(entity));
	}

	public Location getBlockLocation(Location location) {
		return new Location(location.getWorld(), location.getBlockX(),
				location.getBlockY(), location.getBlockZ());
	}

	private boolean setupEconomy() {
		RegisteredServiceProvider<Economy> economyProvider = getServer()
				.getServicesManager().getRegistration(
						net.milkbowl.vault.economy.Economy.class);
		if (economyProvider != null) {
			econ = economyProvider.getProvider();
		}

		return (econ != null);
	}

	public FrameShop getShop(Entity e) {
		for (int i = 0; i < shops.size(); i++) {
			if (shops.get(i).location.equals(getBlockLocation(e.getLocation())))
				return shops.get(i);
		}
		return null;
	}

}
