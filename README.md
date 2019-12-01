# MinecraftPicocli

This library allows you to use the [PicoCLI](https://picocli.info/) command framework with [Minecraft Forge](https://files.minecraftforge.net/).



## Getting Started

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
        sender.sendMessage(new TextChatComponent("You said " + echo));
    }
    
}
```

Now, simply register your command as you would any other using the `FMLServerStartingEvent`

```java
@EventHandler
public void onServerStart(FMLServerStartingEvent event) {
    ServerCommandManager manager = 
        (ServerCommandManager) event.getServer().getcommandManager();
    
    manager.registerCommand(new PicocliCommandBase(MyCommand.class));
}
```

Try out your command:

```
/command "hello there, buddy"
> You said hello there buddy
```





