package dev.dgteam.behaviorlog;

import cn.nukkit.Player;
import cn.nukkit.block.BlockPressurePlateBase;
import cn.nukkit.entity.Entity;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.block.BlockBreakEvent;
import cn.nukkit.event.block.BlockPistonEvent;
import cn.nukkit.event.block.BlockPlaceEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.event.entity.EntityDeathEvent;
import cn.nukkit.event.entity.EntityExplodeEvent;
import cn.nukkit.event.entity.ProjectileHitEvent;
import cn.nukkit.event.inventory.InventoryCloseEvent;
import cn.nukkit.event.inventory.InventoryOpenEvent;
import cn.nukkit.event.inventory.InventoryPickupItemEvent;
import cn.nukkit.event.inventory.InventoryTransactionEvent;
import cn.nukkit.event.player.*;
import cn.nukkit.level.Location;
import cn.nukkit.level.Position;
import cn.nukkit.plugin.PluginBase;
import com.csvreader.CsvWriter;
import org.joda.time.DateTime;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class BehaviorLog extends PluginBase implements Listener {
    private static final String logFolderPath = "./plugins/BehaviorLog/logs";
    public final ExecutorService executor = Executors.newFixedThreadPool(3);
    public static final LinkedBlockingQueue<BehaviorLogItem> queue = new LinkedBlockingQueue<>();
    public static BehaviorLog behaviorLog;
    public static String output_format = "log";
    public static boolean shutDown = true;
    @Override
    public void onEnable(){
        behaviorLog = this;
        File logFolder = new File(logFolderPath);
        if (!logFolder.exists()){
            logFolder.mkdirs();
        }
        saveDefaultConfig();
        if (getConfig().getString("output_format","log").equals("csv")){
            output_format = getConfig().getString("output_format","log");
        }

        executor.submit(() -> {
            logI("BehaviorLog thread started.");
            DateTime dateTime = new DateTime();
            File file = null;
           if (output_format.equals("csv")){
               file = new File(logFolderPath + File.separator + dateTime.toString("yyyy-MM-dd HH:mm:ss").replace(":","：") + ".csv");
               if (file.exists()){
                   logW("Log file is exists, please delete it or delete it before");
                   getServer().shutdown();
                   return;
               }
               try {
                   file.createNewFile();
               } catch (IOException e) {
                   throw new RuntimeException(e);
               }
               FileOutputStream ops;
               CsvWriter csvWriter = null;
               try {
                   ops = new FileOutputStream(file, true);
                   csvWriter =new CsvWriter(ops, ',', Charset.forName("GBK"));
                   String[] headers = new String[]{"time","object","position","behavior"};
                   csvWriter.writeRecord(headers);
                   while (shutDown){
                       BehaviorLogItem behaviorLogItem = queue.take();
                       csvWriter.writeRecord(new String[]{behaviorLogItem.getTime(),behaviorLogItem.getPlayer(),behaviorLogItem.getPosition(),behaviorLogItem.getBehavior()});
                       csvWriter.flush();
                   }

               } catch (Exception e) {
                   throw new RuntimeException(e);
               }finally {
                   if (csvWriter != null){
                       csvWriter.close();
                   }
               }

           }else {
              file = new File(logFolderPath + File.separator + dateTime.toString("yyyy-MM-dd HH:mm:ss").replace(":","：") + ".log");
               if (file.exists()){
                   logW("Log file is exists, please delete it or delete it before");
                   getServer().shutdown();
                   return;
               }
               try {
                   file.createNewFile();
               } catch (IOException e) {
                   throw new RuntimeException(e);
               }
               FileOutputStream ops = null;
               BufferedOutputStream bops = null;
               try {
                   ops = new FileOutputStream(file, true);
                   bops = new BufferedOutputStream(ops);
                   while (shutDown) {
                       BehaviorLogItem behaviorLogItem = queue.take();
                       String writeString = behaviorLogItem + "\n";
                       bops.write(writeString.getBytes(StandardCharsets.UTF_8));
                       bops.flush();
                   }
               } catch (Exception e) {
                   throw new RuntimeException(e);
               } finally {
                   if (ops != null) {
                       try {
                           ops.close();
                       } catch (IOException e) {
                           throw new RuntimeException(e);
                       }
                   }
                   if (bops != null) {
                       try {
                           bops.close();
                       } catch (IOException e) {
                           throw new RuntimeException(e);
                       }
                   }
               }
           }
        });
        getServer().getPluginManager().registerEvents(this,this);
        logI("BehaviorLog plugin was loaded.");
    }

    @Override
    public void onDisable(){
        shutDown =false;
        executor.shutdown();
    }

    public static void logI(String text){
        behaviorLog.getLogger().info(text);
    }

    public static void logW(String text){
        behaviorLog.getLogger().warning(text);
    }

   @EventHandler
   public void onPreJoin(PlayerPreLoginEvent e){
       Player player = e.getPlayer();
       DateTime dateTime = new DateTime();
       try {
           queue.put(new BehaviorLogItem(dateTime.toString("yyyy-MM-dd HH:mm:ss"),player.getName(),posToString(player.getPosition()),"prepare login server",null));
       } catch (InterruptedException ex) {
           throw new RuntimeException(ex);
       }
   }

    @EventHandler
    public void onJoin(PlayerJoinEvent e){
        Player player = e.getPlayer();
        DateTime dateTime = new DateTime();
        try {
            queue.put(new BehaviorLogItem(dateTime.toString("yyyy-MM-dd HH:mm:ss"),player.getName(),posToString(player.getPosition()),"joined server",null));
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    @EventHandler
    public void onJoin(PlayerQuitEvent e){
        Player player = e.getPlayer();
        DateTime dateTime = new DateTime();
        try {
            queue.put(new BehaviorLogItem(dateTime.toString("yyyy-MM-dd HH:mm:ss"),player.getName(),posToString(player.getPosition()),"quited server",null));
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent e){
        Entity entity = e.getEntity();
        DateTime dateTime = new DateTime();
        try {
            queue.put(new BehaviorLogItem(dateTime.toString("yyyy-MM-dd HH:mm:ss"),entity.getName(),posToString(entity.getPosition()),"died",null));
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e){
        Player player = e.getPlayer();
        DateTime dateTime = new DateTime();
        try {
            queue.put(new BehaviorLogItem(dateTime.toString("yyyy-MM-dd HH:mm:ss"),player.getName(),posToString(player.getPosition()),"sneaked",null));
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    @EventHandler
    public void onExplore(EntityExplodeEvent e){
        Entity entity = e.getEntity();
        DateTime dateTime = new DateTime();
        try {
            queue.put(new BehaviorLogItem(dateTime.toString("yyyy-MM-dd HH:mm:ss"),entity.getName(),posToString(entity.getPosition()),"exploded",null));
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e){
        Entity entity = e.getEntity();
        DateTime dateTime = new DateTime();
        try {
            queue.put(new BehaviorLogItem(dateTime.toString("yyyy-MM-dd HH:mm:ss"),entity.getName(),posToString(entity.getPosition()),"was damaged by " + e.getCause(),null));
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }


    @EventHandler
    public void onInteract(PlayerInteractEvent e){
        Player player = e.getPlayer();
        DateTime dateTime = new DateTime();
        if (e.getAction() == PlayerInteractEvent.Action.PHYSICAL){
            try {
                if (e.getBlock() == null ) return;
                if (!(e.getBlock() instanceof BlockPressurePlateBase)) return;
                queue.put(new BehaviorLogItem(dateTime.toString("yyyy-MM-dd HH:mm:ss"),player.getName(),posToString(player.getPosition()),"tread the " + e.getBlock().getName() + " block",null));
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        }
        try {
            if (e.getBlock() == null) return;
            queue.put(new BehaviorLogItem(dateTime.toString("yyyy-MM-dd HH:mm:ss"),player.getName(),posToString(player.getPosition()),"interacted the " + e.getBlock().getName() + " block",null));
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    @EventHandler
    public void onInventoryChange(InventoryTransactionEvent e){

    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e){
        Player player = e.getPlayer();
        DateTime dateTime = new DateTime();
        try {
            queue.put(new BehaviorLogItem(dateTime.toString("yyyy-MM-dd HH:mm:ss"),player.getName(),posToString(player.getPosition()),"respawned",null));
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e){
        Player player = e.getPlayer();
        DateTime dateTime = new DateTime();
        try {
            queue.put(new BehaviorLogItem(dateTime.toString("yyyy-MM-dd HH:mm:ss"),player.getName(),posToString(player.getPosition()),"breaked the "+e.getBlock().getName()+" block("+loacToString(e.getBlock().getLocation())+")",null));
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e){
        Player player = e.getPlayer();
        DateTime dateTime = new DateTime();
        try {
            queue.put(new BehaviorLogItem(dateTime.toString("yyyy-MM-dd HH:mm:ss"),player.getName(),posToString(player.getPosition()),"placed the "+e.getBlock().getName()+" block("+loacToString(e.getBlock().getLocation())+")",null));
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }



    @EventHandler
    public void onHit(ProjectileHitEvent e){
        Entity entity = e.getEntity();
        DateTime dateTime = new DateTime();
        try {
            queue.put(new BehaviorLogItem(dateTime.toString("yyyy-MM-dd HH:mm:ss"),entity.getName(),posToString(entity.getPosition()),"hit",null));
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    @EventHandler
    public void onPickItem(InventoryPickupItemEvent e){
        Player[] player = e.getViewers();
        DateTime dateTime = new DateTime();
        try {
            queue.put(new BehaviorLogItem(dateTime.toString("yyyy-MM-dd HH:mm:ss"), Arrays.stream(player).map(Player::getName).collect(Collectors.joining()), Arrays.stream(player).map(player1 -> posToString(player1.getPosition())).collect(Collectors.joining()),"picked the " + e.getItem().getName() + " item",null));
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    @EventHandler
    public void onDropItem(PlayerDropItemEvent e){
        Player player = e.getPlayer();
        DateTime dateTime = new DateTime();
        try {
            queue.put(new BehaviorLogItem(dateTime.toString("yyyy-MM-dd HH:mm:ss"), player.getName(), posToString(player.getPosition()) ,"drop the " + e.getItem().getName() + " item",null));
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    @EventHandler
    public void onEat(PlayerEatFoodEvent e){
        Player player = e.getPlayer();
        DateTime dateTime = new DateTime();
        try {
            queue.put(new BehaviorLogItem(dateTime.toString("yyyy-MM-dd HH:mm:ss"), player.getName(), posToString(player.getPosition()) ,"ate the food",null));
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    @EventHandler
    public void onPiston(BlockPistonEvent e){
        DateTime dateTime = new DateTime();
        try {
            queue.put(new BehaviorLogItem(dateTime.toString("yyyy-MM-dd HH:mm:ss"), "Block Piston", loacToString(e.getBlock().getLocation()) ,"was powered",null));
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    @EventHandler
    public void onOpenInventory(InventoryOpenEvent e){
        Player player = e.getPlayer();
        DateTime dateTime = new DateTime();
        try {
            queue.put(new BehaviorLogItem(dateTime.toString("yyyy-MM-dd HH:mm:ss"), player.getName(), posToString(player.getPosition()) ,"opened the " + e.getInventory().getName() + " inventory",null));
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    @EventHandler
    public void onCloseInventory(InventoryCloseEvent e){
        Player player = e.getPlayer();
        DateTime dateTime = new DateTime();
        try {
            queue.put(new BehaviorLogItem(dateTime.toString("yyyy-MM-dd HH:mm:ss"), player.getName(), posToString(player.getPosition()) ,"closed the " + e.getInventory().getName() + " inventory",null));
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }


    @EventHandler
    public void onCmd(PlayerCommandPreprocessEvent e){
        Player player = e.getPlayer();
        DateTime dateTime = new DateTime();
        try {
            queue.put(new BehaviorLogItem(dateTime.toString("yyyy-MM-dd HH:mm:ss"),player.getName(),posToString(player.getPosition()),"invoke the '" + e.getMessage() + "' command",null));
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }





    public String posToString(Position position){
        if (position != null){
            if (position.level != null){
                return Math.floor(position.x) + "," + Math.floor(position.y) + "," + Math.floor(position.z) + "," + position.level.getName();
            }
            return Math.floor(position.x) + "," + Math.floor(position.y) + "," + Math.floor(position.z);
        }
        return " ";
    }

     public String loacToString(Location location){
        if (location != null){
            if (location.level != null){
                return Math.floor(location.x) + "," + Math.floor(location.y) + "," + Math.floor(location.z) + " " + location.level.getName();
            }
            return Math.floor(location.x) + "," + Math.floor(location.y) + "," + Math.floor(location.z);
        }
        return " ";
    }



}