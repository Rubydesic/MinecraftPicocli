package ga.rubydesic.minecraftpicocli.autocomplete;

import com.google.common.collect.ImmutableList;
import net.minecraftforge.common.DimensionManager;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Iterator;

public class AutocompleterWorld implements Iterable<String> {

	ImmutableList<String> worldNames;

	AutocompleterWorld() {
		worldNames = Arrays.stream(DimensionManager.getWorlds())
				.map(world -> world.provider.getDimensionType().getName())
				.collect(ImmutableList.toImmutableList());
	}

	@Nonnull
	@Override
	public Iterator<String> iterator() {
		return worldNames.iterator();
	}

}
