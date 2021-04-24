% instance data to "help" alpha"
:- not rack(1).
:- not frame(2).
:- not frame(3).
:- not frame(4).
:- not frame(5).
:- not elementa(6).
%:- not null(7).
%:- not null(8).
%:- not null(9).
%:- not null(10).
%

n(1).n(2).n(3).n(4).n(5).n(6).n(7).n(8).n(9).n(10).
%n(11).n(12).n(13).n(14).n(15).n(16).n(17).n(18).n(19).n(20).
%n(21).n(22).n(23).n(24).n(25).n(26).n(27).n(28).n(29).n(30).
%n(31).n(32).n(33).n(34).n(35).n(36).n(37).n(38).n(39).n(40).
%n(41).n(42).n(43).n(44).n(45).n(46).n(47).n(48).n(49).n(50).
%n(51).n(52).n(53).n(54).n(55).n(56).n(57).n(58).n(49).n(60).
%n(61).n(62).n(63).n(64).n(65).n(66).n(67).n(68).n(59).n(70).
%n(71).n(72).n(73).n(74).n(75).n(76).n(77).n(78).n(69).n(80).
%n(81).n(82).n(83).n(84).n(85).n(86).n(87).n(88).n(79).n(90).
%n(91).n(92).n(93).n(94).n(95).n(96).n(97).n(98).n(89).n(100).

null(ID) :- n(ID), not rack(ID), not frame(ID), not module(ID), not element(ID).
rack(ID) :- n(ID), not null(ID), not frame(ID), not module(ID), not element(ID).
frame(ID) :- n(ID), not null(ID), not rack(ID), not module(ID), not element(ID).
module(ID) :- n(ID), not null(ID), not rack(ID), not frame(ID), not element(ID).
element(ID) :- n(ID), not null(ID), not rack(ID), not frame(ID), not module(ID).

% subclasses
elementa(ID) :- element(ID), not elementb(ID), not elementc(ID), not elementd(ID).
elementb(ID) :- element(ID), not elementa(ID), not elementc(ID), not elementd(ID).
elementc(ID) :- element(ID), not elementa(ID), not elementb(ID), not elementd(ID).
elementd(ID) :- element(ID), not elementa(ID), not elementb(ID), not elementc(ID).

moduleI(ID) :- module(ID), not moduleII(ID), not moduleIII(ID), not moduleIV(ID), not moduleV(ID).
moduleII(ID) :- module(ID), not moduleI(ID), not moduleIII(ID), not moduleIV(ID), not moduleV(ID).
moduleIII(ID) :- module(ID), not moduleI(ID), not moduleII(ID),  not moduleIV(ID), not moduleV(ID).
moduleIV(ID) :- module(ID), not moduleI(ID), not moduleII(ID), not moduleIII(ID), not moduleV(ID).
moduleV(ID) :- module(ID), not moduleI(ID), not moduleII(ID), not moduleIII(ID), not moduleIV(ID).

racksingle(ID) :- rack(ID), not rackdouble(ID).
rackdouble(ID) :- rack(ID), not racksingle(ID).

% inrack(F,R) frame F is in rack R
inrack(F,R) :- frame(F), rack(R), not notinrack(F,R).
notinrack(F,R) :- frame(F), rack(R), not inrack(F,R).
:- inrack(F,R1), inrack(F,R2), R1!=R2.
hasrack(F) :- inrack(F,R).
:- frame(F), not hasrack(F).
hasframe(R) :- inrack(F,R).
:- rack(R), not hasframe(R).

