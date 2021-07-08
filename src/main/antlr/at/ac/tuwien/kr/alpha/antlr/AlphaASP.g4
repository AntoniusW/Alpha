grammar AlphaASP;

import ASPLexer, ASPCore2;

/* The ASP-Core-2 grammar is defined in ASPCore2.g4.
 * Here, it is extended a bit to parse widespread syntax (e.g. used by gringo/clasp).
 * It is also extended by heuristic directives for Alpha.
 */

program : statements? query? EOF;

statements : statement+;

statement : head DOT                     # statement_fact
          | CONS body DOT                # statement_constraint
          | head CONS body DOT           # statement_rule
          | WCONS body? DOT weight_annotation        # statement_weightConstraint
          | directive                    # statement_directive;   // NOT Core2 syntax.

naf_literal : NAF? atom;

atom : (external_atom | classical_literal | builtin_atom);

external_atom : MINUS? AMPERSAND ID (SQUARE_OPEN input = terms SQUARE_CLOSE)? (PAREN_OPEN output = terms PAREN_CLOSE)?; // NOT Core2 syntax.

term : ID                                   # term_const
     | ID (PAREN_OPEN terms? PAREN_CLOSE)   # term_func
     | NUMBER                               # term_number
     | QUOTED_STRING                        # term_string
     | variable                             # term_variable
     | ANONYMOUS_VARIABLE                   # term_anonymousVariable
     | PAREN_OPEN term PAREN_CLOSE          # term_parenthesisedTerm
     | interval                             # term_interval // Syntax extension.
     | MINUS term                           # term_minusArithTerm
     |<assoc=right> term POWER term         # term_powerArithTerm
     | term (TIMES | DIV | MODULO) term     # term_timesdivmodArithTerm
     | term (PLUS | MINUS) term             # term_plusminusArithTerm
     | term BITXOR term                     # term_bitxorArithTerm
     ;

interval : (lowerNum=NUMBER | lowerVar=variable) DOT DOT (upperNum=NUMBER | upperVar=variable); // NOT Core2 syntax, but widespread

directive : directive_enumeration | directive_heuristic;  // NOT Core2 syntax, allows solver specific directives. Further directives shall be added here.

directive_enumeration : SHARP 'enumeration_predicate_is' ID DOT;  // NOT Core2 syntax, used for aggregate translation.

directive_heuristic : SHARP 'heuristic' heuristic_head_atom (heuristic_body)? DOT heuristic_weight_annotation?;

heuristic_head_atom : heuristic_head_sign? basic_atom;

heuristic_head_sign : HEU_SIGN_T | HEU_SIGN_F;

heuristic_body : COLON heuristic_body_literal (COMMA heuristic_body_literal)*;

heuristic_body_literal : NAF? heuristic_body_atom;

heuristic_body_atom : (heuristic_body_sign? basic_atom) | builtin_atom | external_atom;

heuristic_body_sign : (HEU_SIGN_T | HEU_SIGN_M | HEU_SIGN_F | HEU_BODY_SIGN)+; // single-char signs have their own classes in the lexer

heuristic_weight_annotation : SQUARE_OPEN heuristic_weight_at_level SQUARE_CLOSE;

heuristic_weight_at_level : term (AT term)?;

variable : HEU_SIGN_T | HEU_SIGN_M | HEU_SIGN_F | HEU_BODY_SIGN | VARIABLE; // to be able to treat heuristic sign keywords as variable identifiers
