package at.ac.tuwien.kr.alpha.api.programs.analysis;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ComponentGraph {

	List<SCComponent> getComponents();

	List<SCComponent> getEntryPoints();

	interface SCComponent {

		Map<Integer, Boolean> getDependencyIds();

		Set<Integer> getDependentIds();

		boolean hasNegativeCycle();

		List<DependencyGraph.Node> getNodes();

		int getId();

	}

}
