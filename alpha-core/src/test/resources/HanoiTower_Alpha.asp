% HanoiTower
% Source: ASP Competition 2013 Official (disjunction shifted by ASP Shifter)
% ADAPTIONS FOR ALPHA:
% - replaced "TP1 = T + 1" by "succ(T,TP1)" because arithmetics are not supported
% - replaced "TM1 = T - 1" by "succ(TM1,T)" for the same reason

peg(1).
peg(2).
peg(3).
peg(4).

% Read in data 
on(0, N1, N) :- on0(N, N1).
onG(K, N1, N) :- ongoal(N, N1), steps(K).

% Specify valid arrangements of disks
% Basic condition. Smaller disks are on larger ones
:- time(T), on(T, N1, N), N1 >= N.

% Specify a valid move (only for T<t)
% pick a disk to move
move(T, N) :- disk(N), steps(K), time(T), T < K, not noMove(T, N).
noMove(T, N) :- disk(N), steps(K), time(T), T < K, not move(T, N).
:- move(T, N1), move(T, N2), N1 != N2.
:- time(T), steps(K), T < K, not diskMoved(T).
diskMoved(T) :- move(T, Fv1).

% pick a disk onto which to move
where(T, N) :- disk(N), steps(K), time(T), T < K, not noWhere(T, N).
noWhere(T, N) :- disk(N), steps(K), time(T), T < K, not where(T, N).
:- where(T, N1), where(T, N2), N1 != N2.
:- time(T), steps(K), T < K, not diskWhere(T).
diskWhere(T) :- where(T, Fv1).

% pegs cannot be moved
:- move(T, N), peg(N).

% only top disk can be moved
:- on(T, N, N1), move(T, N).

% a disk can be placed on top only.
:- on(T, N, N1), where(T, N).

% no disk is moved in two consecutive moves
:- move(T, N), move(TM1, N), succ(TM1,T).

% Specify effects of a move
on(TP1, N1, N) :- move(T, N), where(T, N1), succ(T,TP1).
on(TP1, N, N1) :- steps(K), time(T), T < K, on(T, N, N1), not move(T, N1), succ(T,TP1).

% Goal description
:- not on(K, N, N1), onG(K, N, N1), steps(K).
:- on(K, N, N1), not onG(K, N, N1), steps(K).

% Solution
put(T, M, N) :- move(T, N), where(T, M), steps(K), T < K.

% ADDITIONS FOR ALPHA:
succ(0,1) :- time(0).
succ(1,2) :- time(1).
succ(2,3) :- time(2).
succ(3,4) :- time(3).
succ(4,5) :- time(4).
succ(5,6) :- time(5).
succ(6,7) :- time(6).
succ(7,8) :- time(7).
succ(8,9) :- time(8).
succ(9,10) :- time(9).
succ(10,11) :- time(10).
succ(11,12) :- time(11).
succ(12,13) :- time(12).
succ(13,14) :- time(13).
succ(14,15) :- time(14).
succ(15,16) :- time(15).
succ(16,17) :- time(16).
succ(17,18) :- time(17).
succ(18,19) :- time(18).
succ(19,20) :- time(19).
succ(20,21) :- time(20).
succ(21,22) :- time(21).
succ(22,23) :- time(22).
succ(23,24) :- time(23).
succ(24,25) :- time(24).
succ(25,26) :- time(25).
succ(26,27) :- time(26).
succ(27,28) :- time(27).
succ(28,29) :- time(28).
succ(29,30) :- time(29).
succ(30,31) :- time(30).
succ(31,32) :- time(31).
succ(32,33) :- time(32).
succ(33,34) :- time(33).
succ(34,35) :- time(34).
succ(35,36) :- time(35).
succ(36,37) :- time(36).
succ(37,38) :- time(37).
succ(38,39) :- time(38).
succ(39,40) :- time(39).