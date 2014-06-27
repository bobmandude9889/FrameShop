package net.bobmandude9889.FrameShop;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;

public class FrameShop {
	public double buyPrice = 0;
	public double sellPrice = 0;
	public Location location = null;
	public int buyAmount;
	public int sellAmount;
	public Material material;
	public short durability;
	public BlockFace face;

	public FrameShop(double buyPrice, double sellPrice, Location location,
			int buyAmount, int sellAmount, Material material, short durability,
			BlockFace face) {
		this.buyPrice = buyPrice;
		this.sellPrice = sellPrice;
		this.location = location;
		this.buyAmount = buyAmount;
		this.sellAmount = sellAmount;
		this.material = material;
		this.durability = durability;
		this.face = face;
	}
}
