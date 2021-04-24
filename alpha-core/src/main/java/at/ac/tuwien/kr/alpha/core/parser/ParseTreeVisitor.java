/**
 * Copyright (c) 2016-2018, the Alpha Team.
 * All rights reserved.
 * <p>
 * Additional changes made by Siemens.
 * <p>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * <p>
 * 1) Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * <p>
 * 2) Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package at.ac.tuwien.kr.alpha.core.parser;

import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;

import at.ac.tuwien.kr.alpha.antlr.ASPCore2BaseVisitor;
import at.ac.tuwien.kr.alpha.antlr.ASPCore2Lexer;
import at.ac.tuwien.kr.alpha.antlr.ASPCore2Parser;
import at.ac.tuwien.kr.alpha.api.AnswerSet;
import at.ac.tuwien.kr.alpha.api.ComparisonOperator;
import at.ac.tuwien.kr.alpha.api.common.fixedinterpretations.PredicateInterpretation;
import at.ac.tuwien.kr.alpha.api.programs.InlineDirectives;
import at.ac.tuwien.kr.alpha.api.programs.Predicate;
import at.ac.tuwien.kr.alpha.api.programs.atoms.AggregateAtom;
import at.ac.tuwien.kr.alpha.api.programs.atoms.Atom;
import at.ac.tuwien.kr.alpha.api.programs.atoms.BasicAtom;
import at.ac.tuwien.kr.alpha.api.programs.atoms.ComparisonAtom;
import at.ac.tuwien.kr.alpha.api.programs.atoms.ExternalAtom;
import at.ac.tuwien.kr.alpha.api.programs.literals.AggregateLiteral;
import at.ac.tuwien.kr.alpha.api.programs.literals.Literal;
import at.ac.tuwien.kr.alpha.api.rules.ChoiceHead;
import at.ac.tuwien.kr.alpha.api.rules.ChoiceHead.ChoiceElement;
import at.ac.tuwien.kr.alpha.api.rules.Head;
import at.ac.tuwien.kr.alpha.api.rules.NormalHead;
import at.ac.tuwien.kr.alpha.api.terms.ArithmeticOperator;
import at.ac.tuwien.kr.alpha.api.terms.ConstantTerm;
import at.ac.tuwien.kr.alpha.api.terms.FunctionTerm;
import at.ac.tuwien.kr.alpha.api.terms.Term;
import at.ac.tuwien.kr.alpha.api.terms.VariableTerm;
import at.ac.tuwien.kr.alpha.commons.AnswerSets;
import at.ac.tuwien.kr.alpha.commons.Predicates;
import at.ac.tuwien.kr.alpha.commons.atoms.Atoms;
import at.ac.tuwien.kr.alpha.commons.comparisons.ComparisonOperators;
import at.ac.tuwien.kr.alpha.commons.literals.Literals;
import at.ac.tuwien.kr.alpha.commons.terms.IntervalTerm;
import at.ac.tuwien.kr.alpha.commons.terms.Terms;
import at.ac.tuwien.kr.alpha.core.programs.InputProgram;
import at.ac.tuwien.kr.alpha.core.rules.BasicRule;
import at.ac.tuwien.kr.alpha.core.rules.heads.ChoiceHeadImpl;
import at.ac.tuwien.kr.alpha.core.rules.heads.ChoiceHeadImpl.ChoiceElementImpl;
import at.ac.tuwien.kr.alpha.core.rules.heads.NormalHeadImpl;

/**
 * Copyright (c) 2016-2018, the Alpha Team.
 */
public class ParseTreeVisitor extends ASPCore2BaseVisitor<Object> {
	private final Map<String, PredicateInterpretation> externals;
	private final boolean acceptVariables;

	private InputProgram.Builder programBuilder;
	private InlineDirectives inlineDirectives;

	public ParseTreeVisitor(Map<String, PredicateInterpretation> externals) {
		this(externals, true);
	}

	public ParseTreeVisitor(Map<String, PredicateInterpretation> externals, boolean acceptVariables) {
		this.externals = externals;
		this.acceptVariables = acceptVariables;
	}

	private UnsupportedOperationException notSupported(RuleContext ctx) {
		return new UnsupportedOperationException("Unsupported syntax encountered: " + ctx.getText());
	}

