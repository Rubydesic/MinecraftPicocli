package ga.rubydesic.minecraftpicocli;

import com.google.common.collect.ImmutableList;
import ga.rubydesic.minecraftpicocli.converter.ConverterWorld;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import org.apache.commons.io.output.NullOutputStream;
import picocli.AutoComplete;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.Model.CommandSpec;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.PrintWriter;
import java.util.*;


/**
 * This class can be registered as a standard Minecraft command. It's used to wrap a PicoCLI command class. For example,
 * consider you have the PicoCLI @Command-annotated class <code>MyCommand</code>
 *
 * You can register it using
 *
 * @param <K>
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PicocliCommandBase<K> extends CommandBase {

	private Class<K> cmdClass;
	private List<String> aliases;
	private String name;
	/**
	 * Whether or not the commands should be configured with some preset defaults that are generally
	 * useful for Minecraft mods. See {@link #configureCommandSpec(CommandSpec)}
	 */
	private boolean defaultConfigOptions;
	/**
	 * These are ITypeConverters that do not need to be instantiated multiple times and are
	 * singletons or effectively static.
	 */
	// This has to be a raw type, but we can at least guarantee that Class and
	// ITypeConverter have the same generic type
	@SuppressWarnings("rawtypes")
	private static Map<Class, ITypeConverter> pureConverters = new HashMap<Class, ITypeConverter>() {{
		put(World.class, new ConverterWorld());
	}};

	public static <T> void registerPureConverter(Class<T> forClass, ITypeConverter<T> converter) {
		pureConverters.put(forClass, converter);
	}

	public PicocliCommandBase(Class<K> cmdClass) {
		this(cmdClass, true);
	}

	/**
	 * @param cmdClass The PicoCLI class to wrap
	 * @param defaultConfigSettings Whether or not to use some sensible default settings tailored for Minecraft mods
	 *                              rather than console applications
	 */
	public PicocliCommandBase(Class<K> cmdClass, boolean defaultConfigSettings) {
		this.defaultConfigOptions = defaultConfigSettings;
		if (cmdClass.getAnnotation(Command.class) == null) {
			throw new IllegalArgumentException("The class must have the PicoCLI @Command annotation!");
		}

		this.cmdClass = cmdClass;
		this.name = this.cmdClass.getAnnotation(Command.class).name();
		this.aliases = Arrays.asList(this.cmdClass.getAnnotation(Command.class).aliases());
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public List<String> getAliases() {
		return aliases;
	}

	@Override
	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender,
										  final String[] splitArgs, @Nullable BlockPos targetPos) {
		CommandFactory factory = new CommandFactory(sender);
		// We don't need a full CommandLine, just the CommandSpec since all we want is tab-completions
		CommandSpec spec = CommandSpec.forAnnotatedObject(factory.create(cmdClass), factory);

		configureCommandSpec(spec);

		// Make the args into a readable form for PicoCLI
		String[] args = CommandUtil.toTabCompleteArgs(splitArgs);
		List<CharSequence> candidates = new LinkedList<>();

		// Add completion candidates to the list
		AutoComplete.complete(spec, args, args.length - 1,
				args[args.length - 1].length(), 500, candidates);

		return candidates.stream()
				.distinct() // sometimes returns multiple of the same completion candidate
				.map(CharSequence::toString) // Convert CharSequences to string
				// If we have /test hel<tab> the completion candidate will be 'lo', but we want 'hello'
				.map(s -> args[args.length - 1] + s)
				.collect(ImmutableList.toImmutableList());
	}

	@Override
	public String getUsage(ICommandSender sender) {
		CommandFactory factory = new CommandFactory(sender);
		CommandLine commandLine = new CommandLine(factory.create(cmdClass), factory);

		configureCommandSpec(commandLine);

		return commandLine.getUsageMessage();
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
		CommandFactory factory = new CommandFactory(sender);

		CommandLine commandLine = new CommandLine(factory.create(cmdClass), factory);
		pureConverters.forEach(commandLine::registerConverter);

		ChatWriter chatOut = new ChatWriter(sender);
		commandLine.setOut(chatOut);
		commandLine.setErr(chatOut);

		configureCommandSpec(commandLine);

		args = CommandUtil.toProperArgs(args);
		commandLine.execute(args);
	}


	private void configureCommandSpec(CommandLine line) {
		configureCommandSpec(line.getCommandSpec());
	}

	/**
	 * Adds some defaults to the supplied {@link CommandSpec} which are useful for Minecraft commands
	 * if {@link #defaultConfigOptions} is true
	 */
	private void configureCommandSpec(CommandSpec spec) {
		if (defaultConfigOptions) {
			spec.usageMessage().width(55);
			spec.mixinStandardHelpOptions(true);
		}
	}

	/**
	 * This class rewrites calls to print and such to Minecraft chat - used because PicoCLI
	 * expects to be writing to console
	 */
	private static class ChatWriter extends PrintWriter {

		ICommandSender sender;

		ChatWriter(ICommandSender sender) {
			super(new NullOutputStream());
			this.sender = sender;
		}

		@Override
		public void print(Object object) {
			this.print(object.toString());
		}

		@Override
		public void print(String string) {
			string = string.replace("\r", "");
			sender.sendMessage(new TextComponentString(string));
		}

		@Override
		public void println(String string) {
			this.print(string);
		}

	}

}
