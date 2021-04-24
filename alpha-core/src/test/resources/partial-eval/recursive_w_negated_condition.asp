%%%
% Test program intended to verify correct handling of negative basic literals
% in StratifiedEvaluation
%%%
basefact1(1).
basefact2(1).

max_value(10).
min_value(1).

basefact1(9).
basefact2(9).

%% Rule R0
base(X) :- basefact1(X), basefact2(X).

%% Rule R1
inc_value(I) :- base(I), min_value(I).

%% Rule R2
% Since R2 negatively depends on base/1, it is on a higher stratum than
% R0. The component containing R2 needs several evaluation runs since,
% due to it's recursive character, R2 will yield new information while
% X < 8.
inc_value(Y) :- inc_value(X), Y = X + 1, Y < M, max_value(M), not base(Y).

%% The purpose of this program is to highlight whether StratifiedEvaluation
%% correctly checks ALL known instances for a predicate when checking negative
%% literals rather than just the ones derived during the last evaluation run
%% (which is fine for positive literals).
%% In case the check is performed incorrectly, the answer set will contain
%% the atom inc_value(9), which it must not.