	/**
	 * Translates a program context (referring to a node in an ATN specific to ANTLR) to the internal representation of Alpha.
	 */
	public InputProgram translate(ASPCore2Parser.ProgramContext input) {
		return visitProgram(input);
	}

	/**
	 * Translates a context for answer sets (referring to a node in an ATN specific to ANTLR) to the representation that Alpha uses.
	 */
	public Set<AnswerSet> translate(ASPCore2Parser.Answer_setsContext input) {
		return visitAnswer_sets(input);
	}

	@Override
	public Set<AnswerSet> visitAnswer_sets(ASPCore2Parser.Answer_setsContext ctx) {
		Set<AnswerSet> result = new TreeSet<>();

		for (ASPCore2Parser.Answer_setContext answerSetContext : ctx.answer_set()) {
			result.add(visitAnswer_set(answerSetContext));
		}

		return result;
	}

	@Override
	public AnswerSet visitAnswer_set(ASPCore2Parser.Answer_setContext ctx) {
		SortedSet<Predicate> predicates = new TreeSet<>();
		Map<Predicate, SortedSet<Atom>> predicateInstances = new TreeMap<>();

		for (ASPCore2Parser.Classical_literalContext classicalLiteralContext : ctx.classical_literal()) {
			Atom atom = visitClassical_literal(classicalLiteralContext);

			predicates.add(atom.getPredicate());
			predicateInstances.compute(atom.getPredicate(), (k, v) -> {
				if (v == null) {
					v = new TreeSet<>();
				}
				v.add(atom);
				return v;
			});

		}

		return AnswerSets.newAnswerSet(predicates, predicateInstances);
	}

	@Override
	public String visitTerminal(TerminalNode node) {
		return node.getText();
	}

	@Override
	public InputProgram visitProgram(ASPCore2Parser.ProgramContext ctx) {
		// program : statements? query?;
		if (ctx.query() != null) {
			throw notSupported(ctx.query());
		}

		if (ctx.statements() == null) {
			return InputProgram.EMPTY;
		}
		inlineDirectives = new InlineDirectivesImpl();
		programBuilder = InputProgram.builder();
		visitStatements(ctx.statements());
		programBuilder.addInlineDirectives(inlineDirectives);
		return programBuilder.build();
	}

	@Override
	public Object visitStatements(ASPCore2Parser.StatementsContext ctx) {
		// statements : statement+;
		for (ASPCore2Parser.StatementContext statementContext : ctx.statement()) {
			visit(statementContext);
		}
		return null;
	}

	@Override
	public Object visitStatement_fact(ASPCore2Parser.Statement_factContext ctx) {
		// head DOT
		Head head = visitHead(ctx.head());
		if (head instanceof NormalHead) {
			programBuilder.addFact(((NormalHead) head).getAtom());
		} else {
			// Treat facts with choice or disjunction in the head like a rule.
			programBuilder.addRule(new BasicRule(head, emptyList()));
		}
		return null;
	}

	@Override
	public Object visitStatement_constraint(ASPCore2Parser.Statement_constraintContext ctx) {
		// CONS body DOT
		programBuilder.addRule(new BasicRule(null, visitBody(ctx.body())));
		return null;
	}

	@Override
	public Object visitStatement_rule(ASPCore2Parser.Statement_ruleContext ctx) {
		// head CONS body DOT
		programBuilder.addRule(new BasicRule(visitHead(ctx.head()), visitBody(ctx.body())));
		return null;
	}

	@Override
	public Object visitStatement_weightConstraint(ASPCore2Parser.Statement_weightConstraintContext ctx) {
		// WCONS body? DOT SQUARE_OPEN weight_at_level SQUARE_CLOSE
		throw notSupported(ctx);
	}

	@Override
	public Object visitStatement_directive(ASPCore2Parser.Statement_directiveContext ctx) {
		// directive
		visitDirective(ctx.directive());
		// Parsed directives are globally stored, nothing to return here.
		return null;
	}

