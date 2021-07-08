package at.ac.tuwien.kr.alpha.grounder.transformation.aggregates.encoders;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;

import at.ac.tuwien.kr.alpha.Util;
import at.ac.tuwien.kr.alpha.common.ComparisonOperator;
import at.ac.tuwien.kr.alpha.common.atoms.AggregateAtom.AggregateFunctionSymbol;

public final class CountEncoder extends StringtemplateBasedAggregateEncoder {

	private static final STGroup AGGREGATE_ENCODINGS = Util.loadStringTemplateGroup(
			SumEncoder.class.getResource("/stringtemplates/aggregate-encodings.stg"));

	private static final ST CNT_LE_SORTING_GRID_TEMPLATE = AGGREGATE_ENCODINGS.getInstanceOf("cnt_le_sorting_grid");
	private static final ST CNT_EQ_TEMPLATE = AGGREGATE_ENCODINGS.getInstanceOf("cnt_eq");
	private static final ST CNT_LE_COUNTING_GRID_TEMPLATE = AGGREGATE_ENCODINGS.getInstanceOf("cnt_le_counting_grid");

	private CountEncoder(ComparisonOperator acceptedOperator, ST encodingTemplate) {
		super(AggregateFunctionSymbol.COUNT, acceptedOperator, encodingTemplate);
	}

	public static CountEncoder buildCountLessOrEqualEncoder(boolean useSortingGrid) {
		return new CountEncoder(ComparisonOperator.LE, useSortingGrid ? CNT_LE_SORTING_GRID_TEMPLATE : CNT_LE_COUNTING_GRID_TEMPLATE);
	}

	public static CountEncoder buildCountEqualsEncoder() {
		return new CountEncoder(ComparisonOperator.EQ, CNT_EQ_TEMPLATE);
	}

}
