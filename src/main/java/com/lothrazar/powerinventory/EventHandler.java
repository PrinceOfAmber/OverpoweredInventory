package com.lothrazar.powerinventory;
 
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.StatCollector;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.GuiScreenEvent.InitGuiEvent;
import net.minecraftforge.event.entity.EntityEvent.EntityConstructing;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.world.WorldEvent;

import org.apache.logging.log4j.Level;

import com.lothrazar.powerinventory.inventory.client.GuiButtonOpenInventory; 
import com.lothrazar.powerinventory.net.EnderChestPacket;
import com.lothrazar.powerinventory.net.EnderPearlPacket;
import com.lothrazar.powerinventory.net.HotbarSwapPacket;
import com.lothrazar.powerinventory.proxy.ClientProxy;

import net.minecraftforge.fml.client.event.ConfigChangedEvent.OnConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
/**
 * @author https://github.com/Funwayguy/InfiniteInvo
 * @author Forked and altered by https://github.com/PrinceOfAmber/InfiniteInvo
 */
public class EventHandler
{
	public static File worldDir;
	public static HashMap<String, Integer> unlockCache = new HashMap<String, Integer>();
	public static HashMap<String, Container> lastOpened = new HashMap<String, Container>();
	
	@SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) 
    {   
        if(ClientProxy.keyEnderpearl.isPressed() )
        { 	     
        	 ModInv.instance.network.sendToServer( new EnderPearlPacket());   
        }  
        if(ClientProxy.keyEnderchest.isPressed())
        { 	      
        	 ModInv.instance.network.sendToServer( new EnderChestPacket());   
        }  
        if(ClientProxy.keyHotbar.isPressed())
        { 	      
        	 ModInv.instance.network.sendToServer( new HotbarSwapPacket());   
        }   
    }
	
	@SubscribeEvent
	public void onConfigChanged(OnConfigChangedEvent event) 
	{
		if (event.modID.equals(Const.MODID)) ModConfig.syncConfig();
	}

	@SubscribeEvent
	public void onEntityConstruct(EntityConstructing event) // More reliable than on entity join
	{
		if(event.entity instanceof EntityPlayer)
		{
			EntityPlayer player = (EntityPlayer)event.entity;
			
			if(InventoryPersistProperty.get(player) == null)
			{
				InventoryPersistProperty.register(player);
			}
		}
	}
	
	@SubscribeEvent
	public void onEntityJoinWorld(EntityJoinWorldEvent event)
	{
		if(event.entity instanceof EntityPlayer)
		{
			EntityPlayer player = (EntityPlayer)event.entity;
			
			if(InventoryPersistProperty.get(player) != null)
			{
				InventoryPersistProperty.get(player).onJoinWorld();
			} 
		}
	}
 
	@SubscribeEvent
	public void onEntityDeath(LivingDeathEvent event)
	{
		if(event.entityLiving instanceof EntityPlayer)
		{
			if(!event.entityLiving.worldObj.isRemote && event.entityLiving.worldObj.getGameRules().getGameRuleBooleanValue("keepInventory"))
			{
				InventoryPersistProperty.keepInvoCache.put(event.entityLiving.getUniqueID(), ((EntityPlayer)event.entityLiving).inventory.writeToNBT(new NBTTagList()));
			}
		}
	}

	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	public void onGuiPostInit(InitGuiEvent.Post event)
	{
		if(event.gui == null){return;}//probably doesnt ever happen
		 
		int button_id = 256;
		//trapped, regular chests, minecart chests, and enderchest all use this class
		//which extends  GuiContainer

		int padding = 10;
		
		int x,y = padding,w = 20,h = w;

		if(event.gui instanceof net.minecraft.client.gui.inventory.GuiInventory)
		{
			x = Minecraft.getMinecraft().displayWidth/2 - w - padding;//align to right side

			event.buttonList.add(new GuiButtonOpenInventory(button_id++, x,y,w,h,StatCollector.translateToLocal("button.compat")));
		}
	}
	
	@SubscribeEvent
	public void onWorldLoad(WorldEvent.Load event)
	{
		if(!event.world.isRemote && worldDir == null && MinecraftServer.getServer().isServerRunning())
		{
			MinecraftServer server = MinecraftServer.getServer();
			
			if(ModInv.proxy.isClient())
			{
				worldDir = server.getFile("saves/" + server.getFolderName());
			} 
			else
			{
				worldDir = server.getFile(server.getFolderName());
			}

			new File(worldDir, "data/").mkdirs();
			LoadCache(new File(worldDir, "data/SlotUnlockCache"));
		}
	}
	
	@SubscribeEvent
	public void onWorldSave(WorldEvent.Save event)
	{
		if(!event.world.isRemote && worldDir != null && MinecraftServer.getServer().isServerRunning())
		{
			new File(worldDir, "data/").mkdirs();
			SaveCache(new File(worldDir, "data/SlotUnlockCache"));
		}
	}
	
	@SubscribeEvent
	public void onWorldUnload(WorldEvent.Unload event)
	{
		if(!event.world.isRemote && worldDir != null && !MinecraftServer.getServer().isServerRunning())
		{
			new File(worldDir, "data/").mkdirs();
			SaveCache(new File(worldDir, "data/SlotUnlockCache"));
			
			worldDir = null;
			unlockCache.clear();
			InventoryPersistProperty.keepInvoCache.clear();
		}
	}
	
	public static void SaveCache(File file)
	{
		try
		{
			if(!file.exists())
			{
				file.createNewFile();
			}
			
			FileOutputStream fos = new FileOutputStream(file);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			
			oos.writeObject(unlockCache);
			
			oos.close();
			fos.close();
		} 
		catch(Exception e)
		{
			ModInv.logger.log(Level.ERROR, "Failed to save slot unlock cache", e);
		}
	}
	
	@SuppressWarnings("unchecked")
	public static void LoadCache(File file)
	{
		try
		{
			if(!file.exists())
			{
				file.createNewFile();
			}
			
			FileInputStream fis = new FileInputStream(file);
			
			if(fis.available() <= 0)
			{
				fis.close();
				return;
			}
			
			ObjectInputStream ois = new ObjectInputStream(fis);
			
			unlockCache = (HashMap<String,Integer>)ois.readObject();
			
			ois.close();
			fis.close();
		} catch(Exception e)
		{
			ModInv.logger.log(Level.ERROR, "Failed to load slot unlock cache", e);
		}
	}

	@SideOnly(Side.CLIENT)
	@SubscribeEvent//(priority = EventPriority.NORMAL)
    public void onRenderOverlay(RenderGameOverlayEvent event)
    {
		//force it to always show food - otherwise its hidden when riding a horse
		
		if(ModConfig.alwaysShowHungerbar)
		{
			GuiIngameForge.renderFood = true;
		}
    } 
	
}