% rackslot(RACK,SLOT,FRAME) SLOT = 1..8
rackslot(R,1,F) :- inrack(F,R), not rackslot(R,2,F), not rackslot(R,3,F), not rackslot(R,4,F), not rackslot(R,5,F), not rackslot(R,6,F), not rackslot(R,7,F), not rackslot(R,8,F).
rackslot(R,2,F) :- inrack(F,R), not rackslot(R,1,F), not rackslot(R,3,F), not rackslot(R,4,F), not rackslot(R,5,F), not rackslot(R,6,F), not rackslot(R,7,F), not rackslot(R,8,F).
rackslot(R,3,F) :- inrack(F,R), not rackslot(R,1,F), not rackslot(R,2,F), not rackslot(R,4,F), not rackslot(R,5,F), not rackslot(R,6,F), not rackslot(R,7,F), not rackslot(R,8,F).
rackslot(R,4,F) :- inrack(F,R), not rackslot(R,1,F), not rackslot(R,2,F), not rackslot(R,3,F), not rackslot(R,5,F), not rackslot(R,6,F), not rackslot(R,7,F), not rackslot(R,8,F).
rackslot(R,5,F) :- inrack(F,R), not rackslot(R,1,F), not rackslot(R,2,F), not rackslot(R,3,F), not rackslot(R,4,F), not rackslot(R,6,F), not rackslot(R,7,F), not rackslot(R,8,F).
rackslot(R,6,F) :- inrack(F,R), not rackslot(R,1,F), not rackslot(R,2,F), not rackslot(R,3,F), not rackslot(R,4,F), not rackslot(R,5,F), not rackslot(R,7,F), not rackslot(R,8,F).
rackslot(R,7,F) :- inrack(F,R), not rackslot(R,1,F), not rackslot(R,2,F), not rackslot(R,3,F), not rackslot(R,4,F), not rackslot(R,5,F), not rackslot(R,6,F), not rackslot(R,8,F).
rackslot(R,8,F) :- inrack(F,R), not rackslot(R,1,F), not rackslot(R,2,F), not rackslot(R,3,F), not rackslot(R,4,F), not rackslot(R,5,F), not rackslot(R,6,F), not rackslot(R,7,F).
:- rackslot(R,S,F1), rackslot(R,S,F2), F1!=F2.
usedrackslot(RACK,SLOT) :- rackslot(RACK,SLOT,FRAME). 

% a single rack uses 4 slots, a double rack uses 8 slots
:- rack(R), not usedrackslot(R,1).
:- rack(R), not usedrackslot(R,2).
:- rack(R), not usedrackslot(R,3).
:- rack(R), not usedrackslot(R,4).
:- racksingle(R), usedrackslot(R,5).
:- racksingle(R), usedrackslot(R,6).
:- racksingle(R), usedrackslot(R,7).
:- racksingle(R), usedrackslot(R,8).
:- rackdouble(R), not usedrackslot(R,5).
:- rackdouble(R), not usedrackslot(R,6).
:- rackdouble(R), not usedrackslot(R,7).
:- rackdouble(R), not usedrackslot(R,8).

% inframe(MODULE,FRAME) module M is in frame F
inframe(M,F) :- module(M), frame(F), not notinframe(M,F).
notinframe(M,F) :- module(M), frame(F), not inframe(M,F).
:- inframe(M,F1), inframe(M,F2), F1!=F2.
hasframe(M) :- inframe(M,F).
:- module(M), not hasframe(M).

% frameslot(FRAME,SLOT,M) SLOT = 1..6
frameslot(F,1,M) :- inframe(M,F), not frameslot(F,2,M), not frameslot(F,3,M), not frameslot(F,4,M), not frameslot(F,5,M) ,not frameslot(F,6,M).
frameslot(F,2,M) :- inframe(M,F), not frameslot(F,1,M), not frameslot(F,3,M), not frameslot(F,4,M), not frameslot(F,5,M) ,not frameslot(F,6,M).
frameslot(F,3,M) :- inframe(M,F), not frameslot(F,1,M), not frameslot(F,2,M), not frameslot(F,4,M), not frameslot(F,5,M) ,not frameslot(F,6,M).
frameslot(F,4,M) :- inframe(M,F), not frameslot(F,1,M), not frameslot(F,2,M), not frameslot(F,3,M), not frameslot(F,5,M) ,not frameslot(F,6,M).
frameslot(F,5,M) :- inframe(M,F), not frameslot(F,1,M), not frameslot(F,2,M), not frameslot(F,3,M), not frameslot(F,4,M), not frameslot(F,6,M).
frameslot(F,6,M) :- inframe(M,F), not frameslot(F,1,M), not frameslot(F,2,M), not frameslot(F,3,M), not frameslot(F,4,M), not frameslot(F,5,M).
:- frameslot(F,S,M1), frameslot(F,S,M2), M1!=M2.