	@Override
	public Head visitDisjunction(ASPCore2Parser.DisjunctionContext ctx) {
		// disjunction : classical_literal (OR disjunction)?;
		if (ctx.disjunction() != null) {
			throw notSupported(ctx);
		}
		return new NormalHeadImpl(visitClassical_literal(ctx.classical_literal()));
	}

	@Override
	public Head visitHead(ASPCore2Parser.HeadContext ctx) {
		// head : disjunction | choice;
		if (ctx.choice() != null) {
			return visitChoice(ctx.choice());
		}
		return visitDisjunction(ctx.disjunction());
	}

	@Override
	public Head visitChoice(ASPCore2Parser.ChoiceContext ctx) {
		// choice : (lt=term lop=binop)? CURLY_OPEN choice_elements? CURLY_CLOSE (uop=binop ut=term)?;
		Term lt = null;
		ComparisonOperator lop = null;
		Term ut = null;
		ComparisonOperator uop = null;
		if (ctx.lt != null) {
			lt = (Term) visit(ctx.lt);
			lop = visitBinop(ctx.lop);
		}
		if (ctx.ut != null) {
			ut = (Term) visit(ctx.ut);
			uop = visitBinop(ctx.uop);
		}
		return new ChoiceHeadImpl(visitChoice_elements(ctx.choice_elements()), lt, lop, ut, uop);
	}

	@Override
	public List<ChoiceElement> visitChoice_elements(ASPCore2Parser.Choice_elementsContext ctx) {
		// choice_elements : choice_element (SEMICOLON choice_elements)?;
		List<ChoiceHead.ChoiceElement> choiceElements;
		if (ctx.choice_elements() != null) {
			choiceElements = visitChoice_elements(ctx.choice_elements());
		} else {
			choiceElements = new LinkedList<>();
		}
		choiceElements.add(0, visitChoice_element(ctx.choice_element()));
		return choiceElements;
	}

	@Override
	public ChoiceHead.ChoiceElement visitChoice_element(ASPCore2Parser.Choice_elementContext ctx) {
		// choice_element : classical_literal (COLON naf_literals?)?;
		BasicAtom atom = (BasicAtom) visitClassical_literal(ctx.classical_literal());
		if (ctx.naf_literals() != null) {
			return new ChoiceElementImpl(atom, visitNaf_literals(ctx.naf_literals()));
		} else {
			return new ChoiceElementImpl(atom, Collections.emptyList());
		}
	}

	@Override
	public List<Literal> visitNaf_literals(ASPCore2Parser.Naf_literalsContext ctx) {
		// naf_literals : naf_literal (COMMA naf_literals)?;
		List<Literal> literals;
		if (ctx.naf_literals() != null) {
			literals = visitNaf_literals(ctx.naf_literals());
		} else {
			literals = new LinkedList<>();
		}
		literals.add(0, visitNaf_literal(ctx.naf_literal()));
		return literals;
	}

	@Override
	public Object visitDirective_enumeration(ASPCore2Parser.Directive_enumerationContext ctx) {
		// directive_enumeration : SHARP 'enum_predicate_is' ID DOT;
		inlineDirectives.addDirective(InlineDirectives.DIRECTIVE.enum_predicate_is, ctx.ID().getText());
		return null;
	}

	@Override
	public List<Literal> visitBody(ASPCore2Parser.BodyContext ctx) {
		// body : ( naf_literal | aggregate ) (COMMA body)?;
		if (ctx == null) {
			return emptyList();
		}

		final List<Literal> literals = new ArrayList<>();
		do {
			if (ctx.naf_literal() != null) {
				literals.add(visitNaf_literal(ctx.naf_literal()));
			} else {
				literals.add(visitAggregate(ctx.aggregate()));
			}
		} while ((ctx = ctx.body()) != null);

		return literals;
	}

