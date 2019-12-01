package ga.rubydesic.minecraftpicocli;

import net.minecraft.command.ICommandSender;
import picocli.CommandLine;

import javax.inject.Inject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

public class CommandFactory implements CommandLine.IFactory {
	private ICommandSender sender;

	CommandFactory(ICommandSender sender) {
		this.sender = sender;
	}

	@Override
	public <K> K create(Class<K> clazz) {
		try {
			// Instantiate the class
			K instance = getInstance(clazz, this.sender);

			// If there are any @Inject annotated ICommandSender fields, inject the instance we have
			for (Field field : clazz.getDeclaredFields())  {
				if (ICommandSender.class.isAssignableFrom(field.getType()) &&
						field.getAnnotation(Inject.class) != null) {
					field.setAccessible(true);
					field.set(instance, this.sender);
				}
			}

			return instance;
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException e)   {
			throw new IllegalStateException(
					String.format("Unable to initialize %s!", clazz.getCanonicalName()), e);
		}
	}

	/**
	 * Instantiates the given class with ICommandSender or with a no-args constructor and then returns it
	 */
	private static <K> K getInstance(Class<K> clazz, ICommandSender sender) throws IllegalAccessException,
			InvocationTargetException, InstantiationException, IllegalArgumentException {

		try {
			Constructor<K> constructor = clazz.getDeclaredConstructor(ICommandSender.class);
			constructor.setAccessible(true);
			return constructor.newInstance(sender);
		} catch (NoSuchMethodException ignored) {}

		try {
			Constructor<K> constructor = clazz.getDeclaredConstructor();
			constructor.setAccessible(true);
			return constructor.newInstance();
		} catch (NoSuchMethodException ignored) {}

		throw new IllegalArgumentException("The supplied class did not have a no-args or " +
				"ICommandSender.class constructor");
	}
}