% all modules of the same element are in the same frame
:- inframe(M1,F1), inframe(M2,F2), belongsto(M1,E), belongsto(M2,E), F1!=F2.

% belongsto(M,E) module M belongsto element E
belongsto(M,E) :- module(M), element(E), not notbelongsto(M,E).
notbelongsto(M,E) :- module(M), element(E), not belongsto(M,E).
:- belongsto(M,E1), belongsto(M,E2), E1 != E2.
haselement(M) :- belongsto(M,E).
:- moduleI(M), not haselement(M).
:- moduleII(M), not haselement(M).
:- moduleIII(M), not haselement(M).
:- moduleIV(M), not haselement(M).
hasmodule(E) :- belongsto(M,E).
:- element(E), not hasmodule(E).

% types of elements
:- belongsto(M,E), moduleI(M), not elementa(E).
:- belongsto(M,E), moduleII(M), not elementb(E).
:- belongsto(M,E), moduleIII(M), not elementc(E).
:- belongsto(M,E), moduleIV(M), not elementd(E).
:- belongsto(M,E), not moduleI(M), elementa(E).
:- belongsto(M,E), not moduleII(M), elementb(E).
:- belongsto(M,E), not moduleIII(M), elementc(E).
:- belongsto(M,E), not moduleIV(M), elementd(E).

% elementslot(ELEMENT,SLOT,MODULE) 1..4
elementslot(E,1,M) :- belongsto(M,E), not elementslot(E,2,M), not elementslot(E,3,M), not elementslot(E,4,M).
elementslot(E,2,M) :- belongsto(M,E), not elementslot(E,1,M), not elementslot(E,3,M), not elementslot(E,4,M).
elementslot(E,3,M) :- belongsto(M,E), not elementslot(E,1,M), not elementslot(E,2,M), not elementslot(E,4,M).
elementslot(E,4,M) :- belongsto(M,E), not elementslot(E,1,M), not elementslot(E,2,M), not elementslot(E,3,M).
:- elementslot(E,S,M1), elementslot(E,S,M2), M1!=M2.
usedelementslot(E,S) :- elementslot(E,S,M).
% moduleI
:- moduleI(M), belongsto(M,E), not usedelementslot(E,1).
:- moduleI(M), belongsto(M,E), usedelementslot(E,2).
:- moduleI(M), belongsto(M,E), usedelementslot(E,3).
:- moduleI(M), belongsto(M,E), usedelementslot(E,4).
% moduleII
:- moduleII(M), belongsto(M,E), not usedelementslot(E,1).
:- moduleII(M), belongsto(M,E), not usedelementslot(E,2).
:- moduleII(M), belongsto(M,E), usedelementslot(E,3).
:- moduleII(M), belongsto(M,E), usedelementslot(E,4).
% moduleIII
:- moduleIII(M), belongsto(M,E), not usedelementslot(E,1).
:- moduleIII(M), belongsto(M,E), not usedelementslot(E,2).
:- moduleIII(M), belongsto(M,E), not usedelementslot(E,3).
:- moduleIII(M), belongsto(M,E), usedelementslot(E,4).
% moduleIV
:- moduleIV(M), belongsto(M,E), not usedelementslot(E,1).
:- moduleIV(M), belongsto(M,E), not usedelementslot(E,2).
:- moduleIV(M), belongsto(M,E), not usedelementslot(E,3).
:- moduleIV(M), belongsto(M,E), not usedelementslot(E,4).