	@Override
	public AggregateLiteral visitAggregate(ASPCore2Parser.AggregateContext ctx) {

		// aggregate : NAF? (lt=term lop=binop)? aggregate_function CURLY_OPEN aggregate_elements CURLY_CLOSE (uop=binop ut=term)?;
		boolean isPositive = ctx.NAF() == null;
		Term lt = null;
		ComparisonOperator lop = null;
		Term ut = null;
		ComparisonOperator uop = null;
		if (ctx.lt != null) {
			lt = (Term) visit(ctx.lt);
			lop = visitBinop(ctx.lop);
		}
		if (ctx.ut != null) {
			ut = (Term) visit(ctx.ut);
			uop = visitBinop(ctx.uop);
		}
		AggregateAtom.AggregateFunction aggregateFunction = visitAggregate_function(ctx.aggregate_function());
		List<AggregateAtom.AggregateElement> aggregateElements = visitAggregate_elements(ctx.aggregate_elements());
		return Atoms.newAggregateAtom(lop, lt, uop, ut, aggregateFunction, aggregateElements).toLiteral(isPositive);
	}

	@Override
	public List<AggregateAtom.AggregateElement> visitAggregate_elements(ASPCore2Parser.Aggregate_elementsContext ctx) {
		// aggregate_elements : aggregate_element (SEMICOLON aggregate_elements)?;
		final List<AggregateAtom.AggregateElement> aggregateElements = new ArrayList<>();
		do {
			aggregateElements.add(visitAggregate_element(ctx.aggregate_element()));
		} while ((ctx = ctx.aggregate_elements()) != null);

		return aggregateElements;
	}

	@Override
	public AggregateAtom.AggregateElement visitAggregate_element(ASPCore2Parser.Aggregate_elementContext ctx) {
		// aggregate_element : basic_terms? (COLON naf_literals?)?;
		List<Term> basicTerms = ctx.basic_terms() != null ? visitBasic_terms(ctx.basic_terms()) : null;
		if (ctx.naf_literals() != null) {
			return Atoms.newAggregateElement(basicTerms, visitNaf_literals(ctx.naf_literals()));
		}
		return Atoms.newAggregateElement(basicTerms, Collections.emptyList());
	}

	@Override
	public List<Term> visitBasic_terms(ASPCore2Parser.Basic_termsContext ctx) {
		// basic_terms : basic_term (COMMA basic_terms)? ;
		List<Term> termList = new ArrayList<>();
		do {
			termList.add(visitBasic_term(ctx.basic_term()));
		} while ((ctx = ctx.basic_terms()) != null);
		return termList;
	}

	@Override
	public Term visitBasic_term(ASPCore2Parser.Basic_termContext ctx) {
		// basic_term : ground_term | variable_term;
		if (ctx.ground_term() != null) {
			return visitGround_term(ctx.ground_term());
		} else {
			return visitVariable_term(ctx.variable_term());
		}
	}

	@Override
	public Term visitGround_term(ASPCore2Parser.Ground_termContext ctx) {
		// ground_term : ID | QUOTED_STRING | MINUS? NUMBER;
		if (ctx.ID() != null) {
			return Terms.newSymbolicConstant(ctx.ID().getText());
		} else if (ctx.QUOTED_STRING() != null) {
			String quotedString = ctx.QUOTED_STRING().getText();
			return Terms.newConstant(quotedString.substring(1, quotedString.length() - 1));
		} else {
			int multiplier = 1;
			if (ctx.MINUS() != null) {
				multiplier = -1;
			}
			return Terms.newConstant(multiplier * Integer.parseInt(ctx.NUMBER().getText()));
		}
	}

	@Override
	public Term visitVariable_term(ASPCore2Parser.Variable_termContext ctx) {
		// variable_term : VARIABLE | ANONYMOUS_VARIABLE;
		if (ctx.VARIABLE() != null) {
			return Terms.newVariable(ctx.VARIABLE().getText());
		} else {
			return Terms.newAnonymousVariable();
		}
	}

	@Override
	public AggregateAtom.AggregateFunction visitAggregate_function(ASPCore2Parser.Aggregate_functionContext ctx) {
		// aggregate_function : AGGREGATE_COUNT | AGGREGATE_MAX | AGGREGATE_MIN | AGGREGATE_SUM;
		if (ctx.AGGREGATE_COUNT() != null) {
			return AggregateAtom.AggregateFunction.COUNT;
		} else if (ctx.AGGREGATE_MAX() != null) {
			return AggregateAtom.AggregateFunction.MAX;
		} else if (ctx.AGGREGATE_MIN() != null) {
			return AggregateAtom.AggregateFunction.MIN;
		} else if (ctx.AGGREGATE_SUM() != null) {
			return AggregateAtom.AggregateFunction.SUM;
		} else {
			throw notSupported(ctx);
		}
	}

