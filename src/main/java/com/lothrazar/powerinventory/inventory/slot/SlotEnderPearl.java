package com.lothrazar.powerinventory.inventory.slot;

import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class SlotEnderPearl extends Slot
{
	public static String background = "textures/items/empty_enderpearl.png";
	public static int posX;
	public static int posY;
	public int slotIndex;//overrides the private internal one
	
	public SlotEnderPearl(IInventory inventoryIn, int index) 
	{
		super(inventoryIn, index, posX, posY);
 
		slotIndex = index;
		
		//I TRIED THIS< it doesnt WORK
		// this.setBackgroundIconTexture( new ResourceLocation(Const.MODID, "textures/items/empty_enderpearl.png"));
	}
	
	@Override
	public int getSlotIndex()
    {
        return slotIndex;
    }
	
	@Override
	public boolean isItemValid(ItemStack stack)
    {
		return (stack != null && stack.getItem() == Items.ender_pearl);
    }
	
	@SuppressWarnings("deprecation")
	@Override
	public int getSlotStackLimit()
    {
        return Items.ender_pearl.getItemStackLimit();
    }
}
