# MinecraftPicocli

This library allows you to use the [PicoCLI](https://picocli.info/) command framework with [Minecraft Forge](https://files.minecraftforge.net/).

## Installation

Add this to your dependencies in `build.gradle`:

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}
dependencies {
    compile 'com.github.Rubydesic:MinecraftPicocli:master-SNAPSHOT'
}
```

## First Command

To start, make a PicoCLI command. For example, a very basic echo command might look like this:

```java
@Command(name = "command", aliases = {"cmd", "mycommand"})
public class MyCommand implements Runnable {
    
    @Inject
    ICommandSender sender;
    
    @Parameters(index = "0")
    String echo;
    
    @Override
    public void run() {
        sender.sendMessage(new TextComponentString("You said " + echo));
    }
    
}
```

Now, simply register your command as you would any other using the `FMLServerStartingEvent`

```java
@EventHandler
public void onServerStart(FMLServerStartingEvent event) {
    ServerCommandManager manager = 
        (ServerCommandManager) event.getServer().getCommandManager();
    
    manager.registerCommand(new PicocliCommandBase<>(MyCommand.class));
}
```

Try out your command:

```
/command "hello there, buddy"
> You said hello there buddy
```

## Autocompletion

Let's say you want to autocomplete a player name. First, you'll need an autocompletion class that implements `Iterable<String>`:

```java
public class AutocompleterPlayername implements Iterable<String> {
    
    @Inject
    ICommandSender sender;
    
    @Override
    public Iterator<String> iterator() {
        return sender.playerEntities // List<EntityPlayer>
            .stream() // Stream<EntityPlayer>
            .map(EntityPlayer::getName) // Stream<String>
            .iterator(); // Iterator<String>
    }
    
}
```

Perfect. Here's how you use this autocompletion class in your command so that the `playerName` parameter is autocompleted:

```java
@Command(name = "isplayeronground")
public class MyCommand implements Runnable {
    
    @Inject
    ICommandSender sender;
    
    @Parameters(index = "0", completionCandidates = AutocompleterPlayername.class)
    String playerName;
    
    @Override
    public void run() {
        EntityPlayer player = sender.getEntityWorld().getPlayerEntityByName(playerName);
        
        sender.sendMessage(new TextComponentString(player.onGround ? "Yes" : "No"));
    }
    
}
```

Try it out:

```
/isplayeronground Rub<tab>
/isplayeronground Rubydesic
> Yes
```

## Obtaining `ICommandSender`

Every class constructed by PicoCLI (autocompletion classes, converter classes, command classes) will have the `ICommandSender` associated with it passed in two different ways.

1. Constructor injection

   ```java
   public class MyCommandOrConverterOrWhatever {
       
       ICommandSender sender;
       
       MyCommandOrConverterOrWhatever(ICommandSender sender) {
           this.sender = sender;
       }
       
   }
   ```

2. Field injection using @Inject annotation

   ```java
   public class MyCommandOrConverterOrWhatever {
       
       @Inject
       ICommandSender sender;
       
   }
   ```

   ---

   *NOTE: The constructor is called BEFORE the field injection is performed*

   ---