	@Override
	public ComparisonOperator visitBinop(ASPCore2Parser.BinopContext ctx) {
		// binop : EQUAL | UNEQUAL | LESS | GREATER | LESS_OR_EQ | GREATER_OR_EQ;
		if (ctx.EQUAL() != null) {
			return ComparisonOperators.EQ;
		} else if (ctx.UNEQUAL() != null) {
			return ComparisonOperators.NE;
		} else if (ctx.LESS() != null) {
			return ComparisonOperators.LT;
		} else if (ctx.LESS_OR_EQ() != null) {
			return ComparisonOperators.LE;
		} else if (ctx.GREATER() != null) {
			return ComparisonOperators.GT;
		} else if (ctx.GREATER_OR_EQ() != null) {
			return ComparisonOperators.GE;
		} else {
			throw notSupported(ctx);
		}
	}

	@Override
	public ComparisonAtom visitBuiltin_atom(ASPCore2Parser.Builtin_atomContext ctx) {
		// builtin_atom : term binop term;
		return Atoms.newComparsionAtom(
				(Term) visit(ctx.term(0)),
				(Term) visit(ctx.term(1)),
				visitBinop(ctx.binop()));
	}

	@Override
	public Literal visitNaf_literal(ASPCore2Parser.Naf_literalContext ctx) {
		// naf_literal : NAF? (external_atom | classical_literal | builtin_atom);
		boolean isCurrentLiteralNegated = ctx.NAF() != null;
		if (ctx.builtin_atom() != null) {
			return Literals.fromAtom(visitBuiltin_atom(ctx.builtin_atom()), !isCurrentLiteralNegated);
		} else if (ctx.classical_literal() != null) {
			return Literals.fromAtom(visitClassical_literal(ctx.classical_literal()), !isCurrentLiteralNegated);
		} else if (ctx.external_atom() != null) {
			return Literals.fromAtom(visitExternal_atom(ctx.external_atom()), !isCurrentLiteralNegated);
		}
		throw notSupported(ctx);
	}

	@Override
	public BasicAtom visitClassical_literal(ASPCore2Parser.Classical_literalContext ctx) {
		// classical_literal : MINUS? ID (PAREN_OPEN terms PAREN_CLOSE)?;
		if (ctx.MINUS() != null) {
			throw notSupported(ctx);
		}

		final List<Term> terms = visitTerms(ctx.terms());
		return Atoms.newBasicAtom(Predicates.getPredicate(ctx.ID().getText(), terms.size()), terms);
	}

	@Override
	public List<Term> visitTerms(ASPCore2Parser.TermsContext ctx) {
		// terms : term (COMMA terms)?;
		if (ctx == null) {
			return emptyList();
		}

		final List<Term> terms = new ArrayList<>();
		do {
			ASPCore2Parser.TermContext term = ctx.term();
			terms.add((Term) visit(term));
		} while ((ctx = ctx.terms()) != null);

		return terms;
	}

	@Override
	public ConstantTerm<Integer> visitTerm_number(ASPCore2Parser.Term_numberContext ctx) {
		return Terms.newConstant(Integer.parseInt(ctx.NUMBER().getText()));
	}

	@Override
	public ConstantTerm<String> visitTerm_const(ASPCore2Parser.Term_constContext ctx) {
		return Terms.newSymbolicConstant(ctx.ID().getText());
	}

	@Override
	public ConstantTerm<String> visitTerm_string(ASPCore2Parser.Term_stringContext ctx) {
		String quotedString = ctx.QUOTED_STRING().getText().replace("\\\"", "\"");
		return Terms.newConstant(quotedString.substring(1, quotedString.length() - 1));
	}

	@Override
	public FunctionTerm visitTerm_func(ASPCore2Parser.Term_funcContext ctx) {
		return Terms.newFunctionTerm(ctx.ID().getText(), visitTerms(ctx.terms()));
	}

	@Override
	public VariableTerm visitTerm_anonymousVariable(ASPCore2Parser.Term_anonymousVariableContext ctx) {
		if (!acceptVariables) {
			throw notSupported(ctx);
		}

		return Terms.newAnonymousVariable();
	}

	@Override
	public VariableTerm visitTerm_variable(ASPCore2Parser.Term_variableContext ctx) {
		if (!acceptVariables) {
			throw notSupported(ctx);
		}

		return Terms.newVariable(ctx.VARIABLE().getText());
	}

	@Override
	public Term visitTerm_parenthesisedTerm(ASPCore2Parser.Term_parenthesisedTermContext ctx) {
		return (Term) visit(ctx.term());
	}

	@Override
	public ExternalAtom visitExternal_atom(ASPCore2Parser.External_atomContext ctx) {
		// external_atom : AMPERSAND ID (SQUARE_OPEN input = terms SQUARE_CLOSE)? (PAREN_OPEN output = terms PAREN_CLOSE)?;

		if (ctx.MINUS() != null) {
			throw notSupported(ctx);
		}

		final String predicateName = ctx.ID().getText();
		final PredicateInterpretation interpretation = externals.get(predicateName);

		if (interpretation == null) {
			throw new IllegalArgumentException("Unknown interpretation name encountered: " + predicateName);
		}

		List<Term> outputTerms = visitTerms(ctx.output);

		return Atoms.newExternalAtom(
				Predicates.getPredicate(predicateName, outputTerms.size()),
				interpretation,
				visitTerms(ctx.input),
				outputTerms);
	}

	@Override
	public IntervalTerm visitTerm_interval(ASPCore2Parser.Term_intervalContext ctx) {
		// interval : lower = (NUMBER | VARIABLE) DOT DOT upper = (NUMBER | VARIABLE);
		ASPCore2Parser.IntervalContext ictx = ctx.interval();
		String lowerText = ictx.lower.getText();
		String upperText = ictx.upper.getText();
		Term lower = ictx.lower.getType() == ASPCore2Lexer.NUMBER ? Terms.newConstant(Integer.parseInt(lowerText)) : Terms.newVariable(lowerText);
		Term upper = ictx.upper.getType() == ASPCore2Lexer.NUMBER ? Terms.newConstant(Integer.parseInt(upperText)) : Terms.newVariable(upperText);
		return IntervalTerm.getInstance(lower, upper);
	}

	@Override
	public Object visitTerm_minusArithTerm(ASPCore2Parser.Term_minusArithTermContext ctx) {
		// | MINUS term
		return Terms.newMinusTerm((Term) visit(ctx.term()));
	}

	@Override
	public Object visitTerm_timesdivmodArithTerm(ASPCore2Parser.Term_timesdivmodArithTermContext ctx) {
		// | term (TIMES | DIV | MODULO) term
		ArithmeticOperator op = ctx.TIMES() != null ? ArithmeticOperator.TIMES
				: ctx.DIV() != null ? ArithmeticOperator.DIV : ArithmeticOperator.MODULO;
		return Terms.newArithmeticTerm((Term) visit(ctx.term(0)), op, (Term) visit(ctx.term(1)));
	}

	@Override
	public Object visitTerm_plusminusArithTerm(ASPCore2Parser.Term_plusminusArithTermContext ctx) {
		// | term (PLUS | MINUS) term
		ArithmeticOperator op = ctx.PLUS() != null ? ArithmeticOperator.PLUS : ArithmeticOperator.MINUS;
		return Terms.newArithmeticTerm((Term) visit(ctx.term(0)), op, (Term) visit(ctx.term(1)));
	}

	@Override
	public Object visitTerm_powerArithTerm(ASPCore2Parser.Term_powerArithTermContext ctx) {
		// |<assoc=right> term POWER term
		ArithmeticOperator op = ArithmeticOperator.POWER;
		return Terms.newArithmeticTerm((Term) visit(ctx.term(0)), op, (Term) visit(ctx.term(1)));
	}

	@Override
	public Object visitTerm_bitxorArithTerm(ASPCore2Parser.Term_bitxorArithTermContext ctx) {
		// | term BITXOR term
		return Terms.newArithmeticTerm((Term) visit(ctx.term(0)), ArithmeticOperator.BITXOR, (Term) visit(ctx.term(1)));
	}
}